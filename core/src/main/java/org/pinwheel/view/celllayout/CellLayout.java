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

import java.util.Collection;
import java.util.HashMap;
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
public class CellLayout extends ViewGroup implements CellDirector.LifeCycleCallback {
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
        Sync.prepare();
    }

    @Override
    protected void onDetachedFromWindow() {
        Sync.release();
        super.onDetachedFromWindow();
    }

    private final CellDirector director = new CellDirector();
    private final ViewManager viewManager = new ViewManager();
    private final FocusManager focusManager = new FocusManager();

    private int flag = 0;

    private Paint holderPaint = new Paint();
    private Paint focusPaint = new Paint();
    private static final int FOCUS_STOKE_WIDTH = 4;
    private static final boolean FOCUS_HIGHLIGHT = true;
    private static final boolean FOCUS_SCALE = true;

    private void init() {
        director.setCallback(this);
//        setWillNotDraw(false);
        setChildrenDrawingOrderEnabled(true);

        holderPaint.setColor(Color.parseColor("#4F586E"));
        focusPaint.setColor(Color.WHITE);
        focusPaint.setStyle(Paint.Style.STROKE);
        focusPaint.setStrokeWidth(FOCUS_STOKE_WIDTH);
    }

    public void setAdapter(ViewAdapter adapter) {
        viewManager.setAdapter(adapter);
    }

    public void setRoot(Cell root) {
        director.setRoot(root);
        director.forceLayout();
    }

    public Cell findCellByView(View v) {
        return viewManager.findCellByView(v);
    }

    public View findViewByCell(Cell cell) {
        return viewManager.findViewByCell(cell);
    }

    public void keepCellCenter(Cell cell, boolean withAnimation) {
        if (!director.hasRoot() || null == cell) {
            return;
        }
        final Cell rect = director.getRoot();
        int dx = rect.centerX() - cell.centerX();
        int dy = rect.centerY() - cell.centerY();
//        dx = Math.abs(dx) < rect.width() / 5 ? 0 : dx;
//        dy = Math.abs(dy) < rect.height() / 5 ? 0 : dy;
        final LinearGroup vLinear = director.findLinearGroupBy(cell, LinearGroup.VERTICAL);
        final LinearGroup hLinear = director.findLinearGroupBy(cell, LinearGroup.HORIZONTAL);
        if (!withAnimation || (Math.abs(dx) + Math.abs(dy) < 10)) {
            if (director.scrollBy(vLinear, 0, dy) | director.scrollBy(hLinear, dx, 0)) {
                invalidate();
                director.notifyScrollComplete();
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
                        director.notifyScrollComplete();
                    }
                }
            });
        }

        abstract void onMove(final int dx, final int dy);
    }

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
                    director.notifyScrollComplete();
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
//            return KeyEvent.KEYCODE_DPAD_LEFT == keyCode
//                    || KeyEvent.KEYCODE_DPAD_UP == keyCode
//                    || KeyEvent.KEYCODE_DPAD_RIGHT == keyCode
//                    || KeyEvent.KEYCODE_DPAD_DOWN == keyCode;
            return KeyEvent.KEYCODE_DPAD_UP == keyCode
                    || KeyEvent.KEYCODE_DPAD_DOWN == keyCode;
        }

        private static final int OFFSET = 300;
        private LinearGroup moveGroup;

        private void prepareLongPress(int orientation) {
            if (!focusManager.hasFocus()) {
                return;
            }
            flag |= FLAG_MOVING_LONG_PRESS;
            moveGroup = director.findLinearGroupBy(focusManager.getFocus(), orientation);
            scrollDistance = 0;
            setFocusable(true); // dispatchKeyEvent
            requestFocus();
        }

        private void releaseLongPress(int focusDir) {
            moveGroup = null;
            final boolean tmp = (flag & FLAG_MOVING_LONG_PRESS) != 0;
            flag &= ~FLAG_MOVING_LONG_PRESS;
            if (tmp) {
                director.notifyScrollComplete();
            }
            focusManager.switchToCell(focusDir, director.getRoot());
            scrollDistance = 0;
            clearFocus();
            setFocusable(false);
        }

        int scrollDistance = 0;
        boolean intercept = false;
        int focusDir = 0;

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
                                focusDir = View.FOCUS_LEFT;
                                scrollDistance += OFFSET;
                                break;
                            case KeyEvent.KEYCODE_DPAD_UP:
                                moved = director.scrollBy(moveGroup, 0, OFFSET);
                                focusDir = View.FOCUS_UP;
                                scrollDistance += OFFSET;
                                break;
                            case KeyEvent.KEYCODE_DPAD_RIGHT:
                                moved = director.scrollBy(moveGroup, -OFFSET, 0);
                                focusDir = View.FOCUS_RIGHT;
                                scrollDistance += OFFSET;
                                break;
                            case KeyEvent.KEYCODE_DPAD_DOWN:
                                moved = director.scrollBy(moveGroup, 0, -OFFSET);
                                focusDir = View.FOCUS_DOWN;
                                scrollDistance += OFFSET;
                                break;
                        }
                        if (moved) {
                            invalidate();
                        } else {
                            // move complete at the bottom
                            releaseLongPress(focusDir);
                            intercept = true;
                        }
                    }
                }
            } else if (KeyEvent.ACTION_UP == action) {
                if (!intercept) {
                    releaseLongPress(focusDir);
                }
                intercept = false;
            }
            return true;
        }

        @Override
        public boolean onSinglePress(int keyCode) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_DPAD_LEFT:
                    focusManager.switchToNearestCell(View.FOCUS_LEFT);
                    break;
                case KeyEvent.KEYCODE_DPAD_UP:
                    focusManager.switchToNearestCell(View.FOCUS_UP);
                    break;
                case KeyEvent.KEYCODE_DPAD_RIGHT:
                    focusManager.switchToNearestCell(View.FOCUS_RIGHT);
                    break;
                case KeyEvent.KEYCODE_DPAD_DOWN:
                    focusManager.switchToNearestCell(View.FOCUS_DOWN);
                    break;
            }
            return false;
        }
    };

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        return longKeyPressDirector.dispatchKeyEvent(event) || super.dispatchKeyEvent(event);
    }

    private void drawEmptyHolder(final Canvas canvas) {
        viewManager.foreachActiveCells(new Filter<Cell>() {
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
        final View focus = viewManager.findViewByCell(focusManager.getFocus());
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
        if (!viewManager.isEmpty()) {
            invalidate();
        }
        super.childDrawableStateChanged(child);
    }

    @Override
    protected boolean drawChild(Canvas canvas, View child, long drawingTime) {
        final Cell cell = viewManager.findCellByView(child);
        if (null != cell) {
            if (FOCUS_HIGHLIGHT && cell == focusManager.getFocus()) {
                float dw = cell.width() * (child.getScaleX() - 1);
                float dh = cell.height() * (child.getScaleY() - 1);
                canvas.save();
                canvas.translate(cell.left - dw / 2 - FOCUS_STOKE_WIDTH / 2, cell.top - dh / 2 - FOCUS_STOKE_WIDTH / 2);
                canvas.scale(child.getScaleX(), child.getScaleY());
                canvas.drawRect(0, 0, cell.width() + FOCUS_STOKE_WIDTH, cell.height() + FOCUS_STOKE_WIDTH, focusPaint);
                canvas.restore();
            }
            canvas.save();
            canvas.translate(cell.left - child.getLeft(), cell.top - child.getTop());
            final boolean result = super.drawChild(canvas, child, drawingTime);
            canvas.restore();
            return result;
        } else {
            return false;
        }
    }

    @Override
    public void onCellLayout() {
        viewManager.replaceAllHolder();
        viewManager.layoutAllContent();
        // init focus
        focusManager.setFocus(director.getFirstCell());
    }

    @Override
    public void onScroll(CellGroup group, int dx, int dy) {
    }

    @Override
    public void onVisibleChanged(Cell cell) {
        if (cell instanceof CellGroup) return; // don't care group
        viewManager.onVisibleChanged(cell);
    }

    @Override
    public void onScrollComplete() {
        viewManager.replaceAllHolder();
        viewManager.layoutAllContent();
        // recycle should be in last
        viewManager.checkAndReleaseCache(false);

        viewManager.logInfo();
    }

    public interface ViewAdapter {
        int getViewType(Cell cell);

        View onCreateView(Cell cell);

        void onBindView(Cell cell, View view);

        void onViewRecycled(Cell cell, View view);
    }

    private final class ViewManager {
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

        boolean isEmpty() {
            return 0 == activeCells.size();
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

        void onVisibleChanged(final Cell cell) {
            final ViewPool pool = getViewPool(cell);
            if (cell.isVisible()) { // add active view
//                final View cache = (flag & FLAG_MOVING_LONG_PRESS) != 0 ? null : pool.obtain(cell, true);
                final View cache = null;// skip cache measure
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

        void logInfo() {
            // log viewManager info
            final int size = poolMap.size();
            Log.d(TAG, "[into] --------------");
            for (int i = 0; i < size; i++) {
                Log.d(TAG, "[into] poolMap_key_" + poolMap.keyAt(i) + " size: " + poolMap.valueAt(i).size());
            }
            Log.d(TAG, "[into] activeCells size: " + activeCells.size());
            Log.d(TAG, "[into] --------------");
        }
    }

    private final class FocusManager {
        private Cell currentFocus = null;

        boolean hasFocus() {
            return null != currentFocus;
        }

        void setFocus(Cell cell) {
            if (FOCUS_SCALE) {
                new SwitchScaleAction(viewManager.findViewByCell(currentFocus),
                        viewManager.findViewByCell(cell)).execute();
            }
            currentFocus = cell;
            keepCellCenter(currentFocus, true);
            invalidate();
        }

        Cell getFocus() {
            return currentFocus;
        }

        void switchToNearestCell(final int dir) {
            findNextCell(currentFocus, null, dir);
        }

        void switchToCell(final int dir, Rect area) {
            findNextCell(currentFocus, area, dir);
        }

        private void findNextCell(final Cell from, final Rect limitArea, final int dir) {
            if (null == from || !director.hasRoot()) return;
            final CellGroup group = (CellGroup) director.getRoot();
            switch (dir) {
                case View.FOCUS_LEFT:
                    if (from.left <= group.left + group.paddingLeft) return;
                    break;
                case View.FOCUS_UP:
                    if (from.top <= group.top + group.paddingTop) return;
                    break;
                case View.FOCUS_RIGHT:
                    if (from.right >= group.right - group.paddingRight) return;
                    break;
                case View.FOCUS_DOWN:
                    if (from.bottom >= group.bottom - group.paddingBottom) return;
                    break;
            }
            // find new focus in group
            Sync.execute(new Sync.Function<Cell>() {
                Cell tmp = null;

                @Override
                public Cell call() {
                    group.foreachAllCells(false, new Filter<Cell>() {
                        @Override
                        public boolean call(Cell cell) {
                            if (null != limitArea && !limitArea.contains(cell)) {
                                return false;
                            }
                            if (null == tmp) {
                                tmp = cell;
                                return false;
                            }
                            int newDistance, oldDistance;
                            switch (dir) {
                                case View.FOCUS_LEFT:
                                    newDistance = Math.abs(cell.right - from.left);
                                    oldDistance = Math.abs(tmp.right - from.left);
                                    if (newDistance < oldDistance || (newDistance == oldDistance
                                            && Math.abs(cell.top - from.top) < Math.abs(tmp.top - from.top))) {
                                        tmp = cell;
                                    }
                                    break;
                                case View.FOCUS_UP:
                                    newDistance = Math.abs(cell.bottom - from.top);
                                    oldDistance = Math.abs(tmp.bottom - from.top);
                                    if (newDistance < oldDistance || (newDistance == oldDistance
                                            && Math.abs(cell.left - from.left) < Math.abs(tmp.left - from.left))) {
                                        tmp = cell;
                                    }
                                    break;
                                case View.FOCUS_RIGHT:
                                    newDistance = Math.abs(cell.left - from.right);
                                    oldDistance = Math.abs(tmp.left - from.right);
                                    if (newDistance < oldDistance || (newDistance == oldDistance
                                            && Math.abs(cell.top - from.top) < Math.abs(tmp.top - from.top))) {
                                        tmp = cell;
                                    }
                                    break;
                                case View.FOCUS_DOWN:
                                    newDistance = Math.abs(cell.top - from.bottom);
                                    oldDistance = Math.abs(tmp.top - from.bottom);
                                    if (newDistance < oldDistance || (newDistance == oldDistance
                                            && Math.abs(cell.left - from.left) < Math.abs(tmp.left - from.left))) {
                                        tmp = cell;
                                    }
                                    break;
                            }
                            return false;
                        }
                    });
                    return tmp;
                }
            }, new Sync.Action<Cell>() {
                @Override
                public void call(Cell cell) {
                    if (null != cell) {
                        setFocus(cell);
                    }
                }
            });
        }

        private final static float SCALE_MAX = 1.1f;
        private final static float SCALE_MIN = 1f;

        private final class SwitchScaleAction {
            int sum = 4;
            final float unit = (SCALE_MAX - SCALE_MIN) / sum;
            final View zoomIn, zoomOut;

            SwitchScaleAction(View zoomIn, View zoomOut) {
                this.zoomIn = zoomIn == CellLayout.this ? null : zoomIn;
                this.zoomOut = zoomOut;
            }

            final void execute() {
                flag |= FLAG_SCALING;
                post(new Runnable() {
                    @Override
                    public void run() {
                        if (null != zoomIn) {
                            zoomIn.setScaleX(zoomIn.getScaleX() - unit);
                            zoomIn.setScaleY(zoomIn.getScaleY() - unit);
                        }
                        if (null != zoomOut) {
                            zoomOut.setScaleX(zoomOut.getScaleX() + unit);
                            zoomOut.setScaleY(zoomOut.getScaleY() + unit);
                        }
                        if (sum-- > 0) {
                            post(this);
                        } else {
                            flag &= ~FLAG_SCALING;
                        }
                    }
                });
            }
        }
    }

}