# CameraEGL

## Indroduction
CameraEGL is a simple module to open Android camera. It designs to fit fast Android camera development (e.g. face recognition demo app etc.)

It applies OpenGL ES (EGL14 mostly). The idea is driven by Grafika (copyright by Google). Grafika is more complex with more capability. 

Features: (version 1.0)
 
* preview camera surface
* provide raw camera byte data access 
* mirror camera preview
* recordable camera preview (no sound)
* draw points upon Camera preview


## Integration

if you have clone the project, cameraview is android module to integrate with.   
For Android studio user, you can just simply use `File -> New -> import module ` to import the module. 

Modify/add in your layout:

~~~xml
<com.attrsc.braincs.cameraview.CameraEGLView
android:id="@+id/cameraEglView"
android:layout_width="match_parent"
android:layout_height="match_parent"
app:camera_id="0"/>
~~~

camera_id is the camera you will display in the CameraEGLView. Normally, Android mobile use **id = 0** to indicate **back** camera, whereas **id = 1** for **front** camera.

Then in you relative Activity, you can setup the camera preview ratio.

~~~java
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
    
}
~~~

Where you get the **camera frame raw data**:

~~~java
Camera.PreviewCallback cameraPreviewCallback = new Camera.PreviewCallback() {
    @Override
    public void onPreviewFrame(byte[] bytes, Camera camera) {
        System.out.println(bytes.toString());
    }
};
~~~

If you want to **mirror the camera preview** you can simply use this

~~~java

public void mirror(View view){
    cameraView.setMirror(!cameraView.isMirror());
    ((Button)findViewById(R.id.btnMirror)).setText((cameraView.isMirror())?"Mirror":"Normal");
}

~~~

**Recording feature** can be call by

~~~java
public void recording(View view) {
    if (cameraView.isRecording()) {
        cameraView.stopRecording();
        ((Button) findViewById(R.id.btnRecroding)).setText("Record");
    } else {
        cameraView.startRecording(mFile);
        ((Button) findViewById(R.id.btnRecroding)).setText("Recording");

    }
}

~~~

if you do not want to display camera preview, you can hide it 

~~~java
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
~~~
