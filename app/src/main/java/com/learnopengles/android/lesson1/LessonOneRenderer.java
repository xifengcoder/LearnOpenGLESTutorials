package com.learnopengles.android.lesson1;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.SystemClock;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * This class implements our custom renderer. Note that the GL10 parameter passed in is unused for OpenGL ES 2.0
 * renderers -- the static class GLES20 is used instead.
 */
public class LessonOneRenderer implements GLSurfaceView.Renderer {
    private float[] mModelMatrix = new float[16];
    private float[] mViewMatrix = new float[16];
    private float[] mProjectionMatrix = new float[16];
    private float[] mMVPMatrix = new float[16];

    private final FloatBuffer mTriangle1Vertices;
    private final FloatBuffer mTriangle2Vertices;
    private final FloatBuffer mTriangle3Vertices;

    private int mMVPMatrixHandle;
    private int mPositionHandle;
    private int mColorHandle;

    private static final int BYTES_PER_FLOAT = 4;
    private static final int STRIDE_BYTES = 7 * BYTES_PER_FLOAT;
    private static final int POSITION_OFFSET = 0;
    private static final int POSITION_DATA_SIZE = 3;
    private static final int COLOR_OFFSET = 3;
    private static final int COLOR_DATA_SIZE = 4;

    public LessonOneRenderer() {
        final float[] triangle1VerticesData = {
                // X, Y, Z,R, G, B, A
                -0.5f, -0.25f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f,
                0.5f, -0.25f, 0.0f, 0.0f, 0.0f, 1.0f, 1.0f,
                0.0f, 0.559016994f, 0.0f, 0.0f, 1.0f, 0.0f, 1.0f
        };

        final float[] triangle2VerticesData = {
                // X, Y, Z,R, G, B, A
                -0.5f, -0.25f, 0.0f, 1.0f, 1.0f, 0.0f, 1.0f,
                0.5f, -0.25f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f,
                0.0f, 0.559016994f, 0.0f, 1.0f, 0.0f, 1.0f, 1.0f
        };

        final float[] triangle3VerticesData = {
                // X, Y, Z, R, G, B, A
                -0.5f, -0.25f, 0.0f, 1.0f, 1.0f, 1.0f, 1.0f,
                0.5f, -0.25f, 0.0f, 0.5f, 0.5f, 0.5f, 1.0f,
                0.0f, 0.559016994f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f
        };

        mTriangle1Vertices = ByteBuffer.
                allocateDirect(triangle1VerticesData.length * BYTES_PER_FLOAT)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(triangle1VerticesData);
        mTriangle1Vertices.position(0);

        mTriangle2Vertices = ByteBuffer
                .allocateDirect(triangle2VerticesData.length * BYTES_PER_FLOAT)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(triangle2VerticesData);
        mTriangle2Vertices.position(0);

        mTriangle3Vertices = ByteBuffer
                .allocateDirect(triangle3VerticesData.length * BYTES_PER_FLOAT)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(triangle3VerticesData);
        mTriangle3Vertices.position(0);
    }

    @Override
    public void onSurfaceCreated(GL10 glUnused, EGLConfig config) {
        GLES20.glClearColor(0.5f, 0.5f, 0.5f, 0.5f);
        Matrix.setLookAtM(mViewMatrix, 0,
                0.0f, 0.0f, 1.5f, //相机位置
                0.0f, 2.0f, 0f, //目标位置
                0.0f, 1.0f, 0.0f); //相机正上方向量

        final String vertexShader =
                "uniform mat4 u_MVPMatrix;      \n"        // A constant representing the combined model/view/projection matrix.

                        + "attribute vec4 a_Position;     \n"        // Per-vertex position information we will pass in.
                        + "attribute vec4 a_Color;        \n"        // Per-vertex color information we will pass in.

                        + "varying vec4 v_Color;          \n"        // This will be passed into the fragment shader.

                        + "void main()                    \n"        // The entry point for our vertex shader.
                        + "{                              \n"
                        + "   v_Color = a_Color;          \n"        // Pass the color through to the fragment shader.
                        // It will be interpolated across the triangle.
                        + "   gl_Position = u_MVPMatrix   \n"    // gl_Position is a special variable used to store the final position.
                        + "               * a_Position;   \n"     // Multiply the vertex by the matrix to get the final point in
                        + "}                              \n";    // normalized screen coordinates.

        final String fragmentShader =
                "precision mediump float;       \n"        // Set the default precision to medium. We don't need as high of a
                        // precision in the fragment shader.
                        + "varying vec4 v_Color;          \n"        // This is the color from the vertex shader interpolated across the
                        // triangle per fragment.
                        + "void main()                    \n"        // The entry point for our fragment shader.
                        + "{                              \n"
                        + "   gl_FragColor = v_Color;     \n"        // Pass the color directly through the pipeline.
                        + "}                              \n";

        // Load in the vertex shader.
        int vertexShaderHandle = GLES20.glCreateShader(GLES20.GL_VERTEX_SHADER);
        if (vertexShaderHandle != 0) {
            GLES20.glShaderSource(vertexShaderHandle, vertexShader);// Pass in the shader source.
            GLES20.glCompileShader(vertexShaderHandle); // Compile the shader.
            final int[] compileStatus = new int[1]; // Get the compilation status.
            GLES20.glGetShaderiv(vertexShaderHandle, GLES20.GL_COMPILE_STATUS, compileStatus, 0);
            if (compileStatus[0] == 0) {
                // If the compilation failed, delete the shader.
                GLES20.glDeleteShader(vertexShaderHandle);
                vertexShaderHandle = 0;
            }
        }

        if (vertexShaderHandle == 0) {
            throw new RuntimeException("Error creating vertex shader.");
        }

        // Load in the fragment shader shader.
        int fragmentShaderHandle = GLES20.glCreateShader(GLES20.GL_FRAGMENT_SHADER);

        if (fragmentShaderHandle != 0) {

            GLES20.glShaderSource(fragmentShaderHandle, fragmentShader); // Pass in the shader source.
            GLES20.glCompileShader(fragmentShaderHandle); // Compile the shader.
            final int[] compileStatus = new int[1]; // Get the compilation status.
            GLES20.glGetShaderiv(fragmentShaderHandle, GLES20.GL_COMPILE_STATUS, compileStatus, 0);
            if (compileStatus[0] == 0) {
                // If the compilation failed, delete the shader.
                GLES20.glDeleteShader(fragmentShaderHandle);
                fragmentShaderHandle = 0;
            }
        }

        if (fragmentShaderHandle == 0) {
            throw new RuntimeException("Error creating fragment shader.");
        }

        // Create a program object and store the handle to it.
        int programHandle = GLES20.glCreateProgram();

        if (programHandle != 0) {
            GLES20.glAttachShader(programHandle, vertexShaderHandle); // Bind the vertex shader to the program.
            GLES20.glAttachShader(programHandle, fragmentShaderHandle); // Bind the fragment shader to the program.
            GLES20.glBindAttribLocation(programHandle, 0, "a_Position"); // Bind attributes
            GLES20.glBindAttribLocation(programHandle, 1, "a_Color");
            GLES20.glLinkProgram(programHandle); // Link the two shaders together into a program.
            final int[] linkStatus = new int[1]; // Get the link status.
            GLES20.glGetProgramiv(programHandle, GLES20.GL_LINK_STATUS, linkStatus, 0);
            if (linkStatus[0] == 0) {
                // If the link failed, delete the program.
                GLES20.glDeleteProgram(programHandle);
                programHandle = 0;
            }
        }

        if (programHandle == 0) {
            throw new RuntimeException("Error creating program.");
        }

        // Set program handles. These will later be used to pass in values to the program.
        mMVPMatrixHandle = GLES20.glGetUniformLocation(programHandle, "u_MVPMatrix");
        mPositionHandle = GLES20.glGetAttribLocation(programHandle, "a_Position");
        mColorHandle = GLES20.glGetAttribLocation(programHandle, "a_Color");

        // Tell OpenGL to use this program when rendering.
        GLES20.glUseProgram(programHandle);
    }

