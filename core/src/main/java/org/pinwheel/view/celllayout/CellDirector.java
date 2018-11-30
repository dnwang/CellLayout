package org.pinwheel.view.celllayout;

import android.util.Log;

import java.util.Collection;
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
        Sync.execute(new Sync.Function<Collection<Cell>>() {
            @Override
            public Collection<Cell> call() {
                final long begin = System.nanoTime();
                final Set<Cell> stateChangedCells = new HashSet<>();
                group.foreachAllCells(true, new Filter<Cell>() {
                    @Override
                    public boolean call(Cell cell) {
                        if (cell == group) return false;
                        cell.offset(dx, dy);
                        if (setVisibleState(cell)) {
                            stateChangedCells.add(cell);
                        }
                        return false;
                    }
                });
                Log.e(CellLayout.TAG, "[offset] " + (System.nanoTime() - begin) / 1000000f);
                return stateChangedCells;
            }
        }, new Sync.Action<Collection<Cell>>() {
            @Override
            public void call(Collection<Cell> stateChangedCells) {
                for (Cell cell : stateChangedCells) {
                    onCellVisibleChanged(cell);
                }
            }
        });
        onScroll(group, dx, dy);
        Log.e(CellLayout.TAG, "[scrollTo] " + (System.nanoTime() - begin) / 1000000f);
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

    void notifyScrollComplete() {
        // must be wait scroll action complete !
        Sync.execute(new Sync.Action() {
            @Override
            public void call(Object o) {
                if (null != callback) {
                    callback.onScrollComplete();
                }
            }
        });
    }

    private boolean setVisibleState(Cell cell) {
        final boolean oldState = cell.isVisible();
        final Rect area = new Rect(root);
        area.inset(-area.width() / 5, -area.height() / 5);
        cell.setVisible(Rect.intersects(area, cell));
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

    private void onScroll(CellGroup group, int dx, int dy) {
        if (null != callback) {
            callback.onScroll(group, dx, dy);
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

        void onScroll(CellGroup group, int dx, int dy);

        void onVisibleChanged(Cell cell);

        void onScrollComplete();
    }

}