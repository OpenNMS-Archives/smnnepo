package org.opennms.netmgt.storage.sample.cassandra.internal;

import java.util.Arrays;

public class Util{
    public static final double[] interpLinear(double[] x, double[] y, double[] xi) throws IllegalArgumentException {
        if (x.length != y.length) {
            throw new IllegalArgumentException("X and Y must be the same length");
        }
        if (x.length == 1) {
            throw new IllegalArgumentException("X must contain more than one value");
        }

        double dx, dy;

        double[] slope = new double[x.length - 1];
        double[] intercept = new double[x.length - 1];

        // Calculate the line equation (i.e. slope and intercept) between each point
        for (int i = 0; i < x.length - 1; i++) {
            dx = x[i + 1] - x[i];
            if (dx == 0) {
                throw new IllegalArgumentException("X must be montotonic. A duplicate " + "x-value was found");
            }
            if (dx < 0) {
                throw new IllegalArgumentException("X must be sorted");
            }
            dy = y[i + 1] - y[i];
            slope[i] = dy / dx;
            intercept[i] = y[i] - x[i] * slope[i];
        }

        // Perform the interpolation here
        double[] results = new double[xi.length];

        for (int i = 0; i < xi.length; i++) {
            if ((xi[i] > x[x.length - 1]) || (xi[i] < x[0])) {
                results[i] = Double.NaN;
            }
            else {
                int loc = Arrays.binarySearch(x, xi[i]);
                if (loc < -1) {
                    loc = -loc - 2;
                    results[i] = slope[loc] * xi[i] + intercept[loc];
                }
                else {
                    results[i] = y[loc];
                }
            }
        }
        return results;
    }
    
    public static void main(String[] args) {
    	double[] x = {
    			0,
    			299,
    			610,
    			915,
    			1199,
    			1520
    	};
    	double[] y = {
    			10.9,
    			22.0,
    			29.5,
    			41.2,
    			50.0,
    			66.7
    	};
    	double[] xi = {
    			1,
    			300,
    			600,
    			900,
    			1200,
    			1500
    	};
    	
    	for (double dbl : interpLinear(x, y, xi))
    		System.out.println(dbl);
    }
}
