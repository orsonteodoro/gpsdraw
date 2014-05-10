package edu.uci.ics.ics163.gpsdrawupload;

/** This class calculates the distance between two lat,lng pairs 
 * 
 * @author djp3
 *
 */
class Haversine {
    public static final double R = 6372800; // In meters
    //public static final double R = 6372.8; // In kilometers
    /**
     * Distance between two points in meters (if R is in metes)
     * @param lat1
     * @param lon1
     * @param lat2
     * @param lon2
     * @return
     */
    static double haversine(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        lat1 = Math.toRadians(lat1);
        lat2 = Math.toRadians(lat2);
 
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2);
        a += Math.sin(dLon / 2) * Math.sin(dLon / 2) * Math.cos(lat1) * Math.cos(lat2);
        //double c = 2 * Math.asin(Math.sqrt(a));
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        return R * c;
    }
}