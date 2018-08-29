package com.example.frank.vlcdemo;

import android.content.Context;
import android.support.multidex.MultiDex;
import android.support.multidex.MultiDexApplication;

import com.inuker.library.MyContext;

/**
 * Created by liwentian on 2017/10/12.
 */

public class MyApplication extends MultiDexApplication {

    @Override
    public void onCreate() {
        super.onCreate();
        MyContext.setContext(this);
        MultiDex.install(this);
    }

    public static Context getContext() {
        return MyContext.getContext();
    }
}
