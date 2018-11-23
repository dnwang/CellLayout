package org.pinwheel.view.celllayout;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    private static final int FLAG_MOVING_LONG_PRESS = 1;
    private static final int FLAG_MOVING_TOUCH = FLAG_MOVING_LONG_PRESS << 1;
    private static final int FLAG_MOVING_AUTO = FLAG_MOVING_TOUCH << 1;
    private static final int FLAG_SCALING = FLAG_MOVING_TOUCH << 1;

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

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        getViewTreeObserver().addOnGlobalFocusChangeListener(focusListener);
    }

    @Override
    protected void onDetachedFromWindow() {
        getViewTreeObserver().removeOnGlobalFocusChangeListener(focusListener);
        super.onDetachedFromWindow();
    }

    private final ViewTreeObserver.OnGlobalFocusChangeListener focusListener = new ViewTreeObserver.OnGlobalFocusChangeListener() {
        @Override
        public void onGlobalFocusChanged(View oldFocus, View newFocus) {
            oldFocus = (null != oldFocus && oldFocus.getParent() == CellLayout.this) ? oldFocus : null;
            newFocus = (null != newFocus && newFocus.getParent() == CellLayout.this) ? newFocus : null;
            if (null != oldFocus || null != newFocus) {
                new SwitchScaleAction(oldFocus, newFocus).execute();
            }
            keepCellCenter(manager.findCellByView(newFocus), true);
        }
    };

    private final CellDirector director = new CellDirector();
    private final ViewManager manager = new ViewManager();

    private Paint holderPaint = new Paint();

    private int flag = 0;

    private void init() {
        director.setCallback(manager);
        setChildrenDrawingOrderEnabled(true);

        holderPaint.setColor(Color.parseColor("#4F586E"));
    }

    public void setAdapter(ViewAdapter adapter) {
        manager.setAdapter(adapter);
    }

    public void setRoot(Cell root) {
        director.setRoot(root);
        director.forceLayout();
    }

    public Cell findCellById(long id) {
        return director.findCellById(id);
    }

    public Cell findCellByView(View v) {
        return manager.findCellByView(v);
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

    public void keepCellCenter(Cell cell, boolean withAnimation) {
        if (!director.hasRoot() || null == cell) {
            return;
        }
        final Cell rect = director.getRoot();
        int dx = rect.centerX() - cell.centerX();
        dx = Math.abs(dx) < rect.width() / 5 ? 0 : dx;
        int dy = rect.centerY() - cell.centerY();
        dy = Math.abs(dy) < rect.height() / 5 ? 0 : dy;
        final LinearGroup vLinear = director.findLinearGroupBy(cell, LinearGroup.VERTICAL);
        final LinearGroup hLinear = director.findLinearGroupBy(cell, LinearGroup.HORIZONTAL);
        if (!withAnimation || (Math.abs(dx) + Math.abs(dy) < 10)) {
            if (director.moveBy(vLinear, 0, dy) | director.moveBy(hLinear, dx, 0)) {
                invalidate();
                director.onMoveComplete();
            }
        } else {
            new AutoMovingAction(dx, dy, new AutoMovingCallback() {
                @Override
                public void move(int offsetX, int offsetY) {
                    if (director.moveBy(hLinear, offsetX, 0) | director.moveBy(vLinear, 0, offsetY)) {
                        invalidate();
                    }
                }
            }).execute();
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
        if (root instanceof LinearGroup) {
            final CellGroup cell = (LinearGroup) root;
            scrollBy(x - (cell.left + cell.scrollX),
                    y - (cell.top + cell.scrollY),
                    withAnimation);
        }
    }

    @Override
    public void scrollBy(int dx, int dy) {
        scrollBy(dx, dy, false);
    }

    public void scrollBy(int dx, int dy, boolean withAnimation) {
        if (!director.hasRoot() || (0 == dx && 0 == dy)) {
            return;
        }
        final Cell root = director.getRoot();
        if (root instanceof LinearGroup) {
            final LinearGroup cell = (LinearGroup) root;
            if (!withAnimation) {
                if (director.moveBy(cell, dx, dy)) {
                    invalidate();
                    director.onMoveComplete();
                }
            } else {
                new AutoMovingAction(dx, dy, new AutoMovingCallback() {
                    @Override
                    public void move(int offsetX, int offsetY) {
                        if (director.moveBy(cell, offsetX, offsetY)) {
                            invalidate();
                        }
                    }
                }).execute();
            }
        }
    }

    private final class AutoMovingAction implements Runnable {
        int sum;
        final int unitX, unitY;
        final AutoMovingCallback callback;

        AutoMovingAction(int dx, int dy, AutoMovingCallback callback) {
            this.callback = callback;
            final int absDx = Math.abs(dx);
            final int absDy = Math.abs(dy);
            sum = Math.max(absDx, absDy) > 300 ? 6 : 4;
            unitX = absDx > sum ? (dx / sum) : (0 != dx ? (dx / absDx) : 0);
            unitY = absDy > sum ? (dy / sum) : (0 != dy ? (dy / absDy) : 0);
        }

        final void execute() {
            post(this);
            flag |= FLAG_MOVING_AUTO;
        }

        @Override
        public void run() {
            callback.move(unitX, unitY);
            if (sum-- > 0) {
                post(this);
            } else {
                flag &= ~FLAG_MOVING_AUTO;
                director.onMoveComplete();
            }
        }
    }

    private interface AutoMovingCallback {
        void move(int offsetX, int offsetY);
    }

    private final class SwitchScaleAction implements Runnable {
        int sum = 4;
        final float maxScale = 1.1f;
        final float mimScale = 1f;
        final float unit = (maxScale - mimScale) / sum;
        final View zoomIn, zoomOut;

        SwitchScaleAction(View zoomIn, View zoomOut) {
            this.zoomIn = zoomIn;
            this.zoomOut = zoomOut;
        }

        final void execute() {
            post(this);
            flag |= FLAG_SCALING;
        }

        @Override
        public void run() {
            if (null != zoomIn && zoomIn.getScaleX() > mimScale) {
                zoomIn.setScaleX(zoomIn.getScaleX() - unit);
                zoomIn.setScaleY(zoomIn.getScaleY() - unit);
            }
            if (null != zoomOut && zoomOut.getScaleX() < maxScale) {
                zoomOut.setScaleX(zoomOut.getScaleX() + unit);
                zoomOut.setScaleY(zoomOut.getScaleY() + unit);
            }
            if (sum-- > 0) {
                post(this);
            } else {
                flag &= ~FLAG_SCALING;
            }
        }
    }

    private final Point touchPoint = new Point();
    private Cell touchCell = null;

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        final boolean superState = super.dispatchTouchEvent(event);
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                getParent().requestDisallowInterceptTouchEvent(true);
                touchPoint.set((int) event.getX(), (int) event.getY());
                if (null == touchCell) {
                    touchCell = director.findCellByPosition(touchPoint.x, touchPoint.y);
                }
                return true;// can not return superState.
            case MotionEvent.ACTION_MOVE:
                int dx = (int) event.getX() - touchPoint.x;
                int dy = (int) event.getY() - touchPoint.y;
                int absDx = Math.abs(dx);
                int absDy = Math.abs(dy);
                if ((flag & FLAG_MOVING_TOUCH) != 0 || absDx > 10 || absDy > 10) {
                    flag |= FLAG_MOVING_TOUCH;
                    final int dir = absDx > absDy ? LinearGroup.HORIZONTAL : LinearGroup.VERTICAL;
                    touchPoint.set((int) event.getX(), (int) event.getY());
                    boolean moved = director.moveBy(director.findLinearGroupBy(touchCell, dir), dx, dy);
                    if (moved) {
                        invalidate();
                    }
                } else {
                    getParent().requestDisallowInterceptTouchEvent(false);
                }
                return superState;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                final boolean tmp = (flag & FLAG_MOVING_TOUCH) != 0;
                touchCell = null;
                flag &= ~FLAG_MOVING_TOUCH;
                if (tmp) {
                    director.onMoveComplete();
                }
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
                return (flag & FLAG_MOVING_TOUCH) != 0;
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
        private Cell focused = null;
        private LinearGroup moveGroup;

        private void prepareLongPress(int orientation) {
            final View view = findFocus();
            focused = manager.findCellByView(view);
            if (null == focused) {
                return;
            }
            flag |= FLAG_MOVING_LONG_PRESS;
            moveGroup = director.findLinearGroupBy(focused, orientation);
            // switch focus to root
            setFocusable(true);
            requestFocus();
            view.setScaleY(1);
            view.setScaleX(1);
        }

        private void releaseLongPress() {
            flag &= ~FLAG_MOVING_LONG_PRESS;
            moveGroup = null;
            focused = null;
            director.onMoveComplete();
        }

        Cell restoreFocus;

        private void findFocusCell(final int keyCode) {
            if (null == moveGroup || null == focused) {
                return;
            }
            manager.foreachActiveCells(new Filter<Cell>() {
                @Override
                public boolean call(Cell cell) {
                    if (moveGroup.contains(cell)) {
                        switch (keyCode) {
                            case KeyEvent.KEYCODE_DPAD_LEFT:
                                if (null == restoreFocus || cell.left < restoreFocus.left || (cell.left == restoreFocus.left
                                        && Math.abs(cell.centerY() - focused.centerY()) < Math.abs(restoreFocus.centerY() - focused.centerY()))) {
                                    restoreFocus = cell;
                                }
                                break;
                            case KeyEvent.KEYCODE_DPAD_UP:
                                if (null == restoreFocus || cell.top < restoreFocus.top || (cell.top == restoreFocus.top
                                        && Math.abs(cell.centerX() - focused.centerX()) < Math.abs(restoreFocus.centerX() - focused.centerX()))) {
                                    restoreFocus = cell;
                                }
                                break;
                            case KeyEvent.KEYCODE_DPAD_RIGHT:
                                if (null == restoreFocus || cell.right > restoreFocus.right || (cell.right == restoreFocus.right
                                        && Math.abs(cell.centerY() - focused.centerY()) < Math.abs(restoreFocus.centerY() - focused.centerY()))) {
                                    restoreFocus = cell;
                                }
                                break;
                            case KeyEvent.KEYCODE_DPAD_DOWN:
                                if (null == restoreFocus || cell.bottom > restoreFocus.bottom || (cell.bottom == restoreFocus.bottom
                                        && Math.abs(cell.centerX() - focused.centerX()) < Math.abs(restoreFocus.centerX() - focused.centerX()))) {
                                    restoreFocus = cell;
                                }
                                break;
                        }
                    }
                    return false;
                }
            });
        }

        @Override
        public boolean onLongPress(int action, int keyCode) {
            if (KeyEvent.ACTION_DOWN == action) {
                if ((flag & FLAG_MOVING_LONG_PRESS) == 0) {
                    prepareLongPress((KeyEvent.KEYCODE_DPAD_LEFT == keyCode || KeyEvent.KEYCODE_DPAD_RIGHT == keyCode)
                            ? LinearGroup.HORIZONTAL : LinearGroup.VERTICAL);
                } else { // moving
                    boolean moved = false;
                    switch (keyCode) {
                        case KeyEvent.KEYCODE_DPAD_LEFT:
                            moved = director.moveBy(moveGroup, OFFSET, 0);
                            break;
                        case KeyEvent.KEYCODE_DPAD_UP:
                            moved = director.moveBy(moveGroup, 0, OFFSET);
                            break;
                        case KeyEvent.KEYCODE_DPAD_RIGHT:
                            moved = director.moveBy(moveGroup, -OFFSET, 0);
                            break;
                        case KeyEvent.KEYCODE_DPAD_DOWN:
                            moved = director.moveBy(moveGroup, 0, -OFFSET);
                            break;
                    }
                    if (moved) {
                        invalidate();
                    } else {
                        // move complete at the bottom
                        director.onMoveComplete();
                    }
                }
            } else if (KeyEvent.ACTION_UP == action) {
                findFocusCell(keyCode);
                releaseLongPress();
                // delay wait move complete action replace holder
                postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (null != restoreFocus) {
                            manager.findViewByCell(restoreFocus).requestFocus();
                        }
                        restoreFocus = null;
                    }
                }, 100);
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

    @Override
    protected void onDraw(final Canvas canvas) {
        manager.foreachActiveCells(new Filter<Cell>() {
            @Override
            public boolean call(Cell cell) {
                if (!cell.hasContentView()) {
                    canvas.drawRect(cell.convert(), holderPaint);
                }
                return false;
            }
        });
    }

    private int focusOrder = -1;

    @Override
    protected int getChildDrawingOrder(int childCount, int i) {
        final View focus = findFocus();
        if (getChildAt(i) == focus) {
            focusOrder = i;
            return childCount - 1;
        } else {
            if (i == childCount - 1 && focusOrder >= 0) {
                return focusOrder;
            } else {
                return i;
            }
        }
    }

    @Override
    public void childDrawableStateChanged(View child) {
        if (!manager.isEmpty()) {
            invalidate();
        }
        super.childDrawableStateChanged(child);
    }

//    @Override
//    protected boolean drawChild(Canvas canvas, View child, long drawingTime) {
//        final Cell cell = manager.findCellByView(child);
//        if (null != cell) {
////            if ((flag & (FLAG_MOVING_TOUCH | FLAG_MOVING_LONG_PRESS | FLAG_MOVING_AUTO)) != 0) {
//            // first draw on target position, layout it when move complete!
//            canvas.save();
//            canvas.translate(cell.left, cell.top);
//            child.draw(canvas);
//            canvas.restore();
//            return true;
////            } else {
//            // super method will be draw child by layout position
////                return super.drawChild(canvas, child, drawingTime);
////            }
//        } else {
//            return false;
//        }
//    }

    public interface ViewAdapter {
        int getViewType(Cell cell);

        View onCreateView(Cell cell);

        void onBindView(Cell cell, View view);

        void onViewRecycled(Cell cell, View view);
    }

    private final class ViewManager implements CellDirector.LifeCycleCallback {
        private ViewAdapter adapter;
        private final SparseArray<ViewPool> poolMap = new SparseArray<>();
        private final HashMap<Cell, View> activeCells = new HashMap<>();

        void setAdapter(ViewAdapter adapter) {
            checkAndReleaseCache(true);
            this.adapter = adapter;
        }

        private void checkAndReleaseCache(boolean force) {
            if (force) { // clear all
                removeAllViewsInLayout();
                // clear state
                final Collection<Cell> cells = activeCells.keySet();
                for (Cell cell : cells) {
                    cell.setHasHolderView();
                }
                // clear reference
                activeCells.clear();
                final int size = poolMap.size();
                for (int i = 0; i < size; i++) {
                    poolMap.valueAt(i).keepSize(0, null);
                }
                poolMap.clear();
            } else { // just remove extra holder and content view
                final int size = poolMap.size();
                for (int i = 0; i < size; i++) {
                    poolMap.valueAt(i).keepSize(5, new Filter<View>() {
                        @Override
                        public boolean call(View view) {
                            removeViewInLayout(view);
                            return false;
                        }
                    });
                }
            }
        }

        boolean isEmpty() {
            return activeCells.isEmpty();
        }

        void foreachActiveCells(Filter<Cell> filter) {
            Collection<Cell> cells = activeCells.keySet();
            for (Cell cell : cells) {
                filter.call(cell);
            }
        }

        View findViewByCell(Cell cell) {
            return activeCells.get(cell);
        }

        Cell findCellByView(View view) {
            if (null != view) {
                Collection<Map.Entry<Cell, View>> entrySet = activeCells.entrySet();
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
//            layoutAllContent();
        }

        @Override
        public void onPositionChanged(Cell cell) {
            View v = findViewByCell(cell);
            if (null != v) {
                v.offsetLeftAndRight(cell.left - v.getLeft());
                v.offsetTopAndBottom(cell.top - v.getTop());
            }
        }

        @Override
        public void onVisibleChanged(final Cell cell) {
            if (cell instanceof CellGroup) { // don't care group
                return;
            }
            final ViewPool pool = getViewPool(cell);
            if (cell.isVisible()) { // add active view
//                final View cache = (flag & FLAG_MOVING_LONG_PRESS) != 0 ? null : pool.obtain(cell, true);
                final View cache = null;
                if (null != cache) {
                    bindContentToCell(cell, cache);
                } else { // holder
                    cell.setHasHolderView();
                    activeCells.put(cell, null);
                }
            } else { // remove active view
                final View v = activeCells.remove(cell);
                if (cell.hasContentView()) {
                    cell.setHasHolderView();
                    pool.recycle(v);
                    adapter.onViewRecycled(cell, v);
                }
            }
        }

        private final Runnable completeAction = new Runnable() {
            @Override
            public void run() {
                replaceAllHolder();
                layoutAllContent();
                // recycle should be in last
                checkAndReleaseCache(false);
                // log manager info
                final int size = poolMap.size();
                Log.d(TAG, "[into] --------------");
                for (int i = 0; i < size; i++) {
                    Log.d(TAG, "[into] poolMap_key_" + poolMap.keyAt(i) + " size: " + poolMap.valueAt(i).size());
                }
                Log.d(TAG, "[into] activeCells size: " + activeCells.size());
                Log.d(TAG, "[into] --------------");
            }
        };

        @Override
        public void onMoveComplete() {
            removeCallbacks(completeAction);
            postDelayed(completeAction, 10);
        }

        private void replaceAllHolder() {
            final Collection<Map.Entry<Cell, View>> entrySet = activeCells.entrySet();
            for (Map.Entry<Cell, View> entry : entrySet) {
                final Cell cell = entry.getKey();
                if (!cell.hasContentView()) {
                    // create content
                    View content = getViewPool(cell).obtain(cell, false);
                    if (null == content) {
                        content = createContent(cell);
                    }
                    bindContentToCell(cell, content);
                }
            }
        }

        private ViewPool getViewPool(Cell cell) {
            final int poolId = adapter.getViewType(cell);
            ViewPool pool = poolMap.get(poolId);
            if (null == pool) {
                pool = new ViewPool();
                poolMap.put(poolId, pool);
            }
            return pool;
        }

        private View createContent(Cell cell) {
            final long begin = System.nanoTime();
            final View v = adapter.onCreateView(cell);
            Log.d(TAG, "[adapter.onCreateView] " + (System.nanoTime() - begin) / 1000000f);
            if (null == v) {
                throw new IllegalStateException("Adapter.onCreateView() can't return null !");
            }
            if (CellLayout.this != v.getParent()) {
                addViewInLayout(v, -1, generateDefaultLayoutParams(), true);
            }
            return v;
        }

        private void bindContentToCell(Cell cell, View v) {
            final long begin = System.nanoTime();
            if (v.getMeasuredWidth() != cell.width() || v.getMeasuredHeight() != cell.height()) {
                v.measure(MeasureSpec.makeMeasureSpec(cell.width(), MeasureSpec.EXACTLY),
                        MeasureSpec.makeMeasureSpec(cell.height(), MeasureSpec.EXACTLY));
            }
            // maybe remove
            v.layout(cell.left, cell.top, cell.right, cell.bottom);
            adapter.onBindView(cell, v);
            Log.d(TAG, "[adapter.onBindView] " + (System.nanoTime() - begin) / 1000000f);
            cell.setHasContentView();
            activeCells.put(cell, v);
        }

        private void layoutAllContent() {
            final Collection<Map.Entry<Cell, View>> entrySet = activeCells.entrySet();
            for (Map.Entry<Cell, View> entry : entrySet) {
                final Cell cell = entry.getKey();
                final View v = entry.getValue();
                if (v.getLeft() != cell.left || v.getTop() != cell.top || v.getRight() != cell.right || v.getBottom() != cell.bottom) {
                    v.layout(cell.left, cell.top, cell.right, cell.bottom);
                }
            }
        }
    }

    private static final class ViewPool {
        private final List<View> caches;

        ViewPool() {
            caches = new ArrayList<>();
        }

        int size() {
            return caches.size();
        }

        View obtain(Cell cell, boolean force) {
            if (caches.isEmpty()) {
                return null;
            }
            View view = null;
            if (null != cell) {
                for (View v : caches) {
                    if (v.getMeasuredWidth() == cell.width() && v.getMeasuredHeight() == cell.height()) {
                        view = v;
                        break;
                    }
                }
            }
            if (null != view) {
                caches.remove(view);
            } else if (!force) {
                view = caches.remove(0);
            }
            if (null != view) {
                view.setFocusable(true);
            }
            return view;
        }

        void recycle(final View view) {
            if (caches.contains(view)) {
                throw new IllegalStateException("Already in the pool!");
            }
            view.clearFocus();
            view.setFocusable(false);
            view.setScaleX(1f);
            view.setScaleY(1f);
            caches.add(view);
        }

        void keepSize(int size, Filter<View> filter) {
            size = size < 0 ? 0 : size;
            final int count = caches.size() - size;
            for (int i = 0; i < count; i++) {
                View v = caches.remove(0);
                if (null != filter) {
                    filter.call(v);
                }
            }
        }
    }

}