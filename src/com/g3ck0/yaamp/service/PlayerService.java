package com.g3ck0.yaamp.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import com.g3ck0.yaamp.R;
import com.g3ck0.yaamp.service.utility.LocalBinder;
import com.g3ck0.yaamp.service.utility.SongManager;

import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

public class PlayerService extends Service {
	private static final String TAG = "yaamPlayerService";
	private int NOTIFICATION = R.string.service_name_identifier;
	private boolean isShuffleActive = false;
	private SongManager songManager;
	
	public MediaPlayer mediaPlayer;
	
	public void playSong(String songUri){
		try{
			mediaPlayer.reset();
			mediaPlayer.setDataSource(songUri);
			Log.i(TAG,"songUri: '" + songUri +"'");
			mediaPlayer.prepare();
			mediaPlayer.start();
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
			playSong(songManager.getSongUri(randomSong));
		}
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		return new LocalBinder<PlayerService>(this);
	}
	
	@Override
	public void onCreate(){
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
		mediaPlayer.stop();
	}
	
	@Override
	public void onStart(Intent intent, int startId){
		//mediaPlayer.start();
	}

	public boolean isShuffleActive() {
		return isShuffleActive;
	}

	public void setShuffleActive(boolean isShuffleActive) {
		this.isShuffleActive = isShuffleActive;
	}
	
}