    @Override
    public void onSurfaceChanged(GL10 glUnused, int width, int height) {
        // Set the OpenGL viewport to the same size as the surface.
        GLES20.glViewport(0, 0, width, height);

        // Create a new perspective projection matrix. The height will stay the same
        // while the width will vary as per aspect ratio.
        final float ratio = (float) width / height;
        final float left = -ratio;
        final float right = ratio;
        final float bottom = -1.0f;
        final float top = 1.0f;
        final float near = 1.0f;
        final float far = 20.0f;
        Matrix.frustumM(mProjectionMatrix, 0, left, right, bottom, top, near, far);
    }

    @Override
    public void onDrawFrame(GL10 glUnused) {
        GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);

        // Do a complete rotation every 10 seconds.
        long time = SystemClock.uptimeMillis() % 10000L;
        float angleInDegrees = (360.0f / 10000.0f) * ((int) time);

        // Draw the triangle facing straight on.
        Matrix.setIdentityM(mModelMatrix, 0);
        Matrix.rotateM(mModelMatrix, 0, angleInDegrees, 0.0f, 0.0f, 1.0f);
        drawTriangle(mTriangle1Vertices);

        // Draw one translated a bit down and rotated to be flat on the ground.
        Matrix.setIdentityM(mModelMatrix, 0);
        Matrix.translateM(mModelMatrix, 0, 0.0f, -1.0f, 0.0f);
        Matrix.rotateM(mModelMatrix, 0, 90.0f, 1.0f, 0.0f, 0.0f);
        //Matrix.rotateM(mModelMatrix, 0, angleInDegrees, 0.0f, 0.0f, 1.0f);
        drawTriangle(mTriangle2Vertices);

        // Draw one translated a bit to the right and rotated to be facing to the left.
        Matrix.setIdentityM(mModelMatrix, 0);
        Matrix.translateM(mModelMatrix, 0, 0.5f, 0.0f, 0.0f);
        Matrix.rotateM(mModelMatrix, 0, 90.0f, 0.0f, 1.0f, 0.0f);
        //Matrix.rotateM(mModelMatrix, 0, angleInDegrees, 0.0f, 0.0f, 1.0f);
        drawTriangle(mTriangle3Vertices);
    }

    private void drawTriangle(final FloatBuffer aTriangleBuffer) {
        aTriangleBuffer.position(0);
        GLES20.glVertexAttribPointer(mPositionHandle, POSITION_DATA_SIZE, GLES20.GL_FLOAT, false,
                STRIDE_BYTES, aTriangleBuffer);

        GLES20.glEnableVertexAttribArray(mPositionHandle);

        aTriangleBuffer.position(COLOR_OFFSET);
        GLES20.glVertexAttribPointer(mColorHandle, COLOR_DATA_SIZE, GLES20.GL_FLOAT, false,
                STRIDE_BYTES, aTriangleBuffer);

        GLES20.glEnableVertexAttribArray(mColorHandle);
        Matrix.multiplyMM(mMVPMatrix, 0, mViewMatrix, 0, mModelMatrix, 0);
        Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mMVPMatrix, 0);
        GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mMVPMatrix, 0);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 3);
    }
}
