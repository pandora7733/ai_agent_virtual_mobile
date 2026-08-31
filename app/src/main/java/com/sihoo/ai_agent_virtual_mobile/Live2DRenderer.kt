package com.sihoo.ai_agent_virtual_mobile

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class Live2DRenderer : GLSurfaceView.Renderer {

    override fun onSurfaceCreated(
        gl: GL10?,
        config: EGLConfig?
    ) {
        // 화면 배경색 설정
        GLES20.glClearColor(
            0.08f,
            0.08f,
            0.12f,
            1.0f
        )
    }

    override fun onSurfaceChanged(
        gl: GL10?,
        width: Int,
        height: Int
    ) {
        GLES20.glViewport(
            0,
            0,
            width,
            height
        )
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(
            GLES20.GL_COLOR_BUFFER_BIT or
                    GLES20.GL_DEPTH_BUFFER_BIT
        )
    }
}