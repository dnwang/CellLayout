package org.pinwheel.view.celllayout;

import android.util.Log;

import java.util.Collection;
import java.util.HashSet;

/**
 * Copyright (C), 2018 <br>
 * <br>
 * All rights reserved <br>
 * <br>
 *
 * @author dnwang
 * @version 2018/11/16,8:32
 */
final class CellDirector {

    private Cell root;
    private LifeCycleCallback callback;

    private static final int FLAG_NO_LAYOUT = 1;
    private int state = 0;

    boolean hasRoot() {
        return null != root;
    }

    void setRoot(Cell cell) {
        root = cell;
    }

    Cell getRoot() {
        return root;
    }

    void setCallback(LifeCycleCallback callback) {
        this.callback = callback;
    }

    Cell findCellById(long id) {
        return hasRoot() ? root.findCellById(id) : null;
    }

    private Cell tmp = null;

    Cell findCellByPosition(final int x, final int y) {
        tmp = null;
        foreachAllCells(false, new Filter<Cell>() {
            @Override
            public boolean call(Cell cell) {
                if (cell.contains(x, y)) {
                    tmp = cell;
                    return true;
                }
                return false;
            }
        });
        return tmp;
    }

    LinearGroup findLinearGroupBy(Cell cell, final int orientation) {
        Cell parent = null != cell ? cell.getParent() : null;
        if (parent instanceof LinearGroup
                && orientation == ((LinearGroup) parent).getOrientation()) {
            return (LinearGroup) parent;
        } else if (null != parent) {
            return findLinearGroupBy(parent, orientation);
        } else {
            return null;
        }
    }

    private final int[] offset = new int[2];

    boolean moveBy(final CellGroup group, int tmpDx, int tmpDy) {
        if (null == group) return false;
        final long begin = System.nanoTime();
        offset[0] = tmpDx;
        offset[1] = tmpDy;
        group.fixScrollOffset(offset);
        final int dx = offset[0];
        final int dy = offset[1];
        if (0 == dx && 0 == dy) {
            return false;
        }
        final Collection<Cell> stateChangedCells = new HashSet<>();
        group.foreachAllCells(true, new Filter<Cell>() {
            @Override
            public boolean call(Cell cell) {
                if (cell == group) return false; // don't move self
                cell.offset(dx, dy);
                boolean stateChanged = setVisibleState(cell);
                if (stateChanged) {
                    stateChangedCells.add(cell);
                }
                return false;
            }
        });
        // move first
        group.scrollBy(dx, dy);
        onMoved(group, dx, dy);
        // update visible
        for (Cell cell : stateChangedCells) {
            onCellVisibleChanged(cell);
        }
        Log.e(CellLayout.TAG, "[moveBy] " + (System.nanoTime() - begin) / 1000000f);
        return true;
    }

    void forceLayout() {
        state |= FLAG_NO_LAYOUT;
    }

    void layout(int left, int top, int width, int height) {
        if (hasRoot() && (state & FLAG_NO_LAYOUT) != 0) {
            Log.e(CellLayout.TAG, "[director.Layout] l: " + left + ", t: " + top + ", w: " + width + ", h: " + height);
            state &= ~FLAG_NO_LAYOUT; // clear flag
            root.measure(width, height);
            root.layout(left, top);
            //
            foreachAllCells(true, new Filter<Cell>() {
                @Override
                public boolean call(Cell cell) {
                    // set visible state
                    setVisibleState(cell);
                    // force notify outSide
                    onCellVisibleChanged(cell);
                    return false;
                }
            });
            onCellLayout();
        }
    }

    private boolean setVisibleState(Cell cell) {
        final boolean oldState = cell.isVisible();
        cell.setVisible(root);
        return oldState != cell.isVisible();
    }

    private void foreachAllCells(boolean withGroup, Filter<Cell> filter) {
        if (hasRoot()) {
            if (root instanceof CellGroup) {
                ((CellGroup) root).foreachAllCells(withGroup, filter);
            } else {
                filter.call(root);
            }
        }
    }

    private void onMoved(CellGroup group, int dx, int dy) {
        if (null != callback) {
            callback.onMoved(group, dx, dy);
        }
    }

    void onMoveComplete() {
        if (null != callback) {
            callback.onMoveComplete();
        }
    }

    private void onCellLayout() {
        if (null != callback) {
            callback.onCellLayout();
        }
    }

    private void onCellVisibleChanged(final Cell cell) {
        if (null != callback) {
            callback.onVisibleChanged(cell);
        }
    }

    interface LifeCycleCallback {
        void onCellLayout();

        void onMoved(CellGroup group, int dx, int dy);

        void onVisibleChanged(Cell cell);

        void onMoveComplete();
    }

}