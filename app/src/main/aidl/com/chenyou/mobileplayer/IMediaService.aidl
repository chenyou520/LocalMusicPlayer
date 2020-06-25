// IMediaService.aidl
package com.chenyou.mobileplayer;

// Declare any non-default types here with import statements

interface IMediaService {
    /**
     * Demonstrates some basic types that you can use as parameters
     * and return values in AIDL.
     */
    void basicTypes(int anInt, long aLong, boolean aBoolean, float aFloat,
            double aDouble, String aString);

void openAudio(int position);

	 void start();

	 void pause();

	 void next();

	 void pre();

	 int getPlaymode();

	 void setPlaymode(int playmode);

	 int getCurrentPosition();

	 int getDuration();

	 String getName();

	 String getArtist();

	 void seekTo(int seekto);

	 boolean isPlaying();

	 void notifyChange(String action);

	 String getAudioPath();

	 int getAudioSessionId();
}
