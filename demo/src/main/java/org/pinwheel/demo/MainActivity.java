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
import org.pinwheel.view.celllayout.CellLayout;
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
            public void onClick(Cell cell) {
                Toast.makeText(MainActivity.this, "click: " + cell.getId(), Toast.LENGTH_SHORT).show();
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
                }

                @Override
                public void onSelectChanged(Cell cell, StyleAdapter.Holder holder, boolean isSelected) {
                    final TextView text = holder.get(R.id.desc);
                    text.setBackgroundColor(isSelected ? Color.WHITE : Color.TRANSPARENT);
                    text.setTextColor(isSelected ? Color.BLACK : Color.WHITE);
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