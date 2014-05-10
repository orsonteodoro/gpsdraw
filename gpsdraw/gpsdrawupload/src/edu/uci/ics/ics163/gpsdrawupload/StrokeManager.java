package edu.uci.ics.ics163.gpsdrawupload;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * This is the primary class used in this package.  The StrokeManager holds strokes and points and manages uploading them when necessary.
 * If the upload fails for any reason, the StrokeManager holds the points and alerts the callback function if necessary.  
 * Future uploads will attempt to upload the points again.
 * @author djp3
 *
 */
public class StrokeManager implements StrokeManagerCallback{
	
	/**
	 * This is the maximum number of points that is uploaded at once
	 */
	final static int MAX_CHUNK_SIZE = 250;
	
	/**
	 * This is the distance in meters that two consecutive points must be apart in order to be considered different
	 */
	final static double FILTER_DIST = 10.0;
	
	/**
	 * Used internally to hold colors
	 * @author djp3
	 *
	 */
	class Color{
		int r, g, b;
		
		Color(int r,int g, int b){
			this.r = r;
			this.g = g;
			this.b = b;
		}
	}
	
	/** Manage concurrency with this lock **/
	Object strokeLock = new Object();
	Map<String, Set<Point>> strokes;
	Map<String, Color> strokeColors;
	
	/** Who do we tell on completion? **/
	UploadCallback callback;
	
	/**
	 *  The constructor for this class if no callback is desired
	 */
	public StrokeManager(){
		this(null);
	}
	
	/**
	 *  The constructor for this class if callbacks post-upload are desired.
	 */
	public StrokeManager(UploadCallback callback){
		strokes = new HashMap<String,Set<Point>>();
		strokeColors = new HashMap<String,Color>();
		this.callback = callback;
	}
	
	
	/**
	 * Set the color for a stroke.  If the stroke has previously been assigned a color then the 
	 * color is overwritten.  Strokes without colors default to black.
	 * @param strokeName
	 * @param r
	 * @param g
	 * @param b
	 */
	public void setStrokeColor(String strokeName, int r, int g ,int b){
		synchronized(strokeLock){
			Color color = new Color(r,g,b);
			strokeColors.put(strokeName, color);
		}
		
	}
	
	/**
	 * Add a point to a stroke.  If the strokeName exists, the point is appended to the existing stroke 
	 * @param strokeName
	 * @param point
	 */
	public void addPoint(String strokeName, Point point){
		synchronized(strokeLock){
			Set<Point> set = strokes.get(strokeName);
			if(set == null){
				set = new HashSet<Point>();
			}
			if(point != null){
				set.add(point);
			}
			strokes.put(strokeName, set);
		}
	}
	
	/**
	 * Add a set of points to a stroke.  If the strokeName exists, the points are appended to the existing stroke.
	 * @param strokeName
	 * @param points
	 */
	public void addPoints(String strokeName, Set<Point> points){
		synchronized(strokeLock){
			Set<Point> set = strokes.get(strokeName);
			if(set == null){
				set = new HashSet<Point>();
			}
			if(points != null){
				set.addAll(points);
			}
			strokes.put(strokeName, set);
		}
	}
	
	/**
	 * Remove a point from a stroke.  If the stroke doesn't exist, this fails silently.  If the point doesn't exist this fails silently.
	 * If the point is the final one in a stroke, the stroke is removed.
	 * @param strokeName
	 * @param point
	 */
	public void removePoint(String strokeName, Point point){
		synchronized(strokeLock){
			Set<Point> set = strokes.remove(strokeName);
			if(set != null){
				set.remove(point);
				if(set.size() > 0){
					strokes.put(strokeName, set);
				}
			}
		}
	}
	
	/**
	 * 
	 * Remove points from a stroke.  If the stroke doesn't exist, this fails silently.
	 * If the points don't exist this fails silently for those that don't exist.
	 * If the points are the final ones in a stroke, the stroke is removed.
	 * @param strokeName
	 * @param points
	 */
	public void removePoints(String strokeName, Set<Point> points){
		synchronized(strokeLock){
			Set<Point> set = strokes.remove(strokeName);
			if(set != null){
				set.removeAll(points);
				if(set.size() > 0){
					strokes.put(strokeName, set);
				}
			}
		}
	}
	
	
	
