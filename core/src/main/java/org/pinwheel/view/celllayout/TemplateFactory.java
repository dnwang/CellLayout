package org.pinwheel.view.celllayout;

import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Field;
import java.util.Iterator;

/**
 * Copyright (C), 2018 <br>
 * <br>
 * All rights reserved <br>
 * <br>
 *
 * @author dnwang
 * @version 2018/11/15,18:51
 */
public final class TemplateFactory {
    private static final String TAG = "CellFactory";

    private static final String ATTR_VERSION = "version";
    private static final String ATTR_RESOLUTION = "targetResolution";
    private static final String ATTR_ROOT = "root";
    private static final String ATTR_TYPE = "type";
    private static final String ATTR_DATA = "data";
    private static final String ATTR_SUB_CELLS = "cells";
    private static final String ATTR_GROUP_GRID = "grid";
    private static final String ATTR_GROUP_LINEAR = "linear";

    public static Template load(String jsonString) throws JSONException {
        return load(new JSONObject(jsonString));
    }

    public static Template load(JSONObject json) throws JSONException {
        final int version = json.optInt(ATTR_VERSION, 1);
        final int resolution = json.optInt(ATTR_RESOLUTION, 1080);
        if (version <= 1) {
            IParser parser = new DefaultParser(resolution);
            parser.parse(json);
            return new Template(version, resolution, parser.getRoot(), parser.getData());
        } else {
            throw new JSONException("can't found this special version parser ! version:" + version);
        }
    }

    private static final class DefaultParser implements IParser {
        private SparseArray<Bundle> dataMap;
        private Cell root;
        private final int resolution;

        DefaultParser(int resolution) {
            this.resolution = resolution;
        }

        @Override
        public void parse(JSONObject json) throws JSONException {
            root = _parse(json.getJSONObject(ATTR_ROOT), null);
        }

        @Override
        public Cell getRoot() {
            return root;
        }

        @Override
        public SparseArray<Bundle> getData() {
            return dataMap;
        }

        private Cell _parse(JSONObject args, CellGroup parent) throws JSONException {
            // type
            final String type = args.optString(ATTR_TYPE);
            final Cell cell;
            if (ATTR_GROUP_GRID.equalsIgnoreCase(type)) {
                cell = new GridGroup();
            } else if (ATTR_GROUP_LINEAR.equalsIgnoreCase(type)) {
                cell = new LinearGroup();
            } else {
                cell = new Cell();
            }
            bindingArgs(cell, args);
            // data
            saveCellData(cell.getId(), args.optJSONObject(ATTR_DATA));
            // cells
            if (cell instanceof CellGroup) {
                final JSONArray subArgsList = args.optJSONArray(ATTR_SUB_CELLS);
                final int size = null != subArgsList ? subArgsList.length() : 0;
                for (int i = 0; i < size; i++) {
                    _parse(subArgsList.getJSONObject(i), (CellGroup) cell);
                }
            }
            final CellGroup.Params p = null != parent ? parent.getDefaultParams() : new CellGroup.Params();
            bindingArgs(p, args);
            if (null == parent) {
                cell.setParams(p);
            } else {
                parent.addCell(cell, p);
            }
            return cell;
        }

        private void saveCellData(int cellId, JSONObject json) {
            final Bundle data = (null != json && json.length() > 0) ? new Bundle() : null;
            if (null != data) {
                Iterator<String> iterable = json.keys();
                while (iterable.hasNext()) {
                    String key = iterable.next();
                    Object obj = json.opt(key);
                    if (obj instanceof Integer) {
                        data.putInt(key, (int) obj);
                    } else if (obj instanceof Boolean) {
                        data.putBoolean(key, (boolean) obj);
                    } else if (obj instanceof String) {
                        data.putString(key, (String) obj);
                    } else if (obj instanceof Double) {
                        data.putDouble(key, (double) obj);
                    }
                }
                if (null == dataMap) {
                    dataMap = new SparseArray<>();
                }
                dataMap.put(cellId, data);
            }
        }

        private void bindingArgs(final Object obj, final JSONObject json) {
            if (null == json || 0 == json.length()) {
                return;
            }
            final float screenHeight = Resources.getSystem().getDisplayMetrics().heightPixels;
            final float scale = screenHeight / resolution;
            final int padding = json.optInt("padding", 0);
            final int margin = json.optInt("margin", 0);
            foreachAllField(obj.getClass(), new Filter<Field>() {
                @Override
                public boolean call(Field field) {
                    field.setAccessible(true);
                    if (field.isAnnotationPresent(Attribute.class)) {
                        final Attribute attrInfo = field.getAnnotation(Attribute.class);
                        final String key = "".equals(attrInfo.value()) ? field.getName() : attrInfo.value();
                        final Class type = field.getType();
                        try {
                            if (type == int.class) {
                                int value = 0;
                                if (key.startsWith("padding")) {
                                    value = json.optInt(key, padding);
                                } else if (key.startsWith("margin")) {
                                    value = json.optInt(key, margin);
                                } else if (json.has(key)) {
                                    value = json.optInt(key, 0);
                                }
                                if (attrInfo.fixedResolution()) {
                                    value = (int) (value * scale);
                                }
                                field.set(obj, value);
                            } else if (type == String.class && json.has(key)) {
                                field.set(obj, json.optString(key, null));
                            } else if (type == boolean.class && json.has(key)) {
                                field.set(obj, json.optBoolean(key, false));
                            } else if (type == short.class && json.has(key)) {
                                field.set(obj, (short) json.optDouble(key, 0d));
                            } else if (type == double.class && json.has(key)) {
                                field.set(obj, json.optDouble(key, 0d));
                            }
                        } catch (IllegalAccessException e) {
                            Log.e(TAG, "can't set field [" + key + "]! " + e.getMessage());
                        }
                    }
                    return false;
                }
            });
        }
    }

    private static void foreachAllField(Class cls, Filter<Field> filter) {
        do {
            Field fields[] = cls.getDeclaredFields();
            if (fields.length > 0) {
                for (Field f : fields) {
                    if (filter.call(f)) {
                        break;
                    }
                }
            }
        } while ((cls = cls.getSuperclass()) != null);
    }

    private interface IParser {
        void parse(JSONObject json) throws JSONException;

        SparseArray<Bundle> getData();

        Cell getRoot();
    }

    public static final class Template {
        public final int version;
        public final int targetResolution;
        public final Cell root;
        public final SparseArray<Bundle> data;

        Template(int version, int targetResolution, Cell root, SparseArray<Bundle> data) {
            this.version = version;
            this.targetResolution = targetResolution;
            this.root = root;
            this.data = data;
        }
    }

}