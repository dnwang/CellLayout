package org.pinwheel.demo;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.pinwheel.agility2.utils.IOUtils;
import org.pinwheel.view.celllayout.Cell;
import org.pinwheel.view.celllayout.CellGroup;
import org.pinwheel.view.celllayout.CellLayout;
import org.pinwheel.view.celllayout.LinearGroup;
import org.pinwheel.view.celllayout.StyleAdapter;
import org.pinwheel.view.celllayout.TemplateFactory;

import java.io.IOException;

/**
 * Copyright (C), 2018 <br>
 * <br>
 * All rights reserved <br>
 * <br>
 *
 * @author dnwang
 * @version 2018/11/15,11:18
 */
public final class MainActivity extends Activity {
    private static final String TAG = "MainActivity";

    private CellLayout cellLayout;
    private SparseArray<Bundle> dataMaps = new SparseArray<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        init();
    }

    private void init() {
        cellLayout = findViewById(R.id.cell_layout);
        cellLayout.setAdapter(adapter);
        cellLayout.setHolderDrawable(new CellLayout.DefHolderDrawable(getResources().getDrawable(R.drawable.img_holder)));
        cellLayout.setOnCellClickListener(new CellLayout.OnCellClickListener() {
            @Override
            public boolean onClick(Cell cell) {
                Toast.makeText(MainActivity.this, "click: " + cell.getId(), Toast.LENGTH_SHORT).show();
                return false; // 返回true，将不派发到子视图监听
            }
        });
        cellLayout.setOnRootCellScrollListener(new CellGroup.OnScrollAdapter() {
            @Override
            public void onScroll(CellGroup group, int dx, int dy) {
                // 移动中
            }

            @Override
            public void onScrollComplete(CellGroup group) {
                // 移动结束
            }

            @Override
            public void onScrollToStart(CellGroup group) {
                // 移动到顶部
            }

            @Override
            public void onScrollToEnd(CellGroup group) {
                final Cell root = cellLayout.getContentCell();
                if (root instanceof LinearGroup && ((LinearGroup) root).getOrientation() == LinearGroup.VERTICAL) {
                    try {
                        final TemplateFactory.Template template = TemplateFactory.load(IOUtils.stream2String(getResources().getAssets().open("sample.json")));
                        mergeData(template.data); // 数据可有可无
                        cellLayout.addCell(template.root); // 追加一组模板到最后
                        cellLayout.requestLayout(); // 应用更改
                    } catch (JSONException | IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        initNavigation();
    }

    private void initNavigation() {
        final ViewGroup groups = findViewById(R.id.navigation);
        final int size = groups.getChildCount();
        View def = null;
        for (int i = 0; i < size; i++) {
            if (0 == i) def = groups.getChildAt(i);
            final int index = i;
            groups.getChildAt(i).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    resetCellLayout(TEMPLATE[Math.min(index, TEMPLATE.length - 1)]);
                }
            });
        }
        if (null != def) {
            def.performClick();
        }
    }

    private void resetCellLayout(String json) {
        try {
            final TemplateFactory.Template template = TemplateFactory.load(IOUtils.stream2String(getResources().getAssets().open(json)));
            dataMaps = template.data;
            cellLayout.setContentCell(template.root);
            cellLayout.requestLayout(); // apply
        } catch (JSONException | IOException e) {
            e.printStackTrace();
        }
    }

    private void mergeData(SparseArray<Bundle> data) {
        final int size = data.size();
        for (int i = 0; i < size; i++) {
            dataMaps.put(data.keyAt(i), data.valueAt(i));
        }
    }

    private static final String[] TEMPLATE = new String[]{
            "sample.json",
            "sample_h.json",
            "sample.json",
            "sample_h.json",
    };

    private final StyleAdapter adapter = new StyleAdapter()
            .addStyle(new StyleAdapter.Style(R.layout.item_style_movie) {
                @Override
                public void onBind(final Cell cell, StyleAdapter.Holder holder) {
                    final Bundle args = dataMaps.get(cell.getId());
                    final TextView text = holder.get(R.id.desc);
                    final ImageView image = holder.get(R.id.image);
                    final String title = null != args ? args.getString("title") : "";
                    final String posterUrl = null != args ? args.getString("poster") : null;
                    text.setText(title);
                    BitmapLoader.INSTANCE.display(image, posterUrl);
                    holder.view.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Toast.makeText(MainActivity.this, "OnClickListener", Toast.LENGTH_SHORT).show();
                        }
                    });
                    // 如果绑定了一个已获取焦点的Cell，要直接展示它的焦点状态
                    if (cell.hasFocus()) {
                        onSelectChanged(cell, holder, true);
                    }
                }

                @Override
                public void onSelectChanged(Cell cell, StyleAdapter.Holder holder, boolean isSelected) {
                    // 获得焦点
                    final TextView text = holder.get(R.id.desc);
                    text.setBackgroundColor(isSelected ? Color.WHITE : Color.TRANSPARENT);
                    text.setTextColor(isSelected ? Color.BLACK : Color.WHITE);
                }

                @Override
                public void onRecycled(Cell cell, StyleAdapter.Holder holder) {
                    // 视图被移除可见区域应该清除状态，避免复用时存在历史状态
                    onSelectChanged(cell, holder, false);
                    ((ImageView) holder.get(R.id.image)).setImageResource(0);
                }
            })
            .addStyle(2, new StyleAdapter.Style(R.layout.item_style_poster) {
                @Override
                public void onBind(Cell cell, StyleAdapter.Holder holder) {
                    final Bundle args = dataMaps.get(cell.getId());
                    final String posterUrl = null != args ? args.getString("poster") : null;
                    BitmapLoader.INSTANCE.display((ImageView) holder.get(R.id.image), posterUrl);
                }

                @Override
                public void onSelectChanged(final Cell cell, StyleAdapter.Holder holder, boolean isSelected) {
                    final TextView text = holder.get(R.id.desc);
                    text.setVisibility(isSelected ? View.VISIBLE : View.INVISIBLE);
                    text.setText("付费");
                }
            })
            .addStyle(1, new StyleAdapter.Style(R.layout.item_style_title) {
                @Override
                public void onBind(Cell cell, StyleAdapter.Holder holder) {
                    cell.setNoHolder(true); // 滑动的时候始终展示，不用站位图代替
                    final Bundle args = dataMaps.get(cell.getId());
                    final String title = null != args ? args.getString("title") : "";
                    final TextView text = (TextView) holder.view;
                    text.setText(title);
                }
            });
}