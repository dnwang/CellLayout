package org.pinwheel.view.celllayout;

import android.util.Log;

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

    private final int[] diff = new int[2];

    void moveBy(final CellGroup group, int dx, int dy) {
        if (null == group) {
            return;
        }
        diff[0] = dx;
        diff[1] = dy;
        group.scrollFix(diff);
        final int newDx = diff[0];
        final int newDy = diff[1];
        if (0 == newDx && 0 == newDy) {
            return;
        }
        group.scrollBy(newDx, newDy);
        group.foreachAllCells(true, new Filter<Cell>() {
            @Override
            public boolean call(Cell cell) {
                if (cell == group) {
                    // don't scroll self
                    return false;
                }
                int fromX = cell.getLeft();
                int fromY = cell.getTop();
                cell.offset(newDx, newDy);
                onCellPositionChanged(cell, fromX, fromY);
                // check visible
                updateVisibleState(cell, false);
                return false;
            }
        });
    }

    void forceLayout() {
        state |= FLAG_NO_LAYOUT;
    }

    void layout(int left, int top, int width, int height) {
        if (hasRoot() && (FLAG_NO_LAYOUT == (state & FLAG_NO_LAYOUT))) {
            Log.e(CellLayout.TAG, "[director.Layout] l: " + left + ", t: " + top + ", w: " + width + ", h: " + height);
            root.measure(width, height);
            root.layout(left, top);
            foreachAllCells(true, new Filter<Cell>() {
                @Override
                public boolean call(Cell cell) {
                    // visible state
                    updateVisibleState(cell, true);
                    return false;
                }
            });
            onCellLayout();
        }
    }

    private void updateVisibleState(Cell cell, boolean force) {
        final boolean oldState = cell.isVisible();
        cell.setVisible(root.getLeft(), root.getTop(), root.getRight(), root.getBottom());
        if (force || oldState != cell.isVisible()) {
            onCellVisibleChanged(cell);
        }
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

    private void onCellPositionChanged(Cell cell, int fromX, int fromY) {
        if (null != callback) {
            callback.onPositionChanged(cell, fromX, fromY);
        }
    }

    private void onCellVisibleChanged(final Cell cell) {
        if (null != callback) {
            callback.onVisibleChanged(cell);
        }
    }

    interface LifeCycleCallback {
        void onCellLayout();

        void onMoveComplete();

        void onPositionChanged(Cell cell, int fromX, int fromY);

        void onVisibleChanged(Cell cell);
    }

}