package com.udacity

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.animation.AccelerateInterpolator
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.AppCompatImageView
import java.util.Random
import kotlin.properties.Delegates
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

class AnimatedProgressIndicator @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {
    private var fillX = -1f
    private var fillY = -1f
    private val progressFill = Paint().apply { color = Color.YELLOW }
    private val progressIndicator: RectF = RectF(0f, 0f, 0f, 0f)
    private var progressState = ProgressState.IDLE
    private val progressUnitDrawable = AppCompatResources.getDrawable(context, R.drawable.ic_filled_square)

    private val randomizer: Random = Random().apply { setSeed(System.currentTimeMillis()) }

    private fun animationDuration() = when (progressState) {
        ProgressState.IDLE -> {
            0L
        }

        ProgressState.STARTING -> {
            (randomizer.nextFloat() * 1000 + 500).toLong()
        }

        ProgressState.WORKING -> {
            (randomizer.nextFloat() * 100 + 50).toLong()
        }

        ProgressState.FINISHED -> {
            1L
        }
    }

    override fun onSizeChanged(width: Int, height: Int, oldWidth: Int, oldHeight: Int) {
        super.onSizeChanged(width, height, oldWidth, oldHeight)
        //unitWidth = (width / NUM_UNITS).toFloat()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        progressIndicator.set(0F, 0F, progressIndicatorWidth(), height.toFloat())
        canvas.drawRect(progressIndicator, progressFill)
    }

    private fun progressIndicatorWidth() =
        if (progressState == ProgressState.FINISHED) {
            width.toFloat()
        } else {
            fillX
        }


    //var myIndex = 0

    suspend fun startProgress() {
        progressState = ProgressState.STARTING
        shower(ProgressState.STARTING, 5, 300)
    }

    suspend fun updateProgress() {
        progressState = ProgressState.WORKING
        shower(ProgressState.WORKING, 20, 50)
    }

    fun finishProgress() {
        progressState = ProgressState.FINISHED
        clearAnimation()
    }

    private suspend fun shower(currentState: ProgressState, showerCount: Int, showerDelay: Long) {
        withContext(Dispatchers.Main) {
            while (progressState == currentState) {
                for (i in 0..showerCount) {
                    singleUnit() //showerCount)
                }
                delay(showerDelay)
            }
        }
    }

    var progressUnitLayoutParams =
        LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)


    private fun addNewUnitOfProgress(): AppCompatImageView {
        val progressUnit = AppCompatImageView(context)
        progressUnit.setImageDrawable(progressUnitDrawable)
        progressUnit.layoutParams = progressUnitLayoutParams
        addView(progressUnit)
        return progressUnit
    }

    private fun updateStateAndCheckEnd() : Boolean {
        if (fillX > width) {
            progressState = ProgressState.FINISHED
        }
        return progressState == ProgressState.FINISHED
    }

    private fun singleUnit() {
        if (updateStateAndCheckEnd()) {
            return
        }
        val unitWidth = 16f //TODO get square width
        val unitHeight = 16f //TODO get square height
        val progressUnit = addNewUnitOfProgress()
        progressUnit.translationY = randomizer.nextFloat() * height - unitHeight / 2
        val mover = ObjectAnimator.ofFloat(
            progressUnit, TRANSLATION_X, width + unitWidth, -unitWidth
        )
        mover.interpolator = AccelerateInterpolator(1f)
        val rotator = ObjectAnimator.ofFloat(
            progressUnit, ROTATION, (randomizer.nextFloat() * ONE_ROTATION)
        )
        rotator.interpolator = LinearInterpolator()
        AnimatorSet().apply {
            playTogether(mover, rotator)
            duration = animationDuration()
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    when (progressState) {
                        ProgressState.WORKING -> {
                            progressUnit.translationX = fillX
                            progressUnit.translationY = fillY
                            updateFillTranslation()
                        }

                        else -> {
                            removeView(progressUnit)
                        }
                    }
                }
            })
        }.start()
    }

    private fun updateFillTranslation() {
        fillY += 16f // TODO
        if (fillY > height) {
            fillX += 16f
            fillY = 0f
        }
    }


    enum class ProgressState { IDLE, STARTING, WORKING, FINISHED }

    companion object {
        const val NUM_UNITS = 100
        const val TRICKLE = 500
        const val RAIN = 10
        const val ONE_ROTATION = 360
    }


}