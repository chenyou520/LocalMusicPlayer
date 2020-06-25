package com.chenyou.localmusicplayer.activity;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.media.audiofx.Visualizer;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.chenyou.localmusicplayer.R;
import com.chenyou.localmusicplayer.domain.MediaItem;
import com.chenyou.localmusicplayer.service.MediaService;
import com.chenyou.localmusicplayer.utils.LyricUtils;
import com.chenyou.localmusicplayer.utils.Utils;
import com.chenyou.localmusicplayer.view.BaseVisualizerView;
import com.chenyou.localmusicplayer.view.LyricShowView;
import com.chenyou.mobileplayer.IMediaService;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;

public class AudioPlayer extends Activity implements View.OnClickListener {

    /**
     * 进度更新
     */
    private static final int PROGRESS = 1;
    /**
     * 显示歌词-缓缓往上推移
     */
    private static final int SHOW_LYRIC = 2;

    private RelativeLayout top;
    private ImageView ivIcon;
    private BaseVisualizerView baseVisualizerView;
    private TextView tvName;
    private TextView tvAstist;
    private TextView tvTime;
    private SeekBar seekbarAudio;
    private Button btnAudioPlaymode;
    private Button btnAudioPre;
    private Button btnAudioStartPause;
    private Button btnAudioNext;
    private LyricShowView lyricShowview;

