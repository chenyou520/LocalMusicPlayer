package com.chenyou.localmusicplayer.pager;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.chenyou.localmusicplayer.R;
import com.chenyou.localmusicplayer.activity.AudioPlayer;
import com.chenyou.localmusicplayer.adapter.VideoPagerAdapter;
import com.chenyou.localmusicplayer.domain.MediaItem;
import com.chenyou.localmusicplayer.utils.Utils;

import java.util.ArrayList;

/**
 * 本地音乐
 */
public class AudioPager extends BasePager {
    private ListView lvAudioPager;
    private TextView tvNomedia;
    private ProgressBar pbLoading;

    private Utils mUtils;
    private ArrayList<MediaItem> mMediaItems;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            //主线程
            if (mMediaItems != null && mMediaItems.size() > 0) {
                tvNomedia.setVisibility(View.GONE);
                pbLoading.setVisibility(View.GONE);
                //设置适配器
                lvAudioPager.setAdapter(new VideoPagerAdapter(mContext, mMediaItems, false));
            } else {
                tvNomedia.setVisibility(View.VISIBLE);
                pbLoading.setVisibility(View.GONE);
            }
        }
    };

    public AudioPager(Context context) {
        super(context);
        mUtils = new Utils();
    }

    @Override
    public View initView() {

        View view = View.inflate(mContext, R.layout.audio_pager, null);
        lvAudioPager = (ListView) view.findViewById(R.id.lv_audio_pager);
        tvNomedia = (TextView) view.findViewById(R.id.tv_nomedia);
        pbLoading = (ProgressBar) view.findViewById(R.id.pb_loading);
        //设置点击事件
        lvAudioPager.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                //创建
                Intent intent = new Intent(mContext, AudioPlayer.class);
                intent.putExtra("position", position);//播放列表中的某个音频
                mContext.startActivity(intent);
            }
        });
        return view;
    }

    @Override
    public void initData() {
        super.initData();
        getData();
    }

    private void getData() {
        new Thread() {
            @Override
            public void run() {
                super.run();
                mMediaItems = new ArrayList<MediaItem>();
                ContentResolver contentResolver = mContext.getContentResolver();
                Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                String[] objects = {
                        MediaStore.Audio.Media.DISPLAY_NAME,//在Sdcard显示的名称
                        MediaStore.Audio.Media.DURATION,//视频的长度
                        MediaStore.Audio.Media.SIZE,//视频文件大小
                        MediaStore.Audio.Media.DATA,//视频的绝对地址
                        MediaStore.Audio.Media.ARTIST
                };
                Cursor cursor = contentResolver.query(uri, objects, null, null, null);
                if (cursor != null) {
                    while (cursor.moveToNext()) {
                        MediaItem mediaItem = new MediaItem();

                        String name = cursor.getString(0);
                        mediaItem.setName(name);

                        long duration = cursor.getLong(1);
                        mediaItem.setDuration(duration);

                        long size = cursor.getLong(2);
                        mediaItem.setSize(size);

                        String data = cursor.getString(3);
                        mediaItem.setData(data);

                        String artist = cursor.getString(4);
                        mediaItem.setArtist(artist);

                        //把视频添加到列表中
                        mMediaItems.add(mediaItem);
                    }
                    cursor.close();
                }
                mHandler.sendEmptyMessage(0);
            }
        }.start();
    }
}
