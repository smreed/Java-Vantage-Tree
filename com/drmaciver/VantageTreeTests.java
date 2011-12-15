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
      check(points.size() == tree.toList().size(), "Expected tree.toList() to have " + points.size() + " points but it has " + tree.toList().size());

      for(int i = 1; i < 5; i++){
        for(int j = 0; j < 50; j++){
          sampleNearest(i);
          testNearest(this.points.get(random.nextInt(points.size())), i);
        }
      }

      for(V v : points) check(tree.contains(v), "Expected tree to contain " + v);
    }

    void sampleNearest(int n){
      V sample = points.get(random.nextInt(points.size()));
      V nearest = tree.nearestN(sample, n).iterator().next();

      check(nearest.equals(sample), "Expected the first of the nearest " + n + " points to " + sample + " to be itself but it was " + nearest);
    }

    void testNearest(V v, int n){
      List<V> nearest = tree.nearestN(v, n);

      for(V w : nearest){
        for(V w2 : points){
          if(!nearest.contains(w2)){
            check(metric.distance(v, w) <= metric.distance(v, w2), "The element " + w2 + " is closer to " + v + " than " + w + " is, but the latter is supposed to be one of its " + n + " nearest neighours");
          }
        }
      }
    }

    void check(boolean value, String message){
      if(!value) errors.add(message);
    }
  }

  public static final int MAX_ERRORS = 5;

  void run(){
    boolean failed = false;
    for(TestCase<?> tc : testCases){
      if(!tc.errors.isEmpty()){
        System.err.println(tc.name + " failed");
       
        int counter = 0; 
        for(String err : tc.errors){
          failed = true;
          System.err.println("  " + err);
          counter++;
          if(counter > MAX_ERRORS){
            System.err.println("  Suppressed " + (tc.errors.size() - MAX_ERRORS) + " more errors");
            break;
          }
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
