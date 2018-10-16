package com.example.stitch;

import android.app.Application;

import com.zxy.tiny.Tiny;

/**
 * Created by zhengxiaoyong on 2017/3/14.
 */
public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        Tiny.getInstance().init(this);
        }
}
