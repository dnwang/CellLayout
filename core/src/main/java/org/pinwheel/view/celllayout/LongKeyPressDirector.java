package org.pinwheel.view.celllayout;

import android.view.KeyEvent;

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

    private boolean onKeyLongPress = false;

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
                    return onLongPress(KeyEvent.ACTION_UP, keyCode);
                } else {
                    return onSinglePress(keyCode);
                }
            }
        }
        return false;
    }

}