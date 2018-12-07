package org.pinwheel.view.celllayout;

import android.view.View;

import java.util.ArrayList;
import java.util.List;

import static org.pinwheel.view.celllayout.CellLayout.SCALE_MIN;

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
    private int FLAG_FOCUSABLE = 1;

    private final List<Holder> caches;

    ViewPool() {
        caches = new ArrayList<>();
    }

    int size() {
        return caches.size();
    }

    View obtain() {
        return obtain(null, false);
    }

    View obtain(Cell cell, boolean force) {
        if (caches.isEmpty()) {
            return null;
        }
        Holder holder = null;
        if (null != cell) {
            for (Holder h : caches) {
                View v = h.view;
                if (v.getMeasuredWidth() == cell.width() && v.getMeasuredHeight() == cell.height()) {
                    holder = h;
                    break;
                }
            }
        }
        if (null != holder) {
            caches.remove(holder);
        } else if (!force) {
            holder = caches.remove(0);
        }
        if (null != holder) {
            // restore state
            holder.view.setFocusable((holder.state & FLAG_FOCUSABLE) != 0);
            return holder.view;
        } else {
            return null;
        }
    }

    void recycle(final View view) {
        if (null == view) return;
        if (view.hasFocus()) {
            view.clearFocus();
        }
        view.setScaleX(SCALE_MIN);
        view.setScaleY(SCALE_MIN);
        final Holder holder = new Holder();
        holder.view = view;
        if (view.isFocusable()) {
            view.setFocusable(false);
            holder.state |= FLAG_FOCUSABLE;
        }
        caches.add(holder);
    }

    void keepSize(int size, Filter<View> filter) {
        size = size < 0 ? 0 : size;
        final int count = caches.size() - size;
        for (int i = 0; i < count; i++) {
            Holder h = caches.remove(0);
            if (null != filter) {
                filter.call(h.view);
            }
        }
    }

    private static class Holder {
        View view;
        int state;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Holder that = (Holder) o;
            return view.equals(that.view);
        }
    }
}