package org.pinwheel.demo;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.util.LongSparseArray;
import android.view.LayoutInflater;
import android.view.View;

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
        cellLayout.setRoot(root);
    }

    private void initCellLayout() {
        cellLayout.setAdapter(new CellLayout.ViewAdapter() {
            @Override
            public int getViewType(Cell cell) {
                final Bundle data = cellDataMap.get(cell.getId());
                return (null == data) ? 0 : data.getInt("style", 0);
            }

            @Override
            public View onCreateView(Cell cell) {
                final int id;
                if (getViewType(cell) > 0) {
                    id = R.layout.item_style_0;
                } else {
                    id = R.layout.item_style_1;
                }
                final View view = LayoutInflater.from(MainActivity.this).inflate(id, cellLayout, false);
                view.setTag(new ViewHolder(view));
                return view;
            }

            @Override
            public void onBindView(final Cell cell, View view) {
                final long cellId = cell.getId();
                final Bundle data = cellDataMap.get(cellId);
                final String title = null == data ? String.valueOf(cellId) : data.getString("title");
                final ViewHolder holder = (ViewHolder) view.getTag();
                if (getViewType(cell) > 0) {
                    holder.getTextView(R.id.text1).setText(title);
                    holder.getTextView(R.id.text2).setText(String.valueOf(cellId));
                    holder.getImageView(R.id.image).setImageResource(R.mipmap.ic_launcher);
                } else {
                    holder.getTextView(R.id.text1).setText(title);
                    holder.getImageView(R.id.image).setImageResource(RES_IMG[(int) (Math.random() * RES_IMG.length)]);
                }
                view.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        cellLayout.keepCellCenter(cellLayout.findCellByView(v), true);
                    }
                });
                view.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        cellLayout.keepCellCenter(cellLayout.findCellByView(v), false);
                        return true;
                    }
                });
            }

            @Override
            public void onViewRecycled(Cell cell, View view) {
                final ViewHolder holder = (ViewHolder) view.getTag();
                holder.getImageView(R.id.image).setImageResource(0);
            }
        });
    }

    final int[] RES_IMG = new int[]{
            R.mipmap.poster_1,
            R.mipmap.poster_2,
            R.mipmap.poster_3,
            R.mipmap.poster_4
    };

    private static int getColor() {
        return Color.rgb((int) (Math.random() * 255),
                (int) (Math.random() * 255),
                (int) (Math.random() * 255));
    }

}