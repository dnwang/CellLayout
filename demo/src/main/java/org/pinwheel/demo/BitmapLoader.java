package org.pinwheel.demo;

import android.text.TextUtils;
import android.widget.ImageView;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.RequestCreator;

import java.io.File;

public enum BitmapLoader {

    INSTANCE;

    private static final String NATIVE_HEADER = "native://";

    public static int ID_DEFAULT = R.drawable.img_loading;
    public static int ID_ERR = R.drawable.img_loading;

    BitmapLoader() {
    }

    public void display(ImageView img, String url) {
        display(img, url, ID_DEFAULT, ID_ERR);
    }

    public void display(ImageView img, File file) {
        display(img, file, ID_DEFAULT, ID_ERR);
    }

    public void display(ImageView img, File file, int idDef, int idErr) {
        if (null == img) {
            return;
        }
        RequestCreator creator = Picasso.with(img.getContext())
                .load(file)
                .placeholder(idDef)
                .error(idErr);
        final ImageView.ScaleType scaleType = img.getScaleType();
        if (ImageView.ScaleType.CENTER_INSIDE == scaleType) {
            creator.fit().centerInside();
        } else {
            creator.fit().centerCrop();
        }
        creator.into(img);
    }

    public void display(ImageView img, String url, int idDef, int idErr) {
        if (null == img) {
            return;
        }
        if (isNativeDrawable(url)) {
            int resId = getNativeDrawable(url);
            if (resId > 0) {
                img.setImageResource(resId);
            }
        } else {
            if (TextUtils.isEmpty(url)) {
                img.setImageResource(idErr);
                return;
            }
            RequestCreator creator = Picasso.with(img.getContext())
                    .load(url)
                    .placeholder(idDef)
                    .error(idErr);
            final ImageView.ScaleType scaleType = img.getScaleType();
            if (ImageView.ScaleType.CENTER_INSIDE == scaleType) {
                creator.fit().centerInside();
            } else {
                creator.fit().centerCrop();
            }
            creator.into(img);
        }
    }

    public static boolean isNativeDrawable(String url) {
        return null != url && url.startsWith(NATIVE_HEADER);
    }

    public static String getNativeDrawable(int resId) {
        return NATIVE_HEADER + resId;
    }

    public static int getNativeDrawable(String url) {
        return (int) Double.parseDouble(url.substring(NATIVE_HEADER.length()));
    }

}
