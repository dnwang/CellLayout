package org.pinwheel.view.celllayout;

import android.content.Context;
import android.graphics.Point;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.view.KeyEvent;
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
        final long begin = System.currentTimeMillis();
        runnable.run();
        Log.e("TIME", "[" + tag + "]: " + (System.currentTimeMillis() - begin));
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
        director.setRoot(root);
        director.measure(getMeasuredWidth(), getMeasuredHeight());
        director.layout(true, 0, 0,0,0);
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
        time("director.scrollToCenter", new Runnable() {
            @Override
            public void run() {
                director.scrollToCenter(cell, anim);
            }
        });
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {

    }

    //    @Override
//    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
//        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
//        // don't support wrap_content
//        director.measure(getMeasuredWidth(), getMeasuredHeight());
//        // sync view
//        final int size = getChildCount();
//        for (int i = 0; i < size; i++) {
//            View view = getChildAt(i);
//            Cell cell = manager.findCellByView(view);
//            if (null != cell) {
//                view.measure(
//                        MeasureSpec.makeMeasureSpec(cell.getWidth(), MeasureSpec.EXACTLY),
//                        MeasureSpec.makeMeasureSpec(cell.getHeight(), MeasureSpec.EXACTLY)
//                );
//            }
//        }
//    }
//
//    @Override
//    protected void onLayout(boolean changed, int l, int t, int r, int b) {
//        Log.e(TAG, "onLayout: " + changed);
//        director.layout(changed, l, t, r, b);
//        // sync view
//        final int size = getChildCount();
//        for (int i = 0; i < size; i++) {
//            View view = getChildAt(i);
//            Cell cell = manager.findCellByView(view);
//            if (null != cell) {
//                view.layout(cell.getLeft(), cell.getTop(), cell.getRight(), cell.getBottom());
//            }
//        }
//    }

    @Override
    public void scrollTo(int x, int y) {
        if (director.hasRoot()) {
            final Cell root = director.getRoot();
            if (root instanceof CellGroup) {
                CellGroup cell = (CellGroup) root;
                int left = cell.getLeft() + cell.getScrollX();
                int top = cell.getTop() + cell.getScrollY();
                director.scrollBy(cell, x - left, y - top, false);
            }
        }
    }

    @Override
    public void scrollBy(int x, int y) {
        if (director.hasRoot()) {
            final Cell root = director.getRoot();
            if (root instanceof CellGroup) {
                director.scrollBy((CellGroup) root, x, y, false);
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
//                int dx = (int) event.getX() - tmpPoint.x;
//                int dy = (int) event.getY() - tmpPoint.y;
//                int absDx = Math.abs(dx);
//                int absDy = Math.abs(dy);
//                if (isMoving || absDx > 10 || absDy > 10) {
//                    isMoving = true;
//                    int dir = absDx > absDy ? LinearGroup.HORIZONTAL : LinearGroup.VERTICAL;
//                    director.scrollBy(director.findLinearGroupBy(touchCell, dir), dx, dy, false);
//                    tmpPoint.set((int) event.getX(), (int) event.getY());
//                } else {
//                    getParent().requestDisallowInterceptTouchEvent(false);
//                }
                return superState;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                moveToCenter(touchCell, true);


                touchCell = null;
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

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        return super.dispatchKeyEvent(event);
    }

    public interface ViewAdapter {
        int getViewPoolId(Cell cell);

        View onCreateView(Cell cell);

        void onBindView(View view, Cell cell);

        void onViewRecycled(View view, Cell cell);
    }

    private final class ViewManager implements CellDirector.LifeCycleCallback {
        private ViewAdapter adapter;
        private final SparseArray<ViewPool> cellPool = new SparseArray<>();
        private final Map<Cell, View> cellViewMap = new HashMap<>();

        void setAdapter(ViewAdapter adapter) {
            this.adapter = adapter;
        }

        View findViewByCell(Cell cell) {
            if (null != cell) {
                Set<Map.Entry<Cell, View>> entrySet = cellViewMap.entrySet();
                for (Map.Entry<Cell, View> entry : entrySet) {
                    if (entry.getKey().equals(cell)) {
                        return entry.getValue();
                    }
                }
            }
            return null;
        }

        Cell findCellByView(View view) {
            if (null != view) {
                Set<Map.Entry<Cell, View>> entrySet = cellViewMap.entrySet();
                for (Map.Entry<Cell, View> entry : entrySet) {
                    if (entry.getValue() == view) {
                        return entry.getKey();
                    }
                }
            }
            return null;
        }

        @Override
        public void onAttached(Cell cell) {
            Log.d(TAG, "onAttached: " + cell);
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
            Log.d(TAG, "onVisibleChanged: " + cell + ", is: " + cell.isVisible());
            final ViewPool pool = getPool(adapter.getViewPoolId(cell));
            if (cell.isVisible()) {
                // add
                View view = pool.acquire();
                if (null == view) {
                    view = adapter.onCreateView(cell);
                    if (null == view) {
                        throw new IllegalStateException("'Adapter.onCreateView()' can't got null view !");
                    }
                }
                adapter.onBindView(view, cell);
                if (CellLayout.this != view.getParent()) {
//                    CellLayout.this.addView(view);

                    final View newView = view;
                    time("add", new Runnable() {
                        @Override
                        public void run() {
                            CellLayout.this.addViewInLayout(newView, -1, generateDefaultLayoutParams(), true);
                            newView.measure(
                                    MeasureSpec.makeMeasureSpec(cell.getWidth(), MeasureSpec.EXACTLY),
                                    MeasureSpec.makeMeasureSpec(cell.getHeight(), MeasureSpec.EXACTLY)
                            );
                            newView.layout(cell.getLeft(), cell.getTop(), cell.getRight(), cell.getBottom());
                        }
                    });
                }
                cellViewMap.put(cell, view);
            } else {
                // remove
                recycleView(cell, pool);
            }
        }

        @Override
        public void onDetached(Cell cell) {
            Log.d(TAG, "onDetached: " + cell);
            recycleView(cell, getPool(adapter.getViewPoolId(cell)));
        }

        private ViewPool getPool(int poolId) {
            ViewPool pool = cellPool.get(poolId);
            if (null == pool) {
                pool = new ViewPool(5);
                cellPool.put(poolId, pool);
            }
            return pool;
        }

        private void recycleView(Cell cell, ViewPool pool) {
            final View view = findViewByCell(cell);
            if (null != view) {
                CellLayout.this.removeViewInLayout(view);
                cellViewMap.remove(cell);
                adapter.onViewRecycled(view, cell);
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

}