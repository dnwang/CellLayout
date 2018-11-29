package org.pinwheel.view.celllayout;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.view.KeyEvent;
import android.view.LayoutInflater;
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

    private OnScrollListener onScrollListener;
    private OnSelectChangedListener onSelectChangedListener;

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
        setFocusable(true);
        setFocusableInTouchMode(true);
        setChildrenDrawingOrderEnabled(true);
//        setWillNotDraw(false);

        holderPaint.setColor(Color.parseColor("#4F586E"));
        focusPaint.setColor(Color.WHITE);
        focusPaint.setStyle(Paint.Style.STROKE);
        focusPaint.setStrokeWidth(FOCUS_STOKE_WIDTH);
    }

    public void setAdapter(ViewAdapter adapter) {
        if (adapter instanceof StyleAdapter) {
            final StyleAdapter a = (StyleAdapter) adapter;
            a.inflater = LayoutInflater.from(getContext());
            setOnSelectChangedListener(a);
        }
        viewManager.setAdapter(adapter);
    }

    public void setRoot(Cell root) {
        viewManager.checkAndReleaseCache(true);
        director.setRoot(root);
        director.forceLayout();
    }

    public void setOnSelectChangedListener(OnSelectChangedListener onSelectChangedListener) {
        this.onSelectChangedListener = onSelectChangedListener;
    }

    public void setOnScrollListener(OnScrollListener onScrollListener) {
        this.onScrollListener = onScrollListener;
    }

    public Cell findCellByView(View v) {
        return viewManager.findCellByView(v);
    }

    public View findViewByCell(Cell cell) {
        return viewManager.findViewByCell(cell);
    }

    @Deprecated
    public void keepCellCenter(Cell cell, boolean withAnimation) {
        if (!director.hasRoot() || null == cell) {
            return;
        }
        final Cell rect = director.getRoot();
        int dx = rect.centerX() - cell.centerX();
        int dy = rect.centerY() - cell.centerY();
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

    private final static float SCALE_MAX = 1.1f;
    private final static float SCALE_MIN = 1f;

    private final class SwitchScaleAction {
        int sum = 4;
        final float unit = (SCALE_MAX - SCALE_MIN) / sum;
        final View zoomIn, zoomOut;

        SwitchScaleAction(View zoomIn, View zoomOut) {
            this.zoomIn = zoomIn;
            this.zoomOut = zoomOut;
        }

        final void execute() {
            flag |= FLAG_SCALING;
            post(new Runnable() {
                @Override
                public void run() {
                    float scale;
                    if (null != zoomIn) {
                        scale = Math.max(SCALE_MIN, zoomIn.getScaleX() - unit);
                        zoomIn.setScaleX(scale);
                        zoomIn.setScaleY(scale);
                    }
                    if (null != zoomOut) {
                        scale = Math.min(SCALE_MAX, zoomOut.getScaleX() + unit);
                        zoomOut.setScaleX(scale);
                        zoomOut.setScaleY(scale);
                    }
                    invalidate();
                    if (sum-- > 0) {
                        post(this);
                    } else {
                        flag &= ~FLAG_SCALING;
                    }
                }
            });
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

    private final LongKeyPressDirector longKeyPressDirector = new LongKeyPressDirector() {
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
            if (null == focusManager.getFocus()) {
                return;
            }
            flag |= FLAG_MOVING_LONG_PRESS;
            moveGroup = director.findLinearGroupBy(focusManager.getFocus(), orientation);
            moveDistance = 0;
        }

        private void releaseLongPress() {
            moveGroup = null;
            final boolean tmp = (flag & FLAG_MOVING_LONG_PRESS) != 0;
            flag &= ~FLAG_MOVING_LONG_PRESS;
            if (tmp) {
                director.notifyScrollComplete();
            }
        }

        boolean intercept = false;
        int moveDistance = 0;

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
                            moveDistance += OFFSET;
                            invalidate();
                        } else {
                            // move complete at the bottom
                            releaseLongPress();
                            intercept = true;
                            // find new focus
                            focusManager.moveFocusBy(focusManager.getFocus(), moveDistance, convertKeyCodeToFocusDir(keyCode));
                        }
                    }
                }
            } else if (KeyEvent.ACTION_UP == action) {
                if (!intercept) {
                    releaseLongPress();
                    // find new focus
                    focusManager.moveFocusBy(focusManager.getFocus(), moveDistance, convertKeyCodeToFocusDir(keyCode));
                }
                intercept = false;
            }
            return true;
        }

        @Override
        public boolean onSinglePress(int keyCode) {
            focusManager.moveFocusBy(focusManager.getFocus(), 0, convertKeyCodeToFocusDir(keyCode));
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
        if (getChildAt(i) == viewManager.findViewByCell(focusManager.getFocus())) {
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
            // cellLayout has focus
            if (hasFocus() && FOCUS_HIGHLIGHT && cell == focusManager.getFocus()) {
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
        focusManager.setFocus(findFirstCell((CellGroup) director.getRoot()));
    }

    @Override
    public void onScroll(CellGroup group, int dx, int dy) {
        if (null != onScrollListener) {
            onScrollListener.onScroll(group, dx, dy);
        }
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

        if (null != onScrollListener) {
            onScrollListener.onScrollComplete();
        }
    }

    public interface ViewAdapter {
        int getViewType(Cell cell);

        View onCreateView(Cell cell);

        void onBindView(Cell cell, View view);

        void onViewRecycled(Cell cell, View view);
    }

    public interface OnSelectChangedListener {
        void onSelectChanged(Cell oldCell, View oldView, Cell newCell, View newView);
    }

    public interface OnScrollListener {
        void onScroll(CellGroup group, int dx, int dy);

        void onScrollComplete();
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
                // the cache set null, it can be skip measure
//                final View cache = (flag & FLAG_MOVING_LONG_PRESS) != 0 ? null : pool.obtain(cell, true);
                final View cache = cell.isNoHolder() ? pool.obtain(cell, true) : null;
                if (null != cache) {
                    bindContentToCell(cell, cache);
                } else { // holder
                    cell.setHasContent(false);
                    activeCells.put(cell, null);
                }
            } else { // remove active view
                final View v = activeCells.remove(cell);
                if (cell.hasContent()) {
                    cell.setHasContent(false);
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
            cell.setHasContent(true);
            cell.setFocusable(v.isFocusable());
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
        private Cell focusCell = null;

        Cell getFocus() {
            return focusCell;
        }

        void setFocus(Cell cell) {
            final View from = viewManager.findViewByCell(focusCell);
            final View to = viewManager.findViewByCell(cell);
            if (null != from || null != to) {
                if (FOCUS_SCALE) {
                    new SwitchScaleAction(from, to).execute();
                }
                if (null != onSelectChangedListener) {
                    onSelectChangedListener.onSelectChanged(focusCell, from, cell, to);
                }
            }
            focusCell = cell;
            if (null != focusCell) {
                checkAndMoveFocusVisible();
            }
        }

        private void checkAndMoveFocusVisible() {
            final Rect area = new Rect(director.getRoot());
            final int wSpace = area.width() / 5;
            final int hSpace = area.height() / 5;
            int dx = 0, dy = 0;
            if (focusCell.left < (area.left + wSpace)) {
                dx = (area.left + wSpace) - focusCell.left;
            } else if (focusCell.right > (area.right - wSpace)) {
                dx = (area.right - wSpace) - focusCell.right;
            }
            if (focusCell.top < (area.top + hSpace)) {
                dy = (area.top + hSpace) - focusCell.top;
            } else if (focusCell.bottom > (area.bottom - hSpace)) {
                dy = (area.bottom - hSpace) - focusCell.bottom;
            }
            if (0 != dx || 0 != dy) {
                final LinearGroup vLinear = director.findLinearGroupBy(focusCell, LinearGroup.VERTICAL);
                final LinearGroup hLinear = director.findLinearGroupBy(focusCell, LinearGroup.HORIZONTAL);
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

        void moveFocusBy(final Cell from, final int distance, final int dir) {
            if (null == from) return;
            final CellGroup root = (CellGroup) director.getRoot();
            final int maxWidth, maxHeight;
            if (root instanceof LinearGroup) {
                LinearGroup linear = (LinearGroup) root;
                maxWidth = linear.getContentWidth();
                maxHeight = linear.getContentHeight();
            } else {
                maxWidth = root.width();
                maxHeight = root.height();
            }
            final LinearGroup group = (LinearGroup) director.getRoot();
            final Rect limitArea;
            switch (dir) {
                case View.FOCUS_LEFT:
                    limitArea = new Rect(from.left - maxWidth, from.top, from.right - from.width(), from.bottom);
                    break;
                case View.FOCUS_UP:
                    limitArea = new Rect(from.left, from.top - maxHeight, from.right, from.bottom - from.height());
                    break;
                case View.FOCUS_RIGHT:
                    limitArea = new Rect(from.left + from.width(), from.top, from.right + maxWidth, from.bottom);
                    break;
                case View.FOCUS_DOWN:
                    limitArea = new Rect(from.left, from.top + from.height(), from.right, from.bottom + maxHeight);
                    break;
                default:
                    limitArea = null;
            }
            if (null == limitArea) return;
            Sync.execute(new Sync.Function<Cell>() {
                Cell tmp = null;

                @Override
                public Cell call() {
                    group.foreachAllCells(false, new Filter<Cell>() {
                        @Override
                        public boolean call(Cell cell) {
                            if (cell.isFocusable() && Rect.intersects(limitArea, cell)) {
                                if (null != tmp) {
                                    int d1, d2;
                                    if (View.FOCUS_LEFT == dir || View.FOCUS_RIGHT == dir) {
                                        d1 = Math.abs(cell.left - from.left);
                                        d2 = Math.abs(tmp.left - from.left);
                                    } else {
                                        d1 = Math.abs(cell.top - from.top);
                                        d2 = Math.abs(tmp.top - from.top);
                                    }
                                    if (distance < d1 && d1 < d2) {
                                        tmp = cell;
                                    }
                                } else {
                                    tmp = cell;
                                }
                            }
                            return false;
                        }
                    });
                    return tmp;
                }
            }, new Sync.Action<Cell>() {
                @Override
                public void call(Cell newFocus) {
                    if (null != newFocus) {
                        setFocus(newFocus);
                    }
                }
            });
        }
    }

    private static int convertKeyCodeToFocusDir(final int keyCode) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_LEFT:
                return View.FOCUS_LEFT;
            case KeyEvent.KEYCODE_DPAD_UP:
                return View.FOCUS_UP;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                return View.FOCUS_RIGHT;
            case KeyEvent.KEYCODE_DPAD_DOWN:
                return View.FOCUS_DOWN;
            default:
                return -1;
        }
    }

    private static Cell findFirstCell(CellGroup group) {
        int size = group.getCellCount();
        for (int i = 0; i < size; i++) {
            Cell subCell = group.getCellAt(i);
            if (subCell instanceof CellGroup) {
                CellGroup subCellGroup = (CellGroup) subCell;
                if (subCellGroup.getCellCount() > 0) {
                    Cell target = findFirstCell(subCellGroup);
                    if (null != target) {
                        return target;
                    }
                }
            } else {
                return subCell;
            }
        }
        return null;
    }

}