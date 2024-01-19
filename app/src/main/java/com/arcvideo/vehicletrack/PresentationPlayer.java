package com.arcvideo.vehicletrack;

import android.content.Context;
import android.graphics.Point;
import android.media.MediaPlayer;
import android.util.Log;
import android.view.Display;
import android.widget.RelativeLayout;
import android.widget.VideoView;

import com.arcvideo.vehicletrack.presentation.ArcPresentation;

public class PresentationPlayer {
    private final String TAG = "PresentationPlayer";
    private VideoView videoView = null;
    private Context context;
    private Display display;
    private boolean isHDMI = false;
    private ArcPresentation arcPresentation = null;
    private int videoposition = 0;

    public PresentationPlayer(Context context, Display display, boolean isHDMI) {
        this.context = context;
        this.display = display;
        this.isHDMI = isHDMI;
        createPresentation();
        initVideoView();
    }

    private void createPresentation(){
        arcPresentation = new ArcPresentation(context, display);
        arcPresentation.setContentViewLayoutId(R.layout.video_display);
        arcPresentation.setPresentationRelativeLayoutId(R.id.display);
        arcPresentation.show();
    }

    private void initVideoView(){
        Log.d(TAG, "initSurfaceView: create "+(isHDMI? "HDMI":"DP")+" videoview and play video.");
        videoView = new VideoView(context);
        Point sizePoint1 = new Point();
        arcPresentation.getWindow().getWindowManager().getDefaultDisplay().getRealSize(sizePoint1);
        int nPresScreenWidth  = sizePoint1.x;
        int nPresScreenHeight = sizePoint1.y;
        Log.d(TAG, "initVideoView: Width is "+nPresScreenWidth+", Height is "+nPresScreenHeight);
        if (arcPresentation.getChildViewCount() == 0){
            arcPresentation.addChildView(videoView);
        }

        RelativeLayout.LayoutParams lParams1 = (RelativeLayout.LayoutParams)videoView.getLayoutParams();
        lParams1.width = nPresScreenWidth;
        lParams1.height = nPresScreenHeight;
        lParams1.topMargin = 0;
        lParams1.bottomMargin = 0;
        lParams1.leftMargin = 0;
        lParams1.rightMargin = 0;
        videoView.setLayoutParams(lParams1);
        videoView.setVideoURI(isHDMI? VideoDataSource.HDMIURI:VideoDataSource.DPURI);
        videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mediaPlayer) {
                // 默认循环播放
                mediaPlayer.setLooping(true);
                mediaPlayer.start();
            }
        });
    }

    //timestap 为主屏发送的视频播放时间戳
    public void syncVideo(int timestap){
        if (videoView.isPlaying()){
            int delaytime =  Math.abs(timestap - videoView.getCurrentPosition());
            if (VideoDataSource.SYNCLOG) {
                Log.d(TAG, "ID "+display.getDisplayId()+" Presentation SyncVideo: delay time is "+delaytime);
            }
            // 同步时间超过 300 毫秒，同步视频播放
            if (delaytime > 300){
                videoView.seekTo(timestap);
            }
        }
    }

    public void pause(){
        if (videoView != null){
            videoposition = videoView.getCurrentPosition();
            videoView.pause();
        }
        arcPresentation.dismiss();
        Log.d(TAG, "ID "+display.getDisplayId()+" presentation is pause");
    }

    public void restart(){
        if (videoView != null && !videoView.isPlaying()){
            arcPresentation.show();
            videoView.seekTo(videoposition);
            videoView.start();
        }
        Log.d(TAG, "ID "+display.getDisplayId()+" presentation is restart");
    }

    public void release(){
        if (videoView != null) videoView.stopPlayback();
        if (arcPresentation != null) arcPresentation.cancel();
        Log.d(TAG, "ID "+display.getDisplayId()+" presentation is release");
    }

    public Display getDisplay() {
        return display;
    }
}
