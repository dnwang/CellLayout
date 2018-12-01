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

    private int state = 0;

    void setCallback(LifeCycleCallback callback) {
        this.callback = callback;
    }

    boolean hasRoot() {
        return null != root;
    }

    void setRoot(Cell cell) {
        root = cell;
    }

    Cell getRoot() {
        return root;
    }

    void forceLayout() {
        if (!hasRoot()) return;
        root.forceLayout();
    }

    void measure(int width, int height) {
        if (!hasRoot() || root.isMeasured()) return;
        Log.d(CellLayout.TAG, "[director.measure] w: " + width + ", h: " + height);
        root.measure(width, height);
    }

    void layout(int x, int y) {
        if (!hasRoot() || root.isLayout()) return;
        Log.d(CellLayout.TAG, "[director.layout] x: " + x + ", y: " + y);
        root.layout(x, y, 0, 0);
        invalidate();
    }

    private void invalidate() {
        Log.d(CellLayout.TAG, "[director.invalidate]");
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
        onRefreshAll();
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
        return scrollTo(group, group.getScrollX() + dx, group.getScrollY() + dy);
    }

    boolean scrollTo(final CellGroup group, final int x, final int y) {
        if (null == group) return false;
        final int scrollX = group.getScrollX();
        final int scrollY = group.getScrollY();
        group.scrollTo(x, y);
        final int dx = group.getScrollX() - scrollX;
        final int dy = group.getScrollY() - scrollY;
        if (0 == dx && 0 == dy) {
            return false;
        }
        scrollingGroups.add(group);
        Sync.execute(new Sync.Function<Collection<Cell>>() {
            @Override
            public Collection<Cell> call() {
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
                return stateChangedCells;
            }
        }, new Sync.Action<Collection<Cell>>() {
            @Override
            public void call(Collection<Cell> stateChangedCells) {
                for (Cell cell : stateChangedCells) {
                    onCellVisibleChanged(cell);
                }
                // sync notify, don't use group.getScroll() in method
                notifyGroupScroll(group, dx, dy);
            }
        });
        return true;
    }

    private final Set<CellGroup> scrollingGroups = new HashSet<>(2);

    private void notifyGroupScroll(CellGroup group, int dx, int dy) {
        if (null != group && null != group.onScrollListener) {
            group.onScrollListener.onScroll(group, dx, dy);
        }
    }

    private void notifyGroupScrollComplete(CellGroup group) {
        if (null != group && null != group.onScrollListener) {
            group.onScrollListener.onScrollComplete(group);
            if (group instanceof LinearGroup) {
                final LinearGroup linear = (LinearGroup) group;
                if (LinearGroup.HORIZONTAL == linear.getOrientation()) {
                    int scrollX = group.getScrollX();
                    if (0 <= scrollX) { // left
                        group.onScrollListener.onScrollToStart(group);
                    } else if (scrollX <= group.width() - linear.getContentWidth()) { // right
                        group.onScrollListener.onScrollToEnd(group);
                    }
                } else {
                    int scrollY = group.getScrollY();
                    if (0 <= scrollY) { // top
                        group.onScrollListener.onScrollToStart(group);
                    } else if (scrollY <= group.height() - linear.getContentHeight()) { // bottom
                        group.onScrollListener.onScrollToEnd(group);
                    }
                }
            }
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
                // notify outside listener
                for (CellGroup group : scrollingGroups) {
                    notifyGroupScrollComplete(group);
                }
                scrollingGroups.clear();
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

    private void onRefreshAll() {
        if (null != callback) {
            callback.onRefreshAll();
        }
    }

    private void onCellVisibleChanged(final Cell cell) {
        if (null != callback) {
            callback.onVisibleChanged(cell);
        }
    }

    interface LifeCycleCallback {
        void onRefreshAll();

        void onVisibleChanged(Cell cell);

        void onScrollComplete();
    }

}