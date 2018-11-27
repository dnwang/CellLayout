package org.pinwheel.view.celllayout;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;

/**
 * Copyright (C), 2018 <br>
 * <br>
 * All rights reserved <br>
 * <br>
 *
 * @author dnwang
 * @version 2018/11/27,14:34
 */
final class Sync {

    private static HandlerThread sync;
    private static Handler handler;
    private static Handler uiHandler;

    private static int count = 0;

    static void prepare() {
        count++;
        if (null != sync) {
            return;
        }
        sync = new HandlerThread("CellLayout.Sync");
        sync.start();
        uiHandler = new Handler(Looper.getMainLooper());
        handler = new Handler(sync.getLooper());
    }

    static void release() {
        count--;
        if (count <= 0 && null != sync) {
            sync.quit();
            sync = null;
            handler.removeCallbacksAndMessages(null);
            uiHandler.removeCallbacksAndMessages(null);
            handler = null;
            uiHandler = null;
        }
    }

    static <T> void execute(final Function<T> action, final Action<T> callback) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                final T t = action.call();
                uiHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        callback.call(t);
                    }
                });
            }
        });
    }

    interface Action<T> {
        void call(T t);
    }

    interface Function<T> {
        T call();
    }

}