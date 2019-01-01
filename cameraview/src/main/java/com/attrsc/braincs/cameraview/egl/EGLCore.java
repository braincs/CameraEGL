package com.attrsc.braincs.cameraview.egl;

import android.opengl.EGL14;
import android.opengl.EGLConfig;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.EGLExt;
import android.opengl.EGLSurface;
import android.util.Log;

/**
 * Created by shuai on 11/08/2018.
 */

public class EGLCore {
    private static final String TAG = EGLCore.class.getSimpleName();

    private EGLDisplay mEGLDisplay = EGL14.EGL_NO_DISPLAY;
    private EGLContext mEGLContext = EGL14.EGL_NO_CONTEXT;
    private EGLConfig mEGLConfig = null;
    private EGLSurface mEglSurface = null;

    private int red = 8;
    private int green = 8;
    private int blue = 8;
    private int alpha = 8;
    private int depth = 16;
    private int renderType = EGL14.EGL_OPENGL_ES2_BIT;
    private int bufferType = EGL14.EGL_SINGLE_BUFFER;
    // Android-specific extension.
    private static final int EGL_RECORDABLE_ANDROID = 0x3142;

    public void config(int red, int green, int blue, int alpha, int depth, int renderType) {
        this.red = red;
        this.green = green;
        this.blue = blue;
        this.alpha = alpha;
        this.depth = depth;
        this.renderType = renderType;
    }

    public EGLCore() {
        if (EGLError.OK != eglInit()) Log.d(TAG, "Error in initialisation EGL");
    }

    public EGLCore(EGLContext shareContext){
        if (shareContext == null){
            shareContext = EGL14.EGL_NO_CONTEXT;
        }
        int[] attributes = new int[]{
                EGL14.EGL_RED_SIZE, red,  //指定RGB中的R大小（bits）
                EGL14.EGL_GREEN_SIZE, green, //指定G大小
                EGL14.EGL_BLUE_SIZE, blue,  //指定B大小
                EGL14.EGL_ALPHA_SIZE, alpha, //指定Alpha大小，以上四项实际上指定了像素格式
                EGL14.EGL_DEPTH_SIZE, depth, //指定深度缓存(Z Buffer)大小
                EGL14.EGL_RENDERABLE_TYPE, renderType, //指定渲染api版本, EGL14.EGL_OPENGL_ES2_BIT
                EGL_RECORDABLE_ANDROID, 1,
                EGL14.EGL_NONE};  //总是以EGL14.EGL_NONE结尾

        //init Display
        if (mEGLDisplay != EGL14.EGL_NO_DISPLAY) throw new EGLException("EGL already set up");
        mEGLDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);

        //master version and sub version
        int[] version = new int[2];
        if (!EGL14.eglInitialize(mEGLDisplay, version, 0, version, 1)) {
            mEGLDisplay = null;
            throw new EGLException("unable to initialise EGL14 display");
        }

        //choose config
        EGLConfig[] configs = new EGLConfig[1];
        int[] numConfigs = new int[1];
        if (!EGL14.eglChooseConfig(mEGLDisplay, attributes, 0, configs, 0, configs.length, numConfigs, 0)) {
            throw new EGLException("Unable to find RGB8888 / " + version + ", Unable to find suitableEGLConfig");
        }
        mEGLConfig = configs[0];

        //create surface TODO not here to create surface
//        int[] surface_attr = {
//                EGL14.EGL_WIDTH, width,
//                EGL14.EGL_HEIGHT, height,
//                EGL14.EGL_NONE
//        };
//        mEglSurface = createSurface(surface_attr);

        //create context
        int[] contextAttr = new int[]{
                EGL14.EGL_CONTEXT_CLIENT_VERSION, 2,
                EGL14.EGL_NONE
        };
        EGLContext eglContext = EGL14.eglCreateContext(mEGLDisplay, mEGLConfig, shareContext, contextAttr, 0);
        GlUtil.checkGlError("eglCreateContext");
        mEGLContext = eglContext;
        // Confirm with query.
        int[] values = new int[1];
        EGL14.eglQueryContext(mEGLDisplay, mEGLContext, EGL14.EGL_CONTEXT_CLIENT_VERSION,
                values, 0);
        Log.d(TAG, "EGLContext created, client version " + values[0]);
    }

    /**
     * EGL Context init
     *      Memory region store status of OpenGL ES including
     *      init display {@link EGLDisplay} {@link #mEGLDisplay}
     *      config context {@link EGLConfig} {@link #mEGLConfig}
     *      create context {@link EGLContext} {@link #mEGLContext}
     *      verify context
     * @return init state {@link EGLError}
     */
    private EGLError eglInit() {
        int[] attributes = new int[]{
                EGL14.EGL_RED_SIZE, red,  //指定RGB中的R大小（bits）
                EGL14.EGL_GREEN_SIZE, green, //指定G大小
                EGL14.EGL_BLUE_SIZE, blue,  //指定B大小
                EGL14.EGL_ALPHA_SIZE, alpha, //指定Alpha大小，以上四项实际上指定了像素格式
                EGL14.EGL_DEPTH_SIZE, depth, //指定深度缓存(Z Buffer)大小
                EGL14.EGL_RENDERABLE_TYPE, renderType, //指定渲染api版本, EGL14.EGL_OPENGL_ES2_BIT
                EGL_RECORDABLE_ANDROID, 1,
                EGL14.EGL_NONE};  //总是以EGL14.EGL_NONE结尾

        //init Display
        if (mEGLDisplay != EGL14.EGL_NO_DISPLAY) throw new EGLException("EGL already set up");
        mEGLDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);

        //master version and sub version
        int[] version = new int[2];
        if (!EGL14.eglInitialize(mEGLDisplay, version, 0, version, 1)) {
            mEGLDisplay = null;
            throw new EGLException("unable to initialise EGL14 display");
        }

        //choose config
        EGLConfig[] configs = new EGLConfig[1];
        int[] numConfigs = new int[1];
        if (!EGL14.eglChooseConfig(mEGLDisplay, attributes, 0, configs, 0, configs.length, numConfigs, 0)) {
            throw new EGLException("Unable to find RGB8888 / " + version + ", Unable to find suitableEGLConfig");
        }
        mEGLConfig = configs[0];

        //create surface TODO not here to create surface
//        int[] surface_attr = {
//                EGL14.EGL_WIDTH, width,
//                EGL14.EGL_HEIGHT, height,
//                EGL14.EGL_NONE
//        };
//        mEglSurface = createSurface(surface_attr);

        //create context
        int[] contextAttr = new int[]{
                EGL14.EGL_CONTEXT_CLIENT_VERSION, 2,
                EGL14.EGL_NONE
        };
        EGLContext eglContext = EGL14.eglCreateContext(mEGLDisplay, mEGLConfig, mEGLContext, contextAttr, 0);
        GlUtil.checkGlError("eglCreateContext");
        mEGLContext = eglContext;
        // Confirm with query.
        int[] values = new int[1];
        EGL14.eglQueryContext(mEGLDisplay, mEGLContext, EGL14.EGL_CONTEXT_CLIENT_VERSION, values, 0);
        Log.d(TAG, "EGLContext created, client version " + values[0]);

        return EGLError.OK;
    }

    public void makeCurrent(EGLSurface eglSurface) {
        if (mEGLDisplay == EGL14.EGL_NO_DISPLAY)
            throw new EGLException("No Display during makeCurrent");
        if (mEGLContext == EGL14.EGL_NO_CONTEXT)
            throw new EGLException("No context during makeCurrent");
        if (!EGL14.eglMakeCurrent(mEGLDisplay, eglSurface, eglSurface, mEGLContext))
            throw new EGLException("eglMakeCurrent failed");
    }

    public void makeCurrent(EGLSurface drawSurface, EGLSurface readSurface) {
        if (mEGLDisplay == EGL14.EGL_NO_DISPLAY)
            throw new EGLException("No Display during makeCurrent");
        if (mEGLContext == EGL14.EGL_NO_CONTEXT)
            throw new EGLException("No context during makeCurrent");
        if (!EGL14.eglMakeCurrent(mEGLDisplay, drawSurface, readSurface, mEGLContext))
            throw new EGLException("eglMakeCurrent failed");
    }

    public boolean swapBuffers(EGLSurface eglSurface) {
        return EGL14.eglSwapBuffers(mEGLDisplay, eglSurface);
    }

    /**
     * release context
     *
     */
    public void release(){
        if (mEGLDisplay != EGL14.EGL_NO_DISPLAY) {
            // Android is unusual in that it uses a reference-counted EGLDisplay.  So for
            // every eglInitialize() we need an eglTerminate().
            EGL14.eglMakeCurrent(mEGLDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE,
                    EGL14.EGL_NO_CONTEXT);
            EGL14.eglDestroyContext(mEGLDisplay, mEGLContext);
            EGL14.eglReleaseThread();
            EGL14.eglTerminate(mEGLDisplay);
        }

        mEGLDisplay = EGL14.EGL_NO_DISPLAY;
        mEGLContext = EGL14.EGL_NO_CONTEXT;
        mEGLConfig = null;
    }

    /**
     * create EGLSurface
     *      create window surface to display EGL
     *      {@link EGLSurface}
     * @param surface surface container
     * @return EGLSurface {@link EGLSurface}
     */
    public EGLSurface createWindowSurface(Object surface) {
        int[] surface_attr = {
                EGL14.EGL_NONE
        };
        EGLSurface eglSurface = EGL14.eglCreateWindowSurface(mEGLDisplay, mEGLConfig, surface, surface_attr, 0);
        GlUtil.checkGlError("eglCreateWindowSurface");
        if (eglSurface == null) {
            throw new EGLException("surface is null");
        }
        return eglSurface;
    }

    public EGLSurface createOffScreenSurface(int width, int height) {
        int[] surface_attr = {
                EGL14.EGL_WIDTH, width,
                EGL14.EGL_HEIGHT, height,
                EGL14.EGL_NONE
        };
        EGLSurface eglSurface = EGL14.eglCreatePbufferSurface(mEGLDisplay, mEGLConfig, surface_attr, 0);
        GlUtil.checkGlError("eglCreatePbufferSurface");
        if (eglSurface == null) {
            throw new EGLException("surface is null");
        }
        return eglSurface;
    }

    public void releaseSurface(EGLSurface eglSurface) {
        EGL14.eglDestroySurface(mEGLDisplay, eglSurface);
    }

    @Override
    protected void finalize() throws Throwable {
        try{
            release();
        }finally {
            super.finalize();
        }
    }

    /**
     * Sends the presentation time stamp to EGL.  Time is expressed in nanoseconds.
     */
    public void setPresentationTime(EGLSurface eglSurface, long nsecs) {
        EGLExt.eglPresentationTimeANDROID(mEGLDisplay, eglSurface, nsecs);
    }

    /**
     * Returns true if our context and the specified surface are current.
     */
    public boolean isCurrent(EGLSurface eglSurface) {
        return mEGLContext.equals(EGL14.eglGetCurrentContext()) &&
                eglSurface.equals(EGL14.eglGetCurrentSurface(EGL14.EGL_DRAW));
    }

    /**
     * Performs a simple surface query.
     */
    public int querySurface(EGLSurface eglSurface, int what) {
        int[] value = new int[1];
        EGL14.eglQuerySurface(mEGLDisplay, eglSurface, what, value, 0);
        return value[0];
    }

}
