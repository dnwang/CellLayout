package org.pinwheel.view.celllayout;

import java.io.Serializable;

/**
 * Copyright (C), 2018 <br>
 * <br>
 * All rights reserved <br>
 * <br>
 *
 * @author dnwang
 * @version 2018/11/15,13:35
 */
public class Cell extends Rect implements Serializable {

    private static final int FLAG_VISIBLE = 1;
    // 0: empty, 1: content view
    private static final int FLAG_HAS_CONTENT = FLAG_VISIBLE << 1;
    private static final int FLAG_HAS_FOCUS = FLAG_HAS_CONTENT << 1;

    private static int ID_OFFSET = 0;
    private final int id;
    private int state = 0;
    //
    @Attribute
    public int paddingLeft, paddingTop, paddingRight, paddingBottom;
    //
    private int measureWidth, measureHeight;
    //
    private CellGroup parent;
    private CellGroup.Params p;

    public Cell() {
        this.id = ++ID_OFFSET;
    }

    protected void measure(int width, int height) {
        this.measureWidth = width;
        this.measureHeight = height;
    }

    protected void layout(int x, int y) {
        left = x;
        top = y;
        right = left + measureWidth;
        bottom = top + measureHeight;
    }

    @Override
    public int width() {
        return measureWidth;
    }

    @Override
    public int height() {
        return measureHeight;
    }

    public void setPadding(int paddingLeft, int paddingTop, int paddingRight, int paddingBottom) {
        this.paddingLeft = paddingLeft;
        this.paddingTop = paddingTop;
        this.paddingRight = paddingRight;
        this.paddingBottom = paddingBottom;
    }

    protected final void setParams(CellGroup.Params p) {
        this.p = p;
    }

    protected final void setParent(CellGroup parent) {
        this.parent = parent;
    }

    public final int getId() {
        return id;
    }

    public Cell findCellById(int id) {
        return getId() == id ? this : null;
    }

    public final CellGroup.Params getParams() {
        return p;
    }

    public final CellGroup getParent() {
        return parent;
    }

    public final void removeFromParent() {
        if (null != parent) {
            parent.removeCell(this);
        }
    }

    // --------- state

    public final void clearAllState() {
        state = 0;
    }

    public final void setHasContent(boolean is) {
        if (is) {
            state |= FLAG_HAS_CONTENT;
        } else {
            state &= ~FLAG_HAS_CONTENT;
        }
    }

    public final boolean hasContent() {
        return (state & FLAG_HAS_CONTENT) != 0;
    }

    protected final void setVisible(boolean is) {
        if (is) {
            state |= FLAG_VISIBLE;
        } else {
            state &= ~FLAG_VISIBLE;
        }
    }

    public final boolean isVisible() {
        return (state & FLAG_VISIBLE) != 0;
    }

    public final void setHasFocus(boolean is) {
        if (is) {
            state |= FLAG_HAS_FOCUS;
        } else {
            state &= ~FLAG_HAS_FOCUS;
        }
    }

    public final boolean hasFocus() {
        return (state & FLAG_HAS_FOCUS) != 0;
    }

    @Override
    public String toString() {
        return id + ": " + super.toString();
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        return id == ((Cell) o).id;
    }

    @Override
    public final int hashCode() {
        return Long.valueOf(id).hashCode();
    }

}