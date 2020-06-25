package com.chenyou.localmusicplayer.service;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.os.RemoteException;
import android.provider.MediaStore;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.chenyou.localmusicplayer.R;
import com.chenyou.localmusicplayer.activity.AudioPlayer;
import com.chenyou.localmusicplayer.domain.MediaItem;
import com.chenyou.mobileplayer.IMediaService;

import org.greenrobot.eventbus.EventBus;

import java.io.IOException;
import java.util.ArrayList;

/**
 * 作用：音乐播放服务
 */
public class MediaService extends Service {


    /**
     * 顺序播放-默认的播放
     */
    public static final int REPEAT_ORDER = 1;

    /**
     * 单曲循环
     */
    public static final int REPEAT_SINGLE = 2;


    /**
     * 全部循环
     */
    public static final int REPEAT_ALL = 3;
    /**
     * 当播放音乐成功的时候动作
     */
    public static final String OPEN_AUDIO = "com.atguigu.mobileplayer_OPEN_AUDIO";
    /**
     * 播放模式
     */
    private int playmode = REPEAT_ORDER;
    /**
     * 音频列表
     */
    private ArrayList<MediaItem> mediaItems;
    /**
     * 一首音乐的信息
     */
    private MediaItem mediaItem;
    /**
     * 播放器
     */
    private MediaPlayer mediaPlayer;
    /**
     * 当前列表的播放位置
     */
    private int position;
    /**
     * 通知服务管理
     */
    private NotificationManager manager;

    private IMediaService.Stub stub = new IMediaService.Stub() {
        //拿到Service的实例
        MediaService service = MediaService.this;

        @Override
        public void basicTypes(int anInt, long aLong, boolean aBoolean, float aFloat, double aDouble, String aString) throws RemoteException {

        }

        @Override
        public void openAudio(int position) throws RemoteException {
            service.openAudio(position);
        }

        @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
        @Override
        public void start() throws RemoteException {
            service.start();
        }

        @Override
        public void pause() throws RemoteException {
            service.pause();
        }

        @Override
        public void next() throws RemoteException {
            service.next();
        }

        @Override
        public void pre() throws RemoteException {
            service.pre();
        }

        @Override
        public int getPlaymode() throws RemoteException {
            return service.getPlaymode();
        }

        @Override
        public void setPlaymode(int playmode) throws RemoteException {
            service.setPlaymode(playmode);
        }

        @Override
        public int getCurrentPosition() throws RemoteException {
            return service.getCurrentPosition();
        }

        @Override
        public int getDuration() throws RemoteException {
            return service.getDuration();
        }

        @Override
        public String getName() throws RemoteException {
            return service.getName();
        }

        @Override
        public String getArtist() throws RemoteException {
            return service.getArtist();
        }

        @Override
        public void seekTo(int seekto) throws RemoteException {
            service.seekTo(seekto);
        }

        @Override
        public boolean isPlaying() throws RemoteException {
            return service.isPlaying();
        }

        @Override
        public void notifyChange(String action) throws RemoteException {
            service.notifyChange(action);
        }

        @Override
        public String getAudioPath() throws RemoteException {
            return service.getAudioPath();
        }

        @Override
        public int getAudioSessionId() throws RemoteException {
            return service.getAudioSessionId();
        }

    };

    private int getAudioSessionId() {
        return mediaPlayer.getAudioSessionId();
    }

    /**
     * 音频播放的绝对路径
     *
     * @return
     */
    private String getAudioPath() {
        return mediaItem.getData();
    }

    /**
     * 根据不同的动作发广播
     *
     * @param action
     */
    private void notifyChange(String action) {
        EventBus.getDefault().post(new MediaItem());
    }

    /**
     * 是否在播放中
     *
     * @return
     */
    private boolean isPlaying() {
        return mediaPlayer.isPlaying();
    }

    /**
     * 音频的拖动
     *
     * @param seekto
     */
    private void seekTo(int seekto) {
        mediaPlayer.seekTo(seekto);
    }

    /**
     * 得到演唱者
     *
     * @return
     */
    private String getArtist() {
        return mediaItem.getArtist();
    }

    /**
     * 得到歌曲的名称
     *
     * @return
     */
    private String getName() {
        return mediaItem.getName();
    }

    /**
     * 得到当前的总时长
     *
     * @return
     */
    private int getDuration() {
        return mediaPlayer.getDuration();
    }

    /**
     * 得到当前播放进度
     *
     * @return
     */
    private int getCurrentPosition() {
        return mediaPlayer.getCurrentPosition();
    }

    /**
     * 设置播放模式
     *
     * @param playmode
     */
    private void setPlaymode(int playmode) {
        this.playmode = playmode;
        if (playmode == MediaService.REPEAT_SINGLE) {
            mediaPlayer.setLooping(true);
        } else {
            mediaPlayer.setLooping(false);
        }
    }

    /**
     * 得到播放模式
     *
     * @return
     */
    private int getPlaymode() {
        return playmode;
    }

    /**
     * 上一首
     */
    private void pre() {
        setPrePosition();
        openPrePosition();
    }

    private void openPrePosition() {
        int playmode = getPlaymode();
        if (playmode == MediaService.REPEAT_ORDER) {//顺序播放
            if (position >= 0) {
                openAudio(position);
            } else {
                position = 0;
            }
        } else if (playmode == MediaService.REPEAT_SINGLE) {//单曲播放
            if (position >= 0) {
                openAudio(position);
            }
        } else if (playmode == MediaService.REPEAT_ALL) {//全部循环播放
            openAudio(position);
        } else {
            if (position >= 0) {
                openAudio(position);
            } else {
                position = 0;
            }
        }
    }

