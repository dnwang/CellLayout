package org.pinwheel.view.celllayout;

/**
 * Copyright (C), 2018 <br>
 * <br>
 * All rights reserved <br>
 * <br>
 *
 * @author dnwang
 * @version 2018/12/10,15:02
 */
public final class GridCell extends LinearGroup {

    private int column;

    private int itemHeight = 200;

    GridCell() {
        this(LinearGroup.VERTICAL, 1);
    }

    public GridCell(int orientation, int column) {
        super(orientation);
        this.column = Math.max(1, column);
    }

    private int count;

    public void reSize(int newCount) {
        if (count == newCount || newCount < 0) return;
        int dSize = newCount - count;
        if (dSize > 0) {
            append(dSize);
        } else {
            while (dSize < 0) {
                removeCellInner(getCellAt(getCellCount() - 1));
                dSize++;
            }
        }
        requestMeasureAndLayout();
        count = newCount;
    }

    private void append(int c) {
        GridGroup lastLine = this.count <= 0 ? null : (GridGroup) getCellAt(getCellCount() - 1);
        int sum = null == lastLine ? 0 : column - count;
        if (LinearGroup.HORIZONTAL == getOrientation()) {
            // TODO: 2018/12/10
        } else {
            if (0 != sum) {
                for (int i = 0; i < sum; i++) {
                    lastLine.addCellInner(new Cell(), new GridGroup.Params(column - sum + i, 0, 1, 1));
                    c--;
                }
            }
            GridGroup newLine = null;
            while (c > 0) {
                if (null == newLine || newLine.getCellCount() == column) {
                    if (null != newLine) {
                        addCellInner(newLine, new LinearGroup.Params(itemHeight, itemHeight));
                    }
                    newLine = new GridGroup(1, column);
                    newLine.setDivider(getDivider());
                }
                newLine.addCellInner(new Cell(), new GridGroup.Params(column - 1 - c % column, 0, 1, 1));
                c--;
            }
            if (null != newLine && null == newLine.getParent()) {
                addCellInner(newLine, new LinearGroup.Params(itemHeight, itemHeight));
            }
        }
    }

    private Cell createLine(int count) {
        final GridGroup group;
        if (LinearGroup.HORIZONTAL == getOrientation()) {
            group = new GridGroup(column, 1);
            for (int i = 0; i < count; i++) {
                group.addCellInner(new Cell(), new GridGroup.Params(0, i, 1, 1));
            }
        } else {
            group = new GridGroup(1, column);
            for (int i = 0; i < count; i++) {
                group.addCellInner(new Cell(), new GridGroup.Params(i, 0, 1, 1));
            }
        }
        group.setDivider(getDivider());
        return group;
    }

}