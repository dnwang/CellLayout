package org.pinwheel.view.celllayout;

import android.content.Context;
import android.graphics.Point;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Copyright (C), 2018 <br>
 * <br>
 * All rights reserved <br>
 * <br>
 *
 * @author dnwang
 * @version 2018/11/15,11:21
 */
public class CellLayout extends ViewGroup {
    private static final String TAG = "CellLayout";

    public static void time(String tag, Runnable runnable) {
        final long begin = System.nanoTime();
        runnable.run();
        Log.e(TAG, "TIME >> " + tag + ": " + (System.nanoTime() - begin) / 1000000f);
    }

    public CellLayout(Context context) {
        super(context);
        this.init();
    }

    public CellLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.init();
    }

    public CellLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.init();
    }

    private final CellDirector director = new CellDirector();
    private final ViewManager manager = new ViewManager();

    private void init() {
        director.setCallback(manager);
    }

    public void setAdapter(ViewAdapter adapter) {
        manager.setAdapter(adapter);
    }

    public void setRootCell(Cell root) {
        root.measure(getMeasuredWidth(), getMeasuredHeight());
        director.setRoot(root);
    }

    public Cell findCellById(long id) {
        return director.findCellById(id);
    }

    public View findViewByCell(Cell cell) {
        return manager.findViewByCell(cell);
    }

    public void moveToCenter(View view, boolean anim) {
        moveToCenter(manager.findCellByView(view), anim);
    }

    public void moveToCenter(final Cell cell, final boolean anim) {
        time("moveToCenter", new Runnable() {
            @Override
            public void run() {
                director.moveToCenter(cell, anim);
            }
        });
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        Log.e(TAG, "onMeasure: ");
        director.measure(getMeasuredWidth(), getMeasuredHeight());
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        Log.e(TAG, "onLayout: " + changed);
        if (changed) {
            director.layout(l, t, r, b);
            director.refreshState(true);
        }
    }

    @Override
    public void scrollTo(int x, int y) {
        if (director.hasRoot()) {
            final Cell root = director.getRoot();
            if (root instanceof CellGroup) {
                CellGroup cell = (CellGroup) root;
                int left = cell.getLeft() + cell.getScrollX();
                int top = cell.getTop() + cell.getScrollY();
                director.moveBy(cell, x - left, y - top);
            }
        }
    }

    @Override
    public void scrollBy(int x, int y) {
        if (director.hasRoot()) {
            final Cell root = director.getRoot();
            if (root instanceof CellGroup) {
                director.moveBy((CellGroup) root, x, y);
            }
        }
    }

    private final Point tmpPoint = new Point();
    private Cell touchCell = null;
    private boolean isMoving = false;

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        final boolean superState = super.dispatchTouchEvent(event);
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                getParent().requestDisallowInterceptTouchEvent(true);
                tmpPoint.set((int) event.getX(), (int) event.getY());
                if (null == touchCell) {
                    touchCell = director.findCellByPosition(tmpPoint.x, tmpPoint.y);
                }
                return true;// can not return superState.
            case MotionEvent.ACTION_MOVE:
                int dx = (int) event.getX() - tmpPoint.x;
                int dy = (int) event.getY() - tmpPoint.y;
                int absDx = Math.abs(dx);
                int absDy = Math.abs(dy);
                if (isMoving || absDx > 10 || absDy > 10) {
                    isMoving = true;
                    int dir = absDx > absDy ? LinearGroup.HORIZONTAL : LinearGroup.VERTICAL;
                    director.moveBy(director.findLinearGroupBy(touchCell, dir), dx, dy);
                    tmpPoint.set((int) event.getX(), (int) event.getY());
                } else {
                    getParent().requestDisallowInterceptTouchEvent(false);
                }
                return superState;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                touchCell = null;
                if (isMoving) {
                    director.onMoveComplete();
                }
                isMoving = false;
                getParent().requestDisallowInterceptTouchEvent(false);
                return superState;
            default:
                return superState;
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        boolean superState = super.onInterceptTouchEvent(event);
        switch (event.getAction()) {
            case MotionEvent.ACTION_MOVE:
                return isMoving;
            default:
                return superState;
        }
    }

    public interface ViewAdapter {
        View getHolderView(Cell cell);

        int getViewPoolId(Cell cell);

        View onCreateView(Cell cell);

        void onBindView(Cell cell, View view);

        void onViewRecycled(Cell cell, View view);
    }

    private final class ViewManager implements CellDirector.LifeCycleCallback {
        private ViewAdapter adapter;
        private final SparseArray<ViewPool> cellPool = new SparseArray<>();
        private final HashMap<Cell, ViewHolder> cellViewMap = new HashMap<>();

        void setAdapter(ViewAdapter adapter) {
            this.adapter = adapter;
        }

        View findViewByCell(Cell cell) {
            ViewHolder holder = cellViewMap.get(cell);
            return null != holder ? holder.view : null;
        }

        Cell findCellByView(View view) {
            if (null != view) {
                Set<Map.Entry<Cell, ViewHolder>> entrySet = cellViewMap.entrySet();
                for (Map.Entry<Cell, ViewHolder> entry : entrySet) {
                    if (entry.getValue().view == view) {
                        return entry.getKey();
                    }
                }
            }
            return null;
        }

        @Override
        public void onStateChanged() {
            final Set<Map.Entry<Cell, ViewHolder>> entrySet = cellViewMap.entrySet();
            for (Map.Entry<Cell, ViewHolder> entry : entrySet) {
                final ViewHolder holder = entry.getValue();
                if (holder.state == 0) {
                    // replace holder view
                    replaceHolderView(entry.getKey(), holder);
                }
            }
        }

        @Override
        public void onPositionChanged(Cell cell, int fromX, int fromY) {
            final View view = findViewByCell(cell);
            if (null != view) {
                view.offsetLeftAndRight(cell.getLeft() - fromX);
                view.offsetTopAndBottom(cell.getTop() - fromY);
            }
        }

        @Override
        public void onVisibleChanged(final Cell cell) {
            if (cell instanceof CellGroup) {
                // don't care group
                return;
            }
            if (cell.isVisible()) {
                // add holder
                addHolderView(cell);
            } else {
                // remove
                cellViewMap.remove(cell);
                recycleView(cell, findViewByCell(cell), getPool(adapter.getViewPoolId(cell)));
            }
        }

        @Override
        public void onMoveComplete() {
            onStateChanged();
        }

        private static final int POOL_ID_HOLDER_VIEW = Integer.MAX_VALUE;

        private ViewPool getPool(final int poolId) {
            ViewPool pool = cellPool.get(poolId);
            if (null == pool) {
                pool = new ViewPool(POOL_ID_HOLDER_VIEW == poolId ? 10 : 6);
                cellPool.put(poolId, pool);
            }
            return pool;
        }

        private void replaceHolderView(final Cell cell, final ViewHolder holder) {
            View view = getPool(adapter.getViewPoolId(cell)).acquire();
            if (null == view) {
                view = adapter.onCreateView(cell);
                if (null == view) {
                    throw new IllegalStateException("Adapter.onCreateView() can't return null !");
                }
            }
            adapter.onBindView(cell, view);
            final View v = view;
            time("replaceHolderView", new Runnable() {
                @Override
                public void run() {
                    long time = System.nanoTime();
                    CellLayout.this.addViewInLayout(v, -1, generateDefaultLayoutParams(), true);
                    Log.e(TAG, "run: add: " + (System.nanoTime() - time) / 1000000f);
                    time = System.nanoTime();
                    v.measure(
                            MeasureSpec.makeMeasureSpec(cell.getWidth(), MeasureSpec.EXACTLY),
                            MeasureSpec.makeMeasureSpec(cell.getHeight(), MeasureSpec.EXACTLY)
                    );
                    Log.e(TAG, "run: measure: " + (System.nanoTime() - time) / 1000000f);
                    time = System.nanoTime();
                    v.layout(cell.getLeft(), cell.getTop(), cell.getRight(), cell.getBottom());
                    Log.e(TAG, "run: layout: " + (System.nanoTime() - time) / 1000000f);
                }
            });
            recycleView(cell, holder.view, getPool(POOL_ID_HOLDER_VIEW));
            holder.state = 1;
            holder.view = view;
        }

        private void addHolderView(final Cell cell) {
            View holderView = getPool(POOL_ID_HOLDER_VIEW).acquire();
            if (null == holderView) {
                holderView = adapter.getHolderView(cell);
                if (null == holderView) {
                    throw new IllegalStateException("Adapter.getHolderView() can't return null !");
                }
            }
            final View v = holderView;
            time("addHolderView", new Runnable() {
                @Override
                public void run() {
                    CellLayout.this.addViewInLayout(v, -1, generateDefaultLayoutParams(), true);
                    v.measure(
                            MeasureSpec.makeMeasureSpec(cell.getWidth(), MeasureSpec.EXACTLY),
                            MeasureSpec.makeMeasureSpec(cell.getHeight(), MeasureSpec.EXACTLY)
                    );
                    v.layout(cell.getLeft(), cell.getTop(), cell.getRight(), cell.getBottom());
                }
            });
            cellViewMap.put(cell, new ViewHolder(holderView));
        }

        private void recycleView(Cell cell, View view, ViewPool pool) {
            if (null != view) {
                CellLayout.this.removeViewInLayout(view);
                adapter.onViewRecycled(cell, view);
                if (null != pool) {
                    pool.release(view);
                }
            }
        }
    }

    private static final class ViewPool {
        private final View[] caches;
        private int maxSize;

        ViewPool(int maxPoolSize) {
            if (maxPoolSize <= 0) {
                throw new IllegalArgumentException("The max pool size must be > 0");
            }
            caches = new View[maxPoolSize];
        }

        View acquire() {
            if (maxSize > 0) {
                final int lastPooledIndex = maxSize - 1;
                View instance = caches[lastPooledIndex];
                caches[lastPooledIndex] = null;
                maxSize--;
                return instance;
            }
            return null;
        }

        boolean release(View instance) {
            if (isInPool(instance)) {
                throw new IllegalStateException("Already in the pool!");
            }
            if (maxSize < caches.length) {
                caches[maxSize] = instance;
                maxSize++;
                return true;
            }
            return false;
        }

        private boolean isInPool(View instance) {
            for (int i = 0; i < maxSize; i++) {
                if (caches[i] == instance) {
                    return true;
                }
            }
            return false;
        }
    }

    private static final class ViewHolder {
        View view;
        int state = 0;

        ViewHolder(View view) {
            this.view = view;
        }
    }

}