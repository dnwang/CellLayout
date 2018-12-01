package org.pinwheel.demo;

import android.app.Activity;
import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.XmlResourceParser;
import android.graphics.Color;
import android.os.Environment;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import org.json.JSONException;
import org.pinwheel.agility2.utils.IOUtils;
import org.pinwheel.view.celllayout.Cell;
import org.pinwheel.view.celllayout.CellFactory;
import org.pinwheel.view.celllayout.CellGroup;
import org.pinwheel.view.celllayout.CellLayout;
import org.pinwheel.view.celllayout.LinearGroup;
import org.pinwheel.view.celllayout.StyleAdapter;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;

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
    private final SparseArray<android.os.Bundle> dataMaps = new SparseArray<>();

    @Override
    protected void onCreate(android.os.Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        cellLayout = findViewById(R.id.cell_layout);
        loadGroup();
        initCellLayout();
    }

    private void loadGroup() {
        final LinearGroup root = new LinearGroup(LinearGroup.HORIZONTAL);
        root.setDivider(20);
        root.setPadding(80, 80, 80, 80);
        final String[] groupNames = new String[]{
                "template_h_1.json",
                "template_h_2.json",
//                "template_2.json",
        };
        for (String groupName : groupNames) {
            try {
                CellFactory.Bundle bundle = CellFactory.load(IOUtils.stream2String(getResources().getAssets().open(groupName)));
                // cell
                root.merge(bundle.root);
                // data
                final int size = bundle.dataMap.size();
                for (int i = 0; i < size; i++) {
                    dataMaps.put(bundle.dataMap.keyAt(i), bundle.dataMap.valueAt(i));
                }
            } catch (JSONException | IOException e) {
                e.printStackTrace();
            }
        }
        cellLayout.setContentCell(root);
    }

    private final StyleAdapter adapter = new StyleAdapter()
            .addStyle(new StyleAdapter.Style(R.layout.item_style_movie) {
                @Override
                public void onBind(Cell cell, StyleAdapter.Holder holder) {
                    final android.os.Bundle args = dataMaps.get(cell.getId());
                    final TextView text = holder.get(R.id.desc);
                    final ImageView image = holder.get(R.id.image);
                    final String title = null != args ? args.getString("title") : "";
                    final String posterUrl = null != args ? args.getString("poster") : null;
                    text.setText(title);
                    BitmapLoader.INSTANCE.display(image, posterUrl);
                }

                @Override
                public void onSelectChanged(Cell cell, StyleAdapter.Holder holder, boolean isSelected) {
                    TextView text = holder.get(R.id.desc);
                    text.setBackgroundColor(isSelected ? Color.WHITE : Color.TRANSPARENT);
                    text.setTextColor(isSelected ? Color.BLACK : Color.WHITE);
                }
            })
            .addStyle(2, new StyleAdapter.Style(R.layout.item_style_poster) {
                @Override
                public void onBind(Cell cell, StyleAdapter.Holder holder) {
                    final android.os.Bundle args = dataMaps.get(cell.getId());
                    final String posterUrl = null != args ? args.getString("poster") : null;
                    BitmapLoader.INSTANCE.display((ImageView) holder.get(R.id.image), posterUrl);
                }

                @Override
                public void onSelectChanged(Cell cell, StyleAdapter.Holder holder, boolean isSelected) {
                    TextView text = holder.get(R.id.desc);
                    text.setVisibility(isSelected ? View.VISIBLE : View.INVISIBLE);
                    text.setText("付费");
                }
            })
            .addStyle(1, new StyleAdapter.Style(R.layout.item_style_title) {
                @Override
                public void onBind(Cell cell, StyleAdapter.Holder holder) {
                    cell.setNoHolder(true); // 滑动的时候始终展示，不用站位图代替
                    final android.os.Bundle args = dataMaps.get(cell.getId());
                    final String title = null != args ? args.getString("title") : "";
                    TextView text = (TextView) holder.view;
                    text.setText(title);
                }
            });

    private void initCellLayout() {
        cellLayout.setAdapter(adapter);
        cellLayout.setOnSelectChangedListener(new CellLayout.OnSelectChangedListener() {
            @Override
            public void onSelectChanged(Cell oldCell, View oldView, Cell newCell, View newView) {
                Log.e(TAG, "[onSelectChanged] oldCell:" + oldCell + ", newCell: " + newCell);
            }
        });
        cellLayout.setOnRootCellScrollListener(new CellGroup.OnScrollAdapter() {
            @Override
            public void onScroll(CellGroup group, int dx, int dy) {
                Log.e(TAG, "[onScroll] group:" + group + ", dx: " + dx + ", dy: " + dy);
            }

            @Override
            public void onScrollComplete(CellGroup group) {
                Log.e(TAG, "[onScrollComplete] group:" + group);
            }

            @Override
            public void onScrollToStart(CellGroup group) {
                Log.e(TAG, "[onScrollToStart] group:" + group);
            }

            @Override
            public void onScrollToEnd(CellGroup group) {
                Log.e(TAG, "[onScrollToEnd] group:" + group);
                try {
                    CellFactory.Bundle bundle = CellFactory.load(IOUtils.stream2String(getResources().getAssets().open("template_h_1.json")));
                    // cell
                    cellLayout.addCell(bundle.root);
                    // data
                    final int size = bundle.dataMap.size();
                    for (int i = 0; i < size; i++) {
                        dataMaps.put(bundle.dataMap.keyAt(i), bundle.dataMap.valueAt(i));
                    }
                } catch (JSONException | IOException e) {
                    e.printStackTrace();
                }
                cellLayout.notifyCellChanged();
            }
        });
    }

    /**
     * load layout.xml from sdcard
     */
    private static View loadLayout(Context ctx, File file) {
        try {
            AssetManager am = ctx.getResources().getAssets();
            final Method m = AssetManager.class.getMethod("addAssetPath", String.class);
            m.setAccessible(true);
            int cookie = (int) m.invoke(am, Environment.getExternalStorageDirectory().getAbsolutePath());
            XmlResourceParser parser = am.openXmlResourceParser(cookie, file.getAbsolutePath());
            return LayoutInflater.from(ctx).inflate(parser, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


}