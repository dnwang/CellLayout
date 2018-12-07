package org.pinwheel.view.celllayout;

/**
 * Copyright (C), 2018 <br>
 * <br>
 * All rights reserved <br>
 * <br>
 *
 * @author dnwang
 * @version 2018/12/7,15:05
 */
interface Mask {
    android.graphics.Rect getClipRect();

    android.graphics.Rect getClipRectBy(Cell cell, boolean isMoving);
}
