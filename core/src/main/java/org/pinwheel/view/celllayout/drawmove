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
    private static final int FLAG_SCALING = FLAG_MOVING_AUTO << 1;

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
//                new SwitchScaleAction(oldFocus, newFocus).execute();
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
        setWillNotDraw(false);
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

    public void keepCellCenter(Cell cell, boolean withAnimation) {
        if (!director.hasRoot() || null == cell) {
            return;
        }
        final Cell rect = director.getRoot();
        int dx = rect.centerX() - cell.centerX();
        int dy = rect.centerY() - cell.centerY();
        dx = Math.abs(dx) < rect.width() / 5 ? 0 : dx;
        dy = Math.abs(dy) < rect.height() / 5 ? 0 : dy;
        final LinearGroup vLinear = director.findLinearGroupBy(cell, LinearGroup.VERTICAL);
        final LinearGroup hLinear = director.findLinearGroupBy(cell, LinearGroup.HORIZONTAL);
        if (!withAnimation || (Math.abs(dx) + Math.abs(dy) < 10)) {
            if (director.scrollBy(vLinear, 0, dy) | director.scrollBy(hLinear, dx, 0)) {
                invalidate();
                director.onMoveComplete();
            }
        } else {
            new AutoMovingAction(dx, dy) {
                @Override
                void onMove(final int dx, final int dy) {
                    if (director.scrollBy(vLinear, 0, dy) | director.scrollBy(hLinear, dx, 0)) {
                        invalidate();
                    }
                }
            }.execute();
        }
    }

    private abstract class AutoMovingAction {
        int sum;
        final int unitX, unitY;

        AutoMovingAction(int dx, int dy) {
            final int absDx = Math.abs(dx);
            final int absDy = Math.abs(dy);
            sum = Math.max(absDx, absDy) > 300 ? 6 : 4;
            unitX = absDx > sum ? (dx / sum) : (0 != dx ? (dx / absDx) : 0);
            unitY = absDy > sum ? (dy / sum) : (0 != dy ? (dy / absDy) : 0);
        }

        final void execute() {
            flag |= FLAG_MOVING_AUTO;
            post(new Runnable() {
                @Override
                public void run() {
                    onMove(unitX, unitY);
                    if (sum-- > 0) {
                        post(this);
                    } else {
                        flag &= ~FLAG_MOVING_AUTO;
                        director.onMoveComplete();
                    }
                }
            });
        }

        abstract void onMove(final int dx, final int dy);
    }

    private final static float SCALE_MAX = 1.1f;
    private final static float SCALE_MIN = 1f;

