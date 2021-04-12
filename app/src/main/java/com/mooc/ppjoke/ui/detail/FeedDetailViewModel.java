package com.mooc.ppjoke.ui.detail;

import androidx.annotation.NonNull;
import androidx.paging.DataSource;
import androidx.paging.ItemKeyedDataSource;

import com.alibaba.fastjson.TypeReference;
import com.mooc.libnetwork.ApiResponse;
import com.mooc.libnetwork.ApiService;
import com.mooc.ppjoke.model.Comment;
import com.mooc.ppjoke.ui.AbsViewModel;
import com.mooc.ppjoke.ui.login.UserManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FeedDetailViewModel extends AbsViewModel<Comment> {
    private long itemId;

    //创建一个能提供数据的数据源对象
    @Override
    public DataSource createDataSource() {
        return new DataSource();
    }

    public void setItemId(long itemId) {
        this.itemId = itemId;
    }

    //由于评论的接口时根据列表当中的最后一条item的相关信息来控制分页的，所以这里选择ItemKeyedDataSource
    class DataSource extends ItemKeyedDataSource<Integer, Comment> {

        //加载页面初始化数据
        @Override
        public void loadInitial(@NonNull LoadInitialParams<Integer> params, @NonNull LoadInitialCallback<Comment> callback) {
            //真正加载数据
            loadData(params.requestedInitialKey, params.requestedLoadSize, callback);
        }

        //加载分页
        @Override
        public void loadAfter(@NonNull LoadParams<Integer> params, @NonNull LoadCallback<Comment> callback) {
            if (params.key > 0) {
                //真正加载数据
                loadData(params.key, params.requestedLoadSize, callback);
            }
        }

        //做评论列表的数据请求
        private void loadData(Integer key, int requestedLoadSize, LoadCallback<Comment> callback) {
            ApiResponse<List<Comment>> response = ApiService.get("/comment/queryFeedComments")
                    .addParam("id", key)
                    .addParam("itemId", itemId)
                    .addParam("userId", UserManager.get().getUserId())
                    .addParam("pageCount", requestedLoadSize)
                    .responseType(new TypeReference<ArrayList<Comment>>() {
                    }.getType())
                    .execute();

            List<Comment> list = response.body == null ? Collections.emptyList() : response.body;
            callback.onResult(list);
        }

        //向前加载分页，此处不需要使用
        @Override
        public void loadBefore(@NonNull LoadParams<Integer> params, @NonNull LoadCallback<Comment> callback) {
            callback.onResult(Collections.emptyList());//不需要用到，传入一个空的数据集
        }

        //返回分页时的入参
        @NonNull
        @Override
        public Integer getKey(@NonNull Comment item) {
            return item.id;
        }
    }
}
