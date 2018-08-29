package com.example.frank.vlcdemo;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.EGL14;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import com.example.frank.vlcdemo.mtnccutil.Box;
import com.example.frank.vlcdemo.mtnccutil.MTCNN;
import com.example.frank.vlcdemo.mtnccutil.Utils;
import com.example.frank.vlcdemo.util.BitmapUtil;
import com.inuker.library.RGBProgram;
import com.inuker.library.encoder.BaseMovieEncoder;
import com.inuker.library.encoder.CameraHelper;
import com.inuker.library.encoder.GlUtil;
import com.inuker.library.encoder.MovieEncoder1;
import com.inuker.library.utils.LogUtils;

import java.io.File;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Vector;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import static android.opengl.GLES20.GL_COLOR_BUFFER_BIT;
import static android.opengl.GLES20.GL_DEPTH_BUFFER_BIT;
import static android.opengl.GLES20.glClear;
import static android.opengl.GLES20.glClearColor;

/**
 * Created by liwentian on 2017/10/12.
 */

public class RtspSurfaceRender implements GLSurfaceView.Renderer, RtspHelper.RtspCallback {

    private static final String TAG = "";
    private final MTCNN mtcnn;
    private final ImageView pic;
    private ByteBuffer mBuffer;

    private GLSurfaceView mGLSurfaceView;

    private RGBProgram mProgram;

    private String mRtspUrl;

    private BaseMovieEncoder mVideoEncoder;
    private boolean faceOver = true;
    private Bitmap bmp;

    public RtspSurfaceRender(GLSurfaceView glSurfaceView, MTCNN mtcnn, ImageView pic) {
        mGLSurfaceView = glSurfaceView;
        this.pic = pic;
        this.mtcnn = mtcnn;
    }

    public void setRtspUrl(String url) {
        mRtspUrl = url;
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
    }

    public void startRecording() {
        mGLSurfaceView.queueEvent(new Runnable() {
            @Override
            public void run() {
                if (!mVideoEncoder.isRecording()) {
                    File output = CameraHelper.getOutputMediaFile(CameraHelper.MEDIA_TYPE_VIDEO, "");
                    LogUtils.v(String.format("startRecording: %s", output));
                    mVideoEncoder.startRecording(new BaseMovieEncoder.EncoderConfig(output, EGL14.eglGetCurrentContext()));
                }
            }
        });
    }

    public void stopRecording() {
        mGLSurfaceView.queueEvent(new Runnable() {
            @Override
            public void run() {
                if (mVideoEncoder.isRecording()) {
                    mVideoEncoder.stopRecording();
                }
            }
        });
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        LogUtils.v(String.format("onSurfaceChanged: width = %d, height = %d", width, height));
        mProgram = new RGBProgram(mGLSurfaceView.getContext(), width, height);
        mBuffer = ByteBuffer.allocateDirect(width * height * 4).order(ByteOrder.nativeOrder());
        mVideoEncoder = new MovieEncoder1(mGLSurfaceView.getContext(), width, height);
        RtspHelper.getInstance().createPlayer(mRtspUrl, width, height, this);
    }

    public void onSurfaceDestoryed() {
        RtspHelper.getInstance().releasePlayer();
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        glClearColor(1f, 1f, 1f, 1f);

        mProgram.useProgram();

        synchronized (mBuffer) {
            mProgram.setUniforms(mBuffer.array(), 0);
        }

        mProgram.draw();
    }

    @Override
    public void onPreviewFrame(final ByteBuffer buffer, final int width, final int height) {
        synchronized (mBuffer) {
            mBuffer.rewind();
            buffer.rewind();
            mBuffer.put(buffer);
        }

        if (faceOver) {
            faceOver = false;
            mGLSurfaceView.post(new Runnable() {
                @Override
                public void run() {
//                mVideoEncoder.frameAvailable(buffer.array(), System.nanoTime());
                    long t_start = System.currentTimeMillis();
                    faceDetect(width, height, mBuffer);
                    Log.i(TAG,"[*]Mtcnn Detection Time:"+(System.currentTimeMillis()-t_start));

                    faceOver = true;
                }
            });
        }


        mGLSurfaceView.requestRender();
    }

    private void faceDetect(int width, int height, ByteBuffer buffer) {
//        Bitmap bmp = getBmpFromGL(width, height, buffer);
        bmp = getBmp(width, height, buffer);
        Vector<Box> boxes = mtcnn.detectFaces(bmp, 80);
        if (boxes.size() == 0) {
            pic.setVisibility(View.GONE);
        } else {
            pic.setVisibility(View.VISIBLE);
            bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ALPHA_8);
            Log.i(TAG, "faceDetect" + boxes.size());
            for (int i = 0; i < boxes.size(); i++) {
                Utils.drawBox(bmp, boxes.get(i), 1 + bmp.getWidth() / 500);
            }
            pic.setImageBitmap(bmp);
        }

    }

    public Bitmap getBmp(int width, int height, ByteBuffer buffer) {
        buffer.rewind();
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_4444);
        bitmap.copyPixelsFromBuffer(buffer);
//        bitmap = BitmapUtil.compressByQuality(bitmap, 70);
        return bitmap;
    }


    public Bitmap getBmpFromGL(int width, int height, Buffer buffer) {
        GLES20.glReadPixels(0, 0, width, height,
                GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, buffer);
        GlUtil.checkGlError("glReadPixels");
        buffer.rewind();
        Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        bmp.copyPixelsFromBuffer(buffer);
        return bmp;
    }
}
