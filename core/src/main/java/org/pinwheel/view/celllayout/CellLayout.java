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
                post(new SwitchScaleAction(oldFocus, newFocus));
            }
        }
    };

    private final CellDirector director = new CellDirector(this);
    private final ViewManager manager = new ViewManager();

    private void init() {
        setWillNotDraw(false);
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
        final Cell rootRect = director.getRoot();
        final int dx = rootRect.centerX() - cell.centerX();
        final int dy = rootRect.centerY() - cell.centerY();
        final LinearGroup vLinear = director.findLinearGroupBy(cell, LinearGroup.VERTICAL);
        final LinearGroup hLinear = director.findLinearGroupBy(cell, LinearGroup.HORIZONTAL);
        if (!withAnimation || (Math.abs(dx) + Math.abs(dy) < 10)) {
            director.moveBy(vLinear, 0, dy);
            director.moveBy(hLinear, dx, 0);
            director.onMoveComplete();
        } else {
            post(new MovingAction(dx, dy, new MovingActionCallback() {
                @Override
                public void move(int offsetX, int offsetY) {
                    director.moveBy(hLinear, offsetX, 0);
                    director.moveBy(vLinear, 0, offsetY);
                }

                @Override
                public void end() {
                    director.onMoveComplete();
                }
            }));
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
            int dx = x - (cell.left + cell.scrollX);
            int dy = y - (cell.top + cell.scrollY);
            if (!withAnimation) {
                director.moveBy(cell, dx, dy);
                director.onMoveComplete();
            } else {
                post(new MovingAction(dx, dy, new MovingActionCallback() {
                    @Override
                    public void move(int offsetX, int offsetY) {
                        director.moveBy(cell, offsetX, offsetY);
                    }

                    @Override
                    public void end() {
                        director.onMoveComplete();
                    }
                }));
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
                post(new MovingAction(dx, dy, new MovingActionCallback() {
                    @Override
                    public void move(int offsetX, int offsetY) {
                        director.moveBy(cell, offsetX, offsetY);
                    }

                    @Override
                    public void end() {
                        director.onMoveComplete();
                    }
                }));
            }
        }
    }

    private interface MovingActionCallback {
        void move(int offsetX, int offsetY);

        void end();
    }

    private final class MovingAction implements Runnable {
        int sum;
        final int unitX, unitY;
        final MovingActionCallback callback;

        MovingAction(int dx, int dy, MovingActionCallback callback) {
            this.callback = callback;
            final int absDx = Math.abs(dx);
            final int absDy = Math.abs(dy);
            sum = Math.max(absDx, absDy) > 300 ? 6 : 4;
            unitX = absDx > sum ? (dx / sum) : (0 != dx ? (dx / absDx) : 0);
            unitY = absDy > sum ? (dy / sum) : (0 != dy ? (dy / absDy) : 0);
        }

        @Override
        public void run() {
            callback.move(unitX, unitY);
            if (sum-- > 0) {
                post(this);
            } else {
                callback.end();
            }
        }
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
            }
        }
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
                    final int dir = absDx > absDy ? LinearGroup.HORIZONTAL : LinearGroup.VERTICAL;
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
            // switch focus to root
            setFocusable(true);
            requestFocus();
            final View v = focused.getValue();
            v.setScaleX(1);
            v.setScaleY(1);
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
                setFocusable(false);
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

    private Paint focusP = new Paint();

    @Override
    public void onDraw(Canvas canvas) {
    }

    @Override
    public void onDrawForeground(Canvas canvas) {
        // draw holder
        final Collection<Cell> cells = manager.activeCells.keySet();
        for (Cell cell : cells) {
            if (!cell.hasContentView()) {
                holderPaint.setColor(Color.DKGRAY);
                canvas.drawRect(cell.left, cell.top, cell.right, cell.bottom, holderPaint);
            }
        }
        // draw focus
        if (null != touchCell) {
            focusP.setStyle(Paint.Style.STROKE);
            focusP.setStrokeWidth(8);
            focusP.setColor(Color.RED);
            canvas.drawRect(touchCell.left - 10, touchCell.top - 10, touchCell.right + 10, touchCell.bottom + 10, focusP);
        }
    }

    @Override
    protected int getChildDrawingOrder(int childCount, int i) {
        Log.e(TAG, "getChildDrawingOrder: childCount: " + childCount + ", i: " + i);
        // TODO: 2018/11/23
        return i;
    }

    private Paint holderPaint = new Paint();

    @Override
    protected boolean drawChild(Canvas canvas, View child, long drawingTime) {
        final Cell cell = manager.findCellByView(child);
        if (null != cell) {
            // first draw in target position, layout it on move complete!
            canvas.save();
            canvas.translate(cell.left, cell.top);
            child.draw(canvas);
            canvas.restore();
            return true;
            // super method will be draw child by layout position
            // return super.drawChild(canvas, child, drawingTime);
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

        View findViewByCell(Cell cell) {
            return activeCells.get(cell);
        }

        @Deprecated
        Map.Entry<Cell, View> findFocusedCell() {
            Collection<Map.Entry<Cell, View>> entrySet = activeCells.entrySet();
            for (Map.Entry<Cell, View> entry : entrySet) {
                if (entry.getValue().hasFocus()) {
                    return entry;
                }
            }
            return null;
        }

        @Deprecated
        Map.Entry<Cell, View> randomActiveCell() {
            return activeCells.entrySet().iterator().next();
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
        public void onVisibleChanged(final Cell cell) {
            if (cell instanceof CellGroup) { // don't care group
                return;
            }
            final ViewPool pool = getViewPool(cell);
            if (cell.isVisible()) { // add active view
                final View cache = pool.obtain(cell, true);
                if (null != cache) {
                    Log.e(TAG, "[onVisibleChanged] use content cache !! ");
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
            cell.setHasContentView();
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

        View obtain() {
            return obtain(null, false);
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