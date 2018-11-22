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
    public void addCell(Cell cell, CellGroup.Params p) {
        if (cell instanceof LinearGroup &&
                ((LinearGroup) cell).getOrientation() == orientation) {
            throw new IllegalStateException("cell has the same with parent's orientation ! maybe unboxing the group");
        }
        super.addCell(cell, p);
    }

    @Override
    protected void measure(final int width, final int height) {
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
        measureContent();
    }

    @Override
    protected void layout(int x, int y) {
        super.layout(x, y);
        int tmp;
        if (HORIZONTAL == orientation) {
            tmp = left + paddingLeft;
        } else {
            tmp = top + paddingTop;
        }
        final int size = getCellCount();
        for (int i = 0; i < size; i++) {
            Cell cell = getCellAt(i);
            Params p = (LinearGroup.Params) cell.getParams();
            if (HORIZONTAL == orientation) {
                tmp += 0 == i ? 0 : divider;
                tmp += p.marginLeft;
                cell.layout(tmp, top + paddingTop + p.marginTop);
                tmp += (cell.width() + p.marginRight);
            } else {
                tmp += 0 == i ? 0 : divider;
                tmp += p.marginTop;
                cell.layout(left + paddingLeft + p.marginLeft, tmp);
                tmp += (cell.height() + p.marginBottom);
            }
        }
    }

    public int getOrientation() {
        return orientation;
    }

    @Override
    public void fixScrollOffset(int[] offset) {
        if (0 == offset[0] && 0 == offset[1]) {
            return;
        }
        // fix dx
        int tmp = scrollX + offset[0];
        int max = -(contentWidth - width());
        if (tmp > 0) {
            offset[0] = -scrollX;
        } else if (tmp < max) {
            offset[0] = max - scrollX;
        }
        // fix dy
        tmp = scrollY + offset[1];
        max = -(contentHeight - height());
        if (tmp > 0) {
            offset[1] = -scrollY;
        } else if (tmp < max) {
            offset[1] = max - scrollY;
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
        y = HORIZONTAL == orientation ? top : y;
        x = HORIZONTAL != orientation ? left : x;
        super.scrollTo(x, y);
    }

    @Override
    public int getContentWidth() {
        return contentWidth;
    }

    @Override
    public int getContentHeight() {
        return contentHeight;
    }

    private int contentWidth, contentHeight;

    @Override
    public void measureContent() {
        contentWidth = 0;
        contentHeight = 0;
        final int size = getCellCount();
        for (int i = 0; i < size; i++) {
            Cell cell = getCellAt(i);
            contentWidth += cell.width();
            contentHeight += cell.height();
        }
        if (HORIZONTAL == orientation) {
            contentWidth += paddingLeft + paddingRight + Math.max(0, size - 1) * divider;
            contentHeight = height();
        } else {
            contentWidth = width();
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