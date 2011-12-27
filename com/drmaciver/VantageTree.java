package com.drmaciver;

import java.lang.Iterable;
import java.util.Iterator;
import java.util.PriorityQueue;
import java.util.Collection;
import java.util.Collections;
import java.util.AbstractCollection;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import com.drmaciver.Repeating.RepeatingIterator;

public class VantageTree<V> extends AbstractMetricSearchTree<V>{
  public static final int MAXIMUM_LEAF_SIZE = 200;

  final V center;
  final double threshold;
  final double radius;
  final AbstractMetricSearchTree<V> in;
  final AbstractMetricSearchTree<V> out;
  final int count;
  final int size;

  VantageTree(Metric<V> metric, V center, double threshold, double bound, int count, AbstractMetricSearchTree<V> in, AbstractMetricSearchTree<V> out){
    super(metric);
    this.center = center;
    this.threshold = threshold;
    this.radius = bound;
    this.in = in;
    this.out = out;
    this.count = count;
    this.size = this.in.size() + this.count + this.out.size();
  }

  public String toString(){
    return "VantageTree[" + System.identityHashCode(this) + "]";
  }

  double estimateDistanceTo(V v){ return metric().unbound(metric().distance(v, center), radius); }
  Collection<V> ownElements(){ return new Repeating(center, count); }
  Collection<AbstractMetricSearchTree<V>> subtrees(){ return Arrays.asList(in, out); }
  Collection<AbstractMetricSearchTree<V>> subtreesHitting(V v, double e){
    double r = metric().distance(v, center);

    if(metric().bound(r, this.radius) < e) return subtrees();
    if(metric().bound(e, this.radius) < r) return Collections.emptyList();
    if(metric().bound(e, this.threshold) > r) return Arrays.asList(out);
    if(metric().bound(e, this.threshold) < r) return Arrays.asList(in);
    return subtrees();
  }

  public static <V> V pickAPivot(final Metric<V> metric, List<V> items){
    return (new RecursiveSampler<V>(){
      public double score(V candidate, List<V> sample){
        double[] distances = new double[sample.size()];
        int i = 0;
        for(V v : sample) distances[i++] = metric.distance(v, candidate);
        Arrays.sort(distances);
        double median = distances[distances.length / 2];
        
        double spread = 0;
        for(double d : distances) spread += Math.pow(d - median, 2);
        return -spread; 
      }
    }).pickBestCandidate(items);
  }

  public static <V> AbstractMetricSearchTree<V> build(Metric<V> metric, List<V> items){
  	if(items.size() <= MAXIMUM_LEAF_SIZE) {
      return Leaf.build(metric, items);
    }
  	else {
  		V pivot = pickAPivot(metric, items);
  		double[] distances = new double[items.size()];
  		int i = 0;
  		for(V v : items) distances[i++] = metric.distance(v, pivot);
  		Arrays.sort(distances);
  		double median = distances[distances.length / 2];
  		double max = distances[distances.length - 1];

      if(max <= 0.0 || median >= max){
        // TODO: Optimise this case more sensibly. 
        return Leaf.build(metric, items);
      }

  		List<V> in = new ArrayList<V>();	
  		List<V> out = new ArrayList<V>();	

      int c = 0;

  		for(V v : items){
        if(v == pivot) c++;
        else if(metric.distance(v, pivot) < median) in.add(v);
  			else out.add(v);
  		}

  		assert(in.size() + c + out.size() == items.size());

  		return new VantageTree<V>(metric, pivot, median, max, c, build(metric, in), build(metric, out));
  	}
  }
}
