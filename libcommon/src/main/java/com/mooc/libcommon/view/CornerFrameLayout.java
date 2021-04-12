package com.mooc.libcommon.view;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class CornerFrameLayout extends FrameLayout {
    public CornerFrameLayout(@NonNull Context context) {
        this(context, null);
    }

    public CornerFrameLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CornerFrameLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public CornerFrameLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        //自定义属性，指定哪个边的角为圆角以及圆角的大小，还需要在资源文件value的styles.xml中定义一个资源属性，在里面指定所有边的角都是圆角
        //应用到CornerFrameLayout中
        ViewHelper.setViewOutline(this, attrs, defStyleAttr, defStyleRes);
    }

    //提供一个方法可以实现圆角裁切
    public void setViewOutline(int radius, int radiusSide) {
        ViewHelper.setViewOutline(this, radius, radiusSide);
    }
}
