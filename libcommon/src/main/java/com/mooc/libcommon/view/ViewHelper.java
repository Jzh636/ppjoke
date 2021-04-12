package com.mooc.libcommon.view;

import android.annotation.TargetApi;
import android.content.res.TypedArray;
import android.graphics.Outline;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewOutlineProvider;

import com.mooc.libcommon.R;

import java.util.jar.Attributes;

//ViewHelper自定义属性，裁切圆角
public class ViewHelper {

    public static final int RADIUS_ALL = 0;
    public static final int RADIUS_LEFT = 1;
    public static final int RADIUS_TOP = 2;
    public static final int RADIUS_RIGHT = 3;
    public static final int RADIUS_BOTTOM = 4;

    //解析attributes里面的所有属性
    public static void setViewOutline(View view, AttributeSet attributes, int defStyleAttr, int defStyleRes) {
        TypedArray array = view.getContext().obtainStyledAttributes(attributes, R.styleable.viewOutLineStrategy, defStyleAttr, defStyleRes);
        //TypedArray解析出clip_radius，clip_side两个自定义属性
        int radius = array.getDimensionPixelSize(R.styleable.viewOutLineStrategy_clip_radius, 0);//解析出圆角大小
        int hideSide = array.getInt(R.styleable.viewOutLineStrategy_clip_side, 0);//解析出哪一边需要圆角
        array.recycle();//回收TypedArray，供以后的调用者重新使用
        setViewOutline(view, radius, hideSide);
    }

    public static void setViewOutline(View owner, final int radius, final int radiusSide) {
        //view裁切圆角真正的实现方式是由setOutlineProvider实现的
        owner.setOutlineProvider(new ViewOutlineProvider() {
            @Override
            @TargetApi(21)
            public void getOutline(View view, Outline outline) {
                int w = view.getWidth(), h = view.getHeight();//得到view的宽高
                //宽高==0没有意义
                if (w == 0 || h == 0) {
                    return;
                }

                //判断radiusSide是不是只需要其中的某一边
                if (radiusSide != RADIUS_ALL) {
                    int left = 0, top = 0, right = w, bottom = h;
                    //如果需要裁切的边是左边
                    if (radiusSide == RADIUS_LEFT) {
                        right += radius;
                    } else if (radiusSide == RADIUS_TOP) {
                        bottom += radius;
                    } else if (radiusSide == RADIUS_RIGHT) {
                        left -= radius;
                    } else if (radiusSide == RADIUS_BOTTOM) {
                        top -= radius;
                    }
                    //传递一个圆角矩形，并配合一个圆角大小，就能实现view的圆角裁切了
                    outline.setRoundRect(left, top, right, bottom, radius);
                    return;
                }

                int top = 0, bottom = h, left = 0, right = w;
                //radius <= 0，不能有圆角
                if (radius <= 0) {
                    outline.setRect(left, top, right, bottom);
                } else {
                    outline.setRoundRect(left, top, right, bottom, radius);
                }
            }
        });
        owner.setClipToOutline(radius > 0);
        owner.invalidate();
    }
}
