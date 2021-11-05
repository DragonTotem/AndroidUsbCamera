package com.jiangdg.usbcamera.view;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.os.RemoteException;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.jiangdg.usbcamera.IPcmDataInterface;
import com.jiangdg.usbcamera.IPcmDataListenerInterface;
import com.jiangdg.usbcamera.R;
import com.jiangdg.usbcamera.audio.AudioTrackManager;
import com.jiangdg.usbcamera.audio.AudioTracker;
import com.jiangdg.usbcamera.utils.ToastUtil;

import java.io.File;

/**
 * @Description: StreamDecodeActivity
 * @Author: ZhuQuantao
 * @Date: 2021/11/3 2:44 下午
 * @Version: v1.0
 */
public class StreamDecodeActivity extends AppCompatActivity {

    private static final String TAG = "StreamDecodeActivity";
    private AudioTracker mAudioTracker;

    private boolean mBuiltinPlayPcmData = false;
    private boolean mUsbPlayPcmData = false;

    private IPcmDataListenerInterface mBuiltinToBinder;
    private IPcmDataInterface mBuiltinToListener = new IPcmDataInterface.Stub() {
        @Override
        public void basicTypes(int anInt, long aLong, boolean aBoolean, float aFloat, double aDouble, String aString) throws RemoteException {
        }

        @Override
        public void usbToBuiltinData(byte[] bys, int length) throws RemoteException {
            if (mUsbPlayPcmData) {
                AudioTrackManager.getInstance().playAudio(bys, length);
            }
        }
    };

    private IPcmDataListenerInterface mUsbToBinder;
    private IPcmDataInterface mUsbToListener = new IPcmDataInterface.Stub() {
        @Override
        public void basicTypes(int anInt, long aLong, boolean aBoolean, float aFloat, double aDouble, String aString) throws RemoteException {
        }

        @Override
        public void usbToBuiltinData(byte[] bys, int length) throws RemoteException {
            if (mBuiltinPlayPcmData) {
                mAudioTracker.playAudioData(bys, length);
            }
        }
    };

    private ServiceConnection mBuiltinServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            mBuiltinToBinder = IPcmDataListenerInterface.Stub.asInterface(iBinder);
            if (null != mBuiltinToListener) {
                try {
                    mBuiltinToBinder.registerPcmDataListener(mBuiltinToListener);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBuiltinToBinder = null;
        }
    };

    private ServiceConnection mUsbServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            mUsbToBinder = IPcmDataListenerInterface.Stub.asInterface(iBinder);
            if (null != mUsbToListener) {
                try {
                    mUsbToBinder.registerPcmDataListener(mUsbToListener);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mUsbToBinder = null;
        }
    };

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stream_decode);

        initView();
        initAudioTracker();
    }

    private void initView() {
        findViewById(R.id.tv_play).setOnClickListener(v -> {
            mBuiltinPlayPcmData = true;
            ToastUtil.showShortToast(StreamDecodeActivity.this, "喇叭播放");
        });
        findViewById(R.id.tv_usb_player).setOnClickListener(v -> {
            mUsbPlayPcmData = true;
            ToastUtil.showShortToast(StreamDecodeActivity.this, "usb喇叭播放");
        });
        findViewById(R.id.tv_stop_record).setOnClickListener(v -> {
            mUsbPlayPcmData = false;
            mBuiltinPlayPcmData = false;
//            stopAudio();
            ToastUtil.showShortToast(StreamDecodeActivity.this, "停止播放");
        });
        findViewById(R.id.tv_connect_service).setOnClickListener(v -> {
            Intent intent = new Intent(StreamDecodeActivity.this, UsbToService.class);
            bindService(intent, mUsbServiceConnection, Context.BIND_AUTO_CREATE);
            bindService(new Intent(StreamDecodeActivity.this, BuiltinToService.class),
                    mBuiltinServiceConnection, Context.BIND_AUTO_CREATE);
            ToastUtil.showShortToast(StreamDecodeActivity.this, "开启服务进程录音");
        });
        findViewById(R.id.tv_play_pcm).setOnClickListener(v -> {
            AudioTrackManager.getInstance().startPlay(Environment.getExternalStorageDirectory().getAbsolutePath() +
                    "/AudioRecordFile/audioRecord.pcm");
            ToastUtil.showShortToast(StreamDecodeActivity.this, "开始播放语音");
        });
    }

    public void initAudioTracker() {

        mAudioTracker = new AudioTracker();
        mAudioTracker.createAudioTrack(this, "sample.pcm");

        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        AudioDeviceInfo[] outputDeviceInfo = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS);
        for (AudioDeviceInfo deviceInfo : outputDeviceInfo) {
            if (deviceInfo.getType() == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER) {
                mAudioTracker.setAudioDeviceInfo(deviceInfo);
                break;
            }
        }
        for (AudioDeviceInfo deviceInfo : outputDeviceInfo) {
            if (deviceInfo.getType() == AudioDeviceInfo.TYPE_USB_DEVICE) {
                AudioTrackManager.getInstance().setAudioDeviceInfo(deviceInfo);
                break;
            }
        }
    }

    private void stopAudio() {
        if (mAudioTracker != null) {
            try {
                mAudioTracker.stop();
            } catch (IllegalStateException e) {
                e.printStackTrace();
            }
        }
        AudioTrackManager.getInstance().stopPlay();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        if (mUsbToBinder != null)
            unbindService(mUsbServiceConnection);
        if (mBuiltinToBinder != null)
            unbindService(mBuiltinServiceConnection);
        super.onDestroy();
        stopAudio();
    }
}