	/**
	 * Removes all data from the stroke manager.  This is used internally to manage uploading.
	 * @return
	 */
	Map<String, Set<Point>> removeAllStrokes(){
		synchronized(strokeLock){
			Map<String, Set<Point>> ret = strokes;
			strokes = new HashMap<String, Set<Point>>();
			return ret;
		}
	}
	
	
	/**
	 * This uploads the strokes and points that the StrokeManager knows about to the server.
	 * This is done in batches of MAX_CHUNK_SIZE.  For each batch of MAX_CHUNK_SIZE, a callback is executed (provided
	 * that a callback function was given in the constructor).  After uploading, the points are removed
	 * from the StrokeManager.  If any portion of the upload fails, the points are kept by the StrokeManager.
	 * Subsequent calls to upload will attempt to upload them again. 
	 * 
	 * Upload attempts are asynchronous, so this function will return before all network traffic is completed. 
	 * @param group_name
	 * @param drawing_name
	 * @param r
	 * @param g
	 * @param b
	 */
	public void upload(String group_name,String drawing_name){
		
		Map<String, Set<Point>> tempstrokes;
		
		synchronized(strokeLock){
			
			tempstrokes = removeAllStrokes();
			
			for (Entry<String, Set<Point>> stroke : tempstrokes.entrySet()) {
				
				List<Point> localPoints = new ArrayList<Point>();
				for(Point p:stroke.getValue()){
					localPoints.add(p);
				}
				
				Color strokeColor = strokeColors.get(stroke.getKey());
				if(strokeColor == null){
					strokeColor = new Color(0,0,0);
				}
				
				//Sort points by time
				Collections.sort(localPoints);
				
				try {
					
					while(localPoints.size() > 0){
						JSONObject params = new JSONObject();
						
						params.put("group_name", group_name);
						params.put("drawing_name", drawing_name);
						params.put("stroke_name", stroke.getKey());
						params.put("red", strokeColor.r+"");
						params.put("green", strokeColor.g+"");
						params.put("blue", strokeColor.b+"");
						
						int end = localPoints.size();
						if(end > MAX_CHUNK_SIZE){
							end = MAX_CHUNK_SIZE;
						}
						
						List<Point> chunk = new ArrayList<Point>();
						for(Point p:localPoints.subList(0, end)){
							chunk.add(p);
						}
						
						JSONArray jPoints = new JSONArray();
						
						for(Point p:chunk){
							localPoints.remove(p);
						}
						
						List<Point> filteredChunk = filterPoints(chunk);
						for(Point p: filteredChunk){
							JSONObject point = new JSONObject();
							point.put("time", ""+p.getTime());
							point.put("lat", ""+p.getLat());
							point.put("lng", ""+p.getLng());
							jPoints.put(point);
						}
						params.put("stroke",jPoints);
					
						HashSet<Point> submitUs = new HashSet<Point>(); 
						for(Point p:filteredChunk){
							submitUs.add(p);
						}
						new RESTRequest(params,stroke.getKey(),submitUs,this);
						
					}
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * This removes points that are less than FILTER_DIST m away from each other.  It assumes the input is sorted by time. The first and last point
	 * are always added.
	 * @param points
	 * @return
	 */
	static List<Point> filterPoints(List<Point> points) {
		ArrayList<Point> ret = new ArrayList<Point>();
		if(points == null){
			return ret;
		}
		if(points.size() == 0){
			return ret;
		}
		Point old = null;
		Point now = null;
		boolean addedLast = false;
		for(Point p:points){
			addedLast = false;
			if(old == null){
				old = p;
				ret.add(p);
				addedLast = true;
			}
			else{
				now = p;
				if(Haversine.haversine(old.getLat(), old.getLng(), now.getLat(), now.getLng()) > FILTER_DIST){
					ret.add(now);
					old = now;
					addedLast = true;
				}
				else{
					addedLast = false;
				}
			}
		}
		
		if(!addedLast){
			ret.add(now);
		}
		
		return ret;
	}
	
	/**
	 * Convenience function if you want to know how many strokes are currently in the StrokeManager.
	 * This goes to zero during an upload and may increase again if the upload subsequently fails.
	 * @return
	 */
	public int countStrokes(){
		synchronized(strokeLock){
			return strokes.keySet().size();
		}
	}
	
	/**
	 * Convenience function if you want to know how many points are currently in the StrokeManager
	 * across all strokes.
	 * This goes to zero during an upload and may increase again if the upload subsequently fails.
	 * @return
	 */
	public int countPoints(){
		synchronized(strokeLock){
			int count = 0;
			for( Entry<String, Set<Point>> s:strokes.entrySet()){
				count += s.getValue().size();
			}
			return count;
		}
	}

	@Override
	/** Used by RESTRequest to signal the result of an upload chunk **/
	public void onSuccess(String strokeId, Set<Point> points) {
		if(callback != null){
			callback.onSuccess();
		}
	}

	@Override
	/** Used by RESTRequest to signal the result of an upload chunk **/
	public void onFailure(String strokeId, Set<Point> points) {
		addPoints(strokeId,points);
		if(callback != null){
			callback.onFailure();
		}
	}

}
