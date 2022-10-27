package com.example.test;

import androidx.appcompat.app.AppCompatActivity;

import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.AppOpsManager;
import android.app.PendingIntent;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.StrictMode;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {
    public static Context context;
    public static String current_android_id;
    private Timer timer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        startService(new Intent(this, KeepRunning.class));

        // keep running when phone is locked
        Window window = this.getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        window.addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

        setContentView(R.layout.activity_main);
        context = getApplicationContext();
        current_android_id = getDeviceId((context));
        Log.d("current user", current_android_id);
        try {
            PackageManager packageManager = context.getPackageManager();
            ApplicationInfo applicationInfo = packageManager.getApplicationInfo(context.getPackageName(), 0);
            AppOpsManager appOpsManager = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
            int mode = appOpsManager.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, applicationInfo.uid, applicationInfo.packageName);
            if (mode != AppOpsManager.MODE_ALLOWED){
                startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
            }

        } catch (PackageManager.NameNotFoundException e) {

        }
//        startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
        UsageStatsManager usageStatsManager = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);

        // fix android.os.NetworkOnMainThreadException
        if (android.os.Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }



//        // (2) 使用handler处理接收到的消息
//        Handler mHandler = new Handler(){
//            @Override
//            public void handleMessage(Message msg) {
//                if(msg.what == 0){
//                    try {
//                        insertUsageStats(usageStatsManager);
//                    } catch (Exception e) {
//                        e.printStackTrace();
//                    }
//                }
//            }
//        };
//
//        timer=new Timer();
//        timer.schedule(new TimerTask() {
//            @Override
//            public void run() {
//                // (1) 使用handler发送消息
//                Message message=new Message();
//                message.what=0;
//                mHandler.sendMessage(message);
//            }
//        },0,1000 * 5);//每隔一秒使用handler发送一下消息,也就是每隔一秒执行一次,一直重复执行
//

//        try {
//            insertUsageStats(usageStatsManager);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
    }


    public static String getDeviceId(Context context) {
        String id = Settings.Secure.getString(context.getContentResolver(),
                Settings.Secure.ANDROID_ID);
        return id;
    }

    // go to interval selection view
    public void goToAutoInsert(View view) {
        // Do something in response to button
        Intent i = new Intent(this, auto_insert.class);
        startActivity(i);
    }

    public void goToManualInsert(View view) {
        // Do something in response to button
        Intent i = new Intent(this, manual_insert.class);
        startActivity(i);
    }

    public void queryByTime(View view) {
        // Do something in response to button
        Intent i = new Intent(this, query_by_time.class);
        startActivity(i);
    }

    public static String getAppName(Context context, String pname){
        ApplicationInfo appInfo;
        try {
            appInfo = context.getPackageManager().getApplicationInfo( pname, 0);
        } catch (final PackageManager.NameNotFoundException e) {
            appInfo = null;
        }
        final String applicationName = (String) (appInfo != null ? context.getPackageManager().getApplicationLabel(appInfo) : "(unknown)");

        return applicationName;
    }

    public static String stampToDate(long time) {
        Date date = new Date(time);
        SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss "); // the format of your date
        //sdf.setTimeZone(TimeZone.getTimeZone("GMT-4"));

        return sdf.format(date);
    }
