package org.pinwheel.view.celllayout;

import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;

/**
 * Copyright (C), 2018 <br>
 * <br>
 * All rights reserved <br>
 * <br>
 *
 * @author dnwang
 * @version 2018/11/28,13:49
 */
public final class StyleAdapter implements CellLayout.ViewAdapter, CellLayout.OnCellSelectedChangeListener {

    private static final int DEF_STYLE_ID = 0;

    private SparseArray<Style> viewStyles = new SparseArray<>();
    LayoutInflater inflater;

    public StyleAdapter addStyle(Style style) {
        addStyle(DEF_STYLE_ID, style);
        return this;
    }

    public StyleAdapter addStyle(int styleId, Style style) {
        if (viewStyles.indexOfKey(styleId) > 0) {
            throw new IllegalStateException("the styleId: " + styleId + " has already exist !");
        }
        viewStyles.put(styleId, style);
        return this;
    }

    @Override
    public final int getViewType(Cell cell) {
        final CellGroup.Params p = cell.getParams();
        if (null == p) {
            throw new NullPointerException("can't found cell's params !");
        }
        return p.styleId;
    }

    @Override
    public final View onCreateView(Cell cell) {
        final Style style = viewStyles.get(getViewType(cell));
        if (null == style) {
            throw new UnknownError("unknown view style !");
        }
        final View view = inflater.inflate(style.layoutId, null, false);
        view.setTag(new Holder(view));
        return view;
    }

    @Override
    public final void onBindView(Cell cell, View view) {
        final Style style = viewStyles.get(getViewType(cell));
        if (null != style) {
            style.onBind(cell, getHolder(view));
        }
    }

    @Override
    public final void onViewRecycled(Cell cell, View view) {
        final Style style = viewStyles.get(getViewType(cell));
        if (null != style) {
            style.onRecycled(cell, getHolder(view));
        }
    }

    private Holder getHolder(View view) {
        final Object tag = view.getTag();
        if (!(tag instanceof Holder)) {
            throw new IllegalStateException("Can't found holder from view tag !");
        }
        return (Holder) tag;
    }

    @Override
    public final void onSelectedChanged(Cell oldCell, View oldView, Cell newCell, View newView) {
        if (null != oldCell && null != oldView) {
            final Style oldStyle = viewStyles.get(getViewType(oldCell));
            if (null != oldStyle) {
                oldStyle.onSelectChanged(oldCell, getHolder(oldView), false);
            }
        }
        if (null != newCell && null != newView) {
            final Style newStyle = viewStyles.get(getViewType(newCell));
            if (null != newStyle) {
                newStyle.onSelectChanged(newCell, getHolder(newView), true);
            }
        }
    }

    public static abstract class Style {
        private final int layoutId;

        public Style(int layoutId) {
            this.layoutId = layoutId;
        }

        public abstract void onBind(Cell cell, Holder holder);

        public void onSelectChanged(Cell cell, Holder holder, boolean isSelected) {
        }

        public void onRecycled(Cell cell, Holder holder) {
        }
    }

    public static final class Holder {
        private SparseArray<View> references = null;
        public final View view;

        Holder(View view) {
            this.view = view;
        }

        public <T extends View> T get(int id) {
            if (null == references) {
                references = new SparseArray<>();
            }
            View v = references.get(id);
            if (null == v) {
                v = view.findViewById(id);
                if (null != v) {
                    references.put(id, v);
                }
            }
            return (T) v;
        }
    }

}

