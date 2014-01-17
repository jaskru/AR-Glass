/*
 * VideoStageRenderer
 *
 * Created on: May 20, 2011
 * Author: Dmytro Baryskyy
 */

package com.ne0fhyklabs.freeflight.video;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView.Renderer;
import android.opengl.Matrix;
import android.util.Log;
import com.ne0fhyklabs.freeflight.R;
import com.ne0fhyklabs.freeflight.settings.ApplicationSettings;
import com.ne0fhyklabs.freeflight.ui.gl.GLBGVideoSprite;
import com.ne0fhyklabs.freeflight.ui.hud.Sprite;
import com.ne0fhyklabs.freeflight.utils.TextResourceReader;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Map;

public class VideoStageRenderer implements Renderer {

    private GLBGVideoSprite bgSprite;

    private ArrayList<Sprite> sprites;
    private Map<Integer, Sprite> idSpriteMap;

    private float fps;

    private int screenWidth;
    private int screenHeight;

    // **********************
    private float[] mVMatrix = new float[16];
    private float[] mProjMatrix = new float[16];

    private int program;

    private final String vertexShaderCode;
    private final String fragmentShaderCode;

    private long startTime;

    private long endTime;

    private final ApplicationSettings mSettings;

    // ***********************

    public VideoStageRenderer(Context context, Bitmap initialTexture)
    {
        mSettings = new ApplicationSettings(context);
        bgSprite = new GLBGVideoSprite(context.getResources());
        bgSprite.setAlpha(1.0f);

        idSpriteMap = new Hashtable<Integer, Sprite>();
        sprites = new ArrayList<Sprite>(4);

        vertexShaderCode = TextResourceReader.readTextFileFromResource(context,
                R.raw.vertex_shader_code);
        fragmentShaderCode = TextResourceReader
                .readTextFileFromResource(context, R.raw.fragment_shader_code);
    }

    public void addSprite(Integer id, Sprite sprite)
    {
        if ( !idSpriteMap.containsKey(id) ) {
            idSpriteMap.put(id, sprite);
            synchronized (sprites) {
                sprites.add(sprite);
            }
        }
    }

    public Sprite getSprite(Integer id)
    {
        return idSpriteMap.get(id);
    }

    public void removeSprite(Integer id)
    {
        if ( idSpriteMap.containsKey(id) ) {
            Sprite sprite = idSpriteMap.get(id);
            synchronized (sprites) {
                sprites.remove(sprite);
                idSpriteMap.remove(id);
            }
        }
    }

    public void onDrawFrame(Canvas canvas)
    {
        bgSprite.onDraw(canvas, 0, 0);

        synchronized (sprites) {
            int spritesSize = sprites.size();

            for ( int i = 0; i < spritesSize; ++i ) {
                Sprite sprite = sprites.get(i);

                if ( !sprite.isInitialized() && screenWidth != 0 && screenHeight != 0 ) {
                    onSurfaceChanged(canvas, screenWidth, screenHeight);
                    sprite.surfaceChanged(canvas);
                }

                if ( sprite != null ) {
                    sprite.draw(canvas);
                }
            }
        }
    }

