package com.mooc.ppjoke.ui.login;

import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import androidx.arch.core.executor.ArchTaskExecutor;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.mooc.libcommon.global.AppGlobals;
import com.mooc.libnetwork.ApiResponse;
import com.mooc.libnetwork.ApiService;
import com.mooc.libnetwork.JsonCallback;
import com.mooc.libnetwork.cache.CacheManager;
import com.mooc.ppjoke.model.User;

public class UserManager {
    private static final String KEY_CACHE_USER = "cache_user";
    private static UserManager mUserManager = new UserManager();
    private MutableLiveData<User> userLiveData = new MutableLiveData<>();
    private User mUser;

    //返回UserManager实例
    public static UserManager get() {
        return mUserManager;
    }

    private UserManager() {
        //获取存储的用户信息
        User cache = (User) CacheManager.getCache(KEY_CACHE_USER);
        if (cache != null && cache.expires_time > System.currentTimeMillis()) {
            mUser = cache;
        }
    }

    //持久化用户信息
    public void save(User user) {
        mUser = user;
        CacheManager.save(KEY_CACHE_USER, user);
        //持久化用户信息之后，用liveData发送数据通知调用方
        //数据发送前用userLiveData.hasObservers()判断是否有观察者已经被注册到liveData里面了，否则的话是没必要发送事件
        if (userLiveData.hasObservers()) {
            userLiveData.postValue(user);
        }
    }

    //登录
    //登陆页面的拉起有可能是首页或个人中心页，入口有多个，最好收拢到UserManager里面，统一暴露出API
    public LiveData<User> login(Context context) {
        Intent intent = new Intent(context, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
        return userLiveData;
    }

    //调用者在调用login之前，判断是否登录
    public boolean isLogin() {
        return mUser == null ? false : mUser.expires_time > System.currentTimeMillis();
    }

    public User getUser() {
        //已经登陆了则返回User对象本身
        return isLogin() ? mUser : null;
    }

    public long getUserId()  {
        return isLogin() ? mUser.userId : 0;
    }


    public LiveData<User> refresh() {
        if (!isLogin()) {
            return login(AppGlobals.getApplication());
        }
        MutableLiveData<User> liveData = new MutableLiveData<>();
        ApiService.get("/user/query")
                .addParam("userId", getUserId())
                .execute(new JsonCallback<User>() {
                    @Override
                    public void onSuccess(ApiResponse<User> response) {
                        save(response.body);
                        liveData.postValue(getUser());
                    }

                    @Override
                    public void onError(ApiResponse<User> response) {
                        ArchTaskExecutor.getMainThreadExecutor().execute(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(AppGlobals.getApplication(), response.message, Toast.LENGTH_SHORT).show();
                            }
                        });

                        liveData.postValue(null);
                    }
                });
        return liveData;
    }

    public void logout() {
        CacheManager.delete(KEY_CACHE_USER, mUser);
        mUser = null;
    }
}
