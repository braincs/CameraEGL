package com.attrsc.braincs.cameraegl;

import android.Manifest;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.os.Build;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;

import com.attrsc.braincs.cameraview.CameraEGLView;

import java.io.File;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_CAMERA = 1000;
    private CameraEGLView cameraView;
    private File mFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        cameraView = findViewById(R.id.cameraEglView);
        cameraView.setVideoSize(1280,960);
//        cameraView.setVideoSize(640,480);
        cameraView.setCameraPreviewCallBack(cameraPreviewCallback);

        // path to save your recorded video
        String path = Environment.getExternalStorageDirectory() + File.separator + "TestRecording";
        File parent = new File(path);
        if (!parent.exists()){
            parent.mkdirs();
        }

        //file of recorded video
        mFile = new File(Environment.getExternalStorageDirectory() + File.separator + "TestRecording", "test.mp4");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA);
                return;
            }
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CAMERA+1);
                return;
            }
        }
    }

    Camera.PreviewCallback cameraPreviewCallback = new Camera.PreviewCallback() {
        @Override
        public void onPreviewFrame(byte[] bytes, Camera camera) {
            System.out.println(bytes.toString());
        }
    };

    public void recording(View view) {
        if (cameraView.isRecording()) {
            cameraView.stopRecording();
            ((Button) findViewById(R.id.btnRecroding)).setText("Record");
        } else {
            cameraView.startRecording(mFile);
            ((Button) findViewById(R.id.btnRecroding)).setText("Recording");

        }
    }

    public void mirror(View view){
        cameraView.setMirror(!cameraView.isMirror());
        ((Button)findViewById(R.id.btnMirror)).setText((cameraView.isMirror())?"Mirror":"Normal");
    }

    public void hide(View view){
        int visibility = cameraView.getVisibility();
        if (visibility == View.GONE || visibility == View.INVISIBLE){
            cameraView.setVisibility(View.VISIBLE);
            ((Button)findViewById(R.id.btnHide)).setText("Hide");
        }else {
            cameraView.setVisibility(View.INVISIBLE);
            ((Button)findViewById(R.id.btnHide)).setText("Show");

        }

    }
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        Log.d("debug", "width = "+cameraView.getVIDEO_WIDTH() +", height = "+cameraView.getVIDEO_HEIGHT());
        return super.onTouchEvent(event);
    }
}