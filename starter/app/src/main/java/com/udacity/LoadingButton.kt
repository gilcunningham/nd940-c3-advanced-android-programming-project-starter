package com.udacity

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.ColorDrawable
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import android.view.animation.AccelerateInterpolator
import android.view.animation.LinearInterpolator
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.animation.doOnEnd
import androidx.core.animation.doOnStart
import androidx.core.content.res.ResourcesCompat
import androidx.core.content.withStyledAttributes
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.lifecycleScope
import java.util.Random
import kotlin.math.absoluteValue
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class LoadingButton @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr),
    View.OnClickListener {

    private var backgroundColor =
        if (background is ColorDrawable) {
            (background as ColorDrawable).color
        } else {
            ResourcesCompat.getColor(
                resources, R.color.design_default_color_background, null
            )
        }
    private lateinit var circleProgress: CircularProgress
    private val fillUnitDrawable =
        AppCompatResources.getDrawable(context, R.drawable.ic_solid_square)
    private var fillUnitLayoutParams =
        fillUnitDrawable?.let { LayoutParams(it.minimumWidth, it.minimumHeight) }
    private val fillUnitSize = fillUnitDrawable?.minimumHeight ?: 0
    private lateinit var lifecycleScope: LifecycleCoroutineScope
    private var onClickListener: OnClickListener = this
    private val parentAsViewGroup by lazy { parent as ViewGroup }
    private val paintBorder = Paint().apply {
        color = Color.BLACK
        isAntiAlias = true
        strokeWidth = 2f
        style = Paint.Style.STROKE
    }
    private val textPaint = Paint().apply {
        color = DEFAULT_TEXT_COLOR
        isAntiAlias = true
        textSize = DEFAULT_TEXT_SIZE_PX
    }
    private val showerAnimatorList = mutableListOf<AnimatorSet>()
    private val internalHeight = FILL_HEIGHT + textPaint.textSize + (2 * DEFAULT_INSET)
    private lateinit var offCanvas: Canvas
    private lateinit var offBitmap: Bitmap
    private lateinit var progress: Progress
    private var progressState = ProgressState.IDLE
    private val randomizer: Random = Random().apply { setSeed(System.currentTimeMillis()) }
    private var text = context.getText(R.string.default_loading_button_text).toString()
    private var textFinished = context.getText(R.string.default_loading_button_text_done).toString()

    init {
        context.withStyledAttributes(attrs, R.styleable.LoadingButton) {
            getString(R.styleable.LoadingButton_text)?.let { text = it }
            getString(R.styleable.LoadingButton_textFinished)?.let { textFinished = it }
            textPaint.apply {
                textSize = getDimension(R.styleable.LoadingButton_textSize, DEFAULT_TEXT_SIZE_PX)
                color = getColor(R.styleable.LoadingButton_textColor, DEFAULT_TEXT_COLOR)
            }
            backgroundColor = getColor(R.styleable.LoadingButton_backgroundColor, backgroundColor)
        }
        setOnClickListener(this)
    }

    private fun absoluteCenter(start: Float, finish: Float) = ((start - finish) / 2f).absoluteValue

    private fun addUnitOfProgress(): AppCompatImageView {
        val progressUnit = AppCompatImageView(context)
        progressUnit.setImageDrawable(fillUnitDrawable)
        progressUnit.layoutParams = fillUnitLayoutParams
        parentAsViewGroup.addView(progressUnit)
        return progressUnit
    }

    private fun animateFinishing() = lifecycleScope.launch {
        if (progressState == ProgressState.FINISHING) {
            AnimatorSet().apply {
                startDelay = 2000
                doOnStart {
                    invalidate()
                }
                doOnEnd {
                    progressState = ProgressState.IDLE
                    invalidate()
                }
            }.start()
        }
    }

    private fun animateShower() = lifecycleScope.launch {
        while (isAnimating()) {
            for (i in 0..BURST_COUNT) {
                animateSingle()
            }
            delay(100)
        }
        showerAnimatorList.forEach { animator ->
            animator.cancel()
        }
        showerAnimatorList.clear()
    }

    private fun animateSingle() {
        if (progressState != ProgressState.ANIMATING) {
            return
        }
        val progressUnit = addUnitOfProgress()
        val randomY = randomizer.nextFloat() * (FILL_HEIGHT - (fillUnitSize / 2))
        progressUnit.translationY = y + progress.top + randomY
        progressUnit.translationX = x + progress.right - (fillUnitSize / 2)
        val mover = ObjectAnimator.ofFloat(
            progressUnit, TRANSLATION_X, progressUnit.translationX, x + progress.left
        )
        mover.interpolator = AccelerateInterpolator(1f)
        val animator = AnimatorSet().apply {
            play(mover)
            duration = (randomizer.nextFloat() * 500 + 250).toLong()
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    parentAsViewGroup.removeView(progressUnit)
                }
            })
        }
        showerAnimatorList.add(animator)
        animator.start()
    }

    private fun drawBackground(canvas: Canvas) {
        canvas.drawColor(backgroundColor)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paintBorder)
    }

    private fun drawLabel(canvas: Canvas) {
        val labelBounds = Rect()
        val text = textLabel()
        textPaint.getTextBounds(text, 0, text.length, labelBounds)
        val textX = (width - labelBounds.width()) / 2f
        val textHeight = textPaint.textSize
        val textY = absoluteCenter(height.toFloat(), textHeight) + textHeight - 2F
        canvas.drawText(text, textX, textY, textPaint)
    }

    private fun drawProgress(canvas: Canvas) {
        if (isAnimating()) {
            progress.onDraw(canvas)
        }
        if (isAnimating2()) {
            circleProgress.onDraw(canvas)
        }
    }

    private fun isAnimating() = progressState == ProgressState.ANIMATING
    private fun isAnimating2() = progressState == ProgressState.ANIMATING2

    private fun onAnimate() = lifecycleScope.launch {
        progressState = ProgressState.ANIMATING
        animateShower()
        progress.play(this@LoadingButton)
        if (isAnimating()) {
            progressState = ProgressState.ANIMATING2
            circleProgress.play(this@LoadingButton) {
                if (isAnimating2()) {
                    progressState = ProgressState.FINISHING
                    animateFinishing()
                }
            }
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        layoutParams.height = internalHeight.toInt()
        parentAsViewGroup.findViewTreeLifecycleOwner()?.let {
            lifecycleScope = it.lifecycleScope
        }
    }

    override fun onClick(view: View?) {
        if (progressState != ProgressState.IDLE) {
            return
        }
        lifecycleScope.launch { onAnimate() }
        if (onClickListener != this) {
            onClickListener.onClick(view)
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawBitmap(offBitmap, 0f, 0f, null)
        if (isAnimating() || isAnimating2()) {
            drawProgress(canvas)
        } else {
            drawLabel(canvas)
        }
    }

    override fun onSizeChanged(width: Int, height: Int, oldWidth: Int, oldHeight: Int) {
        super.onSizeChanged(width, height, oldWidth, oldHeight)
        offBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        offCanvas = Canvas(offBitmap)
        progress = Progress(width, height, randomizer = randomizer)
        circleProgress = CircularProgress(progress)
        drawBackground(offCanvas)
    }

    fun stop() {
        if (isAnimating()) {
            circleProgress.animationCount = 1
            progress.stop()
        }
        if (isAnimating2()) {
            onFinished()
        }
    }

    private fun onFinished() {
        progressState = ProgressState.FINISHING
        circleProgress.stop()
        animateFinishing()
        circleProgress.animationCount = MANY_CIRCLE_ANIMATIONS
    }

    override fun setOnClickListener(listener: OnClickListener?) {
        if (listener == this) {
            super.setOnClickListener(listener)
        }
        listener?.let { onClickListener = it }
    }

    private fun textLabel() = when (progressState) {
        ProgressState.IDLE -> text
        else -> textFinished
    }

    private class CircularProgress(
        val constraintRect: RectF,
        size: Float = constraintRect.height() - HALF_INSET,
        duration: Duration = Duration.DEFAULT,
        private val durationMillis: Long = duration.millis,
        color: Int = Color.WHITE,
        left: Float = constraintRect.centerX() - (size / 2),
        top: Float = constraintRect.top + QUARTER_INSET,
        private val paint: Paint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.FILL
            this.color = color
        }
    ) : RectF(left, top, left + size, top + size) {

        var animationCount = MANY_CIRCLE_ANIMATIONS
        private var animating = false
        private var animatorSet = AnimatorSet()
        private var sweep = 0

        fun onDraw(canvas: Canvas) {
            if (animating) {
                canvas.drawArc(this, START_ANGLE, sweep.toFloat(), true, paint)
            }
        }

        suspend fun play(parentView: View, blocking: suspend () -> Unit) {
            sweep = 0
            animating = true
            val list = animatorList(parentView)
            animatorSet = AnimatorSet().apply {
                playSequentially(list)
                doOnEnd {
                    if (animating) {
                        parentView.findViewTreeLifecycleOwner()?.lifecycleScope?.launch {
                            blocking()
                        }
                    }
                }
            }
            animatorSet.start()
        }

        private fun animatorList(parentView: View): List<Animator> {
            return mutableListOf<Animator>().apply {
                for (i in 1..animationCount) {
                    add(ValueAnimator.ofInt(0, 360).apply {
                        duration = durationMillis
                        startDelay = if (i == 1) 0 else CIRCLE_ANIMATION_DELAY
                        interpolator = LinearInterpolator()
                        addUpdateListener {
                            sweep = animatedValue as Int
                            parentView.invalidate()
                        }
                    })
                }
            }
        }

        fun stop() {
            animating = false
            animatorSet.cancel()
        }

        enum class Duration(val millis: Long) {
            SHORT(250),
            DEFAULT(500),
            LONG(1000)
        }
    }

    private class Progress(
        parentWidth: Int,
        parentHeight: Int,
        internalWidth: Float = (parentWidth / 2) + (2f * DEFAULT_INSET),
        left: Float = (parentWidth - internalWidth) / 2f,
        top: Float = (parentHeight - FILL_HEIGHT) / 2f,
        fillColor: Int = DEFAULT_FILL_COLOR,
        private val paintFill: Paint = Paint().apply { color = fillColor },
        private val paintFillBorder: Paint = Paint().apply {
            color = fillColor
            style = Paint.Style.STROKE
            strokeWidth = 2f
        },
        private val randomizer: Random = Random().apply { setSeed(System.currentTimeMillis()) }
    ) : RectF(left, top, left + internalWidth, top + FILL_HEIGHT) {

        private var animating = false
        private var progressFill: Float = 0f
        private val randomProgress = mutableListOf<Float>()

        fun onDraw(canvas: Canvas) {
            canvas.drawRect(left, top, left + progressFill, bottom, paintFill)
            canvas.drawRect(this, paintFillBorder)
        }

        private fun generateRandomProgress() {
            val fillSize = width()
            val increment: Float = (fillSize / 100f)
            val fillSizeOneThird = fillSize / 3
            val fillSizeTwoThirds = fillSize * 2 / 3
            var sum = 0f
            randomProgress.clear()

            while (sum < fillSize) {
                val randIncrement : Float = randomizer.nextFloat() * 100 % increment
                val next = when {
                    sum <= fillSizeOneThird -> randIncrement * 2
                    sum <= fillSizeTwoThirds -> randIncrement
                    else -> {
                        if (sum + randIncrement > fillSize) {
                            fillSize - sum
                        } else {
                            randIncrement / 2
                        }
                    }
                }
                randomProgress.add(next)
                sum += next
            }
        }

        suspend fun play(parentView: View) {
            generateRandomProgress()
            progressFill = 0f
            animating = true
            randomProgress.forEach {
                progressFill += it
                if (animating) {
                    parentView.invalidate()
                    delay(5) //10)
                }
            }
            parentView.invalidate()
            delay(250) //10)
        }

        fun stop() {
            animating = false
        }
    }

    enum class ProgressState { IDLE, ANIMATING, ANIMATING2, FINISHING }

    private companion object {
        const val QUARTER_INSET = 4
        const val HALF_INSET = 8
        const val DEFAULT_INSET = 16
        const val FILL_HEIGHT = 48
        const val DEFAULT_TEXT_SIZE_PX = 54f
        const val DEFAULT_TEXT_COLOR = Color.WHITE
        const val DEFAULT_FILL_COLOR = Color.WHITE
        const val BURST_COUNT = 20
        const val START_ANGLE = 360f
        const val MANY_CIRCLE_ANIMATIONS = 50
        const val CIRCLE_ANIMATION_DELAY = 500L
    }
}