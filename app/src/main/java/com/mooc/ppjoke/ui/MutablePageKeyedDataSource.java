package com.mooc.ppjoke.ui;

import android.annotation.SuppressLint;

import androidx.annotation.NonNull;
import androidx.arch.core.executor.ArchTaskExecutor;
import androidx.paging.PageKeyedDataSource;
import androidx.paging.PagedList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 具体原理见 {@link MutableItemKeyedDataSource}
 *
 * @param <Value>
 */
public class MutablePageKeyedDataSource<Value> extends PageKeyedDataSource<Integer, Value> {
    public List<Value> data = new ArrayList<>();

    //创建pagedList对象
    public PagedList<Value> buildNewPagedList(PagedList.Config config) {
        @SuppressLint("RestrictedApi") PagedList<Value> pagedList = new PagedList.Builder<Integer, Value>(this, config)
                .setFetchExecutor(ArchTaskExecutor.getIOThreadExecutor())//给pagedList提供异步工作的线程池
                .setNotifyExecutor(ArchTaskExecutor.getMainThreadExecutor())//提供能在主线程工作的线程池
                .build();

        return pagedList;
    }

    @Override
    public void loadInitial(@NonNull LoadInitialParams<Integer> params, @NonNull LoadInitialCallback<Integer, Value> callback) {
        callback.onResult(data, null, null);
    }

    @Override
    public void loadBefore(@NonNull LoadParams<Integer> params, @NonNull LoadCallback<Integer, Value> callback) {
        //不需要使用该方法，返回一个空数据集即可
        callback.onResult(Collections.emptyList(), null);
    }

    @Override
    public void loadAfter(@NonNull LoadParams<Integer> params, @NonNull LoadCallback<Integer, Value> callback) {
        //不需要使用该方法，返回一个空数据集即可
        callback.onResult(Collections.emptyList(), null);
    }
}