//    private final class SwitchScaleAction {
//        int sum = 4;
//        final float unit = (SCALE_MAX - SCALE_MIN) / sum;
//        final View zoomIn, zoomOut;
//
//        SwitchScaleAction(View zoomIn, View zoomOut) {
//            this.zoomIn = zoomIn;
//            this.zoomOut = zoomOut;
//        }
//
//        final void execute() {
//            flag |= FLAG_SCALING;
//            post(new Runnable() {
//                @Override
//                public void run() {
//                    if (null != zoomIn && zoomIn.getScaleX() > SCALE_MIN) {
//                        zoomIn.setScaleX(zoomIn.getScaleX() - unit);
//                        zoomIn.setScaleY(zoomIn.getScaleY() - unit);
//                    }
//                    if (null != zoomOut && zoomOut.getScaleX() < SCALE_MAX) {
//                        zoomOut.setScaleX(zoomOut.getScaleX() + unit);
//                        zoomOut.setScaleY(zoomOut.getScaleY() + unit);
//                    }
//                    if (sum-- > 0) {
//                        post(this);
//                    } else {
//                        flag &= ~FLAG_SCALING;
//                    }
//                }
//            });
//        }
//    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        if (changed) {
            director.layout(l, t, getMeasuredWidth(), getMeasuredHeight());
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
                    final int dir = absDx > absDy ? LinearGroup.HORIZONTAL : LinearGroup.VERTICAL;
                    touchPoint.set((int) event.getX(), (int) event.getY());
                    if (director.scrollBy(director.findLinearGroupBy(touchCell, dir), dx, dy)) {
                        flag |= FLAG_MOVING_TOUCH;
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
        }

        private void releaseLongPress(int keyCode) {
            findFocusCell(keyCode);
            moveGroup = null;
            final boolean tmp = (flag & FLAG_MOVING_LONG_PRESS) != 0;
            flag &= ~FLAG_MOVING_LONG_PRESS;
            if (tmp) {
                director.onMoveComplete();
            }
            if (null != focused) {
                manager.findViewByCell(focused).requestFocus();
            }
            focused = null;
        }

        private void findFocusCell(final int keyCode) {
            if (null == moveGroup || null == focused) {
                return;
            }
            final Rect oldFocus = new Rect(focused);
            focused = null;
            manager.foreachActiveCells(new Filter<Cell>() {
                @Override
                public boolean call(Cell cell) {
                    if (moveGroup.contains(cell)) {
                        switch (keyCode) {
                            case KeyEvent.KEYCODE_DPAD_LEFT:
                                if (null == focused || cell.left < focused.left || (cell.left == focused.left
                                        && Math.abs(cell.centerY() - oldFocus.centerY()) < Math.abs(focused.centerY() - oldFocus.centerY()))) {
                                    focused = cell;
                                }
                                break;
                            case KeyEvent.KEYCODE_DPAD_UP:
                                if (null == focused || cell.top < focused.top || (cell.top == focused.top
                                        && Math.abs(cell.centerX() - oldFocus.centerX()) < Math.abs(focused.centerX() - oldFocus.centerX()))) {
                                    focused = cell;
                                }
                                break;
                            case KeyEvent.KEYCODE_DPAD_RIGHT:
                                if (null == focused || cell.right > focused.right || (cell.right == focused.right
                                        && Math.abs(cell.centerY() - oldFocus.centerY()) < Math.abs(focused.centerY() - oldFocus.centerY()))) {
                                    focused = cell;
                                }
                                break;
                            case KeyEvent.KEYCODE_DPAD_DOWN:
                                if (null == focused || cell.bottom > focused.bottom || (cell.bottom == focused.bottom
                                        && Math.abs(cell.centerX() - oldFocus.centerX()) < Math.abs(focused.centerX() - oldFocus.centerX()))) {
                                    focused = cell;
                                }
                                break;
                        }
                    }
                    return false;
                }
            });
        }

        boolean intercept = false;

        @Override
        public boolean onLongPress(int action, int keyCode) {
            if (KeyEvent.ACTION_DOWN == action) {
                if (!intercept) {
                    if ((flag & FLAG_MOVING_LONG_PRESS) == 0) {
                        prepareLongPress((KeyEvent.KEYCODE_DPAD_LEFT == keyCode || KeyEvent.KEYCODE_DPAD_RIGHT == keyCode)
                                ? LinearGroup.HORIZONTAL : LinearGroup.VERTICAL);
                    } else { // moving
                        boolean moved = false;
                        switch (keyCode) {
                            case KeyEvent.KEYCODE_DPAD_LEFT:
                                moved = director.scrollBy(moveGroup, OFFSET, 0);
                                break;
                            case KeyEvent.KEYCODE_DPAD_UP:
                                moved = director.scrollBy(moveGroup, 0, OFFSET);
                                break;
                            case KeyEvent.KEYCODE_DPAD_RIGHT:
                                moved = director.scrollBy(moveGroup, -OFFSET, 0);
                                break;
                            case KeyEvent.KEYCODE_DPAD_DOWN:
                                moved = director.scrollBy(moveGroup, 0, -OFFSET);
                                break;
                        }
                        if (moved) {
                            invalidate();
                        } else {
                            // move complete at the bottom
                            releaseLongPress(keyCode);
                            intercept = true;
                        }
                    }
                }
            } else if (KeyEvent.ACTION_UP == action) {
                if (!intercept) {
                    releaseLongPress(keyCode);
                }
                intercept = false;
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

    private void drawEmptyHolder(final Canvas canvas) {
        manager.foreachActiveCells(new Filter<Cell>() {
            @Override
            public boolean call(Cell cell) {
                if (!cell.hasContent()) {
                    canvas.drawRect(cell.convert(), holderPaint);
                }
                return false;
            }
        });
    }

    @Override
    public void draw(Canvas canvas) {
        focusOrder = -1;
        super.draw(canvas);
        drawEmptyHolder(canvas);
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
        if (!manager.activeCells.isEmpty()) {
            invalidate();
        }
        super.childDrawableStateChanged(child);
    }

    @Override
    protected boolean drawChild(Canvas canvas, View child, long drawingTime) {
        final Cell cell = manager.findCellByView(child);
        if (null != cell) {
            if ((flag & (FLAG_MOVING_TOUCH | FLAG_MOVING_LONG_PRESS | FLAG_MOVING_AUTO)) != 0) {
                // first draw on target position, layout it when move complete!
                canvas.save();
                canvas.translate(cell.left, cell.top);
                child.draw(canvas);
                canvas.restore();
                return false;
            } else {
//             super method will be draw child by layout position
                return super.drawChild(canvas, child, drawingTime);
            }
        } else {
            return false;
        }
    }

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
                    cell.clearAllState();
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
            layoutAllContent();
        }

        @Override
        public void onMoved(final CellGroup group, final int dx, final int dy) {
//            final Collection<Map.Entry<Cell, View>> entries = activeCells.entrySet();
//            for (Map.Entry<Cell, View> entry : entries) {
//                View v = entry.getValue();
//                if (null != v && null != group.findCellById(entry.getKey().getId())) {
//                    v.offsetLeftAndRight(dx);
//                    v.offsetTopAndBottom(dy);
//                }
//            }
        }

        @Override
        public void onVisibleChanged(final Cell cell) {
            if (cell instanceof CellGroup) return; // don't care group
            final ViewPool pool = getViewPool(cell);
            if (cell.isVisible()) { // add active view
//                final View cache = (flag & FLAG_MOVING_LONG_PRESS) != 0 ? null : pool.obtain(cell, true);
                final View cache = null;
                if (null != cache) {
                    bindContentToCell(cell, cache);
                } else { // holder
                    cell.setEmpty();
                    activeCells.put(cell, null);
                }
            } else { // remove active view
                final View v = activeCells.remove(cell);
                if (cell.hasContent()) {
                    cell.setEmpty();
                    pool.recycle(v);
                    adapter.onViewRecycled(cell, v);
                }
            }
        }

        @Override
        public void onMoveComplete() {
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

        private void replaceAllHolder() {
            final Collection<Map.Entry<Cell, View>> entrySet = activeCells.entrySet();
            for (Map.Entry<Cell, View> entry : entrySet) {
                final Cell cell = entry.getKey();
                if (!cell.hasContent()) {
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
            final View v = adapter.onCreateView(cell);
            if (null == v) {
                throw new IllegalStateException("Adapter.onCreateView() can't return null !");
            }
            if (CellLayout.this != v.getParent()) {
                addViewInLayout(v, -1, generateDefaultLayoutParams(), true);
            }
            return v;
        }

        private void bindContentToCell(Cell cell, View v) {
            if (v.getMeasuredWidth() != cell.width() || v.getMeasuredHeight() != cell.height()) {
                v.measure(MeasureSpec.makeMeasureSpec(cell.width(), MeasureSpec.EXACTLY),
                        MeasureSpec.makeMeasureSpec(cell.height(), MeasureSpec.EXACTLY));
            }
            adapter.onBindView(cell, v);
            cell.setHasContent();
            activeCells.put(cell, v);
        }

        private void layoutAllContent() {
            final Collection<Map.Entry<Cell, View>> entrySet = activeCells.entrySet();
            for (Map.Entry<Cell, View> entry : entrySet) {
                final Cell cell = entry.getKey();
                entry.getValue().layout(cell.left, cell.top, cell.right, cell.bottom);
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
            if (null == view) return;
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