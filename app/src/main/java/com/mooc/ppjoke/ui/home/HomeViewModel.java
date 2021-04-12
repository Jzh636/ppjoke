package com.mooc.ppjoke.ui.home;

import android.annotation.SuppressLint;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.arch.core.executor.ArchTaskExecutor;
import androidx.lifecycle.MutableLiveData;
import androidx.paging.DataSource;
import androidx.paging.ItemKeyedDataSource;
import androidx.paging.PagedList;

import com.alibaba.fastjson.TypeReference;
import com.mooc.libnetwork.ApiResponse;
import com.mooc.libnetwork.ApiService;
import com.mooc.libnetwork.JsonCallback;
import com.mooc.libnetwork.Request;
import com.mooc.ppjoke.ui.AbsViewModel;
import com.mooc.ppjoke.model.Feed;
import com.mooc.ppjoke.ui.MutablePageKeyedDataSource;
import com.mooc.ppjoke.ui.login.UserManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class HomeViewModel extends AbsViewModel<Feed> {

    private volatile boolean witchCache = true;
    private MutableLiveData<PagedList<Feed>> cacheLiveData = new MutableLiveData<>();
    //标志位，防止pagging处理了，我们自己在上拉的时候也做了分页，可能造成数据重复，所以在在loadAfter里面做个同步位的标记
    private AtomicBoolean loadAfter = new AtomicBoolean(false);
    private String mFeedType;

    /*
     *一、paging框架内置的dataSource数据源：DataSource<Key, Value>
     *key对应加载数据的条件信息，也就入参; value代表对应的数据实体类，即网络数据返回的javaBean对象
     *
     * 二、paging框架内置的三种dataSource：
     * 1）PageKeyedDataSource<Key, Value>:适用于目标数据根据页信息请求数据
     * 2）ItemKeyedDataSource<Key, Value>：适用于目标数据的加载依赖特定的item的信息，常用
     * 3）PositionalDataSource<Key, Value>：适用于目标数据总数固定，通过特定的位置加载数据
     * */
    @Override
    public DataSource createDataSource() {
        return new FeedDataSource();
    }

    public MutableLiveData<PagedList<Feed>> getCacheLiveData() {
        return cacheLiveData;
    }

    public void setFeedType(String feedType) {

        mFeedType = feedType;
    }

    class FeedDataSource extends ItemKeyedDataSource<Integer, Feed> {
        //在回调的时候都已经切换到了子线程，所以再写网络请求的时候可以直接再在方法里面写同步方法请求，就不用再新开一个线程

        @Override
        public void loadInitial(@NonNull LoadInitialParams<Integer> params, @NonNull LoadInitialCallback<Feed> callback) {
            //加载初始化数据的，先加载缓存再加载网络数据，网络数据更新之后再更新本地缓存
            Log.e("homeviewmodel", "loadInitial: ");
            loadData(0, params.requestedLoadSize, callback);
            witchCache = false;
        }

        @Override
        public void loadAfter(@NonNull LoadParams<Integer> params, @NonNull LoadCallback<Feed> callback) {
            //向后加载分页数据的
            Log.e("homeviewmodel", "loadAfter: ");
            loadData(params.key, params.requestedLoadSize, callback);
        }

        @Override
        public void loadBefore(@NonNull LoadParams<Integer> params, @NonNull LoadCallback<Feed> callback) {
            callback.onResult(Collections.emptyList());
            //能够向前加载数据的
        }

        //通过最后一条item的信息来返回一个Integer对象
        @NonNull
        @Override
        public Integer getKey(@NonNull Feed item) {
            return item.id;
        }
    }

    private void loadData(int key, int count, ItemKeyedDataSource.LoadCallback<Feed> callback) {
        if (key > 0) {
            loadAfter.set(true);
        }
        //get请求
        //feeds/queryHotFeedsList
        Request request = ApiService.get("/feeds/queryHotFeedsList")
                .addParam("feedType", mFeedType)
                .addParam("userId", UserManager.get().getUserId())
                .addParam("feedId", key)
                .addParam("pageCount", count)//pageCount表示分页加载的时候每页多少条
                .responseType(new TypeReference<ArrayList<Feed>>() {
                }.getType());

        //如果需要加载缓存
        if (witchCache) {
            request.cacheStrategy(Request.CACHE_ONLY);
            //开启子线程，就不会阻塞接口数据的请求了
            request.execute(new JsonCallback<List<Feed>>() {
                @Override
                public void onCacheSuccess(ApiResponse<List<Feed>> response) {
                    Log.e("loadData", "onCacheSuccess: ");
                    //response.body返回的是List类型，需要转成pagedList对象，pagedList创建的时候需要绑定一个dataSource
                    MutablePageKeyedDataSource dataSource = new MutablePageKeyedDataSource<Feed>();
                    //将response.body添加进集合
                    dataSource.data.addAll(response.body);

                    //创建pagedList对象，缓存数据就和dataSource关联起来了
                    PagedList pagedList = dataSource.buildNewPagedList(config);
                    //通过livaData将数据发送出去
                    cacheLiveData.postValue(pagedList);

                    //下面的不可取，否则会报
                    // java.lang.IllegalStateException: callback.onResult already called, cannot call again.
                    //if (response.body != null) {
                    //  callback.onResult(response.body);
                    // }
                }
            });
        }

        try {
            //网络数据请求
            //如果使用过缓存，通过request.clone()得到新的request对象，否则就是它本身
            Request netRequest = witchCache ? request.clone() : request;
            //指定策略的时候，上拉分页的话网络请求成功之后不用更新缓存，下拉加载需要更新缓存
            netRequest.cacheStrategy(key == 0 ? Request.NET_CACHE : Request.NET_ONLY);
            ApiResponse<List<Feed>> response = netRequest.execute();
            List<Feed> data = response.body == null ? Collections.emptyList() : response.body;

            callback.onResult(data);

            if (key > 0) {
                //k > 0表示上拉加载
                //通过BoundaryPageData发送数据 告诉UI层 是否应该主动关闭上拉加载分页的动画
                ((MutableLiveData) getBoundaryPageData()).postValue(data.size() > 0);
                loadAfter.set(false);
            }
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }

        Log.e("loadData", "loadData: key:" + key);

    }

    @SuppressLint("RestrictedApi")
    public void loadAfter(int id, ItemKeyedDataSource.LoadCallback<Feed> callback) {
        if (loadAfter.get()) {
            callback.onResult(Collections.emptyList());
            return;
        }
        ArchTaskExecutor.getIOThreadExecutor().execute(new Runnable() {
            @Override
            public void run() {
                loadData(id, config.pageSize, callback);
            }
        });
    }
}