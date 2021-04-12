package com.mooc.ppjoke.ui.detail;

import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.mooc.ppjoke.R;
import com.mooc.ppjoke.databinding.ActivityFeedDetailTypeImageBinding;
import com.mooc.ppjoke.databinding.LayoutFeedDetailTypeImageHeaderBinding;
import com.mooc.ppjoke.model.Feed;
import com.mooc.ppjoke.view.PPImageView;

public class ImageViewHandler extends ViewHandler {

    protected ActivityFeedDetailTypeImageBinding mImageBinding;

    protected LayoutFeedDetailTypeImageHeaderBinding mHeaderBinding;

    public ImageViewHandler(FragmentActivity activity) {
        super(activity);
        mImageBinding = DataBindingUtil.setContentView(activity, R.layout.activity_feed_detail_type_image);
        mInateractionBinding = mImageBinding.interactionLayout;
        mRecyclerView = mImageBinding.recyclerView;

        mImageBinding.actionClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mActivity.finish();
            }
        });

    }

    @Override
    public void bindInitData(Feed feed) {
        super.bindInitData(feed);
        mImageBinding.setFeed(mFeed);

        mHeaderBinding = LayoutFeedDetailTypeImageHeaderBinding.inflate(LayoutInflater.from(mActivity), mRecyclerView, false);
        mHeaderBinding.setFeed(mFeed);//给头部绑定数据

        //给头部图片区域绑定数据
        PPImageView headerImage = mHeaderBinding.headerImage;
        headerImage.bindData(mFeed.width, mFeed.height, mFeed.width > mFeed.height ? 0 : 16, mFeed.cover);
        listAdapter.addHeaderView(mHeaderBinding.getRoot());

        //监听recycleView的滚动事件，滚动距离超过一定距离之后，让标题栏的用户信息布局展示出来
        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                //检测headerView划出屏幕的top值是不是已经小于标题栏的高度，如果滑动的距离超过顶部标题栏的高度，此时让标题栏的用户信息区域布局显示出来
                boolean visible = mHeaderBinding.getRoot().getTop() <= -mImageBinding.titleLayout.getMeasuredHeight();
                mImageBinding.authorInfoLayout.getRoot().setVisibility(visible ? View.VISIBLE : View.GONE);
                mImageBinding.title.setVisibility(visible ? View.GONE : View.VISIBLE);

            }
        });
        handleEmpty(false);
    }
}
