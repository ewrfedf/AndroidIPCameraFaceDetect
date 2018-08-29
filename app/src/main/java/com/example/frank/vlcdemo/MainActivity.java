package com.example.frank.vlcdemo;

import android.app.Activity;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import com.example.frank.vlcdemo.mtnccutil.MTCNN;

public class MainActivity extends Activity {

//    public static final String URL = "rtsp://admin:admin123@10.31.11.79:554/cam/realmonitor?channel=1@subtype=0";

//    public static final String URL = "rtsp://admin:admin@192.168.11.173:554/video2";
    public static final String URL = "rtsp://admin:admin123@192.168.11.111:554";
//    public static final String URL = "rtsp://184.72.239.149/vod/mp4://BigBuckBunny_175k.mov";

//    public static final String URL = "rtsp://10.31.0.61:8554/test.mkv";

    private GLSurfaceView mSurfaceView;
    private RtspSurfaceRender mRender;

    private Button mButton;
    private boolean mRecording;
    private MTCNN mtcnn;
    private ImageView pic;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mtcnn = new MTCNN(getAssets());

        mSurfaceView = findViewById(R.id.surface);
        pic = (ImageView)findViewById(R.id.pic);
        mSurfaceView.setEGLContextClientVersion(3);

        mRender = new RtspSurfaceRender(mSurfaceView, mtcnn,pic);
        mRender.setRtspUrl(URL);

        mButton = findViewById(R.id.btn);
        mButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (mRecording) {
                    mButton.setText("start");
                    mRender.stopRecording();
                } else {
                    mButton.setText("stop");
                    mRender.startRecording();
                }
                mRecording = !mRecording;
            }
        });


        mSurfaceView.setRenderer(mRender);
        mSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mSurfaceView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mSurfaceView.onPause();
    }

    @Override
    protected void onDestroy() {
        mRender.onSurfaceDestoryed();
        super.onDestroy();
    }
}
