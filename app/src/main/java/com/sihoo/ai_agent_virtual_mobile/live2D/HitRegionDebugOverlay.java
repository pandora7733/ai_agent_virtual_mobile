package com.sihoo.ai_agent_virtual_mobile.live2D;

import android.opengl.GLES20;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * Draws the current head / chest / belly hit regions in the same view
 * coordinates used by touch tests, so the ellipses can be tuned visually.
 */
public class HitRegionDebugOverlay implements AutoCloseable {
    private static final String TAG = "PetTouch";
    private static final int GRID_X = 48;
    private static final int GRID_Y = 64;
    private static final int ELLIPSE_SEGMENTS = 64;
    private static final int MAX_GRID_FLOATS = GRID_X * GRID_Y * 6 * 2;

    private static final String VERTEX_SHADER =
            "attribute vec2 aPosition;\n"
                    + "void main() {\n"
                    + "  gl_Position = vec4(aPosition, 0.0, 1.0);\n"
                    + "}\n";

    private static final String FRAGMENT_SHADER =
            "precision mediump float;\n"
                    + "uniform vec4 uColor;\n"
                    + "void main() {\n"
                    + "  gl_FragColor = uColor;\n"
                    + "}\n";

    private final int programId;
    private final int positionLocation;
    private final int colorLocation;
    private final float[] ndcTmp = new float[2];
    private final float[] vertexScratch = new float[MAX_GRID_FLOATS];
    private final float[] lineScratch = new float[8];
    private final FloatBuffer vertexBuffer;
    private boolean loggedBounds;

    public HitRegionDebugOverlay() {
        programId = compileProgram();
        positionLocation = GLES20.glGetAttribLocation(programId, "aPosition");
        colorLocation = GLES20.glGetUniformLocation(programId, "uColor");

        ByteBuffer bb = ByteBuffer.allocateDirect(MAX_GRID_FLOATS * 4);
        bb.order(ByteOrder.nativeOrder());
        vertexBuffer = bb.asFloatBuffer();
    }

    public boolean isReady() {
        return programId != 0;
    }

    public void draw(
            LAppMinimumView view,
            float headCenterX,
            float headCenterY,
            float headRadiusX,
            float headRadiusY,
            float bodyCenterX,
            float bodyCenterY,
            float bodyRadiusX,
            float bodyRadiusY,
            float splitY,
            float lastViewX,
            float lastViewY,
            boolean hasLastTouch,
            LAppMinimumView.HitRegion lastHit
    ) {
        if (!isReady() || view == null) {
            return;
        }

        if (!loggedBounds) {
            loggedBounds = true;
            logBounds(
                    headCenterX,
                    headCenterY,
                    headRadiusX,
                    headRadiusY,
                    bodyCenterX,
                    bodyCenterY,
                    bodyRadiusX,
                    bodyRadiusY,
                    splitY
            );
        }

        GLES20.glUseProgram(programId);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0);
        GLES20.glDisable(GLES20.GL_DEPTH_TEST);
        GLES20.glDisable(GLES20.GL_CULL_FACE);
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
        GLES20.glLineWidth(3.0f);

        float bodyMinX = bodyCenterX - bodyRadiusX;
        float bodyMaxX = bodyCenterX + bodyRadiusX;
        float bodyMinY = bodyCenterY - bodyRadiusY;
        float bodyMaxY = bodyCenterY + bodyRadiusY;

        drawRegionGrid(
                view,
                bodyMinX,
                bodyMaxX,
                bodyMinY,
                bodyMaxY,
                splitY,
                bodyCenterX,
                bodyCenterY,
                bodyRadiusX,
                bodyRadiusY,
                headCenterX,
                headCenterY,
                headRadiusX,
                headRadiusY,
                true,
                1.0f,
                0.35f,
                0.15f,
                0.32f
        );
        drawRegionGrid(
                view,
                bodyMinX,
                bodyMaxX,
                bodyMinY,
                bodyMaxY,
                splitY,
                bodyCenterX,
                bodyCenterY,
                bodyRadiusX,
                bodyRadiusY,
                headCenterX,
                headCenterY,
                headRadiusX,
                headRadiusY,
                false,
                0.95f,
                0.20f,
                0.65f,
                0.32f
        );

