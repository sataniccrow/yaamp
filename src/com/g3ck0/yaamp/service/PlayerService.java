package com.g3ck0.yaamp.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import com.g3ck0.yaamp.R;
import com.g3ck0.yaamp.YaampActivity;
import com.g3ck0.yaamp.service.utility.LocalBinder;
import com.g3ck0.yaamp.service.utility.SongManager;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.IBinder;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Toast;

public class PlayerService extends Service {
	private static final String TAG = "yaamPlayerService";
	private static final int ID = 6969;
	private int NOTIFICATION = R.string.service_name_identifier;
	private boolean isShuffleActive = false;
	private NotificationManager mNM;
	private SongManager songManager;
	private Notification notification;
	private PendingIntent pendingActivity;
	
	public MediaPlayer mediaPlayer;
	
	public void playSong(String songUri){
		try{
			mediaPlayer.reset();
			mediaPlayer.setDataSource(songUri);
			Log.i(TAG,"songUri: '" + songUri +"'");
			mediaPlayer.prepare();
			mediaPlayer.start();
			setNotificationMessage("now playing: " +  extractFileName(songUri));
			Toast.makeText(getApplicationContext(), "starting: " + songUri, Toast.LENGTH_SHORT);
		}catch (IOException e) {
			Log.e("yaamPlayerService", e.getMessage());
		}catch (IllegalStateException ex){
			Log.e("yaamPlayerService", "illegalStateEx");
		}catch (Exception e){
			Log.e("yaamPlayerService", "exception");
		}
	}
	
	public void pauseSong(){
		if(mediaPlayer != null){
			if(mediaPlayer.isPlaying()){
				mediaPlayer.pause();
				Toast.makeText(getApplicationContext(), "Yaamp paused", Toast.LENGTH_SHORT);
			}else{
				mediaPlayer.start();
			}
		}
		
	}
	
	public void stopPlayer(){
		mediaPlayer.reset();
		Toast.makeText(getApplicationContext(), "Yaamp stop", Toast.LENGTH_SHORT);
	}
	
	public boolean setSongsMap(HashMap<Integer, String> map){
		boolean result = false;
		try{
			songManager = new SongManager(map);
			result = true;
		}catch (Exception e) {
			Log.e(TAG,e.getMessage());
			return result;
		}
		return result;
	}
	
	public void startShuffle(){
		setShuffleActive(true);
		shuffle();
	}
	
	public void stopShuffle(){
		mediaPlayer.stop();
		setShuffleActive(false);
	}
	
	public boolean lastSong(){
		boolean result = false;
		if(songManager == null) return false;
		int res = songManager.lastSong();
		if(res == -1){
			result = false;
		}else{
			playSong(songManager.getSongUri(res));
			result = true;
		}
		return result;
	}
	
	public void shuffle(){
		int randomSong = songManager.nextSong();
		if(randomSong == -1){
			mediaPlayer.stop();
			Toast.makeText(getApplicationContext(), "all songs have been played", Toast.LENGTH_SHORT);
			return;
		}else{
			String song = songManager.getSongUri(randomSong);
			playSong(song);
			setNotificationMessage("now playing: " + extractFileName(song));
		}
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		return new LocalBinder<PlayerService>(this);
	}
	
	@Override
	public void onCreate(){
		mNM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		mediaPlayer = new MediaPlayer();
		
		
		mediaPlayer.setOnCompletionListener(new OnCompletionListener() {
			
			@Override
			public void onCompletion(MediaPlayer mp) {
				if(isShuffleActive()){
					shuffle();
				}else{
					
				}
			}
		});
		
		mediaPlayer.setLooping(false);
	}
	
	@Override
	public void onDestroy(){
		pendingActivity.cancel();
		mNM.cancel(ID);
		stopForeground(true);
	}
	
	@Override
	public void onStart(Intent intent, int startId){
		notification = new Notification(R.drawable.ghost, "yaamp service starting", System.currentTimeMillis());
		notification.flags |= Notification.FLAG_AUTO_CANCEL;
		
		//pendingActivity = PendingIntent.getActivity(this, 0, new Intent(this, PlayerService.class), PendingIntent.FLAG_ONE_SHOT);
		Intent yaampActivity= new Intent(this, YaampActivity.class);
		yaampActivity.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
		
		pendingActivity = PendingIntent.getActivity(this, 0, yaampActivity, PendingIntent.FLAG_ONE_SHOT);
		notification.setLatestEventInfo(this, getResources().getString(R.string.app_service_name), "Yaamp service is running",pendingActivity);
		mNM.notify(ID,notification);
		startForeground(ID, notification);
	}
			
	public boolean isShuffleActive() {
		return isShuffleActive;
	}

	public void setShuffleActive(boolean isShuffleActive) {
		this.isShuffleActive = isShuffleActive;
	}

	public void setNotificationMessage(String message){
		if(notification != null && pendingActivity != null){
			notification.setLatestEventInfo(this, getResources().getString(R.string.app_service_name), message, pendingActivity);
			mNM.notify(ID, notification);
		}
	}
	
	public String extractFileName(String originalUri){
		String[] tokens = originalUri.split("/");
		if(tokens.length > 0 ) return tokens[tokens.length-1];
		return originalUri;
	}
	
}
