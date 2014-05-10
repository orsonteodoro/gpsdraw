package edu.uci.ics.ics163.gpsdrawupload;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.json.JSONException;
import org.json.JSONObject;

import android.os.AsyncTask;

/**
 * This class is used internally to send data to the server
 * @author djp3
 *
 */
class RESTRequest {
	
	final String ADD_STROKE_URL = "http://djp3-pc2.ics.uci.edu:9020/add_stroke";
	
	public Boolean success = null;
	private JSONObject params;
	private String strokeId;
	private HashSet<Point> localPoints;
	private StrokeManagerCallback callback;
	

	public static String encode(Object x) {
		try {
			return URLEncoder.encode(String.valueOf(x),"UTF-8");
		} catch (UnsupportedEncodingException e) {
			return(String.valueOf(x));
		}
	}
	

	public RESTRequest(JSONObject params,String strokeId,Set<Point> points,StrokeManagerCallback callback) {
		this.params = params;
		 
		this.strokeId = strokeId;
			
		this.localPoints = new HashSet<Point>();
		this.localPoints.addAll(points);
			
		this.callback = callback;
			
		new ExecuteREST().execute();
			
	}
	
	private class ExecuteREST extends AsyncTask<Void, Void, String> {
		
		protected String doInBackground(Void ...arg0) {
			
 			/* Build URL query */
	 		StringBuffer local = new StringBuffer(ADD_STROKE_URL);
	 		if(params != null){
	 			local.append("?");
	 			Iterator<?> k = params.keys();
	 			while(k.hasNext()){
					try {
						String key = (String) k.next();
						Object value = params.get(key);
						
						local.append(encode(key));
						local.append("=");
						local.append(encode(value));
						if(k.hasNext()){
							local.append("&");
						}
					} catch (JSONException e) {
						e.printStackTrace();
					}
	 			}
	 		}
	 		
	 		HttpURLConnection urlConn = null;
	 		StringBuffer response = new StringBuffer("error");
	 		try {
	 			URL url = new URL(local.toString());
	 			urlConn = (HttpURLConnection) url.openConnection();
	 			urlConn.setRequestMethod("GET");
	 			urlConn.setDoInput(true);
	 			urlConn.setUseCaches(false);
	 			urlConn.setRequestProperty("Content-Type", "application/json");
	 			urlConn.connect();
	 			
	 			//Get Response	
	 			InputStream is = urlConn.getInputStream();
	 			BufferedReader rd = new BufferedReader(new InputStreamReader(is));
	 			String line;
	 			response = new StringBuffer(); 
	 			try{
	 				while((line = rd.readLine()) != null) {
	 					response.append(line);
	 				}
	 			}
	 			finally{
	 				if(urlConn != null){
	 					urlConn.disconnect();
	 					urlConn = null;
	 				}
	 				if(rd != null){
	 					rd.close();
	 					rd = null;
	 				}
	 			}
	 			
	 			if(callback != null){
	 				callback.onSuccess(strokeId, localPoints);
	 			}
	 		} catch (ConnectException e) {
 				callback.onFailure(strokeId, localPoints);
	 		} catch (UnknownHostException e) {
 				callback.onFailure(strokeId, localPoints);
	 		} catch (MalformedURLException e) {
	 			e.printStackTrace();
	 		} catch (EOFException e) {
	 			response = new StringBuffer("No response from server");
	 		} catch (IOException e) {
	 			e.printStackTrace();
	 		} catch (RuntimeException e) {
	 			e.printStackTrace();
	 		}
	 		finally{
	 			if(urlConn != null){
	 				urlConn.disconnect();
	 			}
	 		}
	 		return response.toString();
	     }
	 }

}
