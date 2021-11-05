package com.laifeng.sopcastsdk.audio;

/**
 * @Description: OnAudioRecordListener
 * @Author: ZhuQuantao
 * @Date: 2021/11/4 9:47 上午
 * @Version: v1.0
 */
public interface OnAudioRecordListener {
    void audioRecord(byte[] bys, int length);
}
