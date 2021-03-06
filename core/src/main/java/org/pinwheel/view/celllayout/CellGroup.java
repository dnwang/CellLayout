package org.pinwheel.view.celllayout;

import android.graphics.Rect;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Copyright (C), 2018 <br>
 * <br>
 * All rights reserved <br>
 * <br>
 *
 * @author dnwang
 * @version 2018/11/15,14:01
 */
public class CellGroup extends Cell implements Scrollable, Mask {

    @Attribute("mask")
    boolean openMask = false;

    private final List<Cell> subCells = new ArrayList<>();

    OnScrollListener onScrollListener;

    CellGroup() {
        super();
    }

    public void merge(Cell cell) {
        final CellGroup.Params p = cell.getParams();
        if (null != p) {
            addCell(cell, p);
        } else {
            addCell(cell);
        }
    }

    public void addCell(Cell cell) {
        addCell(cell, getDefaultParams());
    }

    protected CellGroup.Params getDefaultParams() {
        return new Params();
    }

    public void addCell(Cell cell, Params p) {
        addCellInner(cell, p);
        requestMeasureAndLayout();
    }

    final void addCellInner(Cell cell, Params p) {
        final int id = null == cell ? -1 : cell.getId();
        if (id <= 0) {
            throw new IllegalStateException("cell id error !");
        }
        if (null == p) {
            throw new IllegalStateException("cell must be have Params !");
        }
        if (null != cell.getParent()) {
            throw new IllegalStateException("cell already has parent !");
        }
        cell.setParent(this);
        cell.setParams(p);
        subCells.add(cell);
    }

    public boolean removeCell(Cell cell) {
        final boolean result = removeCellInner(cell);
        if (result) {
            requestMeasureAndLayout();
        }
        return result;
    }

    final boolean removeCellInner(Cell cell) {
        if (null == cell) {
            return false;
        }
        if (!subCells.contains(cell)) {
            return false;
        }
        subCells.remove(cell);
        cell.setParent(null);
        return true;
    }

    public Cell getCellAt(int order) {
        return subCells.get(order);
    }

    public int getCellCount() {
        return subCells.size();
    }

    private int scrollX, scrollY;

    @Override
    public void scrollTo(int x, int y) {
        scrollX = x;
        scrollY = y;
    }

    @Override
    public int getScrollX() {
        return scrollX;
    }

    @Override
    public int getScrollY() {
        return scrollY;
    }

    /**
     * {@link CellGroup#getClipRectBy(Cell, boolean)}
     */
    @Deprecated
    @Override
    public Rect getClipRect() {
        return null;
    }

    @Override
    public android.graphics.Rect getClipRectBy(Cell cell, boolean isMoving) {
        return null;
    }

    @Override
    public void measureContent() {
    }

    @Override
    public int getContentWidth() {
        return width();
    }

    @Override
    public int getContentHeight() {
        return height();
    }

    @Override
    public Cell findCellById(int cellId) {
        Cell target = super.findCellById(cellId);
        if (null == target) {
            for (Cell cell : subCells) {
                if (cell.getId() == cellId) {
                    target = cell;
                    break;
                } else if (cell instanceof CellGroup) {
                    target = cell.findCellById(cellId);
                    if (null != target) {
                        break;
                    }
                }
            }
        }
        return target;
    }

    final void foreachSubCells(boolean withGroup, Filter<Cell> filter) {
        final int size = subCells.size();
        for (int i = 0; i < size; i++) {
            Cell cell = subCells.get(i);
            boolean intercept;
            if ((cell instanceof CellGroup) && withGroup) {
                intercept = filter.call(cell);
            } else {
                intercept = filter.call(cell);
            }
            if (intercept) {
                break;
            }
        }
    }

    final void foreachAllCells(boolean withGroup, Filter<Cell> filter) {
        _foreachAllCells(withGroup, this, filter);
    }

    private boolean _foreachAllCells(boolean withGroup, CellGroup group, Filter<Cell> filter) {
        boolean intercept = false;
        if (withGroup) {
            intercept = filter.call(group);
        }
        if (intercept) {
            return true;
        }
        final int size = group.getCellCount();
        for (int i = 0; i < size; i++) {
            Cell cell = group.getCellAt(i);
            if (cell instanceof CellGroup) {
                intercept = _foreachAllCells(withGroup, (CellGroup) cell, filter);
            } else {
                intercept = filter.call(cell);
            }
            if (intercept) {
                break;
            }
        }
        return intercept;
    }

    public void setOnScrollListener(OnScrollListener onScrollListener) {
        this.onScrollListener = onScrollListener;
    }

    public static class Params implements Serializable {
        @Attribute("style")
        public int styleId = StyleAdapter.DEF_STYLE_ID;
        @Attribute(fixedResolution = true)
        public int width, height;
        @Attribute(fixedResolution = true)
        public int marginLeft, marginTop, marginRight, marginBottom;

        Params() {
            this(0, 0);
        }

        public Params(int width, int height) {
            this.width = width;
            this.height = height;
        }
    }

    public interface OnScrollListener {
        void onScroll(CellGroup group, int dx, int dy);

        void onScrollComplete(CellGroup group);

        void onScrollToStart(CellGroup group);

        void onScrollToEnd(CellGroup group);
    }

    public static class OnScrollAdapter implements OnScrollListener {
        @Override
        public void onScroll(CellGroup group, int dx, int dy) {
        }

        @Override
        public void onScrollComplete(CellGroup group) {
        }

        @Override
        public void onScrollToStart(CellGroup group) {
        }

        @Override
        public void onScrollToEnd(CellGroup group) {
        }
    }

}