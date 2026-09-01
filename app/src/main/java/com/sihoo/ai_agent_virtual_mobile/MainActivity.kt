package com.sihoo.ai_agent_virtual_mobile

import android.app.Activity
import android.opengl.GLSurfaceView
import android.os.Bundle
import com.sihoo.ai_agent_virtual_mobile.live2D.GLRendererMinimum
import com.sihoo.ai_agent_virtual_mobile.live2D.LAppMinimumDelegate
import android.view.MotionEvent


class MainActivity : Activity() {

    private lateinit var glSurfaceView: GLSurfaceView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        glSurfaceView = GLSurfaceView(this).apply {
            // Live2D Java 샘플이 OpenGL ES 2.0을 사용하므로 동일하게 설정합니다.
            setEGLContextClientVersion(2)

//            setRenderer(Live2DRenderer())
            setRenderer(GLRendererMinimum())
            // 계속해서 화면을 다시 그립니다.
            renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY

            setOnTouchListener { _, event ->
                val x = event.x
                val y = event.y

                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        queueEvent {
                            LAppMinimumDelegate.getInstance()
                                .onTouchBegan(x, y)
                        }
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

                    MotionEvent.ACTION_UP,
                    MotionEvent.ACTION_CANCEL -> {
                        queueEvent {
                            LAppMinimumDelegate.getInstance()
                                .onTouchEnd(x, y)
                        }
                    }
                }

                true
            }
        }

        setContentView(glSurfaceView)
    }

    override fun onResume() {
        super.onResume()
        glSurfaceView.onResume()
    }

    override fun onPause() {
        // Activity가 실제로 종료되는 경우에만 GL 리소스를 정리합니다.
        // onPause() 이후에는 GL 스레드가 중지될 수 있으므로, 먼저 GL 스레드에
        // 정리 작업을 예약한 다음 GLSurfaceView를 일시 정지합니다.
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
        LAppMinimumDelegate.getInstance().onStart(this)
    }

    override fun onStop() {
        LAppMinimumDelegate.getInstance().onStop()
        super.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}