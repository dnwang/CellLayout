package org.pinwheel.view.celllayout;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
    static final String TAG = "CellLayout";

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
        director.forceLayout();
    }

    public Cell findCellById(long id) {
        return director.findCellById(id);
    }

    public View findViewByCell(Cell cell) {
        return manager.findViewByCell(cell);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        if (changed) {
            director.layout(l, t, getMeasuredWidth(), getMeasuredHeight());
        }
    }

    public void scrollToCenter(View view, boolean withAnimation) {
        if (null == view) {
            return;
        }
        scrollToCenter(manager.findCellByView(view), withAnimation);
    }

    public void scrollToCenter(Cell cell, boolean withAnimation) {
        if (!director.hasRoot() || null == cell) {
            return;
        }
        final Rect rootRect = director.getRoot().getRect();
        final Rect cellRect = cell.getRect();
        final int dx = rootRect.centerX() - cellRect.centerX();
        final int dy = rootRect.centerY() - cellRect.centerY();
        final LinearGroup vLinear = director.findLinearGroupBy(cell, LinearGroup.VERTICAL);
        final LinearGroup hLinear = director.findLinearGroupBy(cell, LinearGroup.HORIZONTAL);
        if (!withAnimation || (Math.abs(dx) + Math.abs(dy) < 10)) {
            final long begin = System.nanoTime();
            director.moveBy(vLinear, 0, dy);
            director.moveBy(hLinear, dx, 0);
            Log.e(TAG, "scrollToCenter: " + (System.nanoTime() - begin) / 1000000f);
            director.onMoveComplete();
        } else {
            autoMoving(dx, dy, new MovingAction() {
                @Override
                public void call(int offsetX, int offsetY) {
                    director.moveBy(hLinear, offsetX, 0);
                    director.moveBy(vLinear, 0, offsetY);
                }
            });
        }
    }

    @Override
    public void scrollTo(int x, int y) {
        scrollTo(x, y, false);
    }

    public void scrollTo(int x, int y, boolean withAnimation) {
        if (!director.hasRoot()) {
            return;
        }
        final Cell root = director.getRoot();
        if (root instanceof CellGroup) {
            final CellGroup cell = (CellGroup) root;
            int dx = x - (cell.getLeft() + cell.getScrollX());
            int dy = y - (cell.getTop() + cell.getScrollY());
            if (!withAnimation) {
                director.moveBy(cell, dx, dy);
                director.onMoveComplete();
            } else {
                autoMoving(dx, dy, new MovingAction() {
                    @Override
                    public void call(int offsetX, int offsetY) {
                        director.moveBy(cell, offsetX, offsetY);
                    }
                });
            }
        }
    }

    @Override
    public void scrollBy(int dx, int dy) {
        scrollBy(dx, dy, false);
    }

    public void scrollBy(int dx, int dy, boolean withAnimation) {
        if (!director.hasRoot()) {
            return;
        }
        final Cell root = director.getRoot();
        if (root instanceof CellGroup) {
            final CellGroup cell = (CellGroup) root;
            if (!withAnimation) {
                director.moveBy((CellGroup) root, dx, dy);
                director.onMoveComplete();
            } else {
                autoMoving(dx, dy, new MovingAction() {
                    @Override
                    public void call(int offsetX, int offsetY) {
                        director.moveBy(cell, offsetX, offsetY);
                    }
                });
            }
        }
    }

    private ValueAnimator movingAnimator;

    private void autoMoving(int dx, int dy, final MovingAction action) {
        if (null != movingAnimator) {
            movingAnimator.cancel();
        }
        movingAnimator = ValueAnimator.ofPropertyValuesHolder(
                PropertyValuesHolder.ofInt("x", 0, dx),
                PropertyValuesHolder.ofInt("y", 0, dy)
        ).setDuration(200);
        movingAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            int lastDx, lastDy;

            @Override
            public void onAnimationUpdate(ValueAnimator anim) {
                int dx = (int) anim.getAnimatedValue("x");
                int dy = (int) anim.getAnimatedValue("y");
                action.call(dx - lastDx, dy - lastDy);
                lastDx = dx;
                lastDy = dy;
            }
        });
        movingAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                director.onMoveComplete();
            }
        });
        movingAnimator.start();
    }

    private interface MovingAction {
        void call(int offsetX, int offsetY);
    }

    private final Point tmpPoint = new Point();
    private Cell touchCell = null;
    private boolean isTouchMoving = false;

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
                if (isTouchMoving || absDx > 10 || absDy > 10) {
                    isTouchMoving = true;
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
                if (isTouchMoving) {
                    director.onMoveComplete();
                }
                isTouchMoving = false;
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
                return isTouchMoving;
            default:
                return superState;
        }
    }

    private final LongKeyPressDirector longKeyPressDirector = new LongKeyPressDirector((Activity) getContext()) {

        @Override
        public boolean interceptLongPress(int keyCode) {
            return KeyEvent.KEYCODE_DPAD_LEFT == keyCode
                    || KeyEvent.KEYCODE_DPAD_UP == keyCode
                    || KeyEvent.KEYCODE_DPAD_RIGHT == keyCode
                    || KeyEvent.KEYCODE_DPAD_DOWN == keyCode;
        }

        private static final int OFFSET = 300;
        private boolean isLongPressMoving = false;
        private Map.Entry<Cell, View> focused = null;
        private LinearGroup moveGroup;

        private void prepareLongPress(int orientation) {
            focused = manager.findFocusedCell();
            if (null == focused) {
                return;
            }
            isLongPressMoving = true;
            moveGroup = director.findLinearGroupBy(focused.getKey(), orientation);
            // clear current focus
            focused.getValue().clearFocus();
        }

        private void releaseLongPress() {
            isLongPressMoving = false;
            moveGroup = null;
            focused = null;
            director.onMoveComplete();
            // restore focus
            Map.Entry<Cell, View> entry = manager.randomActiveCell();
            if (null != entry) {
                entry.getValue().requestFocus();
            }
        }

        @Override
        public boolean onLongPress(int action, int keyCode) {
            if (KeyEvent.ACTION_DOWN == action) {
                if (!isLongPressMoving) {
                    prepareLongPress((KeyEvent.KEYCODE_DPAD_LEFT == keyCode || KeyEvent.KEYCODE_DPAD_RIGHT == keyCode)
                            ? LinearGroup.HORIZONTAL : LinearGroup.VERTICAL);
                } else { // moving
                    switch (keyCode) {
                        case KeyEvent.KEYCODE_DPAD_LEFT:
                            director.moveBy(moveGroup, OFFSET, 0);
                            break;
                        case KeyEvent.KEYCODE_DPAD_UP:
                            director.moveBy(moveGroup, 0, OFFSET);
                            break;
                        case KeyEvent.KEYCODE_DPAD_RIGHT:
                            director.moveBy(moveGroup, -OFFSET, 0);
                            break;
                        case KeyEvent.KEYCODE_DPAD_DOWN:
                            director.moveBy(moveGroup, 0, -OFFSET);
                            break;
                    }
                }
            } else if (KeyEvent.ACTION_UP == action) {
                releaseLongPress();
            }
            return true;
        }

        @Override
        public boolean onSinglePress(int keyCode) {
            return false;
        }
    };

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        return longKeyPressDirector.dispatchKeyEvent(event) || super.dispatchKeyEvent(event);
    }

    public interface ViewAdapter {
        View getHolderView();

        int getViewPoolId(Cell cell);

        View onCreateView(Cell cell);

        void onBindView(Cell cell, View view);

        void onViewRecycled(Cell cell, View view);
    }

    private final class ViewManager implements CellDirector.LifeCycleCallback {
        private ViewAdapter adapter;
        private final SparseArray<ViewPool> cellPool = new SparseArray<>();
        private final HashMap<Cell, View> activeCells = new HashMap<>();

        void setAdapter(ViewAdapter adapter) {
            checkAndReleaseCache(true);
            this.adapter = adapter;
            //
            prepareHolderCache();
        }

        private void checkAndReleaseCache(boolean force) {
            if (force) { // clear all
                CellLayout.this.removeAllViewsInLayout();
                holderCache.clear();
                activeCells.clear();
                final int size = cellPool.size();
                for (int i = 0; i < size; i++) {
                    cellPool.get(i).clear();
                }
                cellPool.clear();
            } else {
                // remove extra holder
                int size = holderCache.size() - DEF_HOLDER_SIZE;
                for (int i = 0; i < size; i++) {
                    View holder = holderCache.remove(0);
                    CellLayout.this.removeViewInLayout(holder);
                }
                // remove content view cache
//                size = cellPool.size();
//                for (int i = 0; i < size; i++) {
//                    CellLayout.this.removeViewInLayout(cellPool.get(i).acquire());
//                }
            }
        }

        View findViewByCell(Cell cell) {
            return activeCells.get(cell);
        }

        Map.Entry<Cell, View> findFocusedCell() {
            Set<Map.Entry<Cell, View>> entrySet = activeCells.entrySet();
            for (Map.Entry<Cell, View> entry : entrySet) {
                if (entry.getValue().hasFocus()) {
                    return entry;
                }
            }
            return null;
        }

        Map.Entry<Cell, View> randomActiveCell() {
            return activeCells.entrySet().iterator().next();
        }

        Cell findCellByView(View view) {
            if (null != view) {
                Set<Map.Entry<Cell, View>> entrySet = activeCells.entrySet();
                for (Map.Entry<Cell, View> entry : entrySet) {
                    if (entry.getValue() == view) {
                        return entry.getKey();
                    }
                }
            }
            return null;
        }

        @Override
        public void onCellLayout() {
            replaceAllHolder();
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
            final ViewPool pool = getPool(cell);
            if (cell.isVisible()) {
                final View contentViewCache = pool.acquire();
                if (null == contentViewCache) {
                    final View holder = acquireHolder();
                    cell.setHasHolderView();
                    layoutViewByCell(cell, holder);
                    activeCells.put(cell, holder);
                    holder.setFocusable(true);
                } else {
                    cell.setHasContentView();
                    layoutViewByCell(cell, contentViewCache);
                    adapter.onBindView(cell, contentViewCache);
                    activeCells.put(cell, contentViewCache);
                    contentViewCache.setFocusable(true);
                }
            } else {
                // remove
                activeCells.remove(cell);
                final View v = findViewByCell(cell);
                if (null != v) {
                    v.setFocusable(false);
                    adapter.onViewRecycled(cell, v);
                    pool.release(v);
                }
            }
        }

        private Runnable scheduleAction = new Runnable() {
            @Override
            public void run() {
                replaceAllHolder();
                checkAndReleaseCache(false);
            }
        };

        @Override
        public void onMoveComplete() {
            CellLayout.this.removeCallbacks(scheduleAction);
            CellLayout.this.postDelayed(scheduleAction, 10);
        }

        private void replaceAllHolder() {
            final Set<Map.Entry<Cell, View>> entrySet = activeCells.entrySet();
            for (Map.Entry<Cell, View> entry : entrySet) {
                final Cell cell = entry.getKey();
                final View holder = entry.getValue();
                final boolean hasFocus = holder.hasFocus();
                if (!cell.hasContentView()) {
                    View contentView = getPool(cell).acquire();
                    if (null == contentView) {
                        contentView = adapter.onCreateView(cell);
                        if (null == contentView) {
                            throw new IllegalStateException("Adapter.onCreateView() can't return null !");
                        }
                        if (CellLayout.this != contentView.getParent()) {
                            CellLayout.this.addViewInLayout(contentView, -1, generateDefaultLayoutParams(), true);
                        }
                    }
                    // layout content view
                    layoutViewByCell(cell, contentView);
                    // upgrade view
                    adapter.onBindView(cell, contentView);
                    activeCells.put(cell, contentView);
                    // set state
                    cell.setHasContentView();
                    releaseHolder(holder);
                    // restore focus
                    if (hasFocus) {
                        contentView.requestFocus();
                    }
                }
            }
        }

        private void layoutViewByCell(Cell cell, View v) {
            if (v.getMeasuredWidth() != cell.getWidth() || v.getMeasuredHeight() != cell.getHeight()) {
                v.measure(
                        MeasureSpec.makeMeasureSpec(cell.getWidth(), MeasureSpec.EXACTLY),
                        MeasureSpec.makeMeasureSpec(cell.getHeight(), MeasureSpec.EXACTLY)
                );
            }
            v.layout(cell.getLeft(), cell.getTop(), cell.getRight(), cell.getBottom());
        }

        private ViewPool getPool(Cell cell) {
            final int poolId = adapter.getViewPoolId(cell);
            ViewPool pool = cellPool.get(poolId);
            if (null == pool) {
                pool = new ViewPool(5);
                cellPool.put(poolId, pool);
            }
            return pool;
        }

        private static final int DEF_HOLDER_SIZE = 20;
        private final List<View> holderCache = new ArrayList<>(DEF_HOLDER_SIZE);

        private void prepareHolderCache() {
            if (holderCache.size() < DEF_HOLDER_SIZE) {
                int size = DEF_HOLDER_SIZE - holderCache.size();
                for (int i = 0; i < size; i++) {
                    holderCache.add(createHolder());
                }
            }
        }

        private View createHolder() {
            final View v = adapter.getHolderView();
            if (null == v) {
                throw new IllegalStateException("Adapter.getHolderView() can't return null !");
            }
            v.setBackgroundColor(Color.GRAY);
            if (CellLayout.this != v.getParent()) {
                CellLayout.this.addViewInLayout(v, -1, generateDefaultLayoutParams(), true);
            }
            return v;
        }

        private View acquireHolder() {
            if (holderCache.size() > 0) {
                View holder = holderCache.remove(0);
                holder.setBackgroundColor(Color.GRAY);
                return holder;
            } else {
                return createHolder();
            }
        }

        private void releaseHolder(View holder) {
            holder.setBackgroundColor(Color.TRANSPARENT);
            holder.setFocusable(false);
            holderCache.add(holder);
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

        void release(View instance) {
            if (isInPool(instance)) {
                throw new IllegalStateException("Already in the pool!");
            }
            if (maxSize < caches.length) {
                caches[maxSize] = instance;
                maxSize++;
            }
        }

        private boolean isInPool(View instance) {
            for (int i = 0; i < maxSize; i++) {
                if (caches[i] == instance) {
                    return true;
                }
            }
            return false;
        }

        private void clear() {
            for (int i = 0; i < maxSize; i++) {
                caches[i] = null;
            }
        }
    }

}