package org.pinwheel.view.celllayout;

/**
 * Copyright (C), 2018 <br>
 * <br>
 * All rights reserved <br>
 * <br>
 *
 * @author dnwang
 * @version 2018/11/15,11:32
 */
public class LinearGroup extends CellGroup {

    public static final int HORIZONTAL = 0;
    public static final int VERTICAL = 1;

    @Attribute
    private int divider;
    @Attribute
    private int orientation;

    LinearGroup() {
        this(VERTICAL);
    }

    public LinearGroup(int orientation) {
        super();
        this.orientation = orientation;
        this.divider = 0;
    }

    @Override
    public CellGroup.Params getDefaultParams() {
        return new LinearGroup.Params();
    }

    @Override
    protected void measure(int width, int height) {
        super.measure(width, height);
        final int size = getCellCount();
        for (int i = 0; i < size; i++) {
            Cell cell = getCellAt(i);
            Params p = (LinearGroup.Params) cell.getParams();
            if (HORIZONTAL == orientation) {
                int h = height - paddingTop - paddingBottom - p.marginTop - p.marginBottom;
                cell.measure(p.width, h);
            } else {
                int w = width - paddingLeft - paddingRight - p.marginLeft - p.marginRight;
                cell.measure(w, p.height);
            }
        }
        setContentSize();
    }

    @Override
    protected void layout(int x, int y) {
        super.layout(x, y);
        int tmp;
        if (HORIZONTAL == orientation) {
            tmp = getLeft() + paddingLeft;
        } else {
            tmp = getTop() + paddingTop;
        }
        final int size = getCellCount();
        for (int i = 0; i < size; i++) {
            Cell cell = getCellAt(i);
            Params p = (LinearGroup.Params) cell.getParams();
            if (HORIZONTAL == orientation) {
                tmp += 0 == i ? 0 : divider;
                tmp += p.marginLeft;
                cell.layout(tmp, getTop() + paddingTop + p.marginTop);
                tmp += (cell.getWidth() + p.marginRight);
            } else {
                tmp += 0 == i ? 0 : divider;
                tmp += p.marginTop;
                cell.layout(getLeft() + paddingLeft + p.marginLeft, tmp);
                tmp += (cell.getHeight() + p.marginBottom);
            }
        }
    }

    public int getOrientation() {
        return orientation;
    }

    @Override
    public void scrollFix(int[] diff) {
        if (0 == diff[0] && 0 == diff[1]) {
            return;
        }
        // fix dx
        int tmp = getScrollX() + diff[0];
        int max = -(contentWidth - getWidth());
        if (tmp > 0) {
            diff[0] = -getScrollX();
        } else if (tmp < max) {
            diff[0] = max - getScrollX();
        }
        // fix dy
        tmp = getScrollY() + diff[1];
        max = -(contentHeight - getHeight());
        if (tmp > 0) {
            diff[1] = -getScrollY();
        } else if (tmp < max) {
            diff[1] = max - getScrollY();
        }
    }

    @Override
    public void scrollBy(int dx, int dy) {
        dy = HORIZONTAL == orientation ? 0 : dy;
        dx = HORIZONTAL != orientation ? 0 : dx;
        super.scrollBy(dx, dy);
    }

    @Override
    public void scrollTo(int x, int y) {
        y = HORIZONTAL == orientation ? getTop() : y;
        x = HORIZONTAL != orientation ? getLeft() : x;
        super.scrollTo(x, y);
    }

    public int getContentWidth() {
        return contentWidth;
    }

    public int getContentHeight() {
        return contentHeight;
    }

    private int contentWidth, contentHeight;

    private void setContentSize() {
        contentWidth = 0;
        contentHeight = 0;
        final int size = getCellCount();
        for (int i = 0; i < size; i++) {
            Cell cell = getCellAt(i);
            contentWidth += cell.getWidth();
            contentHeight += cell.getHeight();
        }
        if (HORIZONTAL == orientation) {
            contentWidth += paddingLeft + paddingRight + Math.max(0, size - 1) * divider;
            contentHeight = getHeight();
        } else {
            contentWidth = getWidth();
            contentHeight += paddingTop + paddingBottom + Math.max(0, size - 1) * divider;
        }
    }

    public static class Params extends CellGroup.Params {
        Params() {
            this(0, 0);
        }

        public Params(int width, int height) {
            super(width, height);
        }
    }

}