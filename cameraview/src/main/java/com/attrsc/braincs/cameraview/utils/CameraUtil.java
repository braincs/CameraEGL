package com.attrsc.braincs.cameraview.utils;

import android.content.Context;
import android.hardware.Camera;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.WindowManager;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by shuai on 11/08/2018.
 */

public class CameraUtil {

    public static Camera.Size getClosestSupportedSize(List<Camera.Size> supportedSizes, final int requestedWidth, final int requestedHeight) {
        return (Camera.Size) Collections.min(supportedSizes, new Comparator<Camera.Size>() {

            private int diff(final Camera.Size size) {
                return Math.abs(requestedWidth - size.width) + Math.abs(requestedHeight - size.height);
            }

            @Override
            public int compare(final Camera.Size lhs, final Camera.Size rhs) {
                return diff(lhs) - diff(rhs);
            }
        });

    }

    public static int getCorrectCameraRotation(Context context, int CAMERA_ID){
        final Display display = ((WindowManager)context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        final int rotation = display.getRotation();
        int degrees = 90;
        switch (rotation) {
            case Surface.ROTATION_0: degrees = 0; break;
            case Surface.ROTATION_90: degrees = 90; break;
            case Surface.ROTATION_180: degrees = 180; break;
            case Surface.ROTATION_270: degrees = 270; break;
        }
        // get whether the camera is front camera or back camera
        final Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(CAMERA_ID, info);
        boolean mIsFrontFace = (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT);
        Log.d("debug", "camera id = " +CAMERA_ID +", facing front = " +mIsFrontFace );
        if (mIsFrontFace) {	// front camera
            degrees = (info.orientation + degrees) % 360;
            return (360 - degrees) % 360;  // reverse
        } else {  // back camera
            return (info.orientation - degrees + 360) % 360;
        }
    }

    public static boolean isFacingFront(int CAMERA_ID){
        final Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(CAMERA_ID, info);
        return (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT);
    }
}
