package com.arcvideo.vehicletrack.arccontrol;

import android.content.Context;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ArcControlCenter {
    private final String TAG = "VehicleTrackActivity";

    private Context mContext;

    private Endpoint endpoint = null;
    private List<Program> programList = new ArrayList<>();
    private String TargetProgramName = "赛道模式";

    public ArcControlCenter(Context mContext) {
        this.mContext = mContext;
        initData();
    }

    private void initData(){
        ExecutorUtils.execute(() -> {
            if (!HttpUtils.isConnected()){
                return;
            }
            getEndpoint(mContext);
            if (endpoint != null){
                getProgram(Long.valueOf(endpoint.getId()));
                startProgram();
            }
        });
    }

    private void getEndpoint(Context context) {
        String ids = Settings.System.getString(context.getContentResolver(), "endpoint_ids");
        if (TextUtils.isEmpty(ids)){
            Log.d(TAG, "getEndpoint: system don't set endpoint_ids.");
            endpoint = null;
            return;
        }
        if (ids.contains(",")){
            ids = Arrays.asList(ids.split(",")).get(0);
        }
        endpoint = HttpUtils.endPointInfo(Long.valueOf(ids));
        Log.d(TAG, "initData: endpoint is "+ endpoint.toString());
    }

    private void getProgram(Long endpointID){
        programList = HttpUtils.programList(Long.valueOf(endpointID));
        Log.d(TAG, "getProgram: program list is "+programList.toString());
    }

    public void startProgram() {
        for (Program program: programList){
            if (program.getName().contains(TargetProgramName)) {
                Log.d(TAG, "startProgram: start program is "+ program.getName());
                ExecutorUtils.execute(() -> {
                    HttpUtils.start(Long.valueOf(program.getId()));
                });
            }
        }
    }

    public void stopProgram() {
        for (Program program: programList){
            if (program.getName().contains(TargetProgramName)) {
                Log.d(TAG, "startProgram: stop program is "+ program.getName());
                ExecutorUtils.execute(() -> {
                    HttpUtils.stop(Long.valueOf(program.getId()));
                });
            }
        }
    }
}
