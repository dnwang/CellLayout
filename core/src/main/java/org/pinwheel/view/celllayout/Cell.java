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
public class Cell implements Serializable {

    private static long ID_OFFSET = 0;
    private final long id;
    //
    @Attribute
    public int paddingLeft, paddingTop, paddingRight, paddingBottom;
    //
    private int left, top;
    private int width, height;
    //
    private boolean isVisible;
    //
    private CellGroup parent;
    private CellGroup.Params p;

    public Cell() {
        this.id = ++ID_OFFSET;
    }

    protected void measure(int width, int height) {
        this.width = width;
        this.height = height;
    }

    protected void layout(int x, int y) {
        setPosition(x, y);
    }

    protected final void setParams(CellGroup.Params p) {
        this.p = p;
    }

    protected final void setParent(CellGroup parent) {
        this.parent = parent;
    }

    protected void setVisible(int l, int t, int r, int b) {
        final int right = getRight(), bottom = getBottom();
        if (r > l && b > t && right > left && bottom > top) {
            isVisible = right >= l && bottom >= t && left <= r && top <= b;
        } else {
            isVisible = false;
        }
    }

    private void setPosition(int x, int y) {
        this.left = x;
        this.top = y;
    }

    public long getId() {
        return id;
    }

    public int getLeft() {
        return left;
    }

    public int getTop() {
        return top;
    }

    public int getRight() {
        return left + width;
    }

    public int getBottom() {
        return top + height;
    }

    public boolean contains(int x, int y) {
        return x >= getLeft() && x < getRight() && y >= getTop() && y < getBottom();
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public final void offset(final int dx, final int dy) {
        if (0 == dx && 0 == dy) {
            return;
        }
        setPosition(getLeft() + dx, getTop() + dy);
    }

    public final CellGroup.Params getParams() {
        return p;
    }

    public final CellGroup getParent() {
        return parent;
    }

    public final boolean isVisible() {
        return isVisible;
    }

    public Cell findCellById(long id) {
        return getId() == id ? this : null;
    }

    @Override
    public String toString() {
        return String.valueOf(id);
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