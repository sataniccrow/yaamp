package com.g3ck0.yaamp.service;

import java.io.IOException;
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
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

public class PlayerService extends Service {
	private static final String TAG = "yaamPlayerService";
	private static final int ID = 6969;
	private boolean isShuffleActive = false;
	private NotificationManager mNM;
	private SongManager songManager;
	private Notification notification;
	private PendingIntent pendingActivity;
	private PhoneStateListener phoneStateListener;
	private boolean isPaused = false;
	private TelephonyManager mgr; 
	
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
	
	public void pausePlayer(){
		if(mediaPlayer != null){
			if(mediaPlayer.isPlaying()){
					mediaPlayer.pause();
					Toast.makeText(getApplicationContext(), "Yaamp has been paused", Toast.LENGTH_SHORT);
					isPaused = true;
			}else{
				if(isPaused){
					mediaPlayer.start();
				}
			}
		}
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
	
	public boolean previousSong(){
		boolean result = false;
		if(songManager == null) return false;
		int res = songManager.previousSong();
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
		phoneStateListener = new PhoneStateListener(){
			@Override
			public void onCallStateChanged(int state, String incomingNumber) {
		        if (state == TelephonyManager.CALL_STATE_RINGING) {
		            //Incoming call: Pause music
		        	if(mediaPlayer != null){
		        		if(mediaPlayer.isPlaying()){
		        			mediaPlayer.pause();
		        			isPaused = true;
		        		}
		        	}
		        } else if(state == TelephonyManager.CALL_STATE_IDLE) {
		            //Not in call: Play music
		        	if(mediaPlayer != null && isPaused){
		        		mediaPlayer.start();
		        		isPaused = false;
		        	}
		        	
		        } else if(state == TelephonyManager.CALL_STATE_OFFHOOK) {
		            //A call is dialing, active or on hold
		        	if(mediaPlayer != null){
		        		if(mediaPlayer.isPlaying()){
		        			mediaPlayer.pause();
		        			isPaused = true;
		        		}
		        	}
		        	
		        }
		        super.onCallStateChanged(state, incomingNumber);
		    }
		};
		
		mgr = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
		
		if(mgr != null) {
		    mgr.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
		}
		
		
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
		
		if(mgr != null) {
		    mgr.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE);
		}
		
		stopForeground(true);
	}
	
	@Override
	public void onStart(Intent intent, int startId){
		notification = new Notification(R.drawable.ghost, "yaamp service starting", System.currentTimeMillis());
		notification.flags |= Notification.FLAG_AUTO_CANCEL;
		
		//pendingActivity = PendingIntent.getActivity(this, 0, new Intent(this, PlayerService.class), PendingIntent.FLAG_ONE_SHOT);
		Intent yaampActivity= new Intent(this, YaampActivity.class);
		yaampActivity.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		
		pendingActivity = PendingIntent.getActivity(this, 0, yaampActivity, PendingIntent.FLAG_UPDATE_CURRENT);
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
