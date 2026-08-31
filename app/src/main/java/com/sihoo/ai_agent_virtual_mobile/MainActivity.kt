package com.sihoo.ai_agent_virtual_mobile

import android.app.Activity
import android.opengl.GLSurfaceView
import android.os.Bundle

class MainActivity : Activity() {

    private lateinit var glSurfaceView: GLSurfaceView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        glSurfaceView = GLSurfaceView(this).apply {
            // Live2D Java 샘플이 OpenGL ES 2.0을 사용하므로 동일하게 설정합니다.
            setEGLContextClientVersion(2)

            setRenderer(Live2DRenderer())

            // 계속해서 화면을 다시 그립니다.
            renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
        }

        setContentView(glSurfaceView)
    }

    override fun onResume() {
        super.onResume()
        glSurfaceView.onResume()
    }

    override fun onPause() {
        glSurfaceView.onPause()
        super.onPause()
    }
}