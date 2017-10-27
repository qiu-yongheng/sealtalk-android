package cn.rongcloud.im.ui.activity;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import cn.rongcloud.im.R;
import cn.rongcloud.im.SealConst;
import cn.rongcloud.im.SealUserInfoManager;
import cn.rongcloud.im.server.network.http.HttpException;
import cn.rongcloud.im.server.response.GetTokenResponse;
import cn.rongcloud.im.server.response.GetUserInfoByIdResponse;
import cn.rongcloud.im.server.response.LoginResponse;
import cn.rongcloud.im.server.utils.AMUtils;
import cn.rongcloud.im.server.utils.CommonUtils;
import cn.rongcloud.im.server.utils.NLog;
import cn.rongcloud.im.server.utils.NToast;
import cn.rongcloud.im.server.utils.RongGenerate;
import cn.rongcloud.im.server.widget.ClearWriteEditText;
import cn.rongcloud.im.server.widget.LoadDialog;
import io.rong.imkit.RongIM;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.model.UserInfo;

/**
 * Created by AMing on 16/1/15.
 * Company RongCloud
 * 1. 输入账号密码登录, 返回token, 使用token登录融云
 * 2. token登录成功, 同步账号信息, 同步好友信息到本地
 * 3. token登录失效, 重新请求新的token进行登录
 */
public class LoginActivity extends BaseActivity implements View.OnClickListener {

    private final static String TAG = "LoginActivity";
    private static final int LOGIN = 5;
    private static final int GET_TOKEN = 6;
    private static final int SYNC_USER_INFO = 9;

    private ImageView mImg_Background;
    private ClearWriteEditText mPhoneEdit, mPasswordEdit;
    private String phoneString;
    private String passwordString;
    private String connectResultId;
    private SharedPreferences sp;
    private SharedPreferences.Editor editor;
    private String loginToken;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        setHeadVisibility(View.GONE);
        sp = getSharedPreferences("config", MODE_PRIVATE);
        editor = sp.edit();
        initView();
    }

    private void initView() {
        mPhoneEdit = (ClearWriteEditText) findViewById(R.id.de_login_phone);
        mPasswordEdit = (ClearWriteEditText) findViewById(R.id.de_login_password);
        Button mConfirm = (Button) findViewById(R.id.de_login_sign);
        TextView mRegister = (TextView) findViewById(R.id.de_login_register);
        TextView forgetPassword = (TextView) findViewById(R.id.de_login_forgot);
        forgetPassword.setOnClickListener(this);
        mConfirm.setOnClickListener(this);
        mRegister.setOnClickListener(this);
        // 设置动画
        mImg_Background = (ImageView) findViewById(R.id.de_img_backgroud);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Animation animation = AnimationUtils.loadAnimation(LoginActivity.this, R.anim.translate_anim);
                mImg_Background.startAnimation(animation);
            }
        }, 200);
        mPhoneEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() == 11) {
                    AMUtils.onInactive(mContext, mPhoneEdit);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        String oldPhone = sp.getString(SealConst.SEALTALK_LOGING_PHONE, "");
        String oldPassword = sp.getString(SealConst.SEALTALK_LOGING_PASSWORD, "");

        if (!TextUtils.isEmpty(oldPhone) && !TextUtils.isEmpty(oldPassword)) {
            mPhoneEdit.setText(oldPhone);
            mPasswordEdit.setText(oldPassword);
        }

        // 判断是否被踢下线
        if (getIntent().getBooleanExtra("kickedByOtherClient", false)) {
            final AlertDialog dlg = new AlertDialog.Builder(LoginActivity.this).create();
            dlg.show();
            Window window = dlg.getWindow();
            window.setContentView(R.layout.other_devices);
            TextView text = (TextView) window.findViewById(R.id.ok);
            text.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dlg.cancel();
                }
            });
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.de_login_sign:
                phoneString = mPhoneEdit.getText().toString().trim();
                passwordString = mPasswordEdit.getText().toString().trim();

                if (TextUtils.isEmpty(phoneString)) {
                    NToast.shortToast(mContext, R.string.phone_number_is_null);
                    mPhoneEdit.setShakeAnimation();
                    return;
                }

//                if (!AMUtils.isMobile(phoneString)) {
//                    NToast.shortToast(mContext, R.string.Illegal_phone_number);
//                    mPhoneEdit.setShakeAnimation();
//                    return;
//                }

                if (TextUtils.isEmpty(passwordString)) {
                    NToast.shortToast(mContext, R.string.password_is_null);
                    mPasswordEdit.setShakeAnimation();
                    return;
                }
                if (passwordString.contains(" ")) {
                    NToast.shortToast(mContext, R.string.password_cannot_contain_spaces);
                    mPasswordEdit.setShakeAnimation();
                    return;
                }
                LoadDialog.show(mContext);
                editor.putBoolean("exit", false);
                editor.apply();
                String oldPhone = sp.getString(SealConst.SEALTALK_LOGING_PHONE, "");
                // 登录请求
                request(LOGIN, true);
                break;
            case R.id.de_login_register:
                // 注册
                startActivityForResult(new Intent(this, RegisterActivity.class), 1);
                break;
            case R.id.de_login_forgot:
                // 忘记密码
                startActivityForResult(new Intent(this, ForgetPasswordActivity.class), 2);
                break;
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 2 && data != null) {
            String phone = data.getStringExtra("phone");
            String password = data.getStringExtra("password");
            mPhoneEdit.setText(phone);
            mPasswordEdit.setText(password);
        } else if (data != null && requestCode == 1) {
            String phone = data.getStringExtra("phone");
            String password = data.getStringExtra("password");
            String id = data.getStringExtra("id");
            String nickname = data.getStringExtra("nickname");
            if (!TextUtils.isEmpty(phone) && !TextUtils.isEmpty(password) && !TextUtils.isEmpty(id) && !TextUtils.isEmpty(nickname)) {
                mPhoneEdit.setText(phone);
                mPasswordEdit.setText(password);
                editor.putString(SealConst.SEALTALK_LOGING_PHONE, phone);
                editor.putString(SealConst.SEALTALK_LOGING_PASSWORD, password);
                editor.putString(SealConst.SEALTALK_LOGIN_ID, id);
                editor.putString(SealConst.SEALTALK_LOGIN_NAME, nickname);
                editor.apply();
            }

        }
        super.onActivityResult(requestCode, resultCode, data);
    }


    /**
     * 请求回调: 执行后台任务
     * @param requestCode
     * @param id
     * @return
     * @throws HttpException
     */
    @Override
    public Object doInBackground(int requestCode, String id) throws HttpException {
        switch (requestCode) {
            case LOGIN:
                return action.login("86", phoneString, passwordString);
            case GET_TOKEN:
                return action.getToken();
            case SYNC_USER_INFO:
                return action.getUserInfoById(connectResultId);
        }
        return null;
    }

    /**
     * 请求回调: 请求成功
     * @param requestCode
     * @param result
     */
    @Override
    public void onSuccess(int requestCode, Object result) {
        if (result != null) {
            switch (requestCode) {
                case LOGIN:
                    LoginResponse loginResponse = (LoginResponse) result;
                    if (loginResponse.getCode() == 200) {
                        loginToken = loginResponse.getResult().getToken();
                        if (!TextUtils.isEmpty(loginToken)) {
                            RongIM.connect(loginToken, new RongIMClient.ConnectCallback() {
                                @Override
                                public void onTokenIncorrect() {
                                    NLog.e("connect", "onTokenIncorrect");
                                    // token不正确, 重新获取token
                                    reGetToken();
                                }

                                @Override
                                public void onSuccess(String s) {
                                    connectResultId = s;
                                    NLog.e("connect", "onSuccess userid:" + s);
                                    // 保存登录账号
                                    editor.putString(SealConst.SEALTALK_LOGIN_ID, s);
                                    editor.apply();
                                    SealUserInfoManager.getInstance().openDB();
                                    // 请求用户信息
                                    request(SYNC_USER_INFO, true);
                                }

                                @Override
                                public void onError(RongIMClient.ErrorCode errorCode) {
                                    NLog.e("connect", "onError errorcode:" + errorCode.getValue());
                                }
                            });
                        }
                    } else if (loginResponse.getCode() == 100) {
                        LoadDialog.dismiss(mContext);
                        NToast.shortToast(mContext, R.string.phone_or_psw_error);
                    } else if (loginResponse.getCode() == 1000) {
                        LoadDialog.dismiss(mContext);
                        NToast.shortToast(mContext, R.string.phone_or_psw_error);
                    }
                    break;
                case SYNC_USER_INFO:
                    GetUserInfoByIdResponse userInfoByIdResponse = (GetUserInfoByIdResponse) result;
                    if (userInfoByIdResponse.getCode() == 200) {
                        if (TextUtils.isEmpty(userInfoByIdResponse.getResult().getPortraitUri())) {
                            userInfoByIdResponse.getResult().setPortraitUri(RongGenerate.generateDefaultAvatar(userInfoByIdResponse.getResult().getNickname(), userInfoByIdResponse.getResult().getId()));
                        }
                        String nickName = userInfoByIdResponse.getResult().getNickname();
                        String portraitUri = userInfoByIdResponse.getResult().getPortraitUri();
                        // 保存用户账号头像
                        editor.putString(SealConst.SEALTALK_LOGIN_NAME, nickName);
                        editor.putString(SealConst.SEALTALK_LOGING_PORTRAIT, portraitUri);
                        editor.apply();
                        RongIM.getInstance().refreshUserInfoCache(new UserInfo(connectResultId, nickName, Uri.parse(portraitUri)));
                    }
                    //不继续在login界面同步好友,群组,群组成员信息
                    SealUserInfoManager.getInstance().getAllUserInfo();
                    // 跳转界面
                    goToMain();
                    break;
                case GET_TOKEN:
                    GetTokenResponse tokenResponse = (GetTokenResponse) result;
                    if (tokenResponse.getCode() == 200) {
                        String token = tokenResponse.getResult().getToken();
                        if (!TextUtils.isEmpty(token)) {
                            RongIM.connect(token, new RongIMClient.ConnectCallback() {
                                @Override
                                public void onTokenIncorrect() {
                                    Log.e(TAG, "reToken Incorrect");
                                }

                                @Override
                                public void onSuccess(String s) {
                                    connectResultId = s;
                                    NLog.e("connect", "onSuccess userid:" + s);
                                    editor.putString(SealConst.SEALTALK_LOGIN_ID, s);
                                    editor.apply();
                                    SealUserInfoManager.getInstance().openDB();
                                    request(SYNC_USER_INFO, true);
                                }

                                @Override
                                public void onError(RongIMClient.ErrorCode e) {

                                }
                            });
                        }
                    }
                    break;
            }
        }
    }

    private void reGetToken() {
        request(GET_TOKEN);
    }

    @Override
    public void onFailure(int requestCode, int state, Object result) {
        if (!CommonUtils.isNetworkConnected(mContext)) {
            LoadDialog.dismiss(mContext);
            NToast.shortToast(mContext, getString(R.string.network_not_available));
            return;
        }
        switch (requestCode) {
            case LOGIN:
                LoadDialog.dismiss(mContext);
                NToast.shortToast(mContext, R.string.login_api_fail);
                break;
            case SYNC_USER_INFO:
                LoadDialog.dismiss(mContext);
                NToast.shortToast(mContext, R.string.sync_userinfo_api_fail);
                break;
            case GET_TOKEN:
                LoadDialog.dismiss(mContext);
                NToast.shortToast(mContext, R.string.get_token_api_fail);
                break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    /**
     * 跳转界面
     * 保存token
     * 保存用户名
     * 保存密码
     */
    private void goToMain() {
        editor.putString("loginToken", loginToken);
        editor.putString(SealConst.SEALTALK_LOGING_PHONE, phoneString);
        editor.putString(SealConst.SEALTALK_LOGING_PASSWORD, passwordString);
        editor.apply();
        LoadDialog.dismiss(mContext);
        NToast.shortToast(mContext, R.string.login_success);
        startActivity(new Intent(LoginActivity.this, MainActivity.class));
        finish();
    }
}
