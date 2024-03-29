package com.drmaciver;
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

    long buildStart = System.currentTimeMillis();
  	VantageTree<double[]> db = new VantageTree<double[]>(Metric.L2_DISTANCE, points){ public boolean debugStatistics(){ return true; } };
    System.out.println("Building tree took " + (System.currentTimeMillis() - buildStart) + "ms");

    {
      int numQueries = 1000;
      long queryStart = System.currentTimeMillis();
      for(int i = 0; i < numQueries; i++){
        double[] p = rv(rnd);
        double e = rnd.nextDouble();

        Collection<double[]> we = db.allWithinEpsilon(p, e);
      }
      long queriesTook = System.currentTimeMillis() - queryStart;
      System.out.println("Within epsilon queries took about " + (queriesTook / ((double)numQueries)) + "ms each");
    }

    {
      int numQueries = 1000;
      long queryStart = System.currentTimeMillis();
      for(int i = 0; i < numQueries; i++){
        double[] p = rv(rnd);
        double e = rnd.nextDouble();

        db.nearest(p);
      }
      long queriesTook = System.currentTimeMillis() - queryStart;
      System.out.println("Nearest neighbour queries took about " + (queriesTook / ((double)numQueries)) + "ms each");
    }

    {
      int numQueries = 5;
      long queryStart = System.currentTimeMillis();
      for(int i = 0; i < numQueries; i++){
        for(Object o : db);
      }
      long queriesTook = System.currentTimeMillis() - queryStart;
      System.out.println("Iterating over the whole collection took about " + (queriesTook / ((double)numQueries)) + "ms each");
    }
  }

  public static double[] rv(Random rnd){
  	double[] x = new double[20];
  	for(int i = 0; i < x.length; i++) x[i] = rnd.nextDouble();
  	return x;
  }
}
