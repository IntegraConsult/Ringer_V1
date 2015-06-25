package com.twilio.example.basicphone;

/**
 * Created by admin on 19/06/2015.
 */

import android.app.Activity;
import android.content.Context;
import android.media.MediaPlayer;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;

public class device {
    private Context mainContext;
    public float leftVolume;
    public float rightVolume;
    private String TAG = "Ringer";


    Map<String, Integer> map = new HashMap<String, Integer>();


    MediaPlayer mp;

    public device(Context context) {
        // this is like onCreate
        this.mainContext = context;

        // map the sounds
        map.put("hello", R.raw.helloworld);
        map.put("0", R.raw.key0);
        map.put("1", R.raw.key1);
        map.put("2", R.raw.key2);
        map.put("3", R.raw.key3);
        map.put("4", R.raw.key4);
        map.put("5", R.raw.key5);
        map.put("6", R.raw.key6);
        map.put("7", R.raw.key7);
        map.put("8", R.raw.key8);
        map.put("9", R.raw.key9);
        map.put("*", R.raw.keyasterix);
        map.put("#", R.raw.keypound);
        map.put("ring", R.raw.ringtone);
        map.put("+", R.raw.key0);
        // set the volume
        leftVolume = rightVolume = (float) 0.2;

    }


    public void play(String sound) {
        int resourceId;
        //Log.d(TAG, "phoneSounds play " + sound);
        resourceId = map.get(sound);
        //Log.d(TAG, "phoneSounds resource " + resourceId);
        if (resourceId != 0) {
            mp = MediaPlayer.create(mainContext.getApplicationContext(), resourceId);
            mp.setVolume(leftVolume, rightVolume);
            mp.start();

        }


    }

    /*
    mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener()

    {
        @Override
        public void onCompletion(MediaPlayer mp)
        {
            String temp;
            temp = number.substring(1,number.length());
            playString(temp);
        }
    });



    public void playString(String number){

         if (number.length() > 0) {
             String sound = number.substring(0, 1);
             Log.d(TAG, "phoneStrings play " + sound);
             int resourceId = map.get(sound);
             Log.d(TAG, "phoneSounds resource " + resourceId);
             if (resourceId != 0) {
                 mp = MediaPlayer.create(context, resourceId);
                 mp.setVolume(leftVolume, rightVolume);
                 mp.start();
             }
         }
    }
    */
}

