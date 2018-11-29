package org.pinwheel.demo;

import android.app.Activity;
import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.XmlResourceParser;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
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

    private CellLayout cellLayout;
    private SparseArray<Bundle> dataMaps;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        cellLayout = findViewById(R.id.cell_layout);
        initCellLayout();
//        loadSingle();
        loadGroup();
    }

    private void loadSingle() {
        try {
            CellFactory.CellBundle bundle = CellFactory.load(IOUtils.stream2String(getResources().getAssets().open("sample.json")));
            dataMaps = bundle.dataMap;
            cellLayout.setRoot(bundle.root);
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadGroup() {
        dataMaps = new SparseArray<>();
        final LinearGroup root = new LinearGroup(LinearGroup.VERTICAL);
        root.setDivider(20);
        root.setPadding(80, 80, 80, 80);
        final String[] groupNames = new String[]{
                "group_0.json",
                "group_0.json",
                "group_0.json",
                "group_0.json",
                "group_0.json",
                "group_0.json",
                "group_0.json",
                "group_0.json",
                "group_0.json",
                "group_0.json",
                "group_0.json",
                "group_0.json",
                "group_0.json",
                "group_0.json",
                "group_0.json",
                "group_0.json",
                "group_0.json",
                "group_0.json",
                "group_0.json",
                "group_0.json",
                "group_0.json",
                "group_0.json",
                "group_0.json",
                "group_1.json",
                "group_2.json",
                "group_2.json",
                "group_1.json",
                "group_2.json",
                "group_2.json",
                "group_2.json",
                "group_2.json",
                "group_2.json",
                "group_2.json",
                "group_2.json",
                "group_2.json",
                "group_2.json",
                "group_2.json",
                "group_2.json",
                "group_2.json",
                "group_2.json",
                "group_2.json",
        };
        for (String groupName : groupNames) {
            try {
                CellFactory.CellBundle bundle = CellFactory.load(IOUtils.stream2String(getResources().getAssets().open(groupName)));
                CellGroup group = (CellGroup) bundle.root;
                int size = group.getCellCount();
                while (size > 0) {
                    Cell cell = group.getCellAt(0);
                    cell.removeFromParent();
                    root.addCell(cell, cell.getParams());
                    size--;
                }
                size = bundle.dataMap.size();
                for (int i = 0; i < size; i++) {
                    dataMaps.put(bundle.dataMap.keyAt(i), bundle.dataMap.valueAt(i));
                }
            } catch (JSONException | IOException e) {
                e.printStackTrace();
            }
        }
        cellLayout.setRoot(root);
    }

    private final StyleAdapter adapter = new StyleAdapter()
            .addStyle(new StyleAdapter.Style(R.layout.item_style_movie) {
                @Override
                public void onBind(Cell cell, StyleAdapter.Holder holder) {
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
                    TextView text = holder.get(R.id.desc);
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
                    final Bundle args = dataMaps.get(cell.getId());
                    final String title = null != args ? args.getString("title") : "";
                    TextView text = (TextView) holder.view;
                    text.setText(title);
                }
            });

    private void initCellLayout() {
        cellLayout.setAdapter(adapter);
    }

    private static int getColor() {
        return Color.rgb((int) (Math.random() * 255),
                (int) (Math.random() * 255),
                (int) (Math.random() * 255));
    }

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