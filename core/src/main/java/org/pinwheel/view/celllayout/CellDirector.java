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
        focusCell = null;
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

    private Cell focusCell = null;

    void setFocus(Cell cell) {
        focusCell = cell;
    }

    Cell getFocus() {
        return focusCell;
    }

    boolean scrollBy(final CellGroup group, final int dx, final int dy) {
        if (null == group) return false;
        return scrollTo(group, group.scrollX + dx, group.scrollY + dy);
    }

    boolean scrollTo(final CellGroup group, final int x, final int y) {
        if (null == group) return false;
        final long begin = System.nanoTime();
        final int scrollX = group.scrollX;
        final int scrollY = group.scrollY;
        group.scrollTo(x, y);
        final int dx = group.scrollX - scrollX;
        final int dy = group.scrollY - scrollY;
        if (0 == dx && 0 == dy) {
            return false;
        }
        group.foreachAllCells(true, new Filter<Cell>() {
            @Override
            public boolean call(Cell cell) {
                if (cell == group) return false; // don't move self
                cell.offset(dx, dy);
                if (setVisibleState(cell)) {
                    onCellVisibleChanged(cell);
                }
                return false;
            }
        });
        onMoved(group, dx, dy);
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
        cell.setVisible(Rect.intersects(root, cell));
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