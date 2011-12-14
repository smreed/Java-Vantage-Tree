package com.drmaciver;

import java.lang.Math;

abstract class Metric<V>{
  public static final Metric<Double> DOUBLE_DISTANCE = new Metric<Double>(){
    public double distance(Double x, Double y){ return Math.abs(x - y); }
  };

  public static final Metric<double[]> L2_DISTANCE = new Metric<double[]>(){
    public double distance(double[] x, double[] y){ 
      if(y.length > x.length){
        double[] z = x;
        x = y;
        y = z; 
      }
      
      int i;
    
      double tot = 0.0;
      for(i = 0; i < y.length; i++){
        tot += Math.pow(x[i] - y[i], 2);
      }
      for(; i < x.length; i++){
        tot += Math.pow(x[i], 2);
      }

      return Math.sqrt(tot);
    }
  };


  public abstract double distance(V x, V y);
  public double bound(double d1, double d2){ return d1 + d2; }
}