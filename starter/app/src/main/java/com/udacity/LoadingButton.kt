package com.udacity

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import android.view.animation.AccelerateInterpolator
import android.view.animation.LinearInterpolator
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.res.ResourcesCompat
import androidx.core.content.withStyledAttributes
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.coroutineScope
import androidx.lifecycle.findViewTreeLifecycleOwner
import java.util.Random
import kotlin.math.absoluteValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoadingButton @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr), View.OnClickListener, LifecycleObserver {
    private var bgColor = ResourcesCompat.getColor(
        resources, R.color.design_default_color_background, null
    )
    private val fillUnitDrawable =
        AppCompatResources.getDrawable(context, R.drawable.ic_solid_square)
    private var fillUnitLayoutParams =
        fillUnitDrawable?.let { LayoutParams(it.minimumWidth, it.minimumHeight) }
    private val fillUnitSize = fillUnitDrawable?.minimumHeight ?: 0
    private lateinit var lifecycleScope: LifecycleCoroutineScope
    private val parentAsViewGroup by lazy { parent as ViewGroup }
    private var paddingHorizontal = DEFAULT_INSET
    private var paddingVertical = DEFAULT_INSET
    private val paintBorder = Paint().apply {
        color = Color.BLACK
        isAntiAlias = true
        strokeWidth = 2f
        style = Paint.Style.STROKE
    }
    private val paintText = Paint().apply {
        color = DEFAULT_TEXT_COLOR
        isAntiAlias = true
        textSize = DEFAULT_TEXT_SIZE_PX
    }
    private val paintTextAnimation = Paint().apply {
        color = DEFAULT_TEXT_COLOR
        isAntiAlias = true
        textSize = DEFAULT_TEXT_ANIM_SIZE_PX
    }
    private val internalHeight =
        (FILL_HEIGHT + paintTextAnimation.textSize + (3 * paddingVertical)).toInt()
    private lateinit var progress: Progress
    private var progressState = ProgressState.IDLE
    private val randomizer: Random = Random().apply { setSeed(System.currentTimeMillis()) }
    private var text = context.getText(R.string.default_loading_button_text).toString()
    private var textAnimation =
        context.getText(R.string.default_loading_button_animation_text).toString()
    private var textAnimation2 =
        context.getText(R.string.default_loading_button_animation_text2).toString()
    private var onClickListener: OnClickListener = this
    private lateinit var offCanvas: Canvas
    private lateinit var offBitmap: Bitmap

    init {
        context.withStyledAttributes(attrs, R.styleable.LoadingButton) {
            getString(R.styleable.LoadingButton_text)?.let { text = it }
            getString(R.styleable.LoadingButton_textAnimation)?.let { textAnimation = it }
            getString(R.styleable.LoadingButton_textAnimation2)?.let { textAnimation2 = it }
            paintText.apply {
                textSize = getDimension(R.styleable.LoadingButton_textSize, DEFAULT_TEXT_SIZE_PX)
                color = getColor(R.styleable.LoadingButton_textColor, DEFAULT_TEXT_COLOR)
            }
            paintTextAnimation.apply {
                color = getColor(R.styleable.LoadingButton_textColor, DEFAULT_TEXT_COLOR)
            }
            bgColor = getColor(R.styleable.LoadingButton_backgroundColor, bgColor)
        }
        setOnClickListener(this)
    }

    private fun absoluteCenter(start: Float, finish: Float) = ((start - finish) / 2f).absoluteValue

    private fun absoluteLength(start: Float, finish: Float): Float = (start - finish).absoluteValue

    private fun addUnitOfProgress(): AppCompatImageView {
        val progressUnit = AppCompatImageView(context)
        progressUnit.setImageDrawable(fillUnitDrawable)
        progressUnit.layoutParams = fillUnitLayoutParams
        parentAsViewGroup.addView(progressUnit)
        return progressUnit
    }

    private fun animateProgress() {
        lifecycleScope.launch {
            progress.play {
                invalidate()
                delay(10)
            }
            progressState = ProgressState.FINISHED
            invalidate()
        }
    }

    private fun animateShower() =
        lifecycleScope.launch {
            while (progressState == ProgressState.STARTING) {
                for (i in 0..BURST_COUNT) {
                    animateSingle()
                }
                if (progressState == ProgressState.STARTING) {
                    delay(100)
                }
            }
        }

    private fun animateSingle() {
        val progressUnit = addUnitOfProgress()
        val randomY = randomizer.nextFloat() * (FILL_HEIGHT - (fillUnitSize / 2))
        progressUnit.translationY = y + progress.top + randomY
        progressUnit.translationX = x + progress.right - (fillUnitSize / 2)
        val mover = ObjectAnimator.ofFloat(
            progressUnit,
            TRANSLATION_X,
            progressUnit.translationX,
            x + progress.left
        )
        mover.interpolator = AccelerateInterpolator(1f)
        AnimatorSet().apply {
            play(mover)
            duration = (randomizer.nextFloat() * 500 + 250).toLong()
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    parentAsViewGroup.removeView(progressUnit)
                }
            })
        }.start()
    }

    private fun drawBackground(canvas: Canvas) {
        canvas.drawColor(bgColor)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paintBorder)
    }
    private fun drawProgress(canvas: Canvas) {
        progress.drawFill(canvas)
        progress.drawBorder(canvas)
    }

    //TODO combine
    private fun drawLabel(canvas: Canvas) {
        val labelBounds = Rect()
        if (progressState == ProgressState.IDLE) {
            paintText.getTextBounds(text, 0, text.length, labelBounds)
            val textX = absoluteCenter(
                width.toFloat(),
                absoluteLength(labelBounds.left.toFloat(), labelBounds.right.toFloat())
            )
            val textHeight = paintText.textHeight(text)
            val textY = absoluteCenter(height.toFloat(), textHeight) + textHeight
            canvas.drawText(text, textX, textY, paintText)
        }
        if (progressState == ProgressState.STARTING || progressState == ProgressState.WORKING) {
            paintTextAnimation.getTextBounds(textAnimation, 0, textAnimation.length, labelBounds)
            val textX = absoluteCenter(
                width.toFloat(),
                absoluteLength(labelBounds.left.toFloat(), labelBounds.right.toFloat())
            )
            val textHeight = paintTextAnimation.textHeight(textAnimation)
            val textY = FILL_HEIGHT + (2f * paddingVertical) + textHeight
            canvas.drawText(textAnimation, textX, textY, paintTextAnimation)
        }
    }

    private fun onAnimate() {
        progressState = ProgressState.STARTING
        animateShower()
        animateProgress()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        layoutParams.height = internalHeight
        parentAsViewGroup.findViewTreeLifecycleOwner()?.let {
            lifecycleScope = it.lifecycle.coroutineScope
            it.lifecycle.addObserver(this)
        }
    }

    override fun onClick(view: View?) {
        if (onClickListener != this) {
            onClickListener.onClick(view)
        }
        lifecycleScope.launch {
            onAnimate()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawBitmap(offBitmap, 0f, 0f, null)
        drawLabel(canvas)
        if (progressState != ProgressState.IDLE) {
            drawProgress(canvas)
        }
    }

    override fun onSizeChanged(width: Int, height: Int, oldWidth: Int, oldHeight: Int) {
        super.onSizeChanged(width, height, oldWidth, oldHeight)
        offBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        offCanvas = Canvas(offBitmap)
        progress = Progress(width, paddingHorizontal, paddingVertical, randomizer = randomizer)
        drawBackground(offCanvas)
    }

    fun reset() {
        progressState = ProgressState.FINISHED
        invalidate()
    }

    override fun setOnClickListener(listener: OnClickListener?) {
        if (listener == this) {
            super.setOnClickListener(listener)
        }
        listener?.let { onClickListener = it }
    }

    private fun Paint.textHeight(text: String): Float {
        val textBounds = Rect()
        getTextBounds(text, 0, text.length, textBounds)
        return absoluteLength(textBounds.top.toFloat(), textBounds.bottom.toFloat())
    }

    private class Progress(
        parentWidth: Int,
        paddingHorizontal: Int = DEFAULT_INSET,
        paddingVertical: Int = DEFAULT_INSET,
        internalWidth: Float = (parentWidth / 2) + (2f * paddingHorizontal),
        _left: Float = (parentWidth - internalWidth) / 2f,
        _top: Float = 2f * paddingVertical,
        _right: Float = _left + internalWidth,
        _bottom: Float = _top + FILL_HEIGHT,
        private var progressFill: Float = 0f,
        private val paintFill: Paint = Paint().apply { color = Color.BLUE },
        private val paintFillBorder: Paint = Paint().apply {
            color = Color.BLUE
            style = Paint.Style.STROKE
            strokeWidth = 2f
        },
        private val randomizer: Random = Random().apply { setSeed(System.currentTimeMillis()) }
    ) : RectF(_left, _top, _right, _bottom) {

        private val randomProgress = mutableListOf<Float>()

        fun drawBorder(canvas: Canvas) {
            canvas.drawRect(this, paintFillBorder)
        }

        fun drawFill(canvas: Canvas) {
            canvas.drawRect(left, top, left + progressFill, bottom, paintFill)
        }

        private fun generateRandomProgress() {
            val fillSize = width()
            val incrSize: Float = (fillSize / 100f) * 4
            val fillSizeOneThird = fillSize / 3
            val fillSizeTwoThirds = fillSize * 2 / 3
            var sum = 0f
            randomProgress.clear()

            while (sum < fillSize) {
                val nextSize: Float = randomizer.nextFloat() * 100 % incrSize
                val next = when {
                    sum <= fillSizeOneThird -> nextSize / 8f
                    sum <= fillSizeTwoThirds -> nextSize / 4f
                    else -> {
                        if (sum + nextSize > fillSize) {
                            fillSize - sum
                        }
                        nextSize
                    }
                }
                randomProgress.add(next)
                sum += next
            }
        }

        private fun increment(incr: Float) {
            progressFill += incr
        }

        suspend fun play(blocking: suspend () -> Unit) {
            generateRandomProgress()
            progressFill = 0f
            randomProgress.forEach {
                increment(it)
                blocking()
            }
        }
    }

    enum class ProgressState { IDLE, STARTING, WORKING, FINISHED }

    companion object {
        const val NUM_UNITS = 100
        const val TRICKLE = 500
        const val RAIN = 10
        const val ONE_ROTATION = 360
        const val SIZE = 8f
        const val DEFAULT_INSET = 16
        const val FILL_HEIGHT = 48
        const val DEFAULT_TEXT_SIZE_PX = 54f
        const val DEFAULT_TEXT_ANIM_SIZE_PX = 42f
        const val DEFAULT_TEXT_COLOR = Color.BLACK
        const val BURST_COUNT = 20
        const val BORDER_WIDTH = 2f
        const val FILL_BORDER_WIDTH = 2f
    }
}