    private void setPrePosition() {
        int playmode = getPlaymode();
        if (playmode == MediaService.REPEAT_ORDER) {//顺序播放
            position--;
        } else if (playmode == MediaService.REPEAT_SINGLE) {//单曲播放
            position--;
        } else if (playmode == MediaService.REPEAT_ALL) {//全部循环播放
            position--;
            if (position < 0) {
                position = mediaItems.size() - 1;
            }
        } else {
            position--;
        }
    }

    /**
     * 下一首
     */
    private void next() {
        setNextPosition();
        openNextPosition();
    }

    private void openNextPosition() {
        int playmode = getPlaymode();
        if (playmode == MediaService.REPEAT_ORDER) {
            if (position < mediaItems.size()) {
                openAudio(position);
            } else {
                position = mediaItems.size() - 1;
            }
        } else if (playmode == MediaService.REPEAT_SINGLE) {
            if (position < mediaItems.size()) {
                openAudio(position);
            }
        } else if (playmode == MediaService.REPEAT_ALL) {
            openAudio(position);
        } else {
            if (position < mediaItems.size()) {
                openAudio(position);
            } else {
                position = mediaItems.size() - 1;
            }
        }
    }

    private void setNextPosition() {
        int playmode = getPlaymode();
        if (playmode == MediaService.REPEAT_ORDER) {
            position++;
        } else if (playmode == MediaService.REPEAT_SINGLE) {
            position++;
        } else if (playmode == MediaService.REPEAT_ALL) {
            position++;
            if (position > mediaItems.size() - 1) {
                position = 0;
            }
        } else {
            position++;
        }
    }

    /**
     * 暂停音乐
     */
    private void pause() {
        mediaPlayer.pause();
        //通知消失掉
        manager.cancel(1);
    }

    /**
     * 播放音乐
     */
    private void start() {
        mediaPlayer.start();
        //弹出通知-点击的时候进入音乐播放器页面
        manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        Intent intent = new Intent(this, AudioPlayer.class);
        intent.putExtra("Notification", true);//从状态栏进入音乐播放页面
        PendingIntent pi = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        @SuppressLint({"NewApi", "LocalSuppress"})
        Notification notification = new Notification.Builder(this)
                .setSmallIcon(R.drawable.notification_music_playing)//设置图标
                .setContentTitle("321音乐")//设置标题
                .setContentText("正在播放：" + getName())//设置内容
                .setContentIntent(pi)
                .setAutoCancel(true)
                .build();//创建
//        notification.flags = Notification.FLAG_ONGOING_EVENT;//发起正在运行事件（活动中）
        notification.flags = Notification.FLAG_AUTO_CANCEL;//用户单击通知后自动消失
        manager.notify(1, notification);
    }

    /**
     * 根据位置打开音乐
     *
     * @param position
     */
    private void openAudio(int position) {
        this.position = position;
        if (mediaItems != null && mediaItems.size() > 0) {
            mediaItem = mediaItems.get(position);
            //把上一次或者正在播放的给释放掉
            if (mediaPlayer != null) {
//                mediaPlayer.release();//释放
                mediaPlayer.reset();//重置
                mediaPlayer = null;
            }

            try {
                mediaPlayer = new MediaPlayer();
                //准备好
                mediaPlayer.setOnPreparedListener(new MyOnPreparedListener());
                //播放完成
                mediaPlayer.setOnCompletionListener(new MyOnCompletionListener());
                //播放失败
                mediaPlayer.setOnErrorListener(new MyOnErrorListener());
                //设置地址
                mediaPlayer.setDataSource(mediaItem.getData());
                mediaPlayer.prepareAsync();//本地资源和网络资源都行

            } catch (IOException e) {
                e.printStackTrace();
            }

        } else {
            //数据还没有加载好
            Toast.makeText(MediaService.this, "数据还没有加载好呢！", Toast.LENGTH_SHORT).show();
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return stub;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        getData();
    }

    private void getData() {
        new Thread() {
            @Override
            public void run() {
                super.run();
                mediaItems = new ArrayList<>();
                ContentResolver contentResolver = getContentResolver();
                Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                String[] objects = {
                        MediaStore.Audio.Media.DISPLAY_NAME,//在Sdcard显示的名称
                        MediaStore.Audio.Media.DURATION,//视频的长度
                        MediaStore.Audio.Media.SIZE,//视频文件大小
                        MediaStore.Audio.Media.DATA,//视频的绝对地址
                        MediaStore.Audio.Media.ARTIST//艺术家
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
                        mediaItems.add(mediaItem);
                    }
                    cursor.close();
                }
            }
        }.start();
    }


    private class MyOnPreparedListener implements MediaPlayer.OnPreparedListener {
        @Override
        public void onPrepared(MediaPlayer mp) {
            start();
            notifyChange(OPEN_AUDIO);
        }
    }

    private class MyOnErrorListener implements MediaPlayer.OnErrorListener {
        @Override
        public boolean onError(MediaPlayer mp, int what, int extra) {
            next();
            return true;
        }
    }

    private class MyOnCompletionListener implements MediaPlayer.OnCompletionListener {
        @Override
        public void onCompletion(MediaPlayer mp) {
            if (playmode == MediaService.REPEAT_SINGLE) {
                openAudio(position);
            } else {
                next();
            }
        }
    }

}
