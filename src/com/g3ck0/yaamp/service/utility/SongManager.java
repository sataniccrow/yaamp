package com.g3ck0.yaamp.service.utility;

import java.util.ArrayList;
import java.util.HashMap;

public class SongManager {
	private HashMap<Integer, String> mapForward;
	private HashMap<Integer, String> mapBackward;
	private ArrayList<Integer> playBackList;
	
	public ArrayList<Integer> getPlayBackList() {
		return playBackList;
	}

	public SongManager(HashMap<Integer, String> map){
		this.mapBackward = map;
		this.mapForward = (HashMap<Integer, String>) map.clone();
		playBackList = new ArrayList<Integer>();
	}
	
	public int randomSongChooser(){
		int randomNumber = (int)(Math.random()*mapForward.size());
		int offset = 0;

		while(mapForward.size() > 0){
			if((randomNumber - offset) > 0  && (randomNumber + offset) > mapForward.size()){
				//do only left pick
				if(mapForward.get(new Integer(randomNumber - offset)) != null){
					return randomNumber - offset;
				}
			}else if((randomNumber - offset) <= 0  && (randomNumber + offset) < mapForward.size()){
				//do only right pick
				if(mapForward.get(new Integer(randomNumber + offset)) != null){
					return randomNumber + offset;
				}
			}else {
				//do right and left pick
				if(mapForward.get(new Integer(randomNumber + offset)) != null){
					return randomNumber + offset;
				}else if(mapForward.get(new Integer(randomNumber - offset)) != null){
					return randomNumber - offset;
				}
			}
			offset++;
		}
		
		return -1;
	}
	
	public int nextSong(){
		int random =randomSongChooser();
		if(mapForward.size() > 0){
			if(mapForward.get(new Integer(random)) != null) mapForward.remove(new Integer(random));
			playBackList.add(new Integer(random));
			return random;
		}else{
			if(mapBackward.size() > 0){
				switchQueues();
				if(mapForward.get(new Integer(random)) != null) mapForward.remove(new Integer(random));
				playBackList.add(new Integer(random));
				return random;
			}else{
				return -1;
			}
		}
	}
	
	public int lastSong(){
		if(playBackList == null || playBackList.size() == 0 || playBackList.size() < 2) return -1;
		if(mapBackward.get(new Integer(playBackList.get(playBackList.size()-2))) != null){
			playBackList.add(playBackList.get(playBackList.size()-2));
			return (int)(playBackList.get(playBackList.size()-1));
		}
		return -1;
	}
	
	public void switchQueues(){
		if(mapForward != null && mapBackward != null){
			mapForward = (HashMap<Integer, String>) mapBackward.clone();
		}
	}
	
	public String getSongUri(int index){
		if(mapBackward != null){
			return (mapBackward.get(new Integer(index)) != null? mapBackward.get(new Integer(index)) : ""); 
		}
		return "";
	}
		

}