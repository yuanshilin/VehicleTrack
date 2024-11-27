package com.arcvideo.vehicletrack.arccontrol;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.InetAddress;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class HttpUtils {
    private static final String TAG = "HttpUtils";

    private static final OkHttpClient mOkHttpClient = new OkHttpClient();
    private static Gson gson = new Gson();

    // 测试 IP 地址
//    private static final String BASE_URL = "http://172.17.22.58:6812";
    // 正式 IP 地址
    private static final String BASE_URL = "http://172.17.24.58:6812";
    private static final String BASE_IP = "172.17.24.58";
    private static boolean connected = false;
    private static Type programListType = new TypeToken<List<Program>>() {
    }.getType();

    public static boolean isConnected(){
        Thread validThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    connected = InetAddress.getByName(BASE_IP).isReachable(1500);
                } catch (IOException e) {
                    connected = false;
                }
                if (connected){
                    Log.d(TAG, "isConnected: net init success.");
                }else{
                    Log.d(TAG, "isConnected: "+BASE_IP+" is unreached, please check the ip address or receiver device network status.");
                }
            }
        });
        validThread.start();
        try {
            validThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return connected;
    }

    public static Response http(Request request) {
        try {
            return mOkHttpClient.newCall(request).execute();
        } catch (IOException e) {
            Log.e(TAG, "http: ", e);
        }
        return null;
    }

    public static void start(Long id) {
        Log.d(TAG, "start: " + id);
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("id", id);
        } catch (JSONException e) {
            Log.e(TAG, "start: ", e);
        }
        RequestBody requestBody =
                RequestBody.create(MediaType.parse("application/json"), String.valueOf(jsonObject));

        Request request = new Request.Builder().url(BASE_URL + "/screenshow/carcontrol/start").post(requestBody).build();
        Response response = http(request);
        if (response == null) return;
        Log.d(TAG, "start: response code= " + response.code());

        try {
            Log.d(TAG, "start: response body= " + response.body().string());
        } catch (IOException e) {
            Log.e(TAG, "start: " + e);
        }
    }

    public static void stop(Long id) {
        Log.d(TAG, "stop: " + id);
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("id", id);
        } catch (JSONException e) {
            Log.e(TAG, "stop: ", e);

        }
        RequestBody requestBody =
                RequestBody.create(MediaType.parse("application/json"), String.valueOf(jsonObject));

        Request request = new Request.Builder().url(BASE_URL + "/screenshow/carcontrol/stop").post(requestBody).build();
        Response response = http(request);
        if (response == null) return;
        Log.d(TAG, "stop: response code= " + response.code());

        try {
            Log.d(TAG, "stop: response body= " + response.body().string());
        } catch (IOException e) {
            Log.e(TAG, "stop: " + e);
        }
    }

    public static Program currentPlay(Long id) {
        Log.d(TAG, "currentPlay: " + id);
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("id", id);
        } catch (JSONException e) {
            Log.e(TAG, "currentPlay: " + e);
        }
        RequestBody requestBody =
                RequestBody.create(MediaType.parse("application/json"), String.valueOf(jsonObject));

        Request request = new Request.Builder().url(BASE_URL + "/screenshow/carcontrol/currentplay").post(requestBody).build();
        Response response = http(request);
        if (response == null) return null;
        Log.d(TAG, "currentPlay: response code= " + response.code());

        try {
            String body = response.body().string();
            Log.d(TAG, "currentPlay: response body= " + body);
            JSONObject data = new JSONObject(body).getJSONObject("data");

            Program program = gson.fromJson(String.valueOf(data), Program.class);

            Log.d(TAG, "currentPlay:  " + program);
            return program;

        } catch (IOException | JSONException e) {
            Log.e(TAG, "currentPlay: " + e);
        }
        return null;
    }

    public static List<Program> programList(Long id) {
        Log.d(TAG, "programList: " + id);

        Request request = new Request.Builder().url(BASE_URL + "/screenshow/carcontrol/programlist?endPointId=" + id).build();
        Response response = http(request);
        if (response == null) return null;
        Log.d(TAG, "programList: response code= " + response.code());

        try {
            String body = response.body().string();
            Log.d(TAG, "programList: response body= " + body);
            List<Program> programList = gson.fromJson(String.valueOf(body), programListType);
            Log.d(TAG, "programList: " + programList);
            return programList;
        } catch (IOException e) {
            Log.e(TAG, "programList: " + e);
        }
        return null;
    }

    public static Endpoint endPointInfo(Long id) {
        Log.d(TAG, "endPointInfo: " + id);
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("id", id);
        } catch (JSONException e) {
            Log.e(TAG, "endPointInfo: " + e);
        }
        RequestBody requestBody =
                RequestBody.create(MediaType.parse("application/json"), String.valueOf(jsonObject));


        Request request = new Request.Builder().url(BASE_URL + "/screenshow/carcontrol/item").post(requestBody).build();
        Response response = http(request);
        if (response == null) return null;
        Log.d(TAG, "endPointInfo: response code= " + response.code());


        try {
            String body = response.body().string();
            Log.d(TAG, "endPointInfo: response body= " + body);

            Endpoint endpoint = gson.fromJson(body, Endpoint.class);

            Log.d(TAG, "endPointInfo:  " + endpoint);
            return endpoint;

        } catch (IOException e) {
            Log.e(TAG, "endPointInfo: " + e);
        }
        return null;
    }
}
