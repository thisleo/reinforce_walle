package com.chendy.reinforcedemo;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.meituan.android.walle.WalleChannelReader;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //TODO: 请现在Project下找到 keystore.properties 文件修改360账号密码
        initView();

    }

    private void initView() {

        TextView titleTv = findViewById(R.id.title_tv);
        String channel = WalleChannelReader.getChannel(this.getApplicationContext());
        titleTv.setText(String.format("当前版本: ==> %s\n当前渠道 ==> %s", getAppVersionName(this), channel));
    }


    public static String getAppVersionName(Context context) {
        String versionName = "";
        try {
            // ---get the package info---
            PackageManager pm = context.getPackageManager();
            PackageInfo pi = pm.getPackageInfo(context.getPackageName(), 0);
            versionName = pi.versionName;
            if (versionName == null || versionName.length() <= 0) {
                return "";
            }
        } catch (Exception e) {
            Log.e("VersionInfo", "Exception", e);
        }
        return versionName;
    }
}
