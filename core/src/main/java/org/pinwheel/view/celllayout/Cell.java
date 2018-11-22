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
    // 0: holder view, 1: content view
    private static final int FLAG_HAS_CONTENT_VIEW = FLAG_VISIBLE << 1;

    private static long ID_OFFSET = 0;
    private final long id;
    private int state;
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

    protected final void setParams(CellGroup.Params p) {
        this.p = p;
    }

    protected final void setParent(CellGroup parent) {
        this.parent = parent;
    }

    protected final void setVisible(Rect referenceArea) {
        if (Rect.intersects(referenceArea, this)) {
            state |= FLAG_VISIBLE;
        } else {
            state &= ~FLAG_VISIBLE;
        }
    }

    public final long getId() {
        return id;
    }

    public Cell findCellById(long id) {
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

    public final boolean hasContentView() {
        return (state & FLAG_HAS_CONTENT_VIEW) != 0;
    }

    public final void setHasContentView() {
        state |= FLAG_HAS_CONTENT_VIEW;
    }

    public final void setHasHolderView() {
        state &= ~FLAG_HAS_CONTENT_VIEW;
    }

    public final boolean isVisible() {
        return (state & FLAG_VISIBLE) != 0;
    }

    public final Rect getRect() {
        return Rect.copy(this);
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