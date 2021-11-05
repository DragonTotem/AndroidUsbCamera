// IPcmDataListenerInterface.aidl
package com.jiangdg.usbcamera;
import com.jiangdg.usbcamera.IPcmDataInterface;

// Declare any non-default types here with import statements

interface IPcmDataListenerInterface {
    /**
     * Demonstrates some basic types that you can use as parameters
     * and return values in AIDL.
     */
    void basicTypes(int anInt, long aLong, boolean aBoolean, float aFloat,
            double aDouble, String aString);

   void registerPcmDataListener(IPcmDataInterface listener);
}