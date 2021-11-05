package com.jiangdg.usbcamera.view;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.os.IBinder;
import android.os.RemoteException;

import androidx.annotation.Nullable;

import com.jiangdg.usbcamera.IPcmDataInterface;
import com.jiangdg.usbcamera.IPcmDataListenerInterface;
import com.laifeng.sopcastsdk.audio.OnAudioRecordListener;
import com.laifeng.sopcastsdk.configuration.AudioConfiguration;
import com.laifeng.sopcastsdk.controller.audio.NormalAudioController;
import com.serenegiant.utils.LogUtils;

/**
 * @Description: UsbToBuiltinService
 * @Author: ZhuQuantao
 * @Date: 2021/11/4 4:18 下午
 * @Version: v1.0
 */
public class UsbToService extends Service {

    private IPcmDataInterface mPcmDataInterfaceListener;

    private IPcmDataListenerInterface mPcmDataInterface = new IPcmDataListenerInterface.Stub() {

        @Override
        public void basicTypes(int anInt, long aLong, boolean aBoolean, float aFloat, double aDouble, String aString) throws RemoteException {
        }

        @Override
        public void registerPcmDataListener(IPcmDataInterface listener) throws RemoteException {
            mPcmDataInterfaceListener = listener;
        }
    };

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mPcmDataInterface.asBinder();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        initUsbToBuiltin();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    // 监测外置mic pcm流
    private NormalAudioController usbAudioController;

    private void initUsbToBuiltin() {

        usbAudioController = new NormalAudioController();
        // 录内置mic的声音
        usbAudioController.setAudioRecordListener(new OnAudioRecordListener() {
            @Override
            public void audioRecord(byte[] bys, int length) {
                LogUtils.d("builtinMicAudioController length:" + length);
                if (null != mPcmDataInterface) {
                    try {
                        mPcmDataInterfaceListener.usbToBuiltinData(bys, length);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        AudioDeviceInfo[] inputDeviceInfo = audioManager.getDevices(AudioManager.GET_DEVICES_INPUTS);
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
        usbAudioController.start();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        usbAudioController.stop();
        super.onDestroy();
    }
}
