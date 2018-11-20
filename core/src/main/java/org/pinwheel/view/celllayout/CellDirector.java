package org.pinwheel.view.celllayout;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;

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

    private ValueAnimator movingAnimator;
    private final static float MOVE_SPEED = 1.5f; // px/ms

    void moveToCenter(final Cell cell, final boolean withAnimation) {
        if (null == cell) {
            return;
        }
        final int centerX = root.getLeft() + root.getWidth() / 2;
        final int centerY = root.getTop() + root.getHeight() / 2;
        final int cellCenterX = cell.getLeft() + cell.getWidth() / 2;
        final int cellCenterY = cell.getTop() + cell.getHeight() / 2;
        if (!withAnimation) {
            moveBy(findLinearGroupBy(cell, LinearGroup.VERTICAL), 0, centerY - cellCenterY);
            moveBy(findLinearGroupBy(cell, LinearGroup.HORIZONTAL), centerX - cellCenterX, 0);
            onMoveComplete();
        } else {
            final LinearGroup vLinear = findLinearGroupBy(cell, LinearGroup.VERTICAL);
            final int dy = centerY - cellCenterY;
            final LinearGroup hLinear = findLinearGroupBy(cell, LinearGroup.HORIZONTAL);
            final int dx = centerX - cellCenterX;
            if (null != movingAnimator) {
                movingAnimator.cancel();
            }
            final long duration = (long) (Math.max(Math.abs(dx), Math.abs(dy)) / MOVE_SPEED);
            movingAnimator = ValueAnimator.ofPropertyValuesHolder(
                    PropertyValuesHolder.ofInt("x", 0, dx),
                    PropertyValuesHolder.ofInt("y", 0, dy)
            ).setDuration(duration);
            movingAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                int lastDx, lastDy;

                @Override
                public void onAnimationUpdate(ValueAnimator anim) {
                    int dx = (int) anim.getAnimatedValue("x");
                    int dy = (int) anim.getAnimatedValue("y");
                    moveBy(hLinear, dx - lastDx, 0);
                    moveBy(vLinear, 0, dy - lastDy);
                    lastDx = dx;
                    lastDy = dy;
                }
            });
            movingAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    onMoveComplete();
                }
            });
            movingAnimator.start();
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

    void measure(int width, int height) {
        if (hasRoot()) {
            root.measure(width, height);
        }
    }

    void layout(int l, int t, int r, int b) {
        if (hasRoot()) {
            root.layout(l, t);
        }
    }

    void refreshState(final boolean force) {
        foreachAllCells(true, new Filter<Cell>() {
            @Override
            public boolean call(Cell cell) {
                // visible state
                updateVisibleState(cell, force);
                return false;
            }
        });
        onStateChanged();
    }

    private void updateVisibleState(Cell cell, boolean force) {
        final boolean oldState = cell.isVisible();
        cell.setVisible(root.getLeft(), root.getTop(), root.getRight(), root.getBottom());
        if (force || oldState != cell.isVisible()) {
            onCellVisibleChanged(cell);
        }
    }

    private void reLayout() {
        if (hasRoot()) {
            measure(root.getWidth(), root.getHeight());
            layout(root.getLeft(), root.getTop(), root.getRight(), root.getBottom());
            refreshState(true);
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

    void onStateChanged() {
        if (null != callback) {
            callback.onStateChanged();
        }
    }

    void onMoveComplete() {
        CellLayout.time("onMoveComplete", new Runnable() {
            @Override
            public void run() {
                if (null != callback) {
                    callback.onMoveComplete();
                }
            }
        });
    }

    private void onCellPositionChanged(Cell cell, int fromX, int fromY) {
        if (null != callback) {
            callback.onPositionChanged(cell, fromX, fromY);
        }
    }

    private void onCellVisibleChanged(final Cell cell) {
        CellLayout.time("onCellVisibleChanged", new Runnable() {
            @Override
            public void run() {
                if (null != callback) {
                    callback.onVisibleChanged(cell);
                }
            }
        });

    }

    interface LifeCycleCallback {
        void onStateChanged();

        void onMoveComplete();

        void onPositionChanged(Cell cell, int fromX, int fromY);

        void onVisibleChanged(Cell cell);
    }

}