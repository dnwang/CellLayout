package org.pinwheel.view.celllayout;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.view.FocusFinder;
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

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public CellLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        this.init();
    }

    @Deprecated
    @Override
    public void addView(View child) {
        throw new UnsupportedOperationException("addView(View) is not supported in CellLayout");
    }

    @Deprecated
    @Override
    public void addView(View child, int index) {
        throw new UnsupportedOperationException("addView(View, int) is not supported in CellLayout");
    }

    @Deprecated
    @Override
    public void addView(View child, LayoutParams params) {
        throw new UnsupportedOperationException("addView(View, LayoutParams) is not supported in CellLayout");
    }

    @Deprecated
    @Override
    public void addView(View child, int index, LayoutParams params) {
        throw new UnsupportedOperationException("addView(View, int, LayoutParams) is not supported in CellLayout");
    }

    @Deprecated
    @Override
    public void addView(View child, int width, int height) {
        throw new UnsupportedOperationException("addView(View, int, int) is not supported in CellLayout");
    }

    @Deprecated
    @Override
    public void removeView(View child) {
        throw new UnsupportedOperationException("removeView(View) is not supported in CellLayout");
    }

    @Deprecated
    @Override
    public void removeViewAt(int index) {
        throw new UnsupportedOperationException("removeViewAt(int) is not supported in CellLayout");
    }

    @Deprecated
    @Override
    public void removeAllViews() {
        throw new UnsupportedOperationException("removeAllViews() is not supported in CellLayout");
    }

    @Override
    protected void onAttachedToWindow() {
        Sync.prepare();
        super.onAttachedToWindow();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        Sync.release();
    }

    private OnSelectChangedListener onSelectChangedListener;

    private final CellDirector director = new CellDirector();
    private final ViewManager viewManager = new ViewManager();
    private final FocusManager focusManager = new FocusManager();

    private int flag = 0;

    private HolderDrawable holderDrawable;
    private BorderDrawable borderDrawable;

    private static final boolean SCALE_FOCUS = true;

    private void init() {
        director.setCallback(this);
        setFocusable(true);
        setFocusableInTouchMode(true);
        setChildrenDrawingOrderEnabled(true);
//        setWillNotDraw(false);

        holderDrawable = new DefHolderDrawable();
        borderDrawable = new DefBorderDrawable();
    }

    public void setHolderDrawable(HolderDrawable drawable) {
        this.holderDrawable = drawable;
    }

    public void setBorderDrawable(BorderDrawable drawable) {
        this.borderDrawable = drawable;
    }

    public void setAdapter(ViewAdapter adapter) {
        if (adapter instanceof StyleAdapter) {
            final StyleAdapter tmp = (StyleAdapter) adapter;
            tmp.inflater = LayoutInflater.from(getContext());
        }
        viewManager.setAdapter(adapter);
    }

    public void setContentCell(Cell root) {
        viewManager.checkAndReleaseCache(true);
        focusManager.clear();
        if (director.getRoot() instanceof CellGroup) { // clear
            ((CellGroup) director.getRoot()).setOnScrollListener(null);
        }
        director.setRoot(root);
    }

    public Cell getContentCell() {
        return director.getRoot();
    }

    public void addCell(Cell cell) {
        if (!director.hasRoot()) return;
        if (director.getRoot() instanceof CellGroup) {
            ((CellGroup) director.getRoot()).merge(cell);
        }
    }

    public void setOnSelectChangedListener(OnSelectChangedListener listener) {
        this.onSelectChangedListener = listener;
    }

    public void setOnRootCellScrollListener(CellGroup.OnScrollListener listener) {
        if (!director.hasRoot()) return;
        final Cell root = director.getRoot();
        if (root instanceof CellGroup) {
            ((CellGroup) root).setOnScrollListener(listener);
        }
    }

    public Cell findCellByView(View v) {
        return viewManager.findCellByView(v);
    }

    public View findViewByCell(Cell cell) {
        return viewManager.findViewByCell(cell);
    }

    public void setFocus(View view) {
        final Cell cell = viewManager.findCellByView(view);
        if (null != cell) {
            setFocus(cell);
        }
    }

    public void setFocus(Cell cell) {
        focusManager.setFocus(cell);
    }

    @Override
    public void scrollTo(int x, int y) {
        if (!director.hasRoot()) return;
        final Cell root = director.getRoot();
        final int dx = x - root.getLeft(), dy = y - root.getTop();
        scrollBy(dx, dy);
    }

    @Override
    public void scrollBy(int dx, int dy) {
        if (!director.hasRoot()) return;
        final Cell root = director.getRoot();
        if (root instanceof CellGroup) {
            if (director.scrollBy((CellGroup) root, dx, dy)) {
                invalidate();
                director.notifyScrollComplete();
            }
        }
    }

    @Deprecated
    public void scrollToCenter(Cell cell, boolean withAnimation) {
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

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        director.measure(getMeasuredWidth(), getMeasuredHeight());
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        director.layout(0, 0);
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
                flag &= ~FLAG_MOVING_TOUCH;
                if (tmp) {
                    director.notifyScrollComplete();
                }
                if (!tmp && null != touchCell && touchCell.isFocusable()) {
                    focusManager.setFocus(touchCell);
                }
                touchCell = null;
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
            if (!director.hasRoot()) return false;
            final Cell root = director.getRoot();
            if (root instanceof LinearGroup) {
                final int orientation = ((LinearGroup) root).getOrientation();
                if (LinearGroup.HORIZONTAL == orientation) {
                    return KeyEvent.KEYCODE_DPAD_LEFT == keyCode
                            || KeyEvent.KEYCODE_DPAD_RIGHT == keyCode;
                } else {
                    return KeyEvent.KEYCODE_DPAD_UP == keyCode
                            || KeyEvent.KEYCODE_DPAD_DOWN == keyCode;
                }
            } else {
                return KeyEvent.KEYCODE_DPAD_LEFT == keyCode
                        || KeyEvent.KEYCODE_DPAD_UP == keyCode
                        || KeyEvent.KEYCODE_DPAD_RIGHT == keyCode
                        || KeyEvent.KEYCODE_DPAD_DOWN == keyCode;
            }
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
            return focusManager.moveFocusBy(focusManager.getFocus(), 0, convertKeyCodeToFocusDir(keyCode));
        }
    };

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        return longKeyPressDirector.dispatchKeyEvent(event) || super.dispatchKeyEvent(event);
    }

    @Override
    public void draw(Canvas canvas) {
        focusOrder = -1;
        super.draw(canvas);
        onDrawHolders(canvas);
    }

    private void onDrawHolders(final Canvas canvas) {
        if (null == holderDrawable) return;
        viewManager.foreachActiveCells(new Filter<Cell>() {
            @Override
            public boolean call(Cell cell) {
                if (!cell.hasContent()) {
                    cell.computeParentScroll();
                    final int l = cell.getLayoutXWithParentScroll();
                    final int t = cell.getLayoutYWithParentScroll();
                    holderDrawable.onDraw(canvas, cell, l, t);
                }
                return false;
            }
        });
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
            cell.computeParentScroll();
            final int l = cell.getLayoutXWithParentScroll();
            final int t = cell.getLayoutYWithParentScroll();
            // cellLayout has focus
            if (null != borderDrawable && hasFocus() && cell.hasFocus()) {
                canvas.save();
                borderDrawable.onDraw(canvas, cell, l, t, child.getScaleX(), child.getScaleY());
                canvas.restore();
            }
            canvas.save();
            canvas.translate(l - child.getLeft(), t - child.getTop());
            final boolean result = super.drawChild(canvas, child, drawingTime);
            canvas.restore();
            return result;
        } else {
            return false;
        }
    }

    @Override
    public void onRefreshActiveCells() {
        viewManager.replaceAllHolder();
        viewManager.layoutAllContent();
        // init focus
        if (null == focusManager.getFocus()) {
            focusManager.setFocus(findFirstFocusableCell((CellGroup) director.getRoot()));
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
    }

    public interface HolderDrawable {
        void onDraw(Canvas canvas, Cell cell, int l, int t);
    }

    public interface BorderDrawable {
        void onDraw(Canvas canvas, Cell cell, int l, int t, float scaleX, float scaleY);
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
                // the cache set null, it will be skip measure
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
                    if (null != v) {
                        pool.recycle(v);
                        adapter.onViewRecycled(cell, v);
                    }
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
            // restore state
            if (cell.hasFocus()) {
                v.setScaleX(SCALE_MAX);
                v.setScaleY(SCALE_MAX);
            }
        }

        private void layoutAllContent() {
            final Collection<Map.Entry<Cell, View>> entrySet = activeCells.entrySet();
            for (Map.Entry<Cell, View> entry : entrySet) {
                final Cell cell = entry.getKey();
                final View v = entry.getValue();
                if (v.getLeft() != cell.getLeft() || v.getTop() != cell.getTop()
                        || v.getRight() != cell.getRight() || v.getBottom() != cell.getBottom()) {
                    v.layout(cell.getLeft(), cell.getTop(), cell.getRight(), cell.getBottom());
                }
            }
        }

        void logInfo() {
            // log viewManager info
            final int size = poolMap.size();
            Log.i(TAG, "[info] ------- CellLayout -------");
            for (int i = 0; i < size; i++) {
                Log.i(TAG, "[info] poolMap_style_" + poolMap.keyAt(i) + " size: " + poolMap.valueAt(i).size());
            }
            Log.i(TAG, "[info] activeCells size: " + activeCells.size());
            Log.i(TAG, "[info] --------------------------");
        }
    }

    private final class FocusManager {
        private Cell focusCell = null;

        void clear() {
            focusCell = null;
        }

        Cell getFocus() {
            return focusCell;
        }

        void setFocus(Cell cell) {
            final View from = viewManager.findViewByCell(focusCell);
            final View to = viewManager.findViewByCell(cell);
            if (null != from || null != to) {
                if (SCALE_FOCUS) {
                    new SwitchScaleAction(from, to).execute();
                }
                if (viewManager.adapter instanceof OnSelectChangedListener) {
                    ((OnSelectChangedListener) viewManager.adapter).onSelectChanged(focusCell, from, cell, to);
                }
                if (null != onSelectChangedListener) {
                    onSelectChangedListener.onSelectChanged(focusCell, from, cell, to);
                }
            }
            if (null != focusCell) {
                focusCell.setFocus(false);
            }
            focusCell = cell;
            if (null != focusCell) {
                focusCell.setFocus(true);
                checkAndMoveFocusVisible();
            }
        }

        private void checkAndMoveFocusVisible() {
            final Rect area = new Rect(director.getRoot());
            area.inset(area.width() / 6, area.height() / 6);
            int dx = 0, dy = 0;
            final int l = focusCell.getLeft(), t = focusCell.getTop(), r = focusCell.getRight(), b = focusCell.getBottom();
            if (l < area.getLeft()) {
                dx = area.getLeft() - l;
            } else if (r > area.getRight()) {
                dx = area.getRight() - r;
            }
            if (t < area.getTop()) {
                dy = area.getTop() - t;
            } else if (b > area.getBottom()) {
                dy = area.getBottom() - b;
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

        boolean moveFocusBy(final Cell from, final int distance, final int dir) {
            if (null == from) return false;
            final CellGroup root = (CellGroup) director.getRoot();
            final int maxWidth = root.getContentWidth();
            final int maxHeight = root.getContentHeight();
            final Rect limitArea;
            switch (dir) {
                case View.FOCUS_LEFT:
                    limitArea = new Rect(from.getLeft() - maxWidth, from.getTop(),
                            from.getRight() - from.width(), from.getBottom());
                    break;
                case View.FOCUS_UP:
                    limitArea = new Rect(from.getLeft(), from.getTop() - maxHeight,
                            from.getRight(), from.getBottom() - from.height());
                    break;
                case View.FOCUS_RIGHT:
                    limitArea = new Rect(from.getLeft() + from.width(), from.getTop(),
                            from.getRight() + maxWidth, from.getBottom());
                    break;
                case View.FOCUS_DOWN:
                    limitArea = new Rect(from.getLeft(), from.getTop() + from.height(),
                            from.getRight(), from.getBottom() + maxHeight);
                    break;
                default:
                    limitArea = null;
            }
            if (null == limitArea) return false;
            Sync.execute(new Sync.Function<Cell>() {
                Cell tmp = null;

                @Override
                public Cell call() {
                    root.foreachAllCells(false, new Filter<Cell>() {
                        @Override
                        public boolean call(Cell cell) {
                            if (cell.isFocusable() && Rect.intersects(limitArea, cell)) {
                                if (null != tmp) {
                                    int d1, d2;
                                    if (View.FOCUS_LEFT == dir || View.FOCUS_RIGHT == dir) {
                                        d1 = Math.abs(tmp.getLeft() - from.getLeft());
                                        d2 = Math.abs(cell.getLeft() - from.getLeft());
                                    } else {
                                        d1 = Math.abs(tmp.getTop() - from.getTop());
                                        d2 = Math.abs(cell.getTop() - from.getTop());
                                    }
                                    if (distance < d2 && (d1 < distance || d2 < d1)) {
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
                    } else {
                        moveSystemFocusBy(CellLayout.this, dir);
                    }
                }
            });
            return true;
        }
    }

    private static final class DefHolderDrawable implements HolderDrawable {
        private Paint paint;

        DefHolderDrawable() {
            paint = new Paint();
            paint.setColor(Color.parseColor("#4F586E"));
        }

        @Override
        public void onDraw(Canvas canvas, Cell cell, int l, int t) {
            if (!cell.isNoHolder()) {
                canvas.drawRect(l, t, l + cell.width(), t + cell.height(), paint);
            }
        }
    }

    private static final class DefBorderDrawable implements BorderDrawable {
        private static final int FOCUS_STOKE_WIDTH = 4;

        private Paint paint;

        DefBorderDrawable() {
            paint = new Paint();
            paint.setColor(Color.WHITE);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(FOCUS_STOKE_WIDTH);
        }

        @Override
        public void onDraw(Canvas canvas, Cell cell, int l, int t, float scaleX, float scaleY) {
            float dw = cell.width() * (scaleX - 1);
            float dh = cell.height() * (scaleY - 1);
            canvas.translate(l - dw / 2 - FOCUS_STOKE_WIDTH / 2, t - dh / 2 - FOCUS_STOKE_WIDTH / 2);
            canvas.scale(scaleX, scaleY);
            canvas.drawRect(0, 0, cell.width() + FOCUS_STOKE_WIDTH, cell.height() + FOCUS_STOKE_WIDTH, paint);
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

    private static Cell findFirstFocusableCell(CellGroup group) {
        int size = group.getCellCount();
        for (int i = 0; i < size; i++) {
            Cell subCell = group.getCellAt(i);
            if (subCell instanceof CellGroup) {
                CellGroup subCellGroup = (CellGroup) subCell;
                if (subCellGroup.getCellCount() > 0) {
                    Cell target = findFirstFocusableCell(subCellGroup);
                    if (null != target && target.isFocusable()) {
                        return target;
                    }
                }
            } else {
                return subCell;
            }
        }
        return null;
    }

    private static void moveSystemFocusBy(View focus, final int dir) {
        if (null == focus) return;
        final View v = FocusFinder.getInstance().findNextFocus((ViewGroup) focus.getRootView(), focus, dir);
        if (v != null) {
            v.requestFocus(dir);
        }
    }

}