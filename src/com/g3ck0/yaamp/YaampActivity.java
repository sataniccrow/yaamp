package com.g3ck0.yaamp;

import java.util.ArrayList;
import java.util.HashMap;

import com.g3ck0.yaamp.R;
import com.g3ck0.yaamp.R.layout;
import com.g3ck0.yaamp.service.PlayerService;
import com.g3ck0.yaamp.service.utility.LocalBinder;

import android.app.Activity;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.drm.DrmStore.Playback;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

public class YaampActivity extends Activity {
    
	private static final String TAG = "yaamActivity";
	private Context context;
    private LocalBinder<PlayerService> localBinder;
    private PlayerService mBoundService;
    private Intent intent;
    private Cursor cursor;
    private ArrayList<String> songsList = new ArrayList<String>();
    private boolean isBound = false;
	private final String SORT_ORDER = MediaStore.Audio.Media.DATE_ADDED + " DESC";
	private final String[] SUMMARY = {MediaStore.Audio.Media._ID, 
			MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.DURATION, MediaStore.Audio.Media.DATA};
	private ListView listView;
	private ArrayAdapter<String> arrayAdapter;
	private HashMap<Integer, String> map = new HashMap<Integer, String>();
	private final String [] STAR= {"*"};
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	context = this;
        super.onCreate(savedInstanceState);
        
        startYaampService();
        
        setContentView(R.layout.main);
        listView = (ListView) findViewById(R.id.listViewSongsList);
        
        listView.setOnItemClickListener(new  OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View v, int position,
					long id) 
			{
				if(isBound){
					//this check should be useless
					if(cursor != null){
						cursor.moveToPosition(position);
						String songUri = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA));
						if(!songUri.equals("")){
							if(mBoundService.isShuffleActive()){
								mBoundService.setShuffleActive(false);
								Toast.makeText(context, "shuffle mode OFF", Toast.LENGTH_SHORT);
							}
							mBoundService.playSong(songUri);
							
						}else{
							Log.w(TAG, "no such item");						
						}
						
					}
				}else{
					Toast.makeText(context, "no service found", Toast.LENGTH_SHORT);
					Log.w(TAG, "no service has been bound");
				}
			}
		});
		
        cursor = managedQuery(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, SUMMARY, MediaStore.Audio.Media.DATA + "<> ''", null, SORT_ORDER);
        //MediaStore.Audio.Media.DATA cursor.getString(3)
        int count =cursor.getCount();
        fillSongsList(cursor);
        
        Toast.makeText(this, "yaamp found " + count + " audio file(s)", Toast.LENGTH_LONG).show();
        
    }
    
    public void startShuffle(View w){
    	if(isBound && map != null && map.size() > 0){
    		if(mBoundService.setSongsMap(map)){
    			mBoundService.setShuffleActive(true);
    			mBoundService.shuffle();
    		}else{
    			Toast.makeText(context, "Yaamp cant shuffle", Toast.LENGTH_SHORT);
    		}
    	}
    }
    
    public void lastSong(View v){
    	if(isBound && map != null && map.size() > 0){
    		if(!mBoundService.lastSong()){
    			Toast.makeText(this, "No previous song available", Toast.LENGTH_SHORT);
    		}
    	}else{
			Toast.makeText(this, "Yaamp cant get last song", Toast.LENGTH_SHORT);
		}
    }
    
    public void fillSongsList(Cursor songsListCursor){
    	ArrayList<String> songsName = new ArrayList<String>();
    	cursor.moveToFirst();
    	
    	while(!cursor.isAfterLast()){
    		songsName.add(cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE)));
    		map.put(new Integer(cursor.getPosition()), cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA)));
    		String name = cursor.getString(1);
    		cursor.moveToNext();
    	}
    	
    	arrayAdapter = new ArrayAdapter<String>(context, R.layout.song, songsName);
    	listView.setAdapter(arrayAdapter);
    }
    
    public void startYaampService (){
    	Toast.makeText(this, "yaamp service start", Toast.LENGTH_LONG).show();
    	intent = new Intent(context,PlayerService.class);
    	startService(intent);
    	bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }
    
    public void stopYaampService(View v){
    	Toast.makeText(this, "yaamp service stop", Toast.LENGTH_SHORT);
    	if(intent != null){
    		if(isBound){
    			mBoundService.stopPlayer();
    			isBound = false;
    			unbindService(mConnection);
    		}
    	}
    	
    	cursor.close(); 
    	System.runFinalizersOnExit(true);
    	 System.exit(0);
    }
    
    public void stopPlayer(View v){
    	Toast.makeText(this, "yaamp stop playing", Toast.LENGTH_LONG).show();
    	if(isBound){
    		mBoundService.stopPlayer();
    	}
    }
    
    public void emptySongsCleaner(){
		Uri playlist_uri= MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI;    
		Cursor deletingCursor= managedQuery(playlist_uri, STAR, null,null,null);
		deletingCursor.moveToFirst();
		for(int r= 0; r<deletingCursor.getCount(); r++, deletingCursor.moveToNext()){
			int i = deletingCursor.getInt(0);
			int l = deletingCursor.getString(1).length();
			if(l>0){
				// keep any playlists with a valid data field, and let me know
			}else{
				// delete any play-lists with a data length of '0'
				Uri uri = ContentUris.withAppendedId(MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI, i);
				getContentResolver().delete(uri, null, null);
			}
		}       
		deletingCursor.close();
	}
    
    @Override
    public void onDestroy(){
    	if(isBound){
    		Toast.makeText(getApplicationContext(), "onDestroy has been called", Toast.LENGTH_SHORT);
    		unbindService(mConnection);
    		isBound = false;
    	}
    	super.onDestroy();
    }
    
    private ServiceConnection mConnection = new ServiceConnection() {
		
    	@Override
    	public void onServiceConnected(ComponentName name, IBinder service) {
    		localBinder = (LocalBinder<PlayerService>) service;
    		mBoundService = localBinder.getService();
    		Toast.makeText(context, "onBind-connect", Toast.LENGTH_SHORT);
    		isBound = true;
    	}
    	
		@Override
		public void onServiceDisconnected(ComponentName name) {
			mBoundService = null;
			isBound = false;
			Toast.makeText(context, "onBind-disconnect", Toast.LENGTH_SHORT);			
		}
	};
}