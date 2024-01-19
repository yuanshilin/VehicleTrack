package com.arcvideo.vehicletrack;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.RelativeLayout;
import android.widget.Toast;
import android.widget.VideoView;

import com.arcvideo.vehicletrack.bean.PresentationData;
import com.arcvideo.vehicletrack.presentation.ArcPresentation;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {
    private final String TAG = "VehicleTrackActivity";
    private final String HDMI_PLUGGED_BROADCAST = "android.intent.action.HDMI_PLUGGED";
    // 安卓低版本需要使用 android.intent.action.HDMISTATUS_CHANGED 来监听hdmi的插拔事件
    private final String HDMI_CHANGED_BROADCAST = "android.intent.action.HDMISTATUS_CHANGED";
    private final String DP_PLUGGED_BROADCAST = "android.intent.action.DP_PLUGGED";
    public static final int OVERLAY_REQUEST_RESULT_CODE = 222;
    public final int START_SYNC_THREAD = 223;
    // HDMI 插拔状态
    private boolean HDMI_PLUGGED = false;
    private boolean HDMI_FIRST_RECEIVE = true;
    // 记录 HDMI 数组下标
    private int HDMI_ORDER = -1;
    // DP 插拔状态
    private boolean DP_PLUGGED = false;
    private boolean DP_FIRST_RECEIVE = true;
    // 记录 DP 数组下标
    private int DP_ORDER = -1;
    private RelativeLayout relativeLayout;
    private VideoView videoView = null;
    private int videoposition = 0;
    private List<ScheduledFuture> scheduledFuturesList = new ArrayList<>();
    private List<PresentationPlayer> presentationList = new ArrayList<>();
    private DisplayPlugListener displayPlugListener = null;
    private Context mContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.video_display);

        //设置程序界面全屏
        setWindowFlag();

        // 注册 HDMI、DP接口插拔监听
        registerBroadCastReceiver();

        mContext = this;

        // 确保标准安卓
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_AUTOMOTIVE)){
            requestPermission();
        }
        if (!Settings.canDrawOverlays((this.getBaseContext()))){
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
            startActivityForResult(intent,OVERLAY_REQUEST_RESULT_CODE);
        }else{
            initialized();
        }
    }

    @Override
    protected void onActivityResult(int requestCode,int resultCode,Intent data){
        super.onActivityResult(requestCode,resultCode,data);
        if (requestCode == OVERLAY_REQUEST_RESULT_CODE){
            if (!Settings.canDrawOverlays(this.getBaseContext())){
                Toast.makeText(this.getBaseContext(),"授权失败",Toast.LENGTH_LONG).show();
            }else{
                initialized();
            }
        }
    }

    private void initialized(){
        // 延迟等待 HDMI、DP 接口广播监听
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                initMainVideoView();
                initPresentationVideoView();
                handler.sendEmptyMessage(START_SYNC_THREAD);
                HDMI_FIRST_RECEIVE = false;
                DP_FIRST_RECEIVE = false;
            }
        }, 300);
    }
    private void initMainVideoView(){
        relativeLayout = findViewById(R.id.display);
        if (videoView == null) {
            videoView = new VideoView(this);
        }
        if (relativeLayout.getChildCount() == 0){
            relativeLayout.addView(videoView);
        }
        Point sizePoint = new Point();
        getWindow().getWindowManager().getDefaultDisplay().getRealSize(sizePoint);
        int nScreenWidth = sizePoint.x;
        int nScreenHeight = sizePoint.y;
        RelativeLayout.LayoutParams lParams = (RelativeLayout.LayoutParams)videoView.getLayoutParams();
        lParams.width = nScreenWidth;
        lParams.height = nScreenHeight;
        lParams.topMargin = 0;
        lParams.bottomMargin = 0;
        lParams.leftMargin = 0;
        lParams.rightMargin = 0;
        videoView.setLayoutParams(lParams);
        videoView.setVideoURI(VideoDataSource.MAINURI);
        videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mediaPlayer) {
                mediaPlayer.setLooping(true);
                mediaPlayer.start();
            }
        });
    }

    private void initPresentationVideoView(){
        Display[] screens = ArcPresentation.getPresentationDisplays(this);
        judgeDisplayId(screens);
        Log.d(TAG, "initPresentationVideoView: HDMI is plugged:"+HDMI_PLUGGED+", HDMI order is "+HDMI_ORDER+
                ", DP is plugged:"+DP_PLUGGED+", DP order is "+DP_ORDER);
        if (HDMI_ORDER != -1){
            PresentationPlayer presentationPlayer = new PresentationPlayer(mContext, screens[HDMI_ORDER], true);
            presentationList.add(presentationPlayer);
        }
        if (DP_ORDER != -1) {
            PresentationPlayer presentationPlayer = new PresentationPlayer(mContext, screens[DP_ORDER], false);
        }
    }

    private void judgeDisplayId(Display[] screens){
        if (screens.length == 1) {
            if (HDMI_PLUGGED) HDMI_ORDER = 0;
            if (DP_PLUGGED && HDMI_ORDER == -1) DP_ORDER = 0;
        } else if (screens.length > 1){
            for (int i=0; i<screens.length; i++){
                Point point = new Point();
                screens[i].getRealSize(point);
                if (point.x > 3840){
                    if (DP_PLUGGED) DP_ORDER = i;
                }
            }
            if (DP_PLUGGED) {
                if (DP_ORDER == -1) {
                    DP_ORDER = screens.length -1;
                    HDMI_ORDER = 0;
                }else{
                    HDMI_ORDER = screens.length - 1 - DP_ORDER;
                }
            } else {
                HDMI_ORDER = 0;
                DP_ORDER = -1;
            }
        }
    }

    private void initSyncThread(){
        //未进行副屏同步播放视频时，无须创建同步线程
        for (PresentationPlayer presentationPlayer:presentationList){
            scheduledFuturesList.add(startSyncThread(presentationPlayer));
        }
    }

    private ScheduledFuture startSyncThread(final PresentationPlayer presentationPlayer){
        // 每 50ms 发送一次同步信号
        return (ScheduledFuture) Executors.newScheduledThreadPool(1, new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                if (VideoDataSource.SYNCLOG) {
                    Log.d(TAG, "initSyncThread: schedule thread is start");
                }
                return new Thread(r, "Presentation_Sync_Thread_"+presentationPlayer.getDisplay().getDisplayId());
            }
        }).scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                int timestap = videoView.getCurrentPosition();
                if (VideoDataSource.SYNCLOG) {
                    Log.d(TAG, "main screen SyncThread: SyncVideo to presentation ID "+
                            presentationPlayer.getDisplay().getDisplayId()+", for delay time "+timestap);
                }
                presentationPlayer.syncVideo(timestap);
            }
        }, 0, 50, TimeUnit.MICROSECONDS);
    }

    @Override
    protected void onPause() {
        super.onPause();
        pause();
    }

    private void pause(){
        if (videoView != null){
            // 注销视频播放同步线程，使用 Iterator ，迭代 List remove 会导致ArrayList ConcurrentModificationException
            Iterator<ScheduledFuture> iterator = scheduledFuturesList.iterator();
            while (iterator.hasNext()){
                ScheduledFuture scheduledFuture = iterator.next();
                scheduledFuture.cancel(true);
                iterator.remove();
            }
            videoposition = videoView.getCurrentPosition();
            videoView.pause();
        }
        for (PresentationPlayer presentationPlayer:presentationList){
            presentationPlayer.pause();
        }
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        restart();
    }

    private void restart(){
        if (videoView != null){
            videoView.seekTo(videoposition);
            videoView.start();
        }
        for (PresentationPlayer presentationPlayer:presentationList){
            presentationPlayer.restart();
        }
        // 重新初始化同步线程
        for (PresentationPlayer presentationPlayer:presentationList){
            scheduledFuturesList.add(startSyncThread(presentationPlayer));
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        onDestroy();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        destroy();
    }

    private void destroy(){
        // 注销视频播放同步线程，使用 Iterator ，迭代 List remove 会导致ArrayList ConcurrentModificationException
        Iterator<ScheduledFuture> iterator = scheduledFuturesList.iterator();
        while (iterator.hasNext()){
            ScheduledFuture scheduledFuture = iterator.next();
            scheduledFuture.cancel(true);
            iterator.remove();
        }
        if (displayPlugListener != null){
            unregisterReceiver(displayPlugListener);
        }
        for (PresentationPlayer presentationPlayer:presentationList){
            presentationPlayer.release();
        }
        if (videoView != null) {
            videoView.stopPlayback();
            videoView = null;
        }
    }

    private void registerBroadCastReceiver(){
        // android.intent.action.HDMI_PLUGGED：HDMI插拔广播
        // android.intent.action.DP_PLUGGED：DP插拔广播
        IntentFilter intentFilter = new IntentFilter(HDMI_PLUGGED_BROADCAST);
        intentFilter.addAction(DP_PLUGGED_BROADCAST);
        // 安卓低版本需要使用 android.intent.action.HDMISTATUS_CHANGED 来监听hdmi的插拔事件
        intentFilter.addAction(HDMI_CHANGED_BROADCAST);
        displayPlugListener = new DisplayPlugListener();
        registerReceiver(displayPlugListener, intentFilter);
    }

    private class DisplayPlugListener extends BroadcastReceiver {
        private final String TAG = "DisplayPlugListener";
        private final String PORT_PLUG_STATE = "state";
        private final String EXTRA_MULTI_DP_PLUGGED_NAME = "extcon_name";
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "DisplayPlugListener onReceive: get broadcast info is "+intent.getAction());
            boolean plug_state = intent.getBooleanExtra(PORT_PLUG_STATE, false);
            switch (intent.getAction()){
                case HDMI_PLUGGED_BROADCAST:
                    HDMI_PLUGGED = plug_state;
                    // 针对粘性广播，忽略初始化值
                    if (HDMI_FIRST_RECEIVE){
                        HDMI_FIRST_RECEIVE = false;
                        return;
                    }
                    Log.d(TAG, "DisplayPlugListener onReceive: HDMI device is "+(plug_state? "plugged":"remove"));
                    dealDisplayPort(plug_state, true,plug_state? 5000:200);
                    break;
                case DP_PLUGGED_BROADCAST:
                    DP_PLUGGED = plug_state;
                    // 针对粘性广播，忽略初始化值
                    if (DP_FIRST_RECEIVE){
                        DP_FIRST_RECEIVE = false;
                        return;
                    }
                    String Dp_Name = intent.getStringExtra(EXTRA_MULTI_DP_PLUGGED_NAME);
                    Log.d(TAG, "DisplayPlugListener onReceive: DP "+Dp_Name+" device is "+(plug_state? "plugged":"remove"));
                    dealDisplayPort(plug_state, false,plug_state? 9000:200);
                    break;
            }

        }
    }

    private synchronized void dealDisplayPort(boolean plugged, boolean IsHdmi, int delaytime){
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(delaytime);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                // 无法在线程里面创建同步线程，所以序列化 PresentationData 对象并以消息队列的方式通知设备插拔情况。
                Bundle bundle = new Bundle();
                bundle.putSerializable("data", new PresentationData(plugged, IsHdmi));
                handler.sendMessage(handler.obtainMessage(1, bundle));

                if (!plugged && !IsHdmi){
                    try {
                        Thread.sleep(6000);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    // DP拔出后，Activity 进入 onStop
                    Log.d(TAG, "DisplayPlugListener onReceive: DP is remove, restart activity");
                    startActivity(getPackageManager().getLaunchIntentForPackage(getPackageName()));
                }
            }
        }).start();
    }

    private Handler handler = new Handler(){
        @Override
        public void handleMessage(@NonNull Message msg) {
            if (msg.what == START_SYNC_THREAD){
                initSyncThread();
            }else{
                PresentationData presentationData = (PresentationData)((Bundle)msg.obj).get("data");
                CreatePresentation(presentationData.isPlugged(), presentationData.isHdmi());
            }
        }
    };

    private void CreatePresentation(final boolean plugged, final boolean IsHdmi){
        if (plugged){
            Display[] screens = ArcPresentation.getPresentationDisplays(getApplicationContext());
            Log.d(TAG, "DisplayPlugListener onReceive: display devices count is "+screens.length+
                    ", current display devices count is "+presentationList.size());
            if (screens.length > presentationList.size()){
                if (IsHdmi){
                    HDMI_ORDER = presentationList.size();
                }else{
                    DP_ORDER = presentationList.size();
                }

                // 生成副屏 Presentation 并播放视频
                PresentationPlayer presentationPlayer =
                        new PresentationPlayer(mContext, screens[IsHdmi? HDMI_ORDER:DP_ORDER], IsHdmi);
                presentationList.add(presentationPlayer);

                // 接入 HDMI 或 DP 后，主 activity 进入 onResume()状态
                restart();
                Log.d(TAG, "DisplayPlugListener onReceive: create "+(IsHdmi?"HDMI":"DP")+" device");
            }
        } else {
            if (presentationList.size() > 0){
                int position = IsHdmi? HDMI_ORDER:DP_ORDER;
                if (position != -1){
                    PresentationPlayer presentationPlayer = presentationList.get(position);
                    presentationPlayer.release();
                    presentationList.remove(presentationPlayer);
                    if (position == 0){
                        if (IsHdmi){
                            HDMI_ORDER = -1;
                            if (DP_ORDER != -1){
                                DP_ORDER--;
                            }
                        }else{
                            DP_ORDER = -1;
                            if (HDMI_ORDER != -1){
                                HDMI_ORDER--;
                            }
                        }
                    }else{
                        if (IsHdmi) {
                            HDMI_ORDER = -1;
                        }else{
                            DP_ORDER = -1;
                        }
                    }
                    Log.d(TAG, "DisplayPlugListener onReceive: remove "+(IsHdmi?"HDMI":"DP")+" device, position is "+position);
                }
            }
        }
    }

    private void setWindowFlag(){
        Window window = getWindow();
        View decorView = window.getDecorView();
        int flag = decorView.getSystemUiVisibility();

        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        // 状态栏隐藏
        flag |= View.SYSTEM_UI_FLAG_FULLSCREEN;
        // 导航栏隐藏
        flag |= View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
        // 布局延伸到导航栏
        flag |= View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;
        // 布局延伸到状态栏
        flag |= View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
        // 全屏时,增加沉浸式体验
        flag |= View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        //  部分国产机型适用.不加会导致退出全屏时布局被状态栏遮挡
        // activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        // android P 以下的刘海屏,各厂商都有自己的适配方式,具体在manifest.xml中可以看到
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            WindowManager.LayoutParams pa = window.getAttributes();
            pa.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            window.setAttributes(pa);
        }
        decorView.setSystemUiVisibility(flag);
    }

    private void requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // 先判断有没有权限
            if (Environment.isExternalStorageManager()) {
                //已经授予了读写外置存储权限，可以直接进行读写文件操作
            } else {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.setData(Uri.parse("package:" + this.getPackageName()));
                startActivityForResult(intent, 1024);
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // 先判断有没有权限
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                //已经授予了读写外置存储权限，可以直接进行读写文件操作
            } else {
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1024);
            }
        } else {
            //已经授予了读写外置存储权限，可以直接进行读写文件操作
        }
    }
}
