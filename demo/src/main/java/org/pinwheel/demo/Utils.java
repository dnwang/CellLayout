package org.pinwheel.demo;

import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.XmlResourceParser;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;

import java.io.File;
import java.lang.reflect.Method;

/**
 * Copyright (C), 2018 <br>
 * <br>
 * All rights reserved <br>
 * <br>
 *
 * @author dnwang
 * @version 2018/12/5,14:29
 */
final class Utils {
    /**
     * load layout.xml from sdcard
     */
    public static View loadLayout(Context ctx, File file) {
        try {
            AssetManager am = ctx.getResources().getAssets();
            final Method m = AssetManager.class.getMethod("addAssetPath", String.class);
            m.setAccessible(true);
            int cookie = (int) m.invoke(am, Environment.getExternalStorageDirectory().getAbsolutePath());
            XmlResourceParser parser = am.openXmlResourceParser(cookie, file.getAbsolutePath());
            return LayoutInflater.from(ctx).inflate(parser, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
