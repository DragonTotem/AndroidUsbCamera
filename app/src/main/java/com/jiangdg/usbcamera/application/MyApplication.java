package com.jiangdg.usbcamera.application;

import android.app.Application;

import com.jiangdg.usbcamera.UVCCameraHelper;
import com.jiangdg.usbcamera.utils.CrashHandler;
import com.serenegiant.utils.LogUtils;

/**
 * application class
 * <p>
 * Created by jianddongguo on 2017/7/20.
 */

public class MyApplication extends Application {
    private CrashHandler mCrashHandler;
    // File Directory in sd card
    public static final String DIRECTORY_NAME = "USBCamera";

    @Override
    public void onCreate() {
        super.onCreate();
        mCrashHandler = CrashHandler.getInstance();
        mCrashHandler.init(getApplicationContext(), getClass());

        LogUtils.logLevel(LogUtils.DEBUG_LEVEL_VERBOSE);
    }
}
