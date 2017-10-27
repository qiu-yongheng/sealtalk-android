package cn.rongcloud.im.ui.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Window;

import cn.rongcloud.im.R;
import cn.rongcloud.im.SealAppContext;
import io.rong.imkit.RongIM;

/**
 * Created by AMing on 16/8/5.
 * Company RongCloud
 * 引导页
 * 1. 判断是否有缓存token
 * 2. 如果有, 直接跳转MAinActivity
 * 3. 没有, 跳转LoginActivity
 */
public class SplashActivity extends Activity {

    private Context context;
    private android.os.Handler handler = new android.os.Handler();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_splash);
        context = this;
        SharedPreferences sp = getSharedPreferences("config", MODE_PRIVATE);
        // 获取缓存的token
        String cacheToken = sp.getString("loginToken", "");
        if (!TextUtils.isEmpty(cacheToken)) {
            // 有缓存的token
            RongIM.connect(cacheToken, SealAppContext.getInstance().getConnectCallback());
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    goToMain();
                }
            }, 800);
        } else {
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    goToLogin();
                }
            }, 800);
        }
    }


    private void goToMain() {
        startActivity(new Intent(context, MainActivity.class));
        finish();
    }

    private void goToLogin() {
        startActivity(new Intent(context, LoginActivity.class));
        finish();
    }

    private boolean isNetworkConnected(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo ni = cm.getActiveNetworkInfo();
        return ni != null && ni.isConnectedOrConnecting();
    }

}
