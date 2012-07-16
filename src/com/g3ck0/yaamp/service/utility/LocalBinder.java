package com.g3ck0.yaamp.service.utility;

import java.lang.ref.WeakReference;

import com.g3ck0.yaamp.service.PlayerService;

import android.os.Binder;

public class LocalBinder<PlayerService> extends Binder {
	
	private String TAG = "LocalBinder";
	private WeakReference<PlayerService> mService;
	
	public LocalBinder(PlayerService ps){
		mService = new WeakReference<PlayerService>(ps);
	}

	public PlayerService getService(){
		return mService.get();
	}
}
