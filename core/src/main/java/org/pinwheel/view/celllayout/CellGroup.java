package org.pinwheel.view.celllayout;

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
public class CellGroup extends Cell implements Movable {

    private final List<Cell> subCells = new ArrayList<>();

    CellGroup() {
        super();
    }

    public void addCell(Cell cell) {
        addCell(cell, getDefaultParams());
    }

    protected CellGroup.Params getDefaultParams() {
        return new Params();
    }

    public void addCell(Cell cell, Params p) {
        final long id = null == cell ? -1 : cell.getId();
        if (id <= 0) {
            throw new IllegalStateException("cell id error !");
        }
        if (null == p) {
            throw new IllegalStateException("cell must be have Params !");
        }
        if (null != cell.getParent()) {
            throw new IllegalStateException("already has parent !");
        }
        cell.setParent(this);
        cell.setParams(p);
        subCells.add(cell);
        //
        final CellDirector director = getDirector();
        if (null != director) {
            director.onCellMount(cell, this);
        }
    }

    public void removeCell(Cell cell) {
        if (null == cell) {
            return;
        }
        final boolean has = subCells.contains(cell);
        if (!has) {
            return;
        }
        subCells.remove(cell);
        cell.setParent(null);
        //
        final CellDirector director = getDirector();
        if (null != director) {
            director.onCellUnMount(cell, this);
        }
    }

    public Cell getCellAt(int order) {
        return subCells.get(order);
    }

    public int getCellCount() {
        return subCells.size();
    }

    private int scrollX, scrollY;

    @Override
    public void scrollBy(final int dx, final int dy) {
        if (0 == dx && 0 == dy) {
            return;
        }
        final int size = getCellCount();
        for (int i = 0; i < size; i++) {
            Cell cell = getCellAt(i);
            if (cell instanceof CellGroup) {
                ((CellGroup) cell).foreachAllCells(true, new Filter<Cell>() {
                    @Override
                    public boolean call(Cell cell) {
                        cell.offset(dx, dy);
                        cell.updateVisibleSate();
                        return false;
                    }
                });
            } else {
                cell.offset(dx, dy);
                cell.updateVisibleSate();
            }
        }
        scrollX += dx;
        scrollY += dy;
    }

    @Override
    public void scrollTo(int x, int y) {
        int left = getLeft() + getScrollX(), top = getTop() + getScrollY();
        if (left == x && top == y) {
            return;
        }
        scrollBy(x - left, y - top);
    }

    @Override
    public int getScrollX() {
        return scrollX;
    }

    @Override
    public int getScrollY() {
        return scrollY;
    }

    @Override
    public Cell findCellById(long id) {
        Cell target = super.findCellById(id);
        if (null == target) {
            for (Cell cell : subCells) {
                if (cell.getId() == id) {
                    target = cell;
                    break;
                } else if (cell instanceof CellGroup) {
                    target = cell.findCellById(id);
                    if (null != target) {
                        break;
                    }
                }
            }
        }
        return target;
    }

    public final void foreachAllCells(boolean withGroup, Filter<Cell> filter) {
        _foreachAllCells(withGroup, this, filter);
    }

    private boolean _foreachAllCells(boolean withGroup, CellGroup group, Filter<Cell> filter) {
        boolean intercept = withGroup && filter.call(group);
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

    public static class Params {
        @Attribute
        public int width, height;
        @Attribute
        public int marginLeft, marginTop, marginRight, marginBottom;

        Params() {
            this(0, 0);
        }

        public Params(int width, int height) {
            this.width = width;
            this.height = height;
        }
    }

}