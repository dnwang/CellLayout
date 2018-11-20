package org.pinwheel.view.celllayout;

/**
 * Copyright (C), 2018 <br>
 * <br>
 * All rights reserved <br>
 * <br>
 *
 * @author dnwang
 * @version 2018/11/15,13:35
 */
public class Cell {

    private static long ID_OFFSET = 0;
    private final long id;
    //
    private CellDirector director;
    //
    @Attribute
    public int paddingLeft, paddingTop, paddingRight, paddingBottom;
    //
    private int x, y;
    private int width, height;
    //
    private boolean isVisible;
    //
    private CellGroup parent;
    private CellGroup.Params p;

    public Cell() {
        this.id = ++ID_OFFSET;
    }

    final void attach(CellDirector director) {
        this.director = director;
        this.director.onCellAttached(this);
    }

    final void detach() {
        parent = null;
        isVisible = false;
        if (null != director) {
            director.onCellDetached(this);
        }
        director = null;
    }

    final void removeFromParent() {
        if (null != parent) {
            parent.removeCell(this);
        }
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

    private void setVisible(int left, int top, int right, int bottom) {
        final int l = getLeft(), t = getTop(), r = getRight(), b = getBottom();
        if (right > left && bottom > top && r > l && b > t) {
            if (r < left || b < top || l > right || t > bottom) {
                isVisible = false;
            } else {
                isVisible = true;
            }
        } else {
            isVisible = false;
        }
    }

    private void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public long getId() {
        return id;
    }

    public int getLeft() {
        return x;
    }

    public int getTop() {
        return y;
    }

    public int getRight() {
        return getLeft() + getWidth();
    }

    public int getBottom() {
        return getTop() + getHeight();
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
        final int oldX = getLeft();
        final int oldY = getTop();
        setPosition(oldX + dx, oldY + dy);
        director.onCellPositionChanged(this, oldX, oldY);
    }

    public final void updateVisibleSate() {
        final boolean oldState = isVisible();
        Cell root = director.getRoot();
        setVisible(root.getLeft(), root.getTop(), root.getRight(), root.getBottom());
        if (oldState != isVisible()) {
            director.onCellVisibleChanged(this);
        }
    }

    public final CellGroup.Params getParams() {
        return p;
    }

    public final CellGroup getParent() {
        return parent;
    }

    public final CellDirector getDirector() {
        return director;
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