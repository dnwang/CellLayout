package org.pinwheel.view.celllayout;

import android.os.Bundle;
import android.util.Log;
import android.util.LongSparseArray;

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
public final class CellFactory {
    private static final String TAG = "CellFactory";

    private static final String ATTR_VERSION = "version";
    private static final String ATTR_ROOT = "root";
    private static final String ATTR_TYPE = "type";
    private static final String ATTR_DATA = "data";
    private static final String ATTR_SUB_CELLS = "cells";
    private static final String ATTR_GROUP_GRID = "grid";
    private static final String ATTR_GROUP_LINEAR = "linear";

    public static CellBundle load(String jsonString) throws JSONException {
        return load(new JSONObject(jsonString));
    }

    public static CellBundle load(JSONObject json) throws JSONException {
        final int version = json.getInt(ATTR_VERSION);
        if (version <= 1) {
            IParser parser = new DefaultParser();
            parser.parse(json);
            return new CellBundle(parser.getRoot(), parser.getDataMap());
        } else {
            throw new JSONException("can't found this special version parser ! version:" + version);
        }
    }

    private static final class DefaultParser implements IParser {
        private LongSparseArray<Bundle> dataMap;
        private Cell root;

        @Override
        public void parse(JSONObject json) throws JSONException {
            root = parse(json.getJSONObject(ATTR_ROOT), null);
        }

        @Override
        public Cell getRoot() {
            return root;
        }

        @Override
        public LongSparseArray<Bundle> getDataMap() {
            return dataMap;
        }

        private Cell parse(JSONObject args, CellGroup parent) throws JSONException {
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
                    parse(subArgsList.getJSONObject(i), (CellGroup) cell);
                }
            }
            if (null != parent) {
                final CellGroup.Params p = parent.getDefaultParams();
                bindingArgs(p, args);
                parent.addCell(cell, p);
            }
            return cell;
        }

        private void saveCellData(long cellId, JSONObject json) {
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
                    dataMap = new LongSparseArray<>();
                }
                dataMap.put(cellId, data);
            }
        }

        private void bindingArgs(final Object obj, final JSONObject json) {
            if (null == json || 0 == json.length()) {
                return;
            }
            final int padding = json.optInt("padding", 0);
            final int margin = json.optInt("margin", 0);
            foreachAllField(obj.getClass(), new Filter<Field>() {
                @Override
                public boolean call(Field field) {
                    field.setAccessible(true);
                    if (field.isAnnotationPresent(Attribute.class)) {
                        final Class type = field.getType();
                        String key = field.getAnnotation(Attribute.class).value();
                        if ("".equals(key)) {
                            key = field.getName();
                        }
                        try {
                            if (type == int.class) {
                                int def = 0;
                                if (key.startsWith("padding")) {
                                    def = padding;
                                } else if (key.startsWith("margin")) {
                                    def = margin;
                                }
                                field.set(obj, json.optInt(key, def));
                            } else if (type == String.class) {
                                field.set(obj, json.optString(key, null));
                            } else if (type == boolean.class) {
                                field.set(obj, json.optBoolean(key, false));
                            } else if (type == short.class) {
                                field.set(obj, (short) json.optDouble(key, 0d));
                            } else if (type == double.class) {
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
            if (null != fields && fields.length > 0) {
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

        LongSparseArray<Bundle> getDataMap();

        Cell getRoot();
    }

    public static final class CellBundle {
        public final Cell root;
        public final LongSparseArray<Bundle> dataMap;

        CellBundle(Cell root, LongSparseArray<Bundle> dataMap) {
            this.root = root;
            this.dataMap = dataMap;
        }
    }

}