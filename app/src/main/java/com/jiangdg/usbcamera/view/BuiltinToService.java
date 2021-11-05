package com.jiangdg.usbcamera.view;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.os.Environment;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import androidx.annotation.Nullable;

import com.jiangdg.usbcamera.IPcmDataInterface;
import com.jiangdg.usbcamera.IPcmDataListenerInterface;
import com.jiangdg.usbcamera.audio.AudioTracker;
import com.laifeng.sopcastsdk.audio.OnAudioRecordListener;
import com.laifeng.sopcastsdk.configuration.AudioConfiguration;
import com.laifeng.sopcastsdk.controller.audio.NormalAudioController;
import com.serenegiant.utils.LogUtils;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * @Description: UsbToBuiltinService
 * @Author: ZhuQuantao
 * @Date: 2021/11/4 4:18 下午
 * @Version: v1.0
 */
public class BuiltinToService extends Service {

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
        initFile();
        initUsbToBuiltin();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    private NormalAudioController builtinAudioController;

    private void initUsbToBuiltin() {

        builtinAudioController = new NormalAudioController();
        // 录内置mic的声音
        builtinAudioController.setAudioRecordListener(new OnAudioRecordListener() {
            @Override
            public void audioRecord(byte[] bys, int length) {
                LogUtils.d("builtinMicAudioController length:" + length);
                try {
                    for (int i = 0; i < length; i++) {
                        mDos.write(bys[i]);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
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
            if (deviceInfo.getType() == AudioDeviceInfo.TYPE_BUILTIN_MIC) {
                usbConfiguration.info = deviceInfo;
                break;
            }
        }
        if (null != usbConfiguration.info)
            LogUtils.d("builtinMicAudioController deviceId:" + usbConfiguration.info.getId());
        builtinAudioController.setAudioConfiguration(usbConfiguration);
        builtinAudioController.start();
    }


    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        builtinAudioController.stop();
        super.onDestroy();
    }

    /*************  存文件 ******/
    private DataOutputStream mDos;
    private File mRecordingFile;//储存AudioRecord录下来的文件
    private File mFileRoot = null;//文件目录
    //存放的目录路径名称
    private static final String mPathName = Environment.getExternalStorageDirectory().getAbsolutePath() + "/AudioRecordFile";
    //保存的音频文件名
    private static final String mFileName = "audioRecord.pcm";

    private void initFile() {
        //创建文件夹
        mFileRoot = new File(mPathName);
        if (!mFileRoot.exists())
            mFileRoot.mkdirs();

        //创建一个流，存放从AudioRecord读取的数据
        mRecordingFile = new File(mFileRoot, mFileName);
        if (mRecordingFile.exists()) {//音频文件保存过了删除
            mRecordingFile.delete();
        }
        try {
            mRecordingFile.createNewFile();//创建新文件
        } catch (IOException e) {
            e.printStackTrace();
            Log.e("BuiltinToService", "创建储存音频文件出错");
        }

        //获取到文件的数据流
        try {
            mDos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(mRecordingFile)));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }
}
