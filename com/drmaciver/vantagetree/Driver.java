package com.drmaciver.vantagetree;
import java.util.List;
import java.util.Collection;
import java.util.Random;
import java.util.ArrayList;
import java.util.Arrays;

class Driver{
	public static void main(String[] args){
		List<double[]> points = new ArrayList<double[]>();
		Random rnd = new Random();

		for(int i = 0; i < 10000; i++){
			points.add(rv(rnd));
		}

		VantageTree<double[]> db = new VantageTree<double[]>(Metric.L2_DISTANCE, points){ public boolean debugStatistics(){ return true; } };

		for(int i = 0; i < 100; i++){
			double[] p = rv(rnd);
			double e = rnd.nextDouble();

      Collection<double[]> we = db.allWithinEpsilon(p, e);
      Collection<double[]> n3 = db.nearestN(p, 3);
			System.out.println("Found " + we.size() + " points within " + e + " of " + Arrays.toString(p));
			System.out.print("Nearest 3 points are " );
			for(double[] xs : n3) System.out.print(Arrays.toString(xs) + "  ");
			System.out.println();
		}
	}

	public static double[] rv(Random rnd){
		double[] x = new double[2];
		for(int i = 0; i < x.length; i++) x[i] = rnd.nextDouble();
		return x;
	}
}