//
//    private boolean checkUsageStatsPermission() {
//        AppOpsManager appOpsManager = getSystemService(AppCompatActivity.APP_OPS_SERVICE)
//        boolean mode;
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
//            appOpsManager.unsafeCheckOpNoThrow(
//                    "android:get_usage_stats",
//                    Process.myUid(), packageName
//            );
//            mode = true;
//        }
//        else {
//            appOpsManager.checkOpNoThrow(
//                    "android:get_usage_stats",
//                    Process.myUid(), packageName
//            )
//        }
//        return mode == AppOpsManager.MODE_ALLOWED
//    }

    public String insertUsageStats(UsageStatsManager usageStatsManager) throws Exception {
        String packageName = null;
        int counter = 0;
        long useTime = 0;
        long duration = 0;
        ArrayList<Integer> result = new ArrayList<>();
        final long INTERVAL = 1000 * 60 * 60 * 72 *0;
        final long end = System.currentTimeMillis();
        final long begin = end - INTERVAL;
        final UsageEvents usageEvents = usageStatsManager.queryEvents(begin, end);
        Log.d("here", String.valueOf(usageEvents));
        while (usageEvents.hasNextEvent()) {
            UsageEvents.Event curEvent = new UsageEvents.Event();
            usageEvents.getNextEvent(curEvent);
            String pname = curEvent.getPackageName();
            Log.d("activity here: ", "[" + pname + "]" + " app name: " + getAppName(context, pname));

            if(curEvent.getEventType() == UsageEvents.Event.ACTIVITY_RESUMED){
                counter++;
                useTime = curEvent.getTimeStamp();
            }
            if(curEvent.getEventType() == UsageEvents.Event.ACTIVITY_PAUSED){
                counter++;
                String appName = getAppName(context, pname);
                duration = curEvent.getTimeStamp() - useTime;
                //result.add(new AppUsageInfoWrapper())
                Log.d("activity paused: ", "[" + appName + "]" + stampToDate(useTime) + "Use time: " + String.valueOf(useTime) + ", duration: " + duration);
                int res = insertData("http://34.216.172.247:3000", stampToDate(useTime), pname, appName, duration);
                Log.d("insert response code", String.valueOf(res));
            }

        }

//        Calendar calendar=Calendar.getInstance();
//        long endt = calendar.getTimeInMillis();//结束时间
//        calendar.add(Calendar.DAY_OF_MONTH, -1);//时间间隔为一个月
//        long statt = calendar.getTimeInMillis();//开始时间
//        UsageStatsManager usageStatsManager2=(UsageStatsManager) getSystemService(USAGE_STATS_SERVICE);
//        //获取一个月内的信息
//        List<UsageStats> queryUsageStats = usageStatsManager2.queryUsageStats(UsageStatsManager.INTERVAL_MONTHLY,statt-1000000,endt);
//        Log.d("list of usage stats: ", String.valueOf(queryUsageStats));

        Calendar beginCal = Calendar.getInstance();
        beginCal.add(Calendar.HOUR_OF_DAY, -1);
        Calendar endCal = Calendar.getInstance();
        UsageStatsManager manager = (UsageStatsManager)getApplicationContext().getSystemService(USAGE_STATS_SERVICE);
        List<UsageStats> stats = manager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, beginCal.getTimeInMillis(), endCal.getTimeInMillis());
        StringBuilder sb = new StringBuilder();
        for(UsageStats us:stats){
            try {
                PackageManager pm = getApplicationContext().getPackageManager();
                ApplicationInfo applicationInfo = pm.getApplicationInfo(us.getPackageName(),PackageManager.GET_META_DATA);
                if((applicationInfo.flags&applicationInfo.FLAG_SYSTEM) <= 0){
                    SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss");
                    String t=format.format(new Date(us.getLastTimeUsed()));
                    sb.append(pm.getApplicationLabel(applicationInfo) + "\t" + t + "\t" + us.getTotalTimeInForeground() + "\n");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        Log.d("hhhhhhhhh", sb.toString());

        return packageName;
    }

    public static int insertData(String path, String useTime, String packagename, String appname, long duration) throws Exception {
        URL url = new URL(path + "/app_usage_data");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(5000);
        conn.setRequestMethod("POST");
        conn.setUseCaches(false);
        conn.setDoOutput(true);

        conn.setRequestProperty("Content-Type", "application/json");

        JSONObject param = new JSONObject();
        param.put("time", useTime);
        param.put("android_id", current_android_id);
        param.put("package_name", packagename);
        param.put("app_name", appname);
        param.put("duration_ms", duration);
        conn.connect();

        OutputStreamWriter writer = new OutputStreamWriter(conn.getOutputStream(), StandardCharsets.UTF_8);
        writer.write(param.toString());
        writer.flush();

        //The server isn't waiting for any data from the client, and when the server exits the connection will be closed. So add a ins.readLine() to the server code:
        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        System.out.println(in.readLine());

        writer.close();

        return conn.getResponseCode();
    }



}