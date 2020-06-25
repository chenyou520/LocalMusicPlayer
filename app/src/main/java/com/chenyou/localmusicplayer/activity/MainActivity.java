package com.chenyou.localmusicplayer.activity;

import android.os.Bundle;
import android.widget.Toast;

import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.chenyou.localmusicplayer.R;
import com.chenyou.localmusicplayer.pager.AudioPager;
import com.chenyou.localmusicplayer.pager.BasePager;
import com.chenyou.localmusicplayer.view.ReplaceFragment;

/**
 * 主页面
 */
public class MainActivity extends FragmentActivity {

    private AudioPager audioPager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        audioPager = new AudioPager(this);
        setFragment();

    }


    private void setFragment() {
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();//开启事务
        ft.replace(R.id.fl_main, new ReplaceFragment(getBasePager()));
        ft.commit();
    }

    private BasePager getBasePager() {
        BasePager basePager = audioPager;
        if (basePager != null && !basePager.isInitData) {
            basePager.isInitData = true;
            basePager.initData();
        }
        return basePager;
    }

    private long startTime;

    @Override
    public void onBackPressed() {
        if (System.currentTimeMillis() - startTime > 2000) {
            startTime = System.currentTimeMillis();
            Toast.makeText(MainActivity.this, "再点一次退出", Toast.LENGTH_SHORT).show();
            return;
        }
        super.onBackPressed();
    }
}
