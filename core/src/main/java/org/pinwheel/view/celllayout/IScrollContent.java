package org.pinwheel.view.celllayout;

/**
 * Copyright (C), 2018 <br>
 * <br>
 * All rights reserved <br>
 * <br>
 *
 * @author dnwang
 * @version 2018/11/30,14:15
 */
interface IScrollContent {
    void measureContent();

    int getContentWidth();

    int getContentHeight();
}