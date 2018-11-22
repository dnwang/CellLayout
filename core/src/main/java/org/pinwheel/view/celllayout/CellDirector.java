package org.pinwheel.view.celllayout;

import android.util.Log;

import java.util.HashSet;
import java.util.Set;

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

    void moveBy(final CellGroup group, int dx, int dy) {
        if (null == group) {
            return;
        }
        offset[0] = dx;
        offset[1] = dy;
        group.fixScrollOffset(offset);
        final int newDx = offset[0];
        final int newDy = offset[1];
        if (0 == newDx && 0 == newDy) {
            return;
        }
        group.scrollBy(newDx, newDy);
        long begin = System.nanoTime();
//        group.foreachAllCells(true, new Filter<Cell>() {
//            @Override
//            public boolean call(Cell cell) {
//                if (cell == group) {
//                    // don't scroll self
//                    return false;
//                }
//                cell.offset(newDx, newDy);
//                onCellPositionChanged(cell);
//                // check visible
//                updateVisibleState(cell, false);
//                return false;
//            }
//        });
//        Log.e(CellLayout.TAG, "old : " + (System.nanoTime() - begin) / 1000000f);

        // ---------------------------------------------

        begin = System.nanoTime();
        final Rect changeArea = group.getRect();
        if (dy > 0) {
            changeArea.top -= Math.abs(dy);
        } else {
            changeArea.bottom += Math.abs(dy);
        }
        final Set<Cell> cells = new HashSet<>();
        group.foreachAllCells(true, new Filter<Cell>() {
            @Override
            public boolean call(Cell cell) {
                if (cell == group) {// don't move self
                    return false;
                }
                if (Rect.intersects(changeArea, cell)) {
                    cells.add(cell);
                }
                cell.offset(newDx, newDy);// after compare
                return false;
            }
        });
        Log.e(CellLayout.TAG, "find rect cells: " + (System.nanoTime() - begin) / 1000000f);
        begin = System.nanoTime();
        for (Cell cell : cells) {
            onCellPositionChanged(cell);
            // check visible
            updateVisibleState(cell, false);
        }
        Log.e(CellLayout.TAG, "apply: " + (System.nanoTime() - begin) / 1000000f);
    }

    void forceLayout() {
        state |= FLAG_NO_LAYOUT;
    }

    void layout(int left, int top, int width, int height) {
        if (hasRoot() && (FLAG_NO_LAYOUT == (state & FLAG_NO_LAYOUT))) {
            Log.e(CellLayout.TAG, "[director.Layout] l: " + left + ", t: " + top + ", w: " + width + ", h: " + height);
            root.measure(width, height);
            root.layout(left, top);
            //
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
        cell.setVisible(root.left, root.top, root.right, root.bottom);
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

    private void onCellPositionChanged(Cell cell) {
        if (null != callback) {
            callback.onPositionChanged(cell);
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

        void onPositionChanged(Cell cell);

        void onVisibleChanged(Cell cell);
    }

}