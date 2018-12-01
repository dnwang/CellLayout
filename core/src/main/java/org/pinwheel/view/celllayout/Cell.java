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

    private static final int FLAG_HAS_MEASURED = 1;
    private static final int FLAG_HAS_LAYOUT = FLAG_HAS_MEASURED << 1;
    private static final int FLAG_VISIBLE = FLAG_HAS_LAYOUT << 1;
    private static final int FLAG_HAS_CONTENT = FLAG_VISIBLE << 1;
    private static final int FLAG_DISABLE_FOCUS = FLAG_HAS_CONTENT << 1;
    private static final int FLAG_HAS_FOCUS = FLAG_DISABLE_FOCUS << 1;
    private static final int FLAG_NO_HOLDER = FLAG_HAS_FOCUS << 1;

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
        state |= FLAG_HAS_MEASURED;
        this.measureWidth = width;
        this.measureHeight = height;
    }

    protected void layout(int x, int y, int scrollX, int scrollY) {
        state |= FLAG_HAS_LAYOUT;
        layoutX = x;
        layoutY = y;
        final int l = x + scrollX, t = y + scrollY;
        set(l, t, l + measureWidth, t + measureHeight);
        parentScrollX = scrollX;
        parentScrollY = scrollY;
    }

    @Override
    void offset(int dx, int dy) {
        super.offset(dx, dy);
        parentScrollX = parentScrollY = 0;
    }

    @Override
    void offsetTo(int newLeft, int newTop) {
        super.offsetTo(newLeft, newTop);
        parentScrollX = parentScrollY = 0;
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

    final void setParams(CellGroup.Params p) {
        this.p = p;
    }

    final void setParent(CellGroup parent) {
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

    int getLayoutXWithParentScroll() {
        return layoutX + parentScrollX;
    }

    int getLayoutYWithParentScroll() {
        return layoutY + parentScrollY;
    }

    private int layoutX, layoutY;
    private int parentScrollX, parentScrollY;

    final void computeParentScroll() {
        parentScrollX = 0;
        parentScrollY = 0;
        if (null != parent) {
            _computeParentScroll(parent);
        }
    }

    private void _computeParentScroll(CellGroup p) {
        parentScrollX += p.getScrollX();
        parentScrollY += p.getScrollY();
        CellGroup pp = p.getParent();
        if (null != pp) {
            _computeParentScroll(pp);
        }
    }

    // --------- state

    final void clearAllState() {
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

    final void setVisible(boolean is) {
        if (is) {
            state |= FLAG_VISIBLE;
        } else {
            state &= ~FLAG_VISIBLE;
        }
    }

    public final boolean isVisible() {
        return (state & FLAG_VISIBLE) != 0;
    }

    public final void setFocusable(boolean is) {
        if (is) {
            state &= ~FLAG_DISABLE_FOCUS;
        } else {
            state |= FLAG_DISABLE_FOCUS;
        }
    }

    public final boolean isFocusable() {
        return (state & FLAG_DISABLE_FOCUS) == 0;
    }

    final void setFocus(boolean is) {
        if (is) {
            state |= FLAG_HAS_FOCUS;
        } else {
            state &= ~FLAG_HAS_FOCUS;
        }
    }

    public final boolean hasFocus() {
        return (state & FLAG_HAS_FOCUS) != 0;
    }

    public final void setNoHolder(boolean is) {
        if (is) {
            state |= FLAG_NO_HOLDER;
        } else {
            state &= ~FLAG_NO_HOLDER;
        }
    }

    public final boolean isNoHolder() {
        return (state & FLAG_NO_HOLDER) != 0;
    }

    final void forceLayout() {
        state &= ~FLAG_HAS_LAYOUT;
    }

    final boolean isLayout() {
        return (state & FLAG_HAS_LAYOUT) != 0;
    }

    final void forceMeasure() {
        state &= ~FLAG_HAS_MEASURED;
    }

    final boolean isMeasured() {
        return (state & FLAG_HAS_MEASURED) != 0;
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