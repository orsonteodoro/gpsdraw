package edu.uci.ics.ics163.gpsdrawupload;

import java.util.Set;

/**
 * This is used internally by StrokeManager to handle RESTRequest success and failure
 * @author djp3
 *
 */
interface StrokeManagerCallback {
	
	void onSuccess(String strokeId, Set<Point> points);
	void onFailure(String strokeId, Set<Point> points);

}
