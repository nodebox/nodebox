/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package nodebox.util;

public class Geometry {

    public static double radians(double degrees) {
        return degrees * Math.PI / 180;
    }

    public static double degrees(double radians) {
        return radians * 180 / Math.PI;
    }

    /**
     * The angle between two points.
     */
    public static double angle(double x0, double y0, double x1, double y1) {
        return degrees(Math.atan2(y1 - y0, x1 - x0));
    }

    /**
     * The distance between two points.
     */
    public static double distance(double x0, double y0, double x1, double y1) {
        return Math.sqrt(Math.pow(x1 - x0, 2) + Math.pow(y1 - y0, 2));
    }

    /**
     * The location of a point based on angle and distance.
     */
    public static double[] coordinates(double x0, double y0, double distance, double angle) {
        double[] point = new double[2];
        point[0] = x0 + Math.cos(radians(angle)) * distance;
        point[1] = y0 + Math.sin(radians(angle)) * distance;
        return point;
    }

    /**
     * The reflection of a point through an origin point.
     */
    public static double[] reflect(double x0, double y0, double x1, double y1, double d, double a) {
        d *= distance(x0, y0, x1, y1);
        a += angle(x0, y0, x1, y1);
        return coordinates(x0, y0, d, a);
    }

}