    @Override
    public void onDrawFrame(GL10 gl)
    {
        // Limiting framerate in order to save some CPU time
        endTime = System.currentTimeMillis();
        long dt = endTime - startTime;

        if ( dt < 33 )
            try {
                Thread.sleep(33 - dt);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        startTime = System.currentTimeMillis();
        boolean isOculusModeEnabled = mSettings.isOculusModeEnabled();

        if ( isOculusModeEnabled ) {
            // Drawing scene on the left half of the screen
            GLES20.glViewport(0, 0, screenWidth / 2, screenHeight);
            GLES20.glScissor(0, 0, screenWidth / 2, screenHeight);
            bgSprite.onDraw(gl, 0, 0);
            GLES20.glFlush();

            // Drawing scene on the right half of the screen
            GLES20.glViewport(screenWidth / 2, 0, screenWidth / 2, screenHeight);
            GLES20.glScissor(screenWidth / 2, 0, screenWidth / 2, screenHeight);
            bgSprite.onDraw(gl, 0, 0);
            GLES20.glFlush();

            // Restore the viewport to the full screen
            GLES20.glViewport(0, 0, screenWidth, screenHeight);
            GLES20.glScissor(0, 0, screenWidth, screenHeight);
        }
        else {
            // Set the viewport to the full screen
            GLES20.glViewport(0, 0, screenWidth, screenHeight);
            GLES20.glScissor(0, 0, screenWidth, screenHeight);

            bgSprite.onDraw(gl, 0, 0);
            GLES20.glFlush();
        }

        synchronized (sprites) {
            int spritesSize = sprites.size();

            for ( int i = 0; i < spritesSize; ++i ) {
                Sprite sprite = sprites.get(i);
                if ( sprite != null ) {
                    if ( !sprite.isInitialized() && screenWidth != 0 && screenHeight != 0 ) {
                        sprite.init(gl, program);
                        sprite.surfaceChanged(null, screenWidth, screenHeight);
                        sprite.setViewAndProjectionMatrices(mVMatrix, mProjMatrix);
                    }

                    sprite.draw(gl);
                }
            }
        }
        GLES20.glFlush();
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height)
    {
        screenWidth = width;
        screenHeight = height;

        Matrix.orthoM(mProjMatrix, 0, 0, width, 0, height, 0, 2f);

        bgSprite.setViewAndProjectionMatrices(mVMatrix, mProjMatrix);
        bgSprite.onSurfaceChanged(gl, width, height);

        synchronized (sprites) {
            int size = sprites.size();
            for ( int i = 0; i < size; ++i ) {
                Sprite sprite = sprites.get(i);

                if ( sprite != null ) {
                    sprite.setViewAndProjectionMatrices(mVMatrix, mProjMatrix);
                    sprite.surfaceChanged(null, width, height);
                }
            }
        }
    }

    public void onSurfaceChanged(Canvas canvas, int width, int height)
    {
        screenWidth = width;
        screenHeight = height;

        bgSprite.onSurfaceChanged(null, width, height);
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config)
    {
        startTime = System.currentTimeMillis();

        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glDisable(GLES20.GL_DEPTH_TEST);
        GLES20.glEnable(GLES20.GL_SCISSOR_TEST);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

        int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
        int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);

        program = GLES20.glCreateProgram();
        GLES20.glAttachShader(program, vertexShader);
        GLES20.glAttachShader(program, fragmentShader);

        GLES20.glLinkProgram(program);
        bgSprite.init(gl, program);

        // Init sprites
        synchronized (sprites) {
            for ( int i = 0; i < sprites.size(); ++i ) {
                sprites.get(i).init(gl, program);
            }
        }

        Matrix.setLookAtM(mVMatrix, 0, /* x */0, /* y */0, /* z */1.5f, 0f, 0f, -5f, 0, 1f, 0.0f);
    }

    public float getFPS()
    {
        return fps;
    }

    public boolean updateVideoFrame()
    {
        return bgSprite.updateVideoFrame();
    }

    private int loadShader(int type, String code)
    {
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, code);
        GLES20.glCompileShader(shader);

        int[] compiled = new int[1];
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);
        if ( compiled[0] == 0 )
        {
            Log.e("opengl", "Could not compile shader");
            Log.e("opengl", GLES20.glGetShaderInfoLog(shader));
            Log.e("opengl", code);
        }

        return shader;
    }

    public void clearSprites()
    {
        synchronized (sprites) {
            for ( int i = 0; i < sprites.size(); ++i ) {
                Sprite sprite = sprites.get(i);
                sprite.freeResources();
            }

            sprites.clear();
        }
    }
}
