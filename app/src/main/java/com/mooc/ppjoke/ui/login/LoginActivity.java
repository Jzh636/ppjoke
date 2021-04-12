package com.mooc.ppjoke.ui.login;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.mooc.libnetwork.ApiResponse;
import com.mooc.libnetwork.ApiService;
import com.mooc.libnetwork.JsonCallback;
import com.mooc.ppjoke.R;
import com.mooc.ppjoke.model.User;
import com.tencent.connect.UserInfo;
import com.tencent.connect.auth.QQToken;
import com.tencent.connect.common.Constants;
import com.tencent.tauth.IUiListener;
import com.tencent.tauth.Tencent;
import com.tencent.tauth.UiError;

import org.json.JSONException;
import org.json.JSONObject;

public class LoginActivity extends AppCompatActivity implements View.OnClickListener {
    private View actionClose;
    private View actionLogin;
    private Tencent tencent;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_layout_login);

        actionClose = findViewById(R.id.action_close);
        actionLogin = findViewById(R.id.action_login);

        actionClose.setOnClickListener(this);
        actionLogin.setOnClickListener(this);
    }

    //按钮点击事件
    @Override
    public void onClick(View v) {

        if (v.getId() == R.id.action_close) {
            //关闭登录
            finish();
        } else if (v.getId() == R.id.action_login) {
            //登录
            login();
        }
    }

    private void login() {
        if (tencent == null) {
            //传入AppId参数
            tencent = Tencent.createInstance("101794421", getApplicationContext());
        }
        //第二个参数：获取用户的全部信息
        tencent.login(this, "all", loginListener);
    }

    IUiListener loginListener = new IUiListener() {
        //登录成功
        @Override
        public void onComplete(Object o) {
            JSONObject response = (JSONObject) o;
            try {
                //拉取登录，登陆成功之后根据返回的下面几个字段，在调用getUser接口获取用户信息
                String openid = response.getString("openid");
                String access_token = response.getString("access_token");
                String expires_in = response.getString("expires_in");
                long expires_time = response.getLong("expires_time");

                //将获取到的几个对象设置到tencent中
                tencent.setOpenId(openid);
                tencent.setAccessToken(access_token, expires_in);
                //通过QQToken获取用户信息
                QQToken qqToken = tencent.getQQToken();
                getUserInfo(qqToken, expires_time, openid);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        //登录失败
        @Override
        public void onError(UiError uiError) {
            Toast.makeText(getApplicationContext(), "登录失败:reason" + uiError.toString(), Toast.LENGTH_SHORT).show();
        }

        //取消登录
        @Override
        public void onCancel() {
            Toast.makeText(getApplicationContext(), "登录取消", Toast.LENGTH_SHORT).show();
        }
    };

    //获取用户信息
    private void getUserInfo(QQToken qqToken, long expires_time, String openid) {
        UserInfo userInfo = new UserInfo(getApplicationContext(), qqToken);
        userInfo.getUserInfo(new IUiListener() {
            //获取用户信息成功
            @Override
            public void onComplete(Object o) {
                JSONObject response = (JSONObject) o;

                try {
                    //昵称
                    String nickname = response.getString("nickname");
                    //大小为100×100像素的QQ空间头像URL
                    String figureurl_2 = response.getString("figureurl_2");

                    //获取以上两字段后需要将当前登陆者的身份上传到自己的服务器，由服务器生成用户的信息，包括userId等，
                    //并且需要将当前登录者的信息存储到本地，存储的时候需要一个过期时间，上传的时候需要一个openId，根据openId生成userId
                    save(nickname, figureurl_2, openid, expires_time);//上传用户信息
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onError(UiError uiError) {
                Toast.makeText(getApplicationContext(), "登录失败:reason" + uiError.toString(), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onCancel() {
                Toast.makeText(getApplicationContext(), "登录取消", Toast.LENGTH_SHORT).show();
            }
        });
    }

    //上传用户信息
    private void save(String nickname, String avatar, String openid, long expires_time) {
        ApiService.get("/user/insert")
                .addParam("name", nickname)
                .addParam("avatar", avatar)
                .addParam("qqOpenId", openid)
                .addParam("expires_time", expires_time)
                .execute(new JsonCallback<User>() {
                    @Override
                    public void onSuccess(ApiResponse<User> response) {
                        if (response.body != null) {
                            //response.body不为空才进行缓存
                            UserManager.get().save(response.body);
                            finish();
                        } else {
                            //onSuccess在子线程执行，此处要返回主线程
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(getApplicationContext(), "登陆失败", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    }

                    @Override
                    public void onError(ApiResponse<User> response) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(getApplicationContext(), "登陆失败,msg:" + response.message, Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == Constants.REQUEST_LOGIN) {
            Tencent.onActivityResultData(requestCode, resultCode, data, loginListener);
        }
    }
}
