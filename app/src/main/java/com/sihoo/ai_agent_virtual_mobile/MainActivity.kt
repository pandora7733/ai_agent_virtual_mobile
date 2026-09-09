package com.sihoo.ai_agent_virtual_mobile

import androidx.appcompat.app.AppCompatActivity
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.widget.FrameLayout
import com.google.android.material.button.MaterialButtonToggleGroup
import com.sihoo.ai_agent_virtual_mobile.live2D.GLRendererMinimum
import com.sihoo.ai_agent_virtual_mobile.live2D.LAppMinimumDelegate
import com.sihoo.ai_agent_virtual_mobile.live2D.LAppMinimumLive2DManager
import com.sihoo.ai_agent_virtual_mobile.live2D.PetPreferences

class MainActivity : AppCompatActivity() {

    private lateinit var glSurfaceView: GLSurfaceView
    private lateinit var outfitToggle: MaterialButtonToggleGroup

    companion object {
        private const val TOUCH_LOG_TAG = "PetTouch"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val live2dContainer = findViewById<FrameLayout>(R.id.live2d_container)
        outfitToggle = findViewById(R.id.outfit_toggle)

        glSurfaceView = GLSurfaceView(this).apply {
            setEGLContextClientVersion(2)
            setRenderer(GLRendererMinimum())
            renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
            setupTouchForwarding()
        }
        live2dContainer.addView(
            glSurfaceView,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )

        setupOutfitToggle()
    }

    private fun GLSurfaceView.setupTouchForwarding() {
        setOnTouchListener { _, event ->
            val x = event.x
            val y = event.y

            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    Log.d(
                        TOUCH_LOG_TAG,
                        "ACTION_DOWN screen=($x, $y) pointers=${event.pointerCount}"
                    )
                    queueEvent {
                        LAppMinimumDelegate.getInstance()
                            .onTouchBegan(x, y)
                    }
                }

                MotionEvent.ACTION_POINTER_DOWN -> {
                    Log.d(
                        TOUCH_LOG_TAG,
                        "ACTION_POINTER_DOWN pointers=${event.pointerCount}"
                    )
                }

                MotionEvent.ACTION_MOVE -> {
                    if (event.pointerCount >= 2) {
                        val x1 = event.getX(0)
                        val y1 = event.getY(0)
                        val x2 = event.getX(1)
                        val y2 = event.getY(1)

                        queueEvent {
                            LAppMinimumDelegate.getInstance()
                                .onTouchMoved(x1, y1, x2, y2)
                        }
                    } else {
                        queueEvent {
                            LAppMinimumDelegate.getInstance()
                                .onTouchMoved(x, y)
                        }
                    }
                }

                MotionEvent.ACTION_POINTER_UP -> {
                    Log.d(
                        TOUCH_LOG_TAG,
                        "ACTION_POINTER_UP pointers=${event.pointerCount}"
                    )
                }

                MotionEvent.ACTION_UP -> {
                    Log.d(
                        TOUCH_LOG_TAG,
                        "ACTION_UP screen=($x, $y)"
                    )
                    queueEvent {
                        LAppMinimumDelegate.getInstance()
                            .onTouchEnd(x, y)
                    }
                }

                MotionEvent.ACTION_CANCEL -> {
                    Log.d(
                        TOUCH_LOG_TAG,
                        "ACTION_CANCEL screen=($x, $y)"
                    )
                    queueEvent {
                        LAppMinimumDelegate.getInstance()
                            .onTouchEnd(x, y)
                    }
                }
            }

            true
        }
    }

    private fun setupOutfitToggle() {
        val savedOutfit = PetPreferences(this).getOutfit()
        outfitToggle.check(buttonIdForOutfit(savedOutfit))

        outfitToggle.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) {
                return@addOnButtonCheckedListener
            }

            val outfitType = outfitTypeForButton(checkedId) ?: return@addOnButtonCheckedListener
            glSurfaceView.queueEvent {
                LAppMinimumLive2DManager.getInstance().applyOutfit(outfitType)
            }
        }
    }

    private fun buttonIdForOutfit(
        outfitType: LAppMinimumLive2DManager.OutfitType
    ): Int {
        return when (outfitType) {
            LAppMinimumLive2DManager.OutfitType.OUTFIT -> R.id.outfit_costume
            LAppMinimumLive2DManager.OutfitType.JACKET_OFF -> R.id.outfit_jacket_off
            LAppMinimumLive2DManager.OutfitType.DEFAULT -> R.id.outfit_default
        }
    }

    private fun outfitTypeForButton(
        buttonId: Int
    ): LAppMinimumLive2DManager.OutfitType? {
        return when (buttonId) {
            R.id.outfit_costume -> LAppMinimumLive2DManager.OutfitType.OUTFIT
            R.id.outfit_jacket_off -> LAppMinimumLive2DManager.OutfitType.JACKET_OFF
            R.id.outfit_default -> LAppMinimumLive2DManager.OutfitType.DEFAULT
            else -> null
        }
    }

    override fun onResume() {
        super.onResume()
        glSurfaceView.onResume()
    }

    override fun onPause() {
        if (isFinishing && !isChangingConfigurations) {
            val delegate = LAppMinimumDelegate.getInstance()
            glSurfaceView.queueEvent {
                delegate.onDestroy()
            }
        }
        glSurfaceView.onPause()
        super.onPause()
    }

    override fun onStart() {
        super.onStart()
        val delegate = LAppMinimumDelegate.getInstance()
        delegate.onStart(this)
        delegate.onScreenShown()
    }

    override fun onStop() {
        if (!isChangingConfigurations) {
            LAppMinimumDelegate.getInstance().onStop()
        }
        super.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}
