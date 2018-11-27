package org.pinwheel.view.celllayout;

import android.util.SparseArray;

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
public class CellGroup extends Cell {

    private final List<Cell> subCells = new ArrayList<>();
    // all cell cache
    private final SparseArray<Cell> allCellCache = new SparseArray<>(); // contains self

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
        // cell changed
        allCellCache.clear();
    }

    public void removeCell(Cell cell) {
        if (null == cell) {
            return;
        }
        if (!subCells.contains(cell)) {
            return;
        }
        subCells.remove(cell);
        cell.setParent(null);
        // cell changed
        allCellCache.clear();
    }

    public Cell getCellAt(int order) {
        return subCells.get(order);
    }

    public int getCellCount() {
        return subCells.size();
    }

    int scrollX, scrollY;

    public void scrollTo(int x, int y) {
        scrollX = x;
        scrollY = y;
    }

    @Override
    public Cell findCellById(int cellId) {
        if (0 != allCellCache.size()) {
            return allCellCache.get(cellId);
        } else {
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
        if (0 != allCellCache.size()) {
            final int size = allCellCache.size();
            for (int i = 0; i < size; i++) {
                Cell cell = allCellCache.valueAt(i);
                if (cell instanceof CellGroup) {
                    if (withGroup) {
                        filter.call(cell);
                    }
                } else {
                    filter.call(cell);
                }
            }
        } else {
            if (_foreachAllCells(withGroup, this, filter)) {
                // has intercept, is not all cell
                allCellCache.clear();
            }
        }
    }

    private boolean _foreachAllCells(boolean withGroup, CellGroup group, Filter<Cell> filter) {
        allCellCache.put(group.getId(), group);
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
                allCellCache.put(cell.getId(), cell);
                intercept = filter.call(cell);
            }
            if (intercept) {
                break;
            }
        }
        return intercept;
    }

    public static class Params implements Serializable {
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