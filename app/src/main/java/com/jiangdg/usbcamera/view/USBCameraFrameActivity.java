package com.jiangdg.usbcamera.view;

import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.media.MediaCodec;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.jiangdg.usbcamera.R;
import com.jiangdg.usbcamera.UVCCameraHelper;
import com.jiangdg.usbcamera.audio.AudioTracker;
import com.jiangdg.usbcamera.utils.FileUtils;
import com.laifeng.sopcastsdk.audio.OnAudioEncodeListener;
import com.laifeng.sopcastsdk.audio.OnAudioRecordListener;
import com.laifeng.sopcastsdk.configuration.AudioConfiguration;
import com.laifeng.sopcastsdk.controller.audio.NormalAudioController;
import com.laifeng.sopcastsdk.stream.packer.Packer;
import com.laifeng.sopcastsdk.stream.packer.flv.FlvPacker;
import com.serenegiant.usb.CameraDialog;
import com.serenegiant.usb.USBMonitor;
import com.serenegiant.usb.common.AbstractUVCCameraHandler;
import com.serenegiant.usb.widget.CameraViewInterface;
import com.serenegiant.utils.LogUtils;

import java.io.File;
import java.nio.ByteBuffer;

import butterknife.BindView;
import butterknife.ButterKnife;

public class USBCameraFrameActivity extends AppCompatActivity implements CameraDialog.CameraDialogParent, CameraViewInterface.Callback {
    private static final String TAG = "Debug";
    @BindView(R.id.camera_view)
    public View mTextureView;

    private UVCCameraHelper mCameraHelper;
    private CameraViewInterface mUVCCameraView;

    private boolean isRequest;
    private boolean isPreview;
    private static final int REQUEST_CODE = 1;

    // 监测摄像头mic pcm流
    private NormalAudioController audioController;
    // 监测手机内置mic pcm流
    private NormalAudioController builtinMicAudioController;

    // 内置speaker播放
    private AudioTracker builtinSpeakerAudioTracker;
    private AudioTracker usbAudioTracker;

    private UVCCameraHelper.OnMyDevConnectListener listener = new UVCCameraHelper.OnMyDevConnectListener() {

        @Override
        public void onAttachDev(UsbDevice device) {
            showShortMsg("request: " + isRequest);
            // request open permission
            if (!isRequest) {
                isRequest = true;
                if (mCameraHelper != null) {
                    mCameraHelper.requestPermission(0);
                }
            }
        }

        @Override
        public void onDettachDev(UsbDevice device) {
            // close camera
            if (isRequest) {
                isRequest = false;
                mCameraHelper.closeCamera();
                showShortMsg(device.getDeviceName() + " is out");
            }
        }

        @Override
        public void onConnectDev(UsbDevice device, boolean isConnected) {
            if (!isConnected) {
                showShortMsg("fail to connect,please check resolution params");
                isPreview = false;
            } else {
                isPreview = true;
                showShortMsg("connecting");
                // initialize seekbar
                // need to wait UVCCamera initialize over
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Thread.sleep(2500);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        Looper.prepare();
                        if (mCameraHelper != null && mCameraHelper.isCameraOpened()) {
                        }
                        Looper.loop();
                    }
                }).start();
            }
        }

        @Override
        public void onDisConnectDev(UsbDevice device) {
            showShortMsg("disconnecting");
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_usbcamera_frame);
        ButterKnife.bind(this);

        initAudioTracker();
        initUVCCameraView();
        initAudioController();
    }

    private void initUVCCameraView() {
        // step.1 initialize UVCCameraHelper
        mUVCCameraView = (CameraViewInterface) mTextureView;
        mUVCCameraView.setCallback(this);
        mCameraHelper = UVCCameraHelper.getInstance();
        mCameraHelper.setDefaultFrameFormat(UVCCameraHelper.FRAME_FORMAT_MJPEG);
        mCameraHelper.initUSBMonitor(this, mUVCCameraView, listener);

        mCameraHelper.setOnPreviewFrameListener(new AbstractUVCCameraHandler.OnPreViewResultListener() {
            @Override
            public void onPreviewResult(byte[] nv21Yuv) {
//                Log.d(TAG, "onPreviewResult: " + nv21Yuv.length);
            }
        });
    }

    public void initAudioController() {
        audioController = new NormalAudioController();
        builtinMicAudioController = new NormalAudioController();

        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        AudioDeviceInfo[] devicesInfo = audioManager.getDevices(AudioManager.GET_DEVICES_ALL);

        AudioConfiguration configuration = AudioConfiguration.createDefault();
        AudioConfiguration builtinConfiguration = AudioConfiguration.createDefault();

        for (AudioDeviceInfo deviceInfo : devicesInfo) {
            if (deviceInfo.getType() == AudioDeviceInfo.TYPE_USB_DEVICE) {
                configuration.info = deviceInfo;
                break;
            }
        }
        for (AudioDeviceInfo deviceInfo : devicesInfo) {
            if (deviceInfo.getType() == AudioDeviceInfo.TYPE_BUILTIN_MIC) {
                builtinConfiguration.info = deviceInfo;
                break;
            }
        }

        // 录USB设备声音
        audioController.setAudioRecordListener(new OnAudioRecordListener() {
            @Override
            public void audioRecord(byte[] bys, int length) {
                builtinSpeakerAudioTracker.playAudioData(bys, length);
                LogUtils.d("audioController length:" + length);
            }
        });
        audioController.setAudioConfiguration(configuration);
        audioController.start();

        // 录内置mic的声音
        builtinMicAudioController.setAudioRecordListener(new OnAudioRecordListener() {
            @Override
            public void audioRecord(byte[] bys, int length) {
                LogUtils.d("builtinMicAudioController length:" + length);
                usbAudioTracker.playAudioData(bys, length);
            }
        });
        builtinMicAudioController.setAudioConfiguration(builtinConfiguration);
        builtinMicAudioController.start();
    }

    public void initAudioTracker() {
        builtinSpeakerAudioTracker = new AudioTracker();
        builtinSpeakerAudioTracker.createAudioTrack(this, "sample.pcm");
        builtinSpeakerAudioTracker.setAudioPlayListener(new AudioTracker.AudioPlayListener() {
            @Override
            public void onStart() {
                LogUtils.d("onStart");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(USBCameraFrameActivity.this, "播放开始", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onStop() {
                LogUtils.d("onStop");
                builtinSpeakerAudioTracker.release();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(USBCameraFrameActivity.this, "播放结束", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onError(String message) {
                LogUtils.w("onError: {} " + message);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(USBCameraFrameActivity.this, "播放错误 " + message, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

        usbAudioTracker = new AudioTracker();
        usbAudioTracker.createAudioTrack(this, "sample.pcm");

        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        AudioDeviceInfo[] devicesInfo = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS);

        for (AudioDeviceInfo deviceInfo : devicesInfo) {
            if (deviceInfo.getType() == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER) {
                builtinSpeakerAudioTracker.setAudioDeviceInfo(deviceInfo);
                break;
            }
        }

        for (AudioDeviceInfo deviceInfo : devicesInfo) {
            if (deviceInfo.getType() == AudioDeviceInfo.TYPE_USB_DEVICE) {
                usbAudioTracker.setAudioDeviceInfo(deviceInfo);
                break;
            }
        }

        usbAudioTracker.setAudioPlayListener(new AudioTracker.AudioPlayListener() {
            @Override
            public void onStart() {
                LogUtils.d("onStart");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(USBCameraFrameActivity.this, "播放开始", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onStop() {
                LogUtils.d("onStop");
                usbAudioTracker.release();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(USBCameraFrameActivity.this, "播放结束", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onError(String message) {
                LogUtils.w("onError: {} " + message);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(USBCameraFrameActivity.this, "播放错误 " + message, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    private void stopAudio() {
        audioController.stop();
        builtinMicAudioController.stop();

        if (builtinSpeakerAudioTracker != null) {
            try {
                builtinSpeakerAudioTracker.stop();
            } catch (IllegalStateException e) {
                e.printStackTrace();
            }
        }
        if (usbAudioTracker != null) {
            try {
                usbAudioTracker.stop();
            } catch (IllegalStateException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        // step.2 register USB event broadcast
        if (mCameraHelper != null) {
            mCameraHelper.registerUSB();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        // step.3 unregister USB event broadcast
        if (mCameraHelper != null) {
            mCameraHelper.unregisterUSB();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        FileUtils.releaseFile();
        // step.4 release uvc camera resources
        if (mCameraHelper != null) {
            mCameraHelper.release();
        }
        stopAudio();
    }

    private void showShortMsg(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    public USBMonitor getUSBMonitor() {
        return mCameraHelper.getUSBMonitor();
    }

    @Override
    public void onDialogResult(boolean canceled) {
        if (canceled) {
            showShortMsg("取消操作");
        }
    }

    public boolean isCameraOpened() {
        return mCameraHelper.isCameraOpened();
    }

    @Override
    public void onSurfaceCreated(CameraViewInterface view, Surface surface) {
        if (!isPreview && mCameraHelper.isCameraOpened()) {
            mCameraHelper.startPreview(mUVCCameraView);
            isPreview = true;
        }
    }

    @Override
    public void onSurfaceChanged(CameraViewInterface view, Surface surface, int width, int height) {
    }

    @Override
    public void onSurfaceDestroy(CameraViewInterface view, Surface surface) {
        if (isPreview && mCameraHelper.isCameraOpened()) {
            mCameraHelper.stopPreview();
            isPreview = false;
        }
    }
}
