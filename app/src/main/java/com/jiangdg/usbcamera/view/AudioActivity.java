package com.jiangdg.usbcamera.view;

import android.content.Context;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.jiangdg.usbcamera.R;
import com.jiangdg.usbcamera.audio.AudioTracker;
import com.jiangdg.usbcamera.utils.FileUtils;
import com.jiangdg.usbcamera.utils.ToastUtil;
import com.laifeng.sopcastsdk.audio.OnAudioRecordListener;
import com.laifeng.sopcastsdk.configuration.AudioConfiguration;
import com.laifeng.sopcastsdk.controller.audio.NormalAudioController;
import com.serenegiant.utils.LogUtils;

import java.io.IOException;

/**
 * @Description: MediaActivity
 * @Author: ZhuQuantao
 * @Date: 2021/11/1 5:34 下午
 * @Version: v1.0
 */
public class AudioActivity extends AppCompatActivity {

    MediaPlayer mediaPlayer;
    MediaPlayer usbMediaPlayer;

    @RequiresApi(api = Build.VERSION_CODES.P)
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio);
        LogUtils.logLevel(5);
        LogUtils.d();

        initMediaPlayer();

        findViewById(R.id.start_player).setOnClickListener(view -> {
            prepareMedia();
            mediaPlayer.start();
            usbMediaPlayer.start();
        });
        findViewById(R.id.tv_stop_player).setOnClickListener(view -> {
            mediaPlayer.stop();
            usbMediaPlayer.stop();
        });
        findViewById(R.id.tv_start_camera).setOnClickListener(view -> {
            startActivity(new Intent(AudioActivity.this, USBCameraActivity.class));
        });
        findViewById(R.id.tv_start_builtin_camera).setOnClickListener(view -> {
            startActivity(new Intent(AudioActivity.this, CustomCameraActivity.class));
        });

        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        AudioDeviceInfo[] devicesInfo = audioManager.getDevices(AudioManager.GET_DEVICES_INPUTS);
        LogUtils.d("input devicesInfo Count:" + devicesInfo.length);
        for (AudioDeviceInfo deviceInfo : devicesInfo) {
            LogUtils.d("info: " + deviceInfo.toString());
            LogUtils.d("address: " + deviceInfo.getAddress());
            LogUtils.d("projectName: " + deviceInfo.getProductName());
            LogUtils.d("id: " + deviceInfo.getId());
            LogUtils.d("type: " + deviceInfo.getType());
            LogUtils.d("-----------------------------------------------------------");
        }

        AudioDeviceInfo[] devicesInfo2 = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS);
        LogUtils.w("output devicesInfo Count:" + devicesInfo2.length);
        for (AudioDeviceInfo deviceInfo : devicesInfo2) {
            LogUtils.w("info: " + deviceInfo.toString());
            LogUtils.w("address: " + deviceInfo.getAddress());
            LogUtils.w("projectName: " + deviceInfo.getProductName());
            LogUtils.w("id: " + deviceInfo.getId());
            LogUtils.w("type: " + deviceInfo.getType());
            LogUtils.w("-----------------------------------------------------------");
            if (deviceInfo.getType() == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER) {
                mediaPlayer.setPreferredDevice(deviceInfo);
            }
        }

        AudioDeviceInfo[] devicesInfo3 = audioManager.getDevices(AudioManager.GET_DEVICES_ALL);
        LogUtils.i("all devicesInfo Count:" + devicesInfo3.length);
        for (AudioDeviceInfo deviceInfo : devicesInfo3) {
            LogUtils.i("info: " + deviceInfo.toString());
            LogUtils.i("address: " + deviceInfo.getAddress());
            LogUtils.i("projectName: " + deviceInfo.getProductName());
            LogUtils.i("id: " + deviceInfo.getId());
            LogUtils.i("type: " + deviceInfo.getType());
            LogUtils.i("-----------------------------------------------------------");
        }

        initAudioRecord();
    }

    private void initMediaPlayer() {
        mediaPlayer = new MediaPlayer();
        usbMediaPlayer = new MediaPlayer();
    }

    private void prepareMedia() {
        AssetFileDescriptor afd;
        try {
            afd = getAssets().openFd("mic/xyf.mp3");
            mediaPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            mediaPlayer.prepareAsync();

            usbMediaPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            usbMediaPlayer.prepareAsync();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /***********************  音频部分 ************************/

    // 音频输入源切换 true 内录外播，false 外录内播
    private boolean builtinInput = true;

    // 监测外置mic pcm流
    private NormalAudioController usbAudioController;
    // 监测手机内置mic pcm流
    private NormalAudioController builtinMicAudioController;

    // 外置扬声器
    private AudioTracker usbAudioTracker;
    // 内置speaker播放
    private AudioTracker builtinSpeakerAudioTracker;

    private void initAudioRecord() {
        initAudioTracker();
        initAudioController();
        setRecordConfig();

        TextView audioRecordTv = findViewById(R.id.tv_audio_record);
        audioRecordTv.setOnClickListener(v -> {
            setBuiltinToUsb();
            ToastUtil.showShortToast(AudioActivity.this, "已开始内录外播");
        });
        findViewById(R.id.tv_stop_audio_record).setOnClickListener(v -> {
            setUsbToBuiltin();
            ToastUtil.showShortToast(AudioActivity.this, "已开始外录内播");
        });
        findViewById(R.id.tv_player_all).setOnClickListener(v -> {
            usbAudioTracker.start();
            builtinSpeakerAudioTracker.start();
            ToastUtil.showShortToast(AudioActivity.this, "开启双路播放");
        });
        findViewById(R.id.tv_stop_player_all).setOnClickListener(v -> {
            usbAudioTracker.stop();
            builtinSpeakerAudioTracker.stop();
            ToastUtil.showShortToast(AudioActivity.this, "已停止双路播放");
        });
        findViewById(R.id.tv_stop_communication).setOnClickListener(v -> {
            usbAudioController.stop();
            builtinMicAudioController.stop();
            usbAudioTracker.stop();
            builtinSpeakerAudioTracker.stop();
            ToastUtil.showShortToast(AudioActivity.this, "已停止通讯");
        });
        findViewById(R.id.tv_start_communication).setOnClickListener(v -> {
            startCommunication();
            ToastUtil.showShortToast(AudioActivity.this, "已经开启通信");
        });
    }

    private void startCommunication() {
        builtinMicAudioController.start();
        usbAudioController.start();
    }

    private void setUsbToBuiltin() {
        builtinSpeakerAudioTracker = new AudioTracker();
        builtinSpeakerAudioTracker.createAudioTrack(this, "sample.pcm");

        usbAudioController = new NormalAudioController();
        // 录内置mic的声音
        usbAudioController.setAudioRecordListener(new OnAudioRecordListener() {
            @Override
            public void audioRecord(byte[] bys, int length) {
                LogUtils.d("builtinMicAudioController length:" + length);
                builtinSpeakerAudioTracker.playAudioData(bys, length);
            }
        });

        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        AudioDeviceInfo[] inputDeviceInfo = audioManager.getDevices(AudioManager.GET_DEVICES_INPUTS);
        AudioDeviceInfo[] outputDeviceInfo = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS);

        AudioConfiguration usbConfiguration = AudioConfiguration.createDefault();
        for (AudioDeviceInfo deviceInfo : inputDeviceInfo) {
            if (deviceInfo.getType() == AudioDeviceInfo.TYPE_USB_DEVICE) {
                usbConfiguration.info = deviceInfo;
                break;
            }
        }
        if (null != usbConfiguration.info)
            LogUtils.d("builtinMicAudioController deviceId:" + usbConfiguration.info.getId());
        usbAudioController.setAudioConfiguration(usbConfiguration);

        for (AudioDeviceInfo deviceInfo : outputDeviceInfo) {
            if (deviceInfo.getType() == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER) {
                LogUtils.d("usbAudioTracker deviceId:" + deviceInfo.getId());
                builtinSpeakerAudioTracker.setAudioDeviceInfo(deviceInfo);
                break;
            }
        }
        usbAudioController.start();
    }

    private void setBuiltinToUsb() {
        usbAudioTracker = new AudioTracker();
        usbAudioTracker.createAudioTrack(this, "sample.pcm");

        builtinMicAudioController = new NormalAudioController();
        // 录内置mic的声音
        builtinMicAudioController.setAudioRecordListener(new OnAudioRecordListener() {
            @Override
            public void audioRecord(byte[] bys, int length) {
                LogUtils.d("builtinMicAudioController length:" + length);
                usbAudioTracker.playAudioData(bys, length);
            }
        });

        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        AudioDeviceInfo[] inputDeviceInfo = audioManager.getDevices(AudioManager.GET_DEVICES_INPUTS);
        AudioDeviceInfo[] outputDeviceInfo = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS);

        AudioConfiguration builtinConfiguration = AudioConfiguration.createDefault();
        for (AudioDeviceInfo deviceInfo : inputDeviceInfo) {
            if (deviceInfo.getType() == AudioDeviceInfo.TYPE_BUILTIN_MIC) {
                builtinConfiguration.info = deviceInfo;
                break;
            }
        }
        if (null != builtinConfiguration.info)
            LogUtils.d("builtinMicAudioController deviceId:" + builtinConfiguration.info.getId());
        builtinMicAudioController.setAudioConfiguration(builtinConfiguration);

        for (AudioDeviceInfo deviceInfo : outputDeviceInfo) {
            if (deviceInfo.getType() == AudioDeviceInfo.TYPE_USB_DEVICE) {
                LogUtils.d("usbAudioTracker deviceId:" + deviceInfo.getId());
                usbAudioTracker.setAudioDeviceInfo(deviceInfo);
                break;
            }
        }

        builtinMicAudioController.start();
    }

    private void setRecordConfig() {
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        AudioDeviceInfo[] inputDeviceInfo = audioManager.getDevices(AudioManager.GET_DEVICES_INPUTS);
        AudioDeviceInfo[] outputDeviceInfo = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS);

        AudioConfiguration usbConfiguration = AudioConfiguration.createDefault();
        AudioConfiguration builtinConfiguration = AudioConfiguration.createDefault();

        for (AudioDeviceInfo deviceInfo : inputDeviceInfo) {
            if (deviceInfo.getType() == AudioDeviceInfo.TYPE_USB_DEVICE) {
                usbConfiguration.info = deviceInfo;
                break;
            }
        }
        for (AudioDeviceInfo deviceInfo : inputDeviceInfo) {
            if (deviceInfo.getType() == AudioDeviceInfo.TYPE_BUILTIN_MIC) {
                builtinConfiguration.info = deviceInfo;
                break;
            }
        }
        if (null != usbConfiguration.info)
            LogUtils.d("usbAudioController deviceId:" + usbConfiguration.info.getId());
        if (null != builtinConfiguration.info)
            LogUtils.d("builtinMicAudioController deviceId:" + builtinConfiguration.info.getId());
        usbAudioController.setAudioConfiguration(usbConfiguration);
        builtinMicAudioController.setAudioConfiguration(builtinConfiguration);

        for (AudioDeviceInfo deviceInfo : outputDeviceInfo) {
            if (deviceInfo.getType() == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER) {
                LogUtils.d("builtinSpeakerAudioTracker deviceId:" + deviceInfo.getId());
                builtinSpeakerAudioTracker.setAudioDeviceInfo(deviceInfo);
                break;
            }
        }

        for (AudioDeviceInfo deviceInfo : outputDeviceInfo) {
            if (deviceInfo.getType() == AudioDeviceInfo.TYPE_USB_DEVICE) {
                LogUtils.d("usbAudioTracker deviceId:" + deviceInfo.getId());
                usbAudioTracker.setAudioDeviceInfo(deviceInfo);
                break;
            }
        }
    }

    public void initAudioController() {
        usbAudioController = new NormalAudioController();
        builtinMicAudioController = new NormalAudioController();

        // 录USB设备声音
        usbAudioController.setAudioRecordListener(new OnAudioRecordListener() {
            @Override
            public void audioRecord(byte[] bys, int length) {
                builtinSpeakerAudioTracker.playAudioData(bys, length);
                LogUtils.d("audioController length:" + length);
            }
        });

        // 录内置mic的声音
        builtinMicAudioController.setAudioRecordListener(new OnAudioRecordListener() {
            @Override
            public void audioRecord(byte[] bys, int length) {
                LogUtils.d("builtinMicAudioController length:" + length);
                usbAudioTracker.playAudioData(bys, length);
            }
        });
    }

    public void initAudioTracker() {
        builtinSpeakerAudioTracker = new AudioTracker();
        builtinSpeakerAudioTracker.createAudioTrack(this, "sample2.pcm");

        usbAudioTracker = new AudioTracker();
        usbAudioTracker.createAudioTrack(this, "sample.pcm");
    }

    /**
     * 开始录音
     */
    private void startRecordAndPlay() {
        if (builtinInput) {
            usbAudioController.stop();
            builtinMicAudioController.start();
        } else {
            builtinMicAudioController.stop();
            usbAudioController.start();
        }
    }

    /**
     * 停止音频操作
     */
    private void stopRecordAndPlay() {
        usbAudioController.stop();
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
    protected void onDestroy() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
            mediaPlayer = null;
        }
        super.onDestroy();
        FileUtils.releaseFile();
        // step.4 release uvc camera resources
        stopRecordAndPlay();
    }
}
