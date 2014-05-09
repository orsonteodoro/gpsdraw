package edu.uci.ics.ics163.gpsdrawupload;


/**
 * 
 * @author djp3
 *
 */
public interface UploadCallback {
	/**
	 * Called after a successful upload of points to the server by StrokeManager.  May be called multiple times
	 * following one StrokeManager.upload() call because that method breaks the upload up into chunks.  Each
	 * chunk may succeed independently of others.
	 */
	void onSuccess();
	
	/**
	 * Called after a failed upload of points to the server by StrokeManager.  May be called multiple times
	 * following one StrokeManager.upload() call because that method breaks the upload up into chunks.  Each
	 * chunk may fail independently.
	 */
	void onFailure();
}
