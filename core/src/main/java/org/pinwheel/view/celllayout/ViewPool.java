package org.pinwheel.view.celllayout;

import android.view.View;

import java.util.ArrayList;
import java.util.List;

/**
 * Copyright (C), 2018 <br>
 * <br>
 * All rights reserved <br>
 * <br>
 *
 * @author dnwang
 * @version 2018/11/27,9:50
 */
final class ViewPool {
    private final List<View> caches;

    ViewPool() {
        caches = new ArrayList<>();
    }

    int size() {
        return caches.size();
    }

    View obtain(Cell cell, boolean force) {
        if (caches.isEmpty()) {
            return null;
        }
        View view = null;
        if (null != cell) {
            for (View v : caches) {
                if (v.getMeasuredWidth() == cell.width() && v.getMeasuredHeight() == cell.height()) {
                    view = v;
                    break;
                }
            }
        }
        if (null != view) {
            caches.remove(view);
        } else if (!force) {
            view = caches.remove(0);
        }
        return view;
    }

    void recycle(final View view) {
        if (null == view) return;
        if (caches.contains(view)) {
            throw new IllegalStateException("Already in the pool!");
        }
        if (view.hasFocus()) {
            view.clearFocus();
        }
        view.setScaleX(1f);
        view.setScaleY(1f);
        caches.add(view);
    }

    void keepSize(int size, Filter<View> filter) {
        size = size < 0 ? 0 : size;
        final int count = caches.size() - size;
        for (int i = 0; i < count; i++) {
            View v = caches.remove(0);
            if (null != filter) {
                filter.call(v);
            }
        }
    }
}