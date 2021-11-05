// IPcmDataInterface.aidl
package com.jiangdg.usbcamera;

// Declare any non-default types here with import statements

interface IPcmDataInterface {
    /**
     * Demonstrates some basic types that you can use as parameters
     * and return values in AIDL.
     */
    void basicTypes(int anInt, long aLong, boolean aBoolean, float aFloat,
            double aDouble, String aString);

    void usbToBuiltinData(in byte[] bys, int length);
}