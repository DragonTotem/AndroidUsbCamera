package com.jiangdg.usbcamera.view;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import butterknife.BindView;
import butterknife.ButterKnife;

import com.jiangdg.usbcamera.R;
import com.jiangdg.usbcamera.utils.SystemUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import android.provider.Settings;


/**
 * permission checking
 * Created by jiangdongguo on 2019/6/27.
 */

public class SplashActivity extends AppCompatActivity {
    private static final String TAG = "Debug";
    private static final String[] REQUIRED_PERMISSION_LIST = new String[]{
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA
    };
    private static final int REQUEST_CODE = 1;
    private List<String> mMissPermissions = new ArrayList<>();
    @BindView(R.id.exit_btn)
    public View exit_btn;
    @BindView(R.id.settings_btn)
    public View settings_btn;
    @BindView(R.id.scamera_btn)
    public View scamera_btn;
    @BindView(R.id.screen_layout)
    public RelativeLayout screen_layout;

    @BindView(R.id.view_top)
    public ImageView view_top;

    @BindView(R.id.view_light)
    public ImageView view_light;
    @BindView(R.id.view_tep)
    public ImageView view_tep;
    @BindView(R.id.view_weather)
    public ImageView view_weather;
    @BindView(R.id.view_water)
    public ImageView view_water;

    @BindView(R.id.tv_time)
    public TextView tv_time;
    @BindView(R.id.tv_date)
    public TextView tv_date;

    private int[] imgs = {
            R.mipmap.light_open,
            R.mipmap.light_close,
            R.mipmap.tep_open,
            R.mipmap.tep_close,
            R.mipmap.wether_open,
            R.mipmap.wether_close,
            R.mipmap.wa_open,
            R.mipmap.wa_close,
            R.mipmap.light_top,
            R.mipmap.air_top,
            R.mipmap.outdoor_top,
            R.mipmap.humidity_top
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_splash);

        this.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        if (isVersionM()) {
            checkAndRequestPermissions();
        }
        ButterKnife.bind(this);
        initView();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        SystemUtil.hideBottomNav(SplashActivity.this);

    }

    @Override
    protected void onResume() {
        super.onResume();
        SystemUtil.hideBottomNav(SplashActivity.this);
    }

    private boolean isVersionM() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M;
    }

    private void checkAndRequestPermissions() {
        mMissPermissions.clear();
        for (String permission : REQUIRED_PERMISSION_LIST) {
            int result = ContextCompat.checkSelfPermission(this, permission);
            if (result != PackageManager.PERMISSION_GRANTED) {
                mMissPermissions.add(permission);
            }
        }
        // check permissions has granted
        if (mMissPermissions.isEmpty()) {
            // startMainActivity();
        } else {
            ActivityCompat.requestPermissions(this,
                    mMissPermissions.toArray(new String[mMissPermissions.size()]),
                    REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(final int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE) {
            for (int i = grantResults.length - 1; i >= 0; i--) {
                if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    mMissPermissions.remove(permissions[i]);
                }
            }
        }
        // Get permissions success or not
        if (mMissPermissions.isEmpty()) {
            //
        } else {
            Toast.makeText(SplashActivity.this, "get permissions failed,exiting...", Toast.LENGTH_SHORT).show();
            SplashActivity.this.finish();
        }
    }

    private void startMainActivity() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                startActivity(new Intent(SplashActivity.this, CustomCameraActivity.class));
//                SplashActivity.this.finish();
            }
        }, 10);
    }

    private void initView() {
        exit_btn.setOnClickListener(view -> {
            this.getPackageManager().clearPackagePreferredActivities(this.getPackageName());
            finish();
        });
        settings_btn.setOnClickListener(view -> startSettings());
        scamera_btn.setOnClickListener(view -> startMainActivity());

        findViewById(R.id.btn_1).setOnClickListener(view -> startMainActivity());
        findViewById(R.id.btn_2).setOnClickListener(view -> {
            startActivity(new Intent(SplashActivity.this, USBCameraActivity.class));
        });
        findViewById(R.id.btn_3).setOnClickListener(view -> {
            startActivity(new Intent(SplashActivity.this, AudioActivity.class));
        });
        findViewById(R.id.btn_4).setOnClickListener(view -> {
            startActivity(new Intent(SplashActivity.this, PortraitActivity.class));
        });
        findViewById(R.id.btn_5).setOnClickListener(view -> {
            startActivity(new Intent(SplashActivity.this, USBCameraFrameActivity.class));
        });
        findViewById(R.id.btn_6).setOnClickListener(view -> {
            startActivity(new Intent(SplashActivity.this, StreamDecodeActivity.class));
        });


        view_light.setImageResource(imgs[0]);
        view_light.setOnClickListener(view -> {
            resetView();
            view_light.setImageResource(imgs[0]);
            view_top.setImageResource(imgs[8]);
            tv_time.setVisibility(View.VISIBLE);
            tv_date.setVisibility(View.VISIBLE);
            screen_layout.setBackgroundResource(R.mipmap.bg_black);
        });
        view_tep.setOnClickListener(view -> {
            resetView();
            view_tep.setImageResource(imgs[2]);
            view_top.setImageResource(imgs[9]);
            screen_layout.setBackgroundResource(R.mipmap.bg_green);
        });
        view_weather.setOnClickListener(view -> {
            resetView();
            view_weather.setImageResource(imgs[4]);
            view_top.setImageResource(imgs[10]);
            screen_layout.setBackgroundResource(R.mipmap.bg_black);
        });
        view_water.setOnClickListener(view -> {
            resetView();
            view_water.setImageResource(imgs[6]);
            view_top.setImageResource(imgs[11]);
            screen_layout.setBackgroundResource(R.mipmap.bg_blue);
        });

    }

    private void resetView() {
        tv_time.setVisibility(View.GONE);
        tv_date.setVisibility(View.GONE);
        view_light.setImageResource(imgs[1]);
        view_tep.setImageResource(imgs[3]);
        view_weather.setImageResource(imgs[5]);
        view_water.setImageResource(imgs[7]);
    }

    private void startSettings() {
        Intent intent = new Intent(Settings.ACTION_SETTINGS);
        startActivity(intent);
    }


}

