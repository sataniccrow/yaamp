package com.g3ck0.yaamp;

import java.util.ArrayList;
import java.util.HashMap;

import com.g3ck0.yaamp.R;
import com.g3ck0.yaamp.R.id;
import com.g3ck0.yaamp.service.PlayerService;
import com.g3ck0.yaamp.service.utility.LocalBinder;
import com.g3ck0.yaamp.service.utility.SongManager;

import android.app.Activity;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

public class YaampActivity extends Activity {
    
	private static final String TAG = "yaamActivity";
	private final String SORT_ORDER = MediaStore.Audio.Media.DATE_ADDED + " DESC";
	private final String [] STAR= {"*"};
	private final String[] SUMMARY = {MediaStore.Audio.Media._ID, 
			MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.DURATION,
			MediaStore.Audio.Media.DATA, MediaStore.Audio.Media.ALBUM_ID,
			MediaStore.Audio.Media.ARTIST};
	private Context context;
    private LocalBinder<PlayerService> localBinder;
    private PlayerService mBoundService;
    private Intent intent;
    private Cursor cursor;
    private boolean isBound = false;
	private ListView listView;
	private ArrayAdapter<String> arrayAdapter;
	private HashMap<Integer, String> map = new HashMap<Integer, String>();
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	context = this;
        super.onCreate(savedInstanceState);
        
        startYaampService();
        
        setContentView(R.layout.main);
        listView = (ListView) findViewById(R.id.listViewSongsList);
        listView.setOnItemLongClickListener(new OnItemLongClickListener() {

			@Override
			public boolean onItemLongClick(AdapterView<?> arg0, View v, int position, long id) {
				if(isBound){
					//this check should be useless
					if(cursor != null){
						cursor.moveToPosition(position);
						
						String songTitle 	= cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE));
						String artist = (((cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST)) == null) 
	    						|| cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST)).equals("")
	    						|| cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST)).equals("<unknown>")) ? 
    									"unknown artist" : cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST)));
						long songLenght 	= cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Media.DURATION));
						long albumId 		= cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID));
						
						setOverlay(new String[]{songTitle, albumId+"", SongManager.getSongLenght(songLenght),artist});
						
						return true;
					}else{
						Log.w(TAG, "no such item");						
						
						return false;
					}						
				}else{
					Toast.makeText(context, "no service found", Toast.LENGTH_SHORT);
					Log.w(TAG, "no service has been bound");
					
					return false;
				}
			}
        	
		});
        
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
					Log.i(TAG, "no service has been bound");
				}
			}
		});
        
        cursor = managedQuery(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, SUMMARY, MediaStore.Audio.Media.DATA + "<> ''", null, SORT_ORDER);
        //MediaStore.Audio.Media.DATA cursor.getString(3)
        int count =cursor.getCount();
        fillSongsList(cursor);
        //Toast.makeText(this, "yaamp found " + count + " audio file(s)", Toast.LENGTH_LONG).show();
        
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.layout.menu, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.closeApplication: 
            	stopYaampService();
            	break;
        }
        return true;
    }
    
    public void startShuffle(){
    	if(isBound && map != null && map.size() > 0){
    		if(mBoundService.setSongsMap(map)){
    			mBoundService.setShuffleActive(true);
    			mBoundService.shuffle();
    		}else{
    			Toast.makeText(context, "Yaamp cant shuffle", Toast.LENGTH_SHORT);
    		}
    	}
    }
    
    public void nextSong(View w){
    	if(isBound){
    		if(!mBoundService.isShuffleActive()){
    			startShuffle();
    		}else{
    			mBoundService.shuffle();
    		}
    	}else{
			Toast.makeText(this, "Yaamp cant start playing", Toast.LENGTH_SHORT);
		}
    }
    
    public void previousSong(View v){
    	if(isBound){
    		if(!mBoundService.previousSong()){
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
    		songsName.add(
    				cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE)) + " - " +
    				(((cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST)) == null) 
    						|| cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST)).equals("")
    						|| cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST)).equals("<unknown>")
    						) ? 
    					"no artist" : cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST)
    				)));
    		map.put(new Integer(cursor.getPosition()), cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA)));
    		cursor.moveToNext();
    	}
    	
    	arrayAdapter = new ArrayAdapter<String>(context, R.layout.song, songsName);
    	listView.setAdapter(arrayAdapter);
    }
    
    public void startYaampService (){
    	intent = new Intent(context,PlayerService.class);
    	startService(intent);
    	bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }
    
    public void stopYaampService(){
    	if(intent != null){
    		if(isBound){
    			mBoundService.stopPlayer();
    			mBoundService.onDestroy();
    			isBound = false;
    			stopService(intent);
    			unbindService(mConnection);
    		}
    	}    	
    	cursor.close(); 
    	System.runFinalizersOnExit(true);
    	 System.exit(0);
    }
    
    public void stopPlayer(View v){
    	if(isBound){
    		mBoundService.stopPlayer();
    	}
    }
    
    public void pausePlayer(View v){
    	if(isBound){
    		mBoundService.pausePlayer();
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
    		unbindService(mConnection);
    		isBound = false;
    	}
    	cursor.close();
    	super.onDestroy();
    }
    
    public void setOverlay(String[] text){
    	final Dialog dialog = new Dialog(context);

    	dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		dialog.setContentView(R.layout.mydialog);
		
		//setting cover only if it exists
		if(text[1] != null){	
			Bitmap cover = SongManager.getCover(context, Integer.valueOf(text[1]).intValue());
			if(cover!= null){
				((ImageView) dialog.findViewById(id.selectedSongPortrait)).setImageBitmap(cover);				
			}
		}
		
		((TextView) dialog.findViewById(id.titleSong)).setText((text[0] != null) ? text[0] : "unknown title");
		((TextView) dialog.findViewById(id.generalInfoSong)).setText((text[2] != null) ? text[2] : "no info about");
		((TextView) dialog.findViewById(id.artistSong)).setText((text[3] != null) ? text[3] : "unknown artist");
		
				
		dialog.show();
    }
    
    private ServiceConnection mConnection = new ServiceConnection() {
		
    	@Override
    	public void onServiceConnected(ComponentName name, IBinder service) {
    		localBinder = (LocalBinder<PlayerService>) service;
    		mBoundService = localBinder.getService();
    		isBound = true;	
    	}
    	
		@Override
		public void onServiceDisconnected(ComponentName name) {
			mBoundService = null;
			isBound = false;
		}
	};
}
