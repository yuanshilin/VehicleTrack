package com.arcvideo.vehicletrack;

import android.net.Uri;
import android.os.Environment;

import java.io.File;

public class VideoDataSource {
    public static final boolean SYNCLOG = false;
    // 网络流，用于测试
//    public static final Uri uri = Uri.parse("http://10.10.118.231:8080/TestVideo/720-1080/h264_differ_level/Valentine_Day_720P_h264_aac.mp4");
    public static final Uri MAINURI = Uri.parse(Environment.getExternalStorageDirectory()+ File.separator+"trackmode.mp4");
    public static final Uri HDMIURI = Uri.parse(Environment.getExternalStorageDirectory()+ File.separator+"trackmode.mp4");
    public static final Uri DPURI = Uri.parse(Environment.getExternalStorageDirectory()+ File.separator+"trackmode_hud.mp4");
}
