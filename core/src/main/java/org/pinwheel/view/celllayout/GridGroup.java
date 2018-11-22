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
public class GridGroup extends CellGroup {

    @Attribute
    private int divider;
    @Attribute
    private int row, column;

    GridGroup() {
        this(1, 1);
    }

    public GridGroup(int row, int column) {
        super();
        this.row = Math.max(1, row);
        this.column = Math.max(1, column);
        this.divider = 0;
    }

    @Override
    public CellGroup.Params getDefaultParams() {
        return new GridGroup.Params();
    }

    @Override
    protected void measure(final int width, final int height) {
        super.measure(width, height);
        final int bW = (int) ((width - paddingLeft - paddingRight - (column - 1) * divider) * 1f / column);
        final int bH = (int) ((height - paddingTop - paddingBottom - (row - 1) * divider) * 1f / row);
        final int size = getCellCount();
        for (int i = 0; i < size; i++) {
            Cell cell = getCellAt(i);
            Params p = (GridGroup.Params) cell.getParams();
            int w = bW * p.columnCount + (p.columnCount - 1) * divider - (p.marginLeft + p.marginRight);
            int h = bH * p.rowCount + (p.rowCount - 1) * divider - (p.marginTop + p.marginBottom);
            cell.measure(w, h);
        }
    }

    @Override
    protected void layout(int x, int y) {
        super.layout(x, y);
        final int bW = (int) ((getMeasureWidth() - paddingLeft - paddingRight - (column - 1) * divider) * 1f / column);
        final int bH = (int) ((getMeasureHeight() - paddingTop - paddingBottom - (row - 1) * divider) * 1f / row);
        final int size = getCellCount();
        for (int i = 0; i < size; i++) {
            Cell cell = getCellAt(i);
            Params p = (GridGroup.Params) cell.getParams();
            int l = left + paddingLeft + p.marginLeft;
            l += p.x * (divider + bW);
            int t = top + paddingTop + p.marginTop;
            t += p.y * (divider + bH);
            cell.layout(l, t);
        }
    }

    public static class Params extends CellGroup.Params {
        @Attribute
        public int x, y;
        @Attribute
        public int columnCount, rowCount;

        Params() {
            this(0, 0, 0, 0);
        }

        public Params(int x, int y, int columnCount, int rowCount) {
            super(0, 0);
            this.x = x;
            this.y = y;
            this.columnCount = columnCount;
            this.rowCount = rowCount;
        }
    }
}