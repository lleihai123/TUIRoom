package com.tencent.liteav.demo;

import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

import com.blankj.utilcode.util.ToastUtils;
import com.tencent.imsdk.v2.V2TIMCallback;
import com.tencent.imsdk.v2.V2TIMManager;
import com.tencent.imsdk.v2.V2TIMSDKConfig;
import com.tencent.imsdk.v2.V2TIMSDKListener;
import com.tencent.imsdk.v2.V2TIMUserFullInfo;
import com.tencent.imsdk.v2.V2TIMValueCallback;
import com.tencent.liteav.basic.AvatarConstant;
import com.tencent.liteav.basic.UserModel;
import com.tencent.liteav.basic.UserModelManager;
import com.tencent.liteav.debug.GenerateTestUserSig;
import com.tencent.qcloud.tuicore.TUILogin;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class LoginActivity extends AppCompatActivity {
    private static final String TAG = LoginActivity.class.getName();

    private EditText          mEditUserId;
    private Button            mButtonLogin;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        initStatusBar();
        initView();
    }

    private void initView() {
        mEditUserId = (EditText) findViewById(R.id.et_userId);
        initButtonLogin();
    }

    private void initButtonLogin() {
        mButtonLogin = (Button) findViewById(R.id.tv_login);
        mButtonLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                login();
            }
        });
    }

    private void login() {
        String userId = mEditUserId.getText().toString().trim();
        final UserModel userModel = new UserModel();
        userModel.userId = userId;
        userModel.userName = userId;
        int index = new Random().nextInt(AvatarConstant.USER_AVATAR_ARRAY.length);
        String coverUrl = AvatarConstant.USER_AVATAR_ARRAY[index];
        userModel.userAvatar = coverUrl;
        userModel.userSig = GenerateTestUserSig.genTestUserSig(userId);
        final UserModelManager manager = UserModelManager.getInstance();
        manager.setUserModel(userModel);
        V2TIMSDKConfig config = new V2TIMSDKConfig();
        config.setLogLevel(V2TIMSDKConfig.V2TIM_LOG_DEBUG);
        TUILogin.init(this, GenerateTestUserSig.SDKAPPID, null, new V2TIMSDKListener() {

            @Override
            public void onKickedOffline() {

            }

            @Override
            public void onUserSigExpired() {

            }
        });
        TUILogin.login(userModel.userId, userModel.userSig, new V2TIMCallback() {
            @Override
            public void onError(int code, String msg) {
                ToastUtils.showLong(R.string.app_toast_login_fail, code, msg);
                Log.d(TAG, "login fail code: " + code + " msg:" + msg);
            }

            @Override
            public void onSuccess() {
                Log.d(TAG, "login onSuccess");
                getUserInfo();
            }
        });
    }

    private void getUserInfo() {
        final UserModelManager manager = UserModelManager.getInstance();
        final UserModel userModel = manager.getUserModel();
        //先查询用户是否存在
        List<String> userIdList = new ArrayList<>();
        userIdList.add(userModel.userId);
        Log.d(TAG, "setUserInfo: userIdList = " + userIdList);
        V2TIMManager.getInstance().getUsersInfo(userIdList, new V2TIMValueCallback<List<V2TIMUserFullInfo>>() {

            @Override
            public void onError(int code, String msg) {
                Log.e(TAG, "get group info list fail, code:" + code + " msg: " + msg);
            }

            @Override
            public void onSuccess(List<V2TIMUserFullInfo> resultList) {
                if (resultList == null || resultList.isEmpty()) {
                    return;
                }
                V2TIMUserFullInfo result = resultList.get(0);
                String userName = result.getNickName();
                String userAvatar = result.getFaceUrl();
                Log.d(TAG, "onSuccess: userName = " + userName + " , userAvatar = " + userAvatar);
                //如果用户名和头像为空,则跳转设置界面进行设置
                if (TextUtils.isEmpty(userName) || TextUtils.isEmpty(userAvatar)) {
                    Intent intent = new Intent(LoginActivity.this, ProfileActivity.class);
                    startActivity(intent);
                    finish();
                } else {
                    userModel.userAvatar = userAvatar;
                    userModel.userName = userName;
                    manager.setUserModel(userModel);
                    //如果用户信息不为空,则直接进入主界面
                    Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                    startActivity(intent);
                    finish();
                }
            }
        });
    }

    private void initStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(Color.TRANSPARENT);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        }
    }
}
