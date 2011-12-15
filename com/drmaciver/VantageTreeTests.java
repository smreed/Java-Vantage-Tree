package com.drmaciver;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

class VantageTreeTests{
  final List<TestCase<?>> testCases = new ArrayList<TestCase<?>>();
  final Random random = new Random();


  VantageTreeTests(){
    new TestCase<Double>("Small doubles", Metric.DOUBLE_DISTANCE, Arrays.asList(1.0, 2.0, 3.0));

    List<Double> largeDoubles = new ArrayList<Double>();
    for(int i = 0; i < 1000; i++) largeDoubles.add(random.nextDouble());
    new TestCase<Double>("Large doubles", Metric.DOUBLE_DISTANCE, largeDoubles);

  }

  class TestCase<V>{
    final String name;
    final Metric<V> metric;
    final List<V> points;
    final VantageTree<V> tree;
    final List<String> errors = new ArrayList<String>();

    TestCase(String name, Metric<V> metric, List<V> points){
      this.name = name;
      this.metric = metric;
      this.points = points;
      this.tree = new VantageTree<V>(metric, points);
      testCases.add(this);
      check(points.size() == tree.size(), "Expected tree to have " + points.size() + " points but it has " + tree.size());

      for(int i = 1; i < 5; i++){
        for(int j = 0; j < 50; j++){
          sampleNearest(i);
        }
      }
    }

    void sampleNearest(int n){
      V sample = points.get(random.nextInt(points.size()));
      V nearest = tree.nearestN(sample, n).iterator().next();

      check(nearest.equals(sample), "Expected the first of the nearest " + n + " points to " + sample + " to be itself but it was " + nearest);
    }

    void check(boolean value, String message){
      if(!value) errors.add(message);
    }
  }

  void run(){
    boolean failed = false;
    for(TestCase<?> tc : testCases){
      if(!tc.errors.isEmpty()){
        System.err.println(tc.name + " failed");
        for(String err : tc.errors){
          failed = true;
          System.err.println("  " + err);
        }
      } else {
        System.err.println(tc.name + " ok");
      }
    }
    if(failed) System.exit(1);
  }

  public static void main(String[] args){
    VantageTreeTests tests = new VantageTreeTests();

    tests.run();
  }


}