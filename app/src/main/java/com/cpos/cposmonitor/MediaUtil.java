package com.cpos.cposmonitor;

import android.app.Service;
import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Vibrator;

public class MediaUtil {

    private static MediaPlayer mMediaPlayer = null;
    private static boolean mRepeat = false;

    private static MediaPlayer.OnCompletionListener mComplte = null;
    private static MediaPlayer.OnErrorListener mError = null;
    MediaUtil(Context context)
    {
        try {

            mComplte = new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mediaPlayer) {

                }
            };

            mError =  new MediaPlayer.OnErrorListener() {
                @Override
                public boolean onError(MediaPlayer mp, int what, int extra) {
                    //此处返回值需要为true，不然还是会执行OnCompletionListener中的onCompletion方法
                    return true;
                }
            };

           // mMediaPlayer.setOnCompletionListener(mComplte);
           // mMediaPlayer.setOnErrorListener(mError);

         } catch (Exception e) {
            e.printStackTrace();
        }

    }
    //开始播放
    public static void playRing(Context context){
        try {
            //用于获取手机默认铃声的Uri
            Uri alert = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
            if (mMediaPlayer == null) mMediaPlayer = new MediaPlayer();

            mMediaPlayer.setDataSource(context, alert);
            //告诉mediaPlayer播放的是铃声流
            mMediaPlayer.setAudioStreamType(AudioManager.STREAM_RING);
            mMediaPlayer.setLooping(true);
            mMediaPlayer.prepare();
            mMediaPlayer.start();
            // mMediaPlayer.setOnCompletionListener(mComplte);
            // mMediaPlayer.setOnErrorListener(mError);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    //停止播放
    public static void stopRing(){

        try {
             if (mMediaPlayer != null){
                if (mMediaPlayer.isPlaying()){
                    mMediaPlayer.stop();
                    mMediaPlayer.release();
                    mMediaPlayer = null;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }


   private static boolean isVirating = false;

    public static void vibrate(Context context, long milliseconds) {
        Vibrator vib = (Vibrator) context.getSystemService(Service.VIBRATOR_SERVICE);
        if(vib.hasVibrator()){  //判断手机硬件是否有振动器
            vib.vibrate(milliseconds);
        }
    }

      //让手机以我们自己设定的pattern[]模式振动
      //long pattern[] = {1000, 20000, 10000, 10000, 30000};
// 开始震动
// VirateUtil.virate(context, new long[]{100, 200, 100, 200}, 0);
    public static void vibrateStart(Context context, long[] pattern, int repeat) {
        if (isVirating == false) {

            Vibrator vib = (Vibrator) context.getSystemService(Service.VIBRATOR_SERVICE);
            if (vib.hasVibrator()) {
                vib.vibrate(pattern, repeat);
            }
            isVirating = true;
        }
    }

    // 取消震动

    public static void virateCancle(Context context){
        if (isVirating) {
            //关闭震动
            Vibrator vib = (Vibrator) context.getSystemService(Service.VIBRATOR_SERVICE);
            vib.cancel();
            isVirating = false;
        }
    }

}

