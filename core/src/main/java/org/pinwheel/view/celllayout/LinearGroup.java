package org.pinwheel.view.celllayout;

import android.util.Log;

import static org.pinwheel.view.celllayout.CellLayout.BORDER_STOKE_WIDTH;
import static org.pinwheel.view.celllayout.CellLayout.SCALE_MAX;
import static org.pinwheel.view.celllayout.CellLayout.SCALE_MIN;

/**
 * Copyright (C), 2018 <br>
 * <br>
 * All rights reserved <br>
 * <br>
 *
 * @author dnwang
 * @version 2018/11/15,11:32
 */
public class LinearGroup extends CellGroup {

    public static final int HORIZONTAL = 0;
    public static final int VERTICAL = 1;

    @Attribute(fixedResolution = true)
    private int divider;
    @Attribute
    private int orientation;

    LinearGroup() {
        this(VERTICAL);
    }

    public LinearGroup(int orientation) {
        super();
        this.orientation = orientation;
        this.divider = 0;
    }

    public void setDivider(int divider) {
        this.divider = divider;
    }

    public void setOrientation(int orientation) {
        this.orientation = orientation;
    }

    @Override
    public CellGroup.Params getDefaultParams() {
        return new LinearGroup.Params();
    }

    @Override
    public void addCell(Cell cell, CellGroup.Params p) {
        if (cell instanceof LinearGroup &&
                ((LinearGroup) cell).getOrientation() == orientation) {
            throw new IllegalStateException("cell has the same with parent's orientation ! maybe unboxing the group");
        }
        super.addCell(cell, p);
    }

    @Override
    public void merge(Cell cell) {
        if (cell instanceof LinearGroup && ((LinearGroup) cell).getOrientation() == orientation) {
            // unboxing
            CellGroup group = (CellGroup) cell;
            int size = group.getCellCount();
            while (size > 0) {
                Cell tmp = group.getCellAt(0);
                tmp.removeFromParent();
                addCellInner(tmp, tmp.getParams());
                size--;
            }
            requestMeasureAndLayout();
        } else {
            super.merge(cell);
        }
    }

    @Override
    protected void measure(final int width, final int height) {
        super.measure(width, height);
        final int size = getCellCount();
        for (int i = 0; i < size; i++) {
            final Cell cell = getCellAt(i);
            if (cell.isMeasured()) continue;
            CellGroup.Params p = cell.getParams();
            if (HORIZONTAL == orientation) {
                if (p.width <= 0) {
                    Log.w(CellLayout.TAG, "[LinearGroup.measure]: cell have no 'width' params in linearGroup with 'HORIZONTAL', the cell will not visible !");
                }
                int h = height - paddingTop - paddingBottom - p.marginTop - p.marginBottom;
                cell.measure(p.width, h);
            } else {
                if (p.height <= 0) {
                    Log.w(CellLayout.TAG, "[LinearGroup.measure]: cell have no 'height' params in linearGroup with 'VERTICAL', the cell will not visible !");
                }
                int w = width - paddingLeft - paddingRight - p.marginLeft - p.marginRight;
                cell.measure(w, p.height);
            }
            measureScaleExpand(i, cell);
        }
        measureContent();
    }

    @Override
    protected void layout(int x, int y, int scrollX, int scrollY) {
        super.layout(x, y, scrollX, scrollY);
        // layout child with scroll offset
        scrollX += getScrollX();
        scrollY += getScrollY();
        int tmp;
        if (HORIZONTAL == orientation) {
            tmp = x + paddingLeft;
        } else {
            tmp = y + paddingTop;
        }
        final int size = getCellCount();
        for (int i = 0; i < size; i++) {
            Cell cell = getCellAt(i);
            CellGroup.Params p = cell.getParams();
            if (HORIZONTAL == orientation) {
                tmp += 0 == i ? 0 : divider;
                tmp += p.marginLeft;
                if (!cell.isLayout()) {
                    cell.layout(tmp, y + paddingTop + p.marginTop, scrollX, scrollY);
                }
                tmp += (cell.width() + p.marginRight);
            } else {
                tmp += 0 == i ? 0 : divider;
                tmp += p.marginTop;
                if (!cell.isLayout()) {
                    cell.layout(x + paddingLeft + p.marginLeft, tmp, scrollX, scrollY);
                }
                tmp += (cell.height() + p.marginBottom);
            }
        }
    }

    public int getOrientation() {
        return orientation;
    }

    private static final float D_SCALE = SCALE_MAX - SCALE_MIN;

    private void measureScaleExpand(final int index, final Cell cell) {
        if (HORIZONTAL == orientation) {
            if (0 == index) {
                leftScaleExpand = (int) (cell.width() * D_SCALE / 2) + BORDER_STOKE_WIDTH * 2;
            } else if (getCellCount() - 1 == index) {
                rightScaleExpand = (int) (cell.width() * D_SCALE / 2) + BORDER_STOKE_WIDTH * 2;
            }
            int tmp = (int) (cell.height() * D_SCALE / 2) + BORDER_STOKE_WIDTH * 2;
            if (tmp > topScaleExpand) {
                topScaleExpand = tmp;
                bottomScaleExpand = tmp;
            }
        } else {
            // TODO: 2018/12/7
        }
    }

    private int leftScaleExpand, topScaleExpand, rightScaleExpand, bottomScaleExpand;

    @Override
    public android.graphics.Rect getClipRectBy(Cell cell, boolean isMoving) {
        int l, t, r, b;
        if (isMoving) {
            l = getLayoutX() + cell.getParentScrollX() - getScrollX();
            t = getLayoutY() + cell.getParentScrollY() - getScrollY();
            r = l + width();
            b = t + height();
        } else {
            l = getLeft();
            t = getTop();
            r = getRight();
            b = getBottom();
        }
        return new android.graphics.Rect(
                l + Math.min(paddingLeft - leftScaleExpand, 0),
                t + Math.min(paddingTop - topScaleExpand, 0),
                r - Math.min(paddingRight - rightScaleExpand, 0),
                b - Math.min(paddingBottom - bottomScaleExpand, 0));
    }

    @Override
    public void scrollTo(int x, int y) {
        super.scrollTo(contentWidth < width() ? 0 : Math.max(width() - contentWidth, Math.min(x, 0)),
                contentHeight < height() ? 0 : Math.max(height() - contentHeight, Math.min(y, 0)));
    }

    @Override
    public int getContentWidth() {
        return contentWidth;
    }

    @Override
    public int getContentHeight() {
        return contentHeight;
    }

    private int contentWidth, contentHeight;

    @Override
    public void measureContent() {
        contentWidth = 0;
        contentHeight = 0;
        final int size = getCellCount();
        for (int i = 0; i < size; i++) {
            Cell cell = getCellAt(i);
            contentWidth += cell.width();
            contentHeight += cell.height();
        }
        if (HORIZONTAL == orientation) {
            contentWidth += paddingLeft + paddingRight + Math.max(0, size - 1) * divider;
            contentHeight = height();
        } else {
            contentWidth = width();
            contentHeight += paddingTop + paddingBottom + Math.max(0, size - 1) * divider;
        }
    }

    public static class Params extends CellGroup.Params {
        Params() {
            this(0, 0);
        }

        public Params(int width, int height) {
            super(width, height);
        }
    }

}