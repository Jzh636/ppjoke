package com.mooc.libcommon.view;

import android.content.Context;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mooc.libcommon.R;

//空布局
public class EmptyView extends LinearLayout {
    private ImageView icon;
    private TextView title;
    private Button action;

    public EmptyView(@NonNull Context context) {
        this(context, null);
    }

    public EmptyView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public EmptyView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public EmptyView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int style) {
        super(context, attrs, defStyleAttr, style);
        setOrientation(VERTICAL);
        setGravity(Gravity.CENTER);
        //初始化方法中加载layout_empty_view空布局
        LayoutInflater.from(context).inflate(R.layout.layout_empty_view, this, true);

        icon = findViewById(R.id.empty_icon);
        title = findViewById(R.id.empty_text);
        action = findViewById(R.id.empty_action);
    }

    //初始化之后暴露几个方法让调用方调用
    //更改空布局的icon目前只支持引用本地文件
    public void setEmptyIcon(@DrawableRes int iconRes) {
        icon.setImageResource(iconRes);
    }

    public void setTitle(String text) {
        if (TextUtils.isEmpty(text)) {
            title.setVisibility(GONE);
        } else {
            title.setText(text);
            title.setVisibility(VISIBLE);
        }

    }

    public void setButton(String text, View.OnClickListener listener) {
        if (TextUtils.isEmpty(text)) {
            action.setVisibility(GONE);
        } else {
            action.setText(text);
            action.setVisibility(VISIBLE);
            action.setOnClickListener(listener);//按钮显示之后要有点击事件
        }
    }

}
