package org.pinwheel.view.celllayout;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.util.Log;
import android.util.LongSparseArray;

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
        if (hasRoot()) {
            onCellUnMount(root, null);
        }
        root = cell;
        onCellMount(root, null);
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

    void foreachAllCells(boolean withGroup, Filter<Cell> filter) {
        if (hasRoot()) {
            if (root instanceof CellGroup) {
                ((CellGroup) root).foreachAllCells(withGroup, filter);
            } else {
                filter.call(root);
            }
        }
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

    private final LongSparseArray<ValueAnimator> movingAnimator = new LongSparseArray<>(4);
    private final static float MOVE_SPEED = 1.5f; // px/ms

    void scrollBy(final CellGroup cell, int dx, int dy, final boolean withAnimation) {
        if (null == cell || (0 == dx && 0 == dy)) {
            return;
        }
        if (cell instanceof LinearGroup) {
            LinearGroup linear = (LinearGroup) cell;
            final int contentWidth = linear.getContentWidth();
            final int contentHeight = linear.getContentHeight();
            // fix dx
            int tmp = linear.getScrollX() + dx;
            int max = -(contentWidth - linear.getWidth());
            if (tmp > 0) {
                dx = -linear.getScrollX();
            } else if (tmp < max) {
                dx = max - linear.getScrollX();
            }
            // fix dy
            tmp = linear.getScrollY() + dy;
            max = -(contentHeight - linear.getHeight());
            if (tmp > 0) {
                dy = -linear.getScrollY();
            } else if (tmp < max) {
                dy = max - linear.getScrollY();
            }
        }
        Log.e("CellLayout", "[scrollBy] dx: " + dx + ", dy: " + dy);
        if (!withAnimation || (Math.abs(dx) + Math.abs(dy) < 10)) {
            cell.scrollBy(dx, dy);
        } else {
            ValueAnimator anim = movingAnimator.get(cell.getId());
            if (null != anim) {
                anim.cancel();
            }
            final long duration = (long) (Math.max(Math.abs(dx), Math.abs(dy)) / MOVE_SPEED);
            anim = ValueAnimator.ofPropertyValuesHolder(
                    PropertyValuesHolder.ofInt("x", 0, dx),
                    PropertyValuesHolder.ofInt("y", 0, dy)
            ).setDuration(duration);
            anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                int lastDx, lastDy;

                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    int dx = (int) animation.getAnimatedValue("x");
                    int dy = (int) animation.getAnimatedValue("y");
                    cell.scrollBy(dx - lastDx, dy - lastDy);
                    lastDx = dx;
                    lastDy = dy;
                }
            });
            anim.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationCancel(Animator animation) {
                    remove();
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    remove();
                }

                private void remove() {
                    movingAnimator.remove(cell.getId());
                }
            });
            anim.start();
            movingAnimator.put(cell.getId(), anim);
        }
    }

    void scrollToCenter(final Cell cell, final boolean withAnimation) {
        if (null == cell) {
            return;
        }
        final int centerX = root.getLeft() + root.getWidth() / 2;
        final int centerY = root.getTop() + root.getHeight() / 2;
        final int cellCenterX = cell.getLeft() + cell.getWidth() / 2;
        final int cellCenterY = cell.getTop() + cell.getHeight() / 2;
        if (!withAnimation) {
            scrollBy(findLinearGroupBy(cell, LinearGroup.VERTICAL), 0, centerY - cellCenterY, false);
            scrollBy(findLinearGroupBy(cell, LinearGroup.HORIZONTAL), centerX - cellCenterX, 0, false);
        } else {
            scrollBy(findLinearGroupBy(cell, LinearGroup.VERTICAL), 0, centerY - cellCenterY, true);
            scrollBy(findLinearGroupBy(cell, LinearGroup.HORIZONTAL), centerX - cellCenterX, 0, true);
        }
    }

    void measure(int width, int height) {
        if (hasRoot() && cellChanged) {
            root.measure(width, height);
        }
    }

    private boolean cellChanged;

    void layout(boolean viewChanged, int l, int t, int r, int b) {
        if (hasRoot() && (viewChanged || cellChanged)) {
            root.layout(l, t);
            cellChanged = false;
            //
            refreshState();
        }
    }

    private void refreshState() {
        foreachAllCells(true, new Filter<Cell>() {
            @Override
            public boolean call(Cell cell) {
                cell.updateVisibleSate();
                return false;
            }
        });
    }

    void onCellMount(Cell cell, CellGroup parent) {
        cellChanged = true;
        if (cell instanceof CellGroup) {
            ((CellGroup) cell).foreachAllCells(true, new Filter<Cell>() {
                @Override
                public boolean call(Cell cell) {
                    cell.attach(CellDirector.this);
                    return false;
                }
            });
        } else {
            cell.attach(CellDirector.this);
        }
    }

    void onCellAttached(Cell cell) {
        if (null != callback) {
            callback.onAttached(cell);
        }
    }

    void onCellDetached(Cell cell) {
        if (null != callback) {
            callback.onDetached(cell);
        }
    }

    void onCellUnMount(Cell cell, CellGroup parent) {
        cellChanged = true;
        if (cell instanceof CellGroup) {
            ((CellGroup) cell).foreachAllCells(true, new Filter<Cell>() {
                @Override
                public boolean call(Cell cell) {
                    cell.detach();
                    return false;
                }
            });
        } else {
            cell.detach();
        }
    }

    void onCellPositionChanged(Cell cell, int fromX, int fromY) {
        if (null != callback) {
            callback.onPositionChanged(cell, fromX, fromY);
        }
    }

    void onCellVisibleChanged(Cell cell) {
        if (null != callback) {
            callback.onVisibleChanged(cell);
        }
    }

    interface LifeCycleCallback {
        void onAttached(Cell cell);

        void onPositionChanged(Cell cell, int fromX, int fromY);

        void onVisibleChanged(Cell cell);

        void onDetached(Cell cell);
    }

}