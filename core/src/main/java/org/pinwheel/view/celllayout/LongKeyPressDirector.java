package org.pinwheel.view.celllayout;

import android.view.FocusFinder;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;

/**
 * Copyright (C), 2018 <br>
 * <br>
 * All rights reserved <br>
 * <br>
 *
 * @author dnwang
 * @version 2018/11/5,16:08
 */
abstract class LongKeyPressDirector {

    public abstract boolean interceptLongPress(final int keyCode);

    public abstract boolean onLongPress(final int action, final int keyCode);

    public abstract boolean onSinglePress(final int keyCode);

    private View target;
    private boolean onKeyLongPress = false;

    LongKeyPressDirector(View view) {
        this.target = view;
    }

    private long downTime = -1;

    final boolean dispatchKeyEvent(KeyEvent event) {
        final int action = event.getAction();
        final int keyCode = event.getKeyCode();
        if (KeyEvent.ACTION_DOWN == action) {
            if (interceptLongPress(keyCode)) {
                final long eventTime = event.getEventTime();
                if (downTime > 0) {
                    onKeyLongPress = true;
                    return onLongPress(KeyEvent.ACTION_DOWN, keyCode);
                }
                downTime = eventTime;
                return true;
            } else {
                return onSinglePress(keyCode);
            }
        } else if (KeyEvent.ACTION_UP == action) {
            final boolean longPress = onKeyLongPress;
            onKeyLongPress = false;
            downTime = -1;
            if (interceptLongPress(keyCode)) {
                if (longPress) {
                    onLongPress(KeyEvent.ACTION_UP, keyCode);
                } else {
                    onSinglePress(keyCode);
                    moveFocusBy(keyCode);
                }
            }
        }
        return false;
    }

    private void moveFocusBy(final int keyCode) {
        int direction = 0;
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_LEFT:
                direction = View.FOCUS_LEFT;
                break;
            case KeyEvent.KEYCODE_DPAD_UP:
                direction = View.FOCUS_UP;
                break;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                direction = View.FOCUS_RIGHT;
                break;
            case KeyEvent.KEYCODE_DPAD_DOWN:
                direction = View.FOCUS_DOWN;
                break;
        }
        if (0 != direction) {
            final View mView = this.target.getRootView();
            final View focused = mView.findFocus();
            View v = FocusFinder.getInstance().findNextFocus((ViewGroup) mView, focused, direction);
            if (v != null) {
                v.requestFocus(direction);
            }
        }
    }

}