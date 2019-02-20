package com.attrsc.braincs.cameraview;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.ImageFormat;
import android.graphics.PointF;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.opengl.EGL14;
import android.opengl.EGLSurface;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Toast;

import com.attrsc.braincs.cameraview.egl.Drawable2d;
import com.attrsc.braincs.cameraview.egl.EGLCore;
import com.attrsc.braincs.cameraview.egl.FlatShadedProgram;
import com.attrsc.braincs.cameraview.egl.FullFrameRect;
import com.attrsc.braincs.cameraview.egl.GlUtil;
import com.attrsc.braincs.cameraview.egl.Sprite2d;
import com.attrsc.braincs.cameraview.egl.Texture2dProgram;
import com.attrsc.braincs.cameraview.encoder.TextureMovieEncoder;
import com.attrsc.braincs.cameraview.utils.CameraUtil;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by shuai on 11/08/2018.
 */

public class CameraEGLView extends SurfaceView implements
        SurfaceHolder.Callback, SurfaceTexture.OnFrameAvailableListener {

    private static final String TAG = CameraEGLView.class.getSimpleName();
    private Context mContext;
    private EGLCore mEGLCore;
    private EGLSurface mEGLSurface;
    private int mCameraID = 1;
    private Camera mCamera;
    private int ROTATE_DEGREE;
    private int VIDEO_WIDTH = -1;  // dimensions for 720p video
    private int VIDEO_HEIGHT = -1;
    private int SURFACE_WIDTH = -1;
    private int SURFACE_HEIGHT = -1;
    private FullFrameRect mFullFrameBlit;
    private int mTextureId;
    private SurfaceTexture mCameraTexture;
    private final float[] mTmpMatrix = new float[16];
    private Camera.PreviewCallback cameraPreviewCallBack;
    private CameraInitCallback cameraInitCallback;
    private boolean isMirror = false;

    // TODO consider this is static so it survives activity restarts
    private TextureMovieEncoder mVideoEncoder = new TextureMovieEncoder();
    private volatile boolean isRecording = false;
    private File mOutputFile;


    private final Drawable2d mPointDrawable = new Drawable2d(Drawable2d.Prefab.RECTANGLE);
    private Sprite2d mPoint;
    private float[] mDisplayProjectionMatrix = new float[16];
    private FlatShadedProgram mPlainProgram;


    // points drawing
    private List<Sprite2d> points;


    private int rotation = 270;
    private final Object sync = new Object();
    private volatile boolean isShowPoints = false;

    public CameraEGLView(Context context) {
        super(context);
        init(context, null);
    }

    public CameraEGLView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public CameraEGLView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

//    public CameraEGLView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
//        super(context, attrs, defStyleAttr, defStyleRes);
//        init(context);
//    }


    private void init(Context context, AttributeSet attrs) {
        mContext = context;
        this.getHolder().addCallback(this);
//        if (null ==  attrs) return;
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.CameraEGLView);

        if (typedArray != null) {
            mCameraID = typedArray.getInt(R.styleable.CameraEGLView_camera_id, 1);
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        mEGLCore = new EGLCore();
        mEGLSurface = mEGLCore.createWindowSurface(this.getHolder().getSurface());
        mEGLCore.makeCurrent(mEGLSurface);

        mFullFrameBlit = new FullFrameRect(
                new Texture2dProgram(Texture2dProgram.ProgramType.TEXTURE_EXT));
        mTextureId = mFullFrameBlit.createTextureObject();
        mCameraTexture = new SurfaceTexture(mTextureId);
        mCameraTexture.setOnFrameAvailableListener(this);

//        mPoint = new Sprite2d(mPointDrawable);
//        mPoint.setColor(1.0f, 1.0f, 1.0f);
//        mPoint.setScale(10 * 2f, 10 * 2f);
//        mPoint.setPosition(100f, 100f);

        mPlainProgram = new FlatShadedProgram();

        //set camera width and height
        if (SURFACE_WIDTH < 0 && SURFACE_HEIGHT < 0) {
            //use default display size for video size
            SURFACE_WIDTH = this.getWidth();
            SURFACE_HEIGHT = this.getHeight();
        }
//        Log.d(TAG, "width = " + this.getWidth() + ", height = " + this.getHeight());
//        mVideoEncoder.setTextureId(mTextureId);
        openConfigCamera(mCameraID);
        startPreview();
        if (cameraInitCallback != null){
            cameraInitCallback.onSuccess();
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int format, int width, int height) {
        // Simple orthographic projection, with (0,0) in lower-left corner.
        Matrix.orthoM(mDisplayProjectionMatrix, 0, 0, width, 0, height, -1, 1);

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        this.onDestroy();
    }

    @Override   // SurfaceTexture.OnFrameAvailableListener; runs on arbitrary thread
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        //Log.d(TAG, "frame available");
//        mHandler.sendEmptyMessage(MainHandler.MSG_FRAME_AVAILABLE);
        drawFrame();
//        drawPoint();
    }

    private void onDestroy() {
        stopRecording();

        releaseCamera();

        if (mCameraTexture != null) {
            mCameraTexture.release();
            mCameraTexture = null;
        }
        if (mEGLSurface != null) {
            mEGLCore.releaseSurface(mEGLSurface);
            mEGLSurface = null;
        }
        if (mFullFrameBlit != null) {
            mFullFrameBlit.release(false);
            mFullFrameBlit = null;
        }
        if (mEGLCore != null) {
            mEGLCore.release();
            mEGLCore = null;
        }
        if (mPlainProgram != null) {
            mPlainProgram.release();
            mPlainProgram = null;
        }
    }

    private void drawFrame() {
        //Log.d(TAG, "drawFrame");
        if (mEGLCore == null) {
            Log.d(TAG, "Skipping drawFrame after shutdown");
            return;
        }

        // Latch the next frame from the camera.
        mEGLCore.makeCurrent(mEGLSurface);
        mCameraTexture.updateTexImage();
        mCameraTexture.getTransformMatrix(mTmpMatrix);

        // Fill the SurfaceView with it.
//        SurfaceView sv = (SurfaceView) findViewById(R.id.continuousCapture_surfaceView);
        int viewWidth = this.getWidth();
        int viewHeight = this.getHeight();
        GLES20.glViewport(0, 0, viewWidth, viewHeight);
//        Matrix.
//        for (int i = 0; i < mTmpMatrix.length; i++) {
//            mTmpMatrix[i] = mTmpMatrix[i] * mMirrorMatrix[i];
//        }

        float[] m = GlUtil.IDENTITY_MATRIX.clone();
        if (isMirror) {
            Matrix.scaleM(m, 0, -1, 1, 1);
        }
        mFullFrameBlit.drawFrame(mTextureId, mTmpMatrix, m);
//        drawExtra(mFrameNum, viewWidth, viewHeight);
//        mPoint.draw(mPlainProgram, mDisplayProjectionMatrix);

        //draw points
        if (isShowPoints) {
            synchronized (sync) {
                if (null != points && points.size() > 0) {
                    for (Sprite2d p : points) {
                        p.draw(mPlainProgram, mDisplayProjectionMatrix);
                    }
                }
            }
        }

        mEGLCore.swapBuffers(mEGLSurface);

        if (isRecording) {
            if (!mVideoEncoder.isRecording()) {
                mVideoEncoder.startRecording(new TextureMovieEncoder.EncoderConfig(
                        mOutputFile, VIDEO_WIDTH, VIDEO_HEIGHT, 1000000, EGL14.eglGetCurrentContext()));
                // Set the video encoder's texture name.  We only need to do this once, but in the
                // current implementation it has to happen after the video encoder is started, so
                // we just do it here.
                mVideoEncoder.setTextureId(mTextureId);
            }

            // Tell the video encoder thread that a new frame is available.
            // This will be ignored if we're not actually recording.
            mVideoEncoder.frameAvailable(mCameraTexture);
        } else {
            if (mVideoEncoder.isRecording()) {
                mVideoEncoder.stopRecording();
            }
        }

        // Send it to the video encoder.
//        if (!mFileSaveInProgress) {
//            mEncoderSurface.makeCurrent();
//            GLES20.glViewport(0, 0, VIDEO_WIDTH, VIDEO_HEIGHT);
//            mFullFrameBlit.drawFrame(mTextureId, mTmpMatrix);
//            drawExtra(mFrameNum, VIDEO_WIDTH, VIDEO_HEIGHT);
//            mCircEncoder.frameAvailableSoon();
//            mEncoderSurface.setPresentationTime(mCameraTexture.getTimestamp());
//            mEncoderSurface.swapBuffers();
//        }

//        mFrameNum++;
    }

    private void drawPoint(){
        mPoint.draw(mPlainProgram, mDisplayProjectionMatrix);
    }

    public boolean isMirror() {
        return isMirror;
    }

    public void setMirror(boolean mirror) {
        isMirror = mirror;
    }

    //region MediaEncoder
    //---------------------Encoder block--------------------------
    public void startRecording(File outputFile) {
        isRecording = true;
        this.mOutputFile = outputFile;
    }

    public void stopRecording() {
        isRecording = false;
    }

    public boolean isRecording() {
        return mVideoEncoder.isRecording();
    }
    //endregion

    //region Camera
    //---------------------Camera code block----------------------
//    class CameraThread extends Thread{
//        private final WeakReference<CameraEGLView> mWeakParent;
//
//        public CameraThread(WeakReference<CameraEGLView> mWeakParent) {
//            super("Camera Thread");
//            this.mWeakParent = mWeakParent;
//        }
//    }

    public void setVideoSize(int width, int height) {
        this.VIDEO_WIDTH = width;
        this.VIDEO_HEIGHT = height;
    }

    public int getVIDEO_WIDTH() {
        return VIDEO_WIDTH;
    }


    public int getVIDEO_HEIGHT() {
        return VIDEO_HEIGHT;
    }


    private void openConfigCamera(int mCameraID) {
        try {
            mCamera = Camera.open(mCameraID);
        } catch (Exception e) {
            e.printStackTrace();
            mCamera = null;
        }
        if (mCamera == null) {
            Toast.makeText(mContext, "fail to open camera", Toast.LENGTH_SHORT).show();
            return;
        }
        Camera.Parameters parameters = mCamera.getParameters();

        //preview imageFormat NV21
        parameters.setPreviewFormat(ImageFormat.NV21);

        //focus mode
        final List<String> focusModes = parameters.getSupportedFocusModes();
        if (focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
        } else if (focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
        } else {
            Log.i(TAG, "Camera does not support autofocus");
        }

        // Give the camera a hint that we're recording video.  This can have a big
        // impact on frame rate.
        parameters.setRecordingHint(true);

        // request closest supported preview size
        final Camera.Size closestSize = CameraUtil.getClosestSupportedSize(
                parameters.getSupportedPreviewSizes(), VIDEO_WIDTH, VIDEO_HEIGHT);
        parameters.setPreviewSize(closestSize.width, closestSize.height);

        // request closest picture size for an aspect ratio issue on Nexus7
        final Camera.Size pictureSize = CameraUtil.getClosestSupportedSize(
                parameters.getSupportedPictureSizes(), VIDEO_WIDTH, VIDEO_HEIGHT);
        parameters.setPictureSize(pictureSize.width, pictureSize.height);


        rotation = CameraUtil.getCorrectCameraRotation(mContext, mCameraID);
        if (CameraUtil.isFacingFront(mCameraID)) {
            isMirror = true;
        }
        Log.d(TAG, "degree = " + rotation);
        if (rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_180) {
            VIDEO_WIDTH = closestSize.width;
            VIDEO_HEIGHT = closestSize.height;
        } else {
            VIDEO_WIDTH = closestSize.height;
            VIDEO_HEIGHT = closestSize.width;
        }
        mCamera.setDisplayOrientation(rotation);

        mCamera.setParameters(parameters);

        if (cameraPreviewCallBack != null) {
            mCamera.setPreviewCallback(cameraPreviewCallBack);
        }
    }

    private void startPreview() {
        if (mCamera == null) return;
        try {
            mCamera.setPreviewTexture(mCameraTexture);
        } catch (IOException e) {
            e.printStackTrace();
        }
        mCamera.startPreview();

    }

    public int getCameraRotation() {
        return rotation;
    }

    public void setCameraPreviewCallBack(Camera.PreviewCallback previewCallBack) {
        this.cameraPreviewCallBack = previewCallBack;
    }

    public void setCameraSuccessCallback(CameraInitCallback callback){
        cameraInitCallback = callback;
    }
    /**
     * Stops camera preview, and releases the camera to the system.
     */
    private void releaseCamera() {
        if (mCamera != null) {
            mCamera.stopPreview();
            cameraPreviewCallBack = null;
            mCamera.setPreviewCallback(null);
            mCamera.release();
            mCamera = null;
            Log.d(TAG, "releaseCamera -- done");
        }
    }
    //endregion


    public boolean isShowPoints() {
        return isShowPoints;
    }

    public void setShowPoints(boolean showPoints) {
        isShowPoints = showPoints;
    }

    public void updatePoints(List<PointF> ps) {
//        if (null == ps || ps.isEmpty()){
//            isShow = false;
//        }else {
            synchronized (sync) {
                if (null == ps){
                    Log.d(TAG, "empty");
                        if (points == null) return;//fixme
                    for (int i = 0; i < points.size(); i++)
                        points.get(i).setPosition(0 , 0);
                    return;
                }

                if (points == null || points.size() != ps.size()) { //init points
                    //create points
                    points = new ArrayList<>();
                    for (PointF p : ps) {
                        Sprite2d point = new Sprite2d(mPointDrawable);
                        point.setColor(0, 1, 1);
                        point.setScale(10f, 10f);
                        point.setPosition(p.x * SURFACE_WIDTH, p.y * SURFACE_HEIGHT);

                        points.add(point);
                    }

                } else {
                    //update points
                    for (int i = 0; i < points.size(); i++) {
//                    ps.get(i).x = ps.get(i).x  * SURFACE_WIDTH;
//                    ps.get(i).y = ps.get(i).y  * SURFACE_HEIGHT;
                        points.get(i).setPosition(ps.get(i).x * SURFACE_WIDTH, ps.get(i).y * SURFACE_HEIGHT);
                    }
                }
            }

    }

    public interface CameraInitCallback{
        void onSuccess();
    }

}