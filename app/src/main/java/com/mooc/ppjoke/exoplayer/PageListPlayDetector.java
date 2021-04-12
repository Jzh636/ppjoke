package com.mooc.ppjoke.exoplayer;

import android.util.Pair;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleEventObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

/**
 * 列表视频自动播放 检测逻辑
 */
public class PageListPlayDetector {

    //收集一个个的能够进行视频播放的 对象，面向接口
    private List<IPlayTarget> mTargets = new ArrayList<>();
    private RecyclerView mRecyclerView;
    //正在播放的那个
    private IPlayTarget playingTarget;

    public void addTarget(IPlayTarget target) {
        mTargets.add(target);
    }

    public void removeTarget(IPlayTarget target) {
        mTargets.remove(target);
    }

    //LifecycleOwner监听数组生命周期
    public PageListPlayDetector(LifecycleOwner owner, RecyclerView recyclerView) {
        mRecyclerView = recyclerView;

        owner.getLifecycle().addObserver(new LifecycleEventObserver() {
            //数组声明周期的每一个状态的变更都会回调到这个方法，Lifecycle.Event代表数组生命周期的状态
            @Override
            public void onStateChanged(@NonNull LifecycleOwner source, @NonNull Lifecycle.Event event) {
                //数组生命周期的状态为ON_DESTROY，进行反注册和清理的工作
                if (event == Lifecycle.Event.ON_DESTROY) {
                    playingTarget = null;
                    mTargets.clear();
                    mRecyclerView.removeCallbacks(delayAutoPlay);
                    recyclerView.removeOnScrollListener(scrollListener);
                    owner.getLifecycle().removeObserver(this);
                }
            }
        });
        recyclerView.getAdapter().registerAdapterDataObserver(mDataObserver);
        recyclerView.addOnScrollListener(scrollListener);

    }

    //监听列表的滚动
    RecyclerView.OnScrollListener scrollListener = new RecyclerView.OnScrollListener() {
        @Override
        public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
            if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                //列表滚动停止之后也要触发自动播放
                autoPlay();
            }
        }

        //当列表不断滚动的时候，需要判断正在播放的target是否还满足条件，如果不满足条件了就应该调用inActive()去关闭它的视频播放
        @Override
        public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
            super.onScrolled(recyclerView, dx, dy);
            if (dx == 0 && dy == 0) {
                //时序问题。当执行了AdapterDataObserver#onItemRangeInserted  可能还没有被布局到RecyclerView上。
                //所以此时 recyclerView.getChildCount()还是等于0的。
                //等childView 被布局到RecyclerView上之后，会执行onScrolled（）方法
                //并且此时 dx,dy都等于0
                postAutoPlay();
            } else {
                //如果有正在播放的,且滑动时被划出了屏幕 则 停止他
                if (playingTarget != null && playingTarget.isPlaying() && !isTargetInBounds(playingTarget)) {
                    playingTarget.inActive();
                }
            }
        }
    };

    private void postAutoPlay() {
        mRecyclerView.post(delayAutoPlay);
    }

    Runnable delayAutoPlay = new Runnable() {
        @Override
        public void run() {
            autoPlay();
        }
    };

    private final RecyclerView.AdapterDataObserver mDataObserver = new RecyclerView.AdapterDataObserver() {
        //有数据被添加到RecycleView之后就会回调这个方法
        @Override
        public void onItemRangeInserted(int positionStart, int itemCount) {
            postAutoPlay();//逻辑检测
        }
    };

    //自动播放
    private void autoPlay() {
        //判断屏幕上是否有视频类型的Item
        if (mTargets.size() <= 0 || mRecyclerView.getChildCount() <= 0) {
            return;
        }

        //判断遍历有没有新的target满足自动播放逻辑之前，判断上一个正在播放的target是否还在屏幕内，是否还正在播放，如果满足这两个条件就没必要检测一个新的target了
        if (playingTarget != null && playingTarget.isPlaying() && isTargetInBounds(playingTarget)) {
            return;
        }

        IPlayTarget activeTarget = null;
        //遍历，判断IPlayTarget是否有一半的view在屏幕内
        for (IPlayTarget target : mTargets) {

            boolean inBounds = isTargetInBounds(target);
            //如果当前检测的target在屏幕内
            if (inBounds) {
                //保存当前target
                activeTarget = target;
                break;
            }
        }

        //遍历之后判断activeTarget是否为空，如果不为空说明找到一个满足条件的
        if (activeTarget != null) {
            //检测到一个新的target满足自动播放逻辑时候，把上一个满足条件的关闭掉
            if (playingTarget != null) {
                playingTarget.inActive();
            }
            //对activeTarget全局保存
            playingTarget = activeTarget;
            //完成一次新的视频播放
            activeTarget.onActive();
        }
    }

    /**
     * 检测 IPlayTarget 所在的 viewGroup 是否至少还有一半的大小在屏幕内
     *
     * @param target
     * @return
     */
    private boolean isTargetInBounds(IPlayTarget target) {
        //得到所在的容器
        ViewGroup owner = target.getOwner();
        ensureRecyclerViewLocation();
        //如果owner没有被展示出来或没有附加到窗口
        if (!owner.isShown() || !owner.isAttachedToWindow()) {
            return false;
        }

        //计算owner在屏幕上所处的位置
        int[] location = new int[2];
        owner.getLocationOnScreen(location);

        //计算owner中心在屏幕的位置
        int center = location[1] + owner.getHeight() / 2;

        //承载视频播放画面的ViewGroup它需要至少一半的大小 在RecyclerView上下范围内
        return center >= rvLocation.first && center <= rvLocation.second;
    }

    private Pair<Integer, Integer> rvLocation = null;

    //RecycleView在屏幕上的位置
    private Pair<Integer, Integer> ensureRecyclerViewLocation() {
        if (rvLocation == null) {
            int[] location = new int[2];
            mRecyclerView.getLocationOnScreen(location);

            int top = location[1];
            int bottom = top + mRecyclerView.getHeight();

            //bottom和top的返回
            rvLocation = new Pair(top, bottom);
        }
        return rvLocation;
    }

    public void onPause() {
        if (playingTarget != null) {
            playingTarget.inActive();
        }
    }

    public void onResume() {
        if (playingTarget != null) {
            playingTarget.onActive();
        }
    }
}
