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

    public void setDivider(int divider) {
        this.divider = divider;
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
    public void scrollTo(int x, int y) {
        super.scrollTo(Math.max(width() - contentWidth, Math.min(x, 0)),
                Math.max(height() - contentHeight, Math.min(y, 0)));
    }

    public int getContentWidth() {
        return contentWidth;
    }

    public int getContentHeight() {
        return contentHeight;
    }

    private int contentWidth, contentHeight;

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