    /**
     * 判断从哪里来
     * true:状态栏点击来的
     * false:从列表点击来的
     */
    private boolean notification;
    private int position;
    private Utils utils;
    private IMediaService mediaService;
    private Visualizer mVisualizer;

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case SHOW_LYRIC://显示歌词-缓缓往上推移
                    try {
                        int currentPosition = mediaService.getCurrentPosition();
                        lyricShowview.setShowNextLyric(currentPosition);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                    removeMessages(SHOW_LYRIC);
                    sendEmptyMessage(SHOW_LYRIC);
                    break;
                case PROGRESS://更新时间
                    try {
                        int currentPosition = mediaService.getCurrentPosition();
                        int duration = mediaService.getDuration();
                        tvTime.setText(utils.stringForTime(currentPosition) + "/" + utils.stringForTime(duration));
                        //跟新进度
                        seekbarAudio.setProgress(currentPosition);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                    handler.removeMessages(PROGRESS);
                    handler.sendEmptyMessageDelayed(PROGRESS, 1000);
                    break;
            }
        }
    };
    private MyReceiver receiver;
    private ServiceConnection con = new ServiceConnection() {
        /**
         * 当Activity和Service连接成功的时候回调这个方法
         * @param name
         * @param service
         */
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mediaService = IMediaService.Stub.asInterface(service);
            ;//转换为代理类

            try {
                if (!notification) {
                    //就可以操作服务了
                    mediaService.openAudio(position);
                } else {
                    //获取数据-要服务发广播
                    mediaService.notifyChange(MediaService.OPEN_AUDIO);
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }

        }

        /**
         * 当Activity和服务断开的时候回调这个方法
         * @param name
         */
        @Override
        public void onServiceDisconnected(ComponentName name) {
            mediaService = null;
        }
    };

    private void findViews() {
        top = (RelativeLayout) findViewById(R.id.top);
        ivIcon = (ImageView) findViewById(R.id.iv_icon);
        baseVisualizerView = (BaseVisualizerView) findViewById(R.id.baseVisualizerView);
        tvName = (TextView) findViewById(R.id.tv_name);
        tvAstist = (TextView) findViewById(R.id.tv_astist);
        tvTime = (TextView) findViewById(R.id.tv_time);
        seekbarAudio = (SeekBar) findViewById(R.id.seekbar_audio);
        btnAudioPlaymode = (Button) findViewById(R.id.btn_audio_playmode);
        btnAudioPre = (Button) findViewById(R.id.btn_audio_pre);
        btnAudioStartPause = (Button) findViewById(R.id.btn_audio_start_pause);
        btnAudioNext = (Button) findViewById(R.id.btn_audio_next);
        lyricShowview = (LyricShowView) findViewById(R.id.lyric_showview);

        btnAudioPlaymode.setOnClickListener(this);
        btnAudioPre.setOnClickListener(this);
        btnAudioStartPause.setOnClickListener(this);
        btnAudioNext.setOnClickListener(this);

        seekbarAudio.setOnSeekBarChangeListener(new MyOnSeekBarChangeListener());
    }

    @Override
    public void onClick(View v) {
        if (v == btnAudioPlaymode) {
            changePlaymode();
        } else if (v == btnAudioPre) {
            try {
                mediaService.pre();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        } else if (v == btnAudioStartPause) {
            try {
                if (mediaService.isPlaying()) {
                    //暂停
                    mediaService.pause();
                    //按钮设置播放状态
                    btnAudioStartPause.setBackgroundResource(R.drawable.btn_audio_start_selector);
                } else {
                    //播放
                    mediaService.start();
                    //按钮设置暂停状态
                    btnAudioStartPause.setBackgroundResource(R.drawable.btn_audio_pause_selector);
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        } else if (v == btnAudioNext) {
            try {
                mediaService.next();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    private void changePlaymode() {
        try {
            int playmode = mediaService.getPlaymode();
            if (playmode == MediaService.REPEAT_ORDER) {
                playmode = MediaService.REPEAT_SINGLE;
            } else if (playmode == MediaService.REPEAT_SINGLE) {
                playmode = MediaService.REPEAT_ALL;
            } else if (playmode == MediaService.REPEAT_ALL) {
                playmode = MediaService.REPEAT_ORDER;
            } else {
                playmode = MediaService.REPEAT_ORDER;
            }
            mediaService.setPlaymode(playmode);
            showPlaymode();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private void showPlaymode() {
        try {
            int playmode = mediaService.getPlaymode();
            if (playmode == MediaService.REPEAT_ORDER) {
                Toast.makeText(AudioPlayer.this, "顺序播放", Toast.LENGTH_SHORT).show();
                btnAudioPlaymode.setBackgroundResource(R.drawable.btn_audio_playmode_normal_selector);
            } else if (playmode == MediaService.REPEAT_SINGLE) {
                Toast.makeText(AudioPlayer.this, "单曲播放", Toast.LENGTH_SHORT).show();
                btnAudioPlaymode.setBackgroundResource(R.drawable.btn_audio_playmode_single_selector);
            } else if (playmode == MediaService.REPEAT_ALL) {
                Toast.makeText(AudioPlayer.this, "全部播放", Toast.LENGTH_SHORT).show();
                btnAudioPlaymode.setBackgroundResource(R.drawable.btn_audio_playmode_all_selector);
            } else {
                Toast.makeText(AudioPlayer.this, "顺序播放", Toast.LENGTH_SHORT).show();
                btnAudioPlaymode.setBackgroundResource(R.drawable.btn_audio_playmode_normal_selector);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio_player);
        findViews();
        initData();
        getData();
        bindAndStartService();
    }

    private void bindAndStartService() {
        Intent intent = new Intent(this, MediaService.class);
        intent.setAction("com.atguigu.mobileplayer_OPENAUDIO");
        bindService(intent, con, Context.BIND_AUTO_CREATE);
        startService(intent);//避免Service被重新创建
    }

    private void getData() {
        notification = getIntent().getBooleanExtra("Notification", false);
        if (!notification) {
            //从列表来的
            position = getIntent().getIntExtra("position", 0);
        }
    }

    private void initData() {
        utils = new Utils();
        //注册广播
        receiver = new MyReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(MediaService.OPEN_AUDIO);//监听打开音乐成功的动作
        registerReceiver(receiver, intentFilter);
        //1.注册EventBus
        EventBus.getDefault().register(this);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(MediaItem mediaItem) {
        //获取视频的名称和演唱者的信息--主线程
        setViewData();
        try {
            seekbarAudio.setMax(mediaService.getDuration());
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        handler.sendEmptyMessage(PROGRESS);
        setPlayAndpauseState();
        showLyric();
        setupVisualizerFxAndUi();
    }

    private void setPlayAndpauseState() {
        try {
            if (mediaService != null) {
                if (mediaService.isPlaying()) {
                    //暂停状态
                    btnAudioStartPause.setBackgroundResource(R.drawable.btn_audio_pause_selector);
                } else {
                    //播放状态
                    btnAudioStartPause.setBackgroundResource(R.drawable.btn_audio_start_selector);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    class MyReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            //获取视频的名称和演唱者的信息--主线程
            setViewData();
            try {
                seekbarAudio.setMax(mediaService.getDuration());
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            handler.sendEmptyMessage(PROGRESS);
        }
    }

    /**
     * 设置歌曲名称和演唱者
     */
    private void setViewData() {
        try {
            tvName.setText(mediaService.getName());
            tvAstist.setText(mediaService.getArtist());
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private void showLyric() {
        LyricUtils lyricUtils = new LyricUtils();
        try {
            String path = mediaService.getAudioPath();
            path = path.substring(0, path.indexOf("."));
            File file = new File(path + ".lrc");
            if (!file.exists()) {
                file = new File(path + ".text");
            }
            //传文件进入解析歌词工具类
            lyricUtils.readLyricFile(file);
            //把解析好的歌词传入显示歌词的控件上
            lyricShowview.setLyrics(lyricUtils.getLyrics());
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        if (lyricUtils.isExistLyric()) {
            handler.sendEmptyMessage(SHOW_LYRIC);
        }
    }

    /**
     * 生成一个VisualizerView对象，使音频频谱的波段能够反映到 VisualizerView上
     */
    private void setupVisualizerFxAndUi() {
        try {
            int audioSessionId = mediaService.getAudioSessionId();
            System.out.println("audioSessionId==" + audioSessionId);
            mVisualizer = new Visualizer(audioSessionId);
            mVisualizer.setCaptureSize(Visualizer.getCaptureSizeRange()[1]);
            // 设置允许波形表示，并且捕获它
            baseVisualizerView.setVisualizer(mVisualizer);
            mVisualizer.setEnabled(true);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mVisualizer.release();
    }

    class MyOnSeekBarChangeListener implements SeekBar.OnSeekBarChangeListener {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if (fromUser) {
                try {
                    mediaService.seekTo(progress);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {

        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (con != null) {
            unbindService(con);
            ;//解绑服务
            con = null;
        }
        //取消注册广播
        if (receiver != null) {
            unregisterReceiver(receiver);
            receiver = null;
        }

        //2.EventBus取消注册
        //把所有的消息和回调移除
        EventBus.getDefault().unregister(this);
    }
}
