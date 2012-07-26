package com.g3ck0.yaamp.service.utility;

import java.util.ArrayList;
import java.util.HashMap;

public class SongManager {
	private HashMap<Integer, String> mapForward;
	private HashMap<Integer, String> mapBackward;
	private ArrayList<Integer> playBackList;
	private int playBackListCursor = -1;
	
	public Integer getNextItem(){
		if(playBackListCursor == -1 || playBackList.size() <= 0){
			return -1;
		}else{
			//[ ] [ ] [*]
			//size = 3
			//pointer @ 2
			//check if((pointer + 2) > size)
			if((playBackListCursor+2) <= playBackList.size() ){
				moveAt(playBackListCursor + 1);
				return playBackList.get(playBackListCursor);
			}else{
				return -1;
			}
		}
	}
	
	public Integer getPreviousItem(){
		if(playBackListCursor == -1 || playBackList.size() <= 0){
			return -1;
		}else{
			//[*] [ ] [ ]
			//size = 3
			//pointer @ 0
			//check if((pointer - 1) >= 0)
			if((playBackListCursor - 1) >= 0){
				moveAt(playBackListCursor -1);
				return playBackList.get(playBackListCursor);
			}else{
				return -1;
			}
		}
	}
	
	public void moveAt(int position){
		System.out.println("cursor position: " + position);
		playBackListCursor = position;
	}
	
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
		
		int result = getNextItem();
		
		if(result != -1){
			return result;
		}
		
		int random =randomSongChooser();
		if(mapForward.size() > 0){
			if(mapForward.get(new Integer(random)) != null) mapForward.remove(new Integer(random));
			playBackList.add(new Integer(random));
			moveAt(playBackListCursor+1);
			return random;
		}else{
			if(mapBackward.size() > 0){
				switchQueues();
				if(mapForward.get(new Integer(random)) != null) mapForward.remove(new Integer(random));
				playBackList.add(new Integer(random));
				moveAt(playBackListCursor+1);
				return random;
			}else{
				return -1;
			}
		}
	}
	
	public int previousSong(){
		int result = getPreviousItem();
		
		if(result != -1){
			
			return result;
		}else{
			return -1;
		}
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
		
	public final static String getSongLenght(long length){
		int totSecs = (int)(length/1000);
		int secs = totSecs%60;
		int mins = (int)totSecs/60;
		
		return mins + "m"+ secs + "s";
	}
}