package com.mooc.ppjoke.ui.share;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.mooc.ppjoke.view.PPImageView;
import com.mooc.libcommon.utils.PixUtils;
import com.mooc.libcommon.view.CornerFrameLayout;
import com.mooc.libcommon.view.ViewHelper;
import com.mooc.ppjoke.R;

import java.util.ArrayList;
import java.util.List;

public class ShareDialog extends AlertDialog {

    List<ResolveInfo> shareitems = new ArrayList<>();
    private ShareAdapter shareAdapter;
    private String shareContent;
    private View.OnClickListener mListener;
    private CornerFrameLayout layout;

    public ShareDialog(@NonNull Context context) {
        super(context);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);

        //CornerFrameLayout应用到ShareDialog
        layout = new CornerFrameLayout(getContext());
        layout.setBackgroundColor(Color.WHITE);//白色背景
        layout.setViewOutline(PixUtils.dp2px(20), ViewHelper.RADIUS_TOP);//给上边切圆角

        RecyclerView gridView = new RecyclerView(getContext());//网格
        gridView.setLayoutManager(new GridLayoutManager(getContext(), 4));//4列

        shareAdapter = new ShareAdapter();
        gridView.setAdapter(shareAdapter);
        //设置分享面板上下左右的偏距
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        int margin = PixUtils.dp2px(20);

        params.leftMargin = params.topMargin = params.rightMargin = params.bottomMargin = margin;
        params.gravity = Gravity.CENTER;
        layout.addView(gridView, params);

        setContentView(layout);
        getWindow().setGravity(Gravity.BOTTOM);//从底部显示出来

        getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));//设置透明背景，否则dialog会有一个上下左右的间距
        getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);//dialog宽高

        queryShareItems();//查询分享面板的分享入口

    }

    //设置分享内容
    public void setShareContent(String shareContent) {
        this.shareContent = shareContent;
    }

    //点击事件的回调
    public void setShareItemClickListener(View.OnClickListener listener) {

        mListener = listener;
    }

    //查询分享面板的分享入口
    private void queryShareItems() {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_SEND);
        intent.setType("text/plain");//分享的内容为文本类型

        //查询到文本类型的所有入口
        List<ResolveInfo> resolveInfos = getContext().getPackageManager().queryIntentActivities(intent, 0);
        for (ResolveInfo resolveInfo : resolveInfos) {
            String packageName = resolveInfo.activityInfo.packageName;
            //过滤分享入口，只保留微信和QQ的
            if (TextUtils.equals(packageName, "com.tencent.mm") || TextUtils.equals(packageName, "com.tencent.mobileqq")) {
                shareitems.add(resolveInfo);
            }
        }
        shareAdapter.notifyDataSetChanged();//刷新界面
    }

    private class ShareAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private final PackageManager packageManager;

        public ShareAdapter() {
            packageManager = getContext().getPackageManager();
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            //应用分享渠道的布局
            View inflate = LayoutInflater.from(getContext()).inflate(R.layout.layout_share_item, parent, false);
            return new RecyclerView.ViewHolder(inflate) {
            };
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            ResolveInfo resolveInfo = shareitems.get(position);
            PPImageView imageView = holder.itemView.findViewById(R.id.share_icon);
            Drawable drawable = resolveInfo.loadIcon(packageManager);//返回分享渠道的图标
            imageView.setImageDrawable(drawable);//设置分享渠道图标

            TextView shareText = holder.itemView.findViewById(R.id.share_text);
            shareText.setText(resolveInfo.loadLabel(packageManager));//设置分享渠道文本

            //点击事件，点击唤醒客户端去分享
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String pkg = resolveInfo.activityInfo.packageName;
                    String cls = resolveInfo.activityInfo.name;
                    Intent intent = new Intent();
                    intent.setAction(Intent.ACTION_SEND);//给Intent指定action
                    intent.setType("text/plain");//分享类型
                    intent.setComponent(new ComponentName(pkg, cls));//设置唤醒的是哪个客户端
                    intent.putExtra(Intent.EXTRA_TEXT, shareContent);

                    getContext().startActivity(intent);

                    //点击的时候判断
                    if (mListener != null) {
                        mListener.onClick(v);//不为空，回调出去
                    }

                    dismiss();
                }
            });
        }

        @Override
        public int getItemCount() {
            return shareitems == null ? 0 : shareitems.size();
        }
    }
}