        drawEllipseFill(
                view,
                headCenterX,
                headCenterY,
                headRadiusX,
                headRadiusY,
                0.10f,
                0.75f,
                1.0f,
                0.28f
        );

        drawEllipseOutline(
                view,
                bodyCenterX,
                bodyCenterY,
                bodyRadiusX,
                bodyRadiusY,
                0.20f,
                0.90f,
                0.35f,
                0.95f
        );
        drawEllipseOutline(
                view,
                headCenterX,
                headCenterY,
                headRadiusX,
                headRadiusY,
                0.15f,
                0.85f,
                1.0f,
                1.0f
        );
        drawSplitLine(
                view,
                bodyCenterX,
                bodyCenterY,
                bodyRadiusX,
                bodyRadiusY,
                splitY
        );
        drawCross(view, headCenterX, headCenterY, 0.04f, 0.85f, 1.0f, 1.0f, 1.0f);
        drawCross(view, bodyCenterX, bodyCenterY, 0.04f, 0.20f, 1.0f, 0.35f, 1.0f);

        if (hasLastTouch) {
            float[] color = colorForRegion(lastHit);
            drawTouchMarker(view, lastViewX, lastViewY, color[0], color[1], color[2]);
        }

        GLES20.glDisableVertexAttribArray(positionLocation);
    }

    @Override
    public void close() {
        if (programId != 0) {
            GLES20.glDeleteProgram(programId);
        }
    }

    private void drawRegionGrid(
            LAppMinimumView view,
            float minX,
            float maxX,
            float minY,
            float maxY,
            float splitY,
            float bodyCenterX,
            float bodyCenterY,
            float bodyRadiusX,
            float bodyRadiusY,
            float headCenterX,
            float headCenterY,
            float headRadiusX,
            float headRadiusY,
            boolean belly,
            float r,
            float g,
            float b,
            float a
    ) {
        float dx = (maxX - minX) / GRID_X;
        float dy = (maxY - minY) / GRID_Y;
        int floatCount = 0;

        for (int iy = 0; iy < GRID_Y; iy++) {
            float y0 = minY + iy * dy;
            float y1 = y0 + dy;
            float cy = (y0 + y1) * 0.5f;

            for (int ix = 0; ix < GRID_X; ix++) {
                float x0 = minX + ix * dx;
                float x1 = x0 + dx;
                float cx = (x0 + x1) * 0.5f;

                boolean insideBody = isInsideEllipse(
                        cx,
                        cy,
                        bodyCenterX,
                        bodyCenterY,
                        bodyRadiusX,
                        bodyRadiusY
                );
                if (!insideBody) {
                    continue;
                }

                boolean insideHead = isInsideEllipse(
                        cx,
                        cy,
                        headCenterX,
                        headCenterY,
                        headRadiusX,
                        headRadiusY
                );
                if (insideHead) {
                    continue;
                }

                boolean isBelly = cy < splitY;
                if (belly != isBelly) {
                    continue;
                }

                if (floatCount + 12 > vertexScratch.length) {
                    break;
                }

                putNdc(view, x0, y0, floatCount);
                putNdc(view, x1, y0, floatCount + 2);
                putNdc(view, x1, y1, floatCount + 4);
                putNdc(view, x0, y0, floatCount + 6);
                putNdc(view, x1, y1, floatCount + 8);
                putNdc(view, x0, y1, floatCount + 10);
                floatCount += 12;
            }
        }

        drawTriangles(floatCount, r, g, b, a);
    }

    private void drawEllipseFill(
            LAppMinimumView view,
            float centerX,
            float centerY,
            float radiusX,
            float radiusY,
            float r,
            float g,
            float b,
            float a
    ) {
        int floatCount = 0;
        putNdc(view, centerX, centerY, floatCount);
        floatCount += 2;

        for (int i = 0; i <= ELLIPSE_SEGMENTS; i++) {
            double theta = (Math.PI * 2.0 * i) / ELLIPSE_SEGMENTS;
            float vx = centerX + radiusX * (float) Math.cos(theta);
            float vy = centerY + radiusY * (float) Math.sin(theta);
            putNdc(view, vx, vy, floatCount);
            floatCount += 2;
        }

        drawPrimitive(GLES20.GL_TRIANGLE_FAN, floatCount, r, g, b, a);
    }

    private void drawEllipseOutline(
            LAppMinimumView view,
            float centerX,
            float centerY,
            float radiusX,
            float radiusY,
            float r,
            float g,
            float b,
            float a
    ) {
        int floatCount = 0;
        for (int i = 0; i < ELLIPSE_SEGMENTS; i++) {
            double theta = (Math.PI * 2.0 * i) / ELLIPSE_SEGMENTS;
            float vx = centerX + radiusX * (float) Math.cos(theta);
            float vy = centerY + radiusY * (float) Math.sin(theta);
            putNdc(view, vx, vy, floatCount);
            floatCount += 2;
        }
        drawPrimitive(GLES20.GL_LINE_LOOP, floatCount, r, g, b, a);
    }

    private void drawSplitLine(
            LAppMinimumView view,
            float bodyCenterX,
            float bodyCenterY,
            float bodyRadiusX,
            float bodyRadiusY,
            float splitY
    ) {
        float dy = (splitY - bodyCenterY) / bodyRadiusY;
        float inner = 1.0f - dy * dy;
        if (inner <= 0.0f) {
            return;
        }

        float halfWidth = bodyRadiusX * (float) Math.sqrt(inner);
        putNdc(view, bodyCenterX - halfWidth, splitY, lineScratch, 0);
        putNdc(view, bodyCenterX + halfWidth, splitY, lineScratch, 2);

        GLES20.glLineWidth(5.0f);
        drawArray(GLES20.GL_LINES, lineScratch, 4, 1.0f, 0.95f, 0.20f, 1.0f);
        GLES20.glLineWidth(3.0f);
    }

    private void drawCross(
            LAppMinimumView view,
            float centerX,
            float centerY,
            float size,
            float r,
            float g,
            float b,
            float a
    ) {
        putNdc(view, centerX - size, centerY, lineScratch, 0);
        putNdc(view, centerX + size, centerY, lineScratch, 2);
        putNdc(view, centerX, centerY - size, lineScratch, 4);
        putNdc(view, centerX, centerY + size, lineScratch, 6);
        drawArray(GLES20.GL_LINES, lineScratch, 8, r, g, b, a);
    }

    private void drawTouchMarker(
            LAppMinimumView view,
            float viewX,
            float viewY,
            float r,
            float g,
            float b
    ) {
        int floatCount = 0;
        putNdc(view, viewX, viewY, floatCount);
        floatCount += 2;

        float markerRadius = 0.035f;
        for (int i = 0; i <= 16; i++) {
            double theta = (Math.PI * 2.0 * i) / 16.0;
            putNdc(
                    view,
                    viewX + markerRadius * (float) Math.cos(theta),
                    viewY + markerRadius * (float) Math.sin(theta) * 0.6f,
                    floatCount
            );
            floatCount += 2;
        }
        drawPrimitive(GLES20.GL_TRIANGLE_FAN, floatCount, r, g, b, 0.95f);
    }

    private void putNdc(LAppMinimumView view, float viewX, float viewY, int offset) {
        view.viewToNdc(viewX, viewY, ndcTmp);
        vertexScratch[offset] = ndcTmp[0];
        vertexScratch[offset + 1] = ndcTmp[1];
    }

    private void putNdc(
            LAppMinimumView view,
            float viewX,
            float viewY,
            float[] dst,
            int offset
    ) {
        view.viewToNdc(viewX, viewY, ndcTmp);
        dst[offset] = ndcTmp[0];
        dst[offset + 1] = ndcTmp[1];
    }

    private void drawTriangles(int floatCount, float r, float g, float b, float a) {
        drawPrimitive(GLES20.GL_TRIANGLES, floatCount, r, g, b, a);
    }

    private void drawPrimitive(
            int mode,
            int floatCount,
            float r,
            float g,
            float b,
            float a
    ) {
        if (floatCount < 4) {
            return;
        }
        drawArray(mode, vertexScratch, floatCount, r, g, b, a);
    }

    private void drawArray(
            int mode,
            float[] data,
            int floatCount,
            float r,
            float g,
            float b,
            float a
    ) {
        vertexBuffer.clear();
        vertexBuffer.put(data, 0, floatCount);
        vertexBuffer.position(0);

        GLES20.glEnableVertexAttribArray(positionLocation);
        GLES20.glVertexAttribPointer(
                positionLocation,
                2,
                GLES20.GL_FLOAT,
                false,
                0,
                vertexBuffer
        );
        GLES20.glUniform4f(colorLocation, r, g, b, a);
        GLES20.glDrawArrays(mode, 0, floatCount / 2);
    }

    private static boolean isInsideEllipse(
            float x,
            float y,
            float centerX,
            float centerY,
            float radiusX,
            float radiusY
    ) {
        float dx = (x - centerX) / radiusX;
        float dy = (y - centerY) / radiusY;
        return (dx * dx) + (dy * dy) <= 1.0f;
    }

    private static float[] colorForRegion(LAppMinimumView.HitRegion region) {
        if (region == LAppMinimumView.HitRegion.HEAD) {
            return new float[] {0.15f, 0.85f, 1.0f};
        }
        if (region == LAppMinimumView.HitRegion.CHEST) {
            return new float[] {0.95f, 0.20f, 0.65f};
        }
        if (region == LAppMinimumView.HitRegion.BELLY) {
            return new float[] {1.0f, 0.45f, 0.10f};
        }
        return new float[] {1.0f, 1.0f, 1.0f};
    }

    private static void logBounds(
            float headCenterX,
            float headCenterY,
            float headRadiusX,
            float headRadiusY,
            float bodyCenterX,
            float bodyCenterY,
            float bodyRadiusX,
            float bodyRadiusY,
            float splitY
    ) {
        Log.d(
                TAG,
                "HIT_OVERLAY colors head=cyan chest=magenta belly=orange split=yellow"
                        + " headY=["
                        + (headCenterY - headRadiusY)
                        + ", "
                        + (headCenterY + headRadiusY)
                        + "]"
                        + " bodyY=["
                        + (bodyCenterY - bodyRadiusY)
                        + ", "
                        + (bodyCenterY + bodyRadiusY)
                        + "]"
                        + " splitY="
                        + splitY
                        + " headCenter=("
                        + headCenterX
                        + ", "
                        + headCenterY
                        + ")"
                        + " bodyCenter=("
                        + bodyCenterX
                        + ", "
                        + bodyCenterY
                        + ")"
        );
    }

    private static int compileProgram() {
        int vertexShader = compileShader(GLES20.GL_VERTEX_SHADER, VERTEX_SHADER);
        int fragmentShader = compileShader(GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER);
        if (vertexShader == 0 || fragmentShader == 0) {
            return 0;
        }

        int program = GLES20.glCreateProgram();
        GLES20.glAttachShader(program, vertexShader);
        GLES20.glAttachShader(program, fragmentShader);
        GLES20.glLinkProgram(program);

        int[] linkStatus = new int[1];
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0);
        GLES20.glDeleteShader(vertexShader);
        GLES20.glDeleteShader(fragmentShader);

        if (linkStatus[0] == 0) {
            Log.e(TAG, "HIT_OVERLAY shader link failed: " + GLES20.glGetProgramInfoLog(program));
            GLES20.glDeleteProgram(program);
            return 0;
        }
        return program;
    }

    private static int compileShader(int type, String source) {
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, source);
        GLES20.glCompileShader(shader);

        int[] compiled = new int[1];
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);
        if (compiled[0] == 0) {
            Log.e(TAG, "HIT_OVERLAY shader compile failed: " + GLES20.glGetShaderInfoLog(shader));
            GLES20.glDeleteShader(shader);
            return 0;
        }
        return shader;
    }
}
