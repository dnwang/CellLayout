package org.pinwheel.demo;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.util.LongSparseArray;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.json.JSONException;
import org.pinwheel.agility2.utils.IOUtils;
import org.pinwheel.agility2.view.ViewHolder;
import org.pinwheel.view.celllayout.Cell;
import org.pinwheel.view.celllayout.CellFactory;
import org.pinwheel.view.celllayout.CellGroup;
import org.pinwheel.view.celllayout.CellLayout;

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

    private CellLayout cellLayout;
    private LongSparseArray<Bundle> cellDataMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        cellLayout = findViewById(R.id.cell_layout);
        initCellLayout();
        loadLayout();
    }

    private void loadLayout() {
        Cell root = null;
        try {
            for (int i = 0; i < 3; i++) {
                CellFactory.CellBundle bundle = CellFactory.load(IOUtils.stream2String(getResources().getAssets().open("layout.json")));
                if (null == root) {
                    root = bundle.root;
                } else {
                    CellGroup group = (CellGroup) bundle.root;
                    int size = group.getCellCount();
                    while (size > 0) {
                        Cell cell = group.getCellAt(0);
                        cell.removeFromParent();
                        ((CellGroup) root).addCell(cell, cell.getParams());
                        size--;
                    }
                }
                if (null == cellDataMap) {
                    cellDataMap = bundle.dataMap;
                } else {
                    int size = bundle.dataMap.size();
                    for (int j = 0; j < size; j++) {
                        cellDataMap.put(bundle.dataMap.keyAt(j), bundle.dataMap.valueAt(j));
                    }
                }
            }
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
        cellLayout.setRootCell(root);
    }

    private void initCellLayout() {
        cellLayout.setAdapter(new CellLayout.ViewAdapter() {
            @Override
            public View getHolderView() {
                return new View(cellLayout.getContext());
            }

            @Override
            public int getViewPoolId(Cell cell) {
                final Bundle data = cellDataMap.get(cell.getId());
                return (null == data) ? 0 : data.getInt("layoutId", 0);
            }

            @Override
            public View onCreateView(Cell cell) {
                final View view;
                if (getViewPoolId(cell) > 0) {
                    view = new Button(MainActivity.this);
                } else {
                    view = LayoutInflater.from(MainActivity.this).inflate(R.layout.item_style_0, cellLayout, false);
                }
                view.setTag(new ViewHolder(view));
                return view;
            }

            @Override
            public void onBindView(final Cell cell, View view) {
                final long cellId = cell.getId();
                final Bundle data = cellDataMap.get(cellId);
                final String title = null == data ? String.valueOf(cellId) : data.getString("title");
                final ViewHolder holder = (ViewHolder) view.getTag();
                if (getViewPoolId(cell) > 0) {
                    TextView text = (TextView) holder.getContentView();
                    text.setGravity(Gravity.CENTER);
                    text.setTextColor(getColor());
                    text.setText(title);
                } else {
                    holder.getTextView(R.id.text1).setText(title);
                    holder.getTextView(R.id.text2).setText(String.valueOf(cellId));
                    holder.getImageView(R.id.image).setImageResource(R.mipmap.jj);
                }
                view.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        cellLayout.scrollToCenter(v, true);
                    }
                });
            }

            @Override
            public void onViewRecycled(Cell cell, View view) {
                final ViewHolder holder = (ViewHolder) view.getTag();
                if (getViewPoolId(cell) > 0) {
                    // nothing
                } else {
                    holder.getImageView(R.id.image).setImageResource(0);
                }
            }
        });
    }

    private static int getColor() {
        return Color.rgb(
                (int) (Math.random() * 255),
                (int) (Math.random() * 255),
                (int) (Math.random() * 255)
        );
    }

}