package edu.uci.ics.ics163.gpsdrawupload;

import java.security.InvalidParameterException;

/**
 * This class defines a point.  It is immutable.
 * When sorted, Point is naturally ordered by increasing time, increasing longitude, then increasing latitude
 * @author djp3
 *
 */
public final class Point implements Comparable<Point>{
	private final long time;
	private final double lat;
	private final double lng;
	
	/** Constructor
	 * 
	 * @param time Time in milliseconds since epoch when the point was recorded see System.currentTimeMillis()
	 * @param lat Latitude as a real number from -90.0 to 90.0
	 * @param lng Longitude as a real number from -180.0 to 180.0
	 */
	public Point(long time, double lat, double lng){
		if(time < 0){
			throw new InvalidParameterException("time must be greater than 0");
		}
		this.time = time;
		
		if((lat < -90.0) || (lat > 90.0)){
			throw new InvalidParameterException("latitude must be greater than or equal to -90.0 and less than or equal to 90.0");
		}
		this.lat = lat;
		
		if((lng < -180.0) || (lng > 180.0)){
			throw new InvalidParameterException("longitude must be greater than or equal to -180.0 and less than or equal to 180.0");
		}
		this.lng = lng;
	}
	
	public long getTime() {
		return time;
	}
	
	public double getLat() {
		return lat;
	}
	
	public double getLng() {
		return lng;
	}
	
	@Override
	public int compareTo(Point that) {
		if(that == null){
			return -1;
		}
		if(this.equals(that)){
			return 0;
		}
		if(this.getTime() == that.getTime()){
			if(this.getLat() == that.getLat()){
				if((this.getLng() - that.getLng()) > 0){
					return 1;
				}
				else{
					return -1;
				}
			}
			else{
				if((this.getLat() - that.getLat()) > 0){
					return 1;
				}
				else{
					return -1;
				}
			}
		}
		else{
			return (int) (this.getTime() - that.getTime());
		}
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		long temp;
		temp = Double.doubleToLongBits(lat);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(lng);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result + (int) (time ^ (time >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Point other = (Point) obj;
		if (Double.doubleToLongBits(lat) != Double.doubleToLongBits(other.lat))
			return false;
		if (Double.doubleToLongBits(lng) != Double.doubleToLongBits(other.lng))
			return false;
		if (time != other.time)
			return false;
		return true;
	}
	
	@Override
	public String toString(){
		StringBuffer sb = new StringBuffer();
		sb.append("[");
		sb.append("time:" + this.getTime());
		sb.append(",latitude:" + this.getLat());
		sb.append(",longitude:" + this.getLng());
		sb.append("]");
		return(sb.toString());
	}

	
}
