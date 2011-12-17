package com.drmaciver;

import java.lang.Iterable;
import java.util.Iterator;
import java.util.PriorityQueue;
import java.util.Collection;
import java.util.Collections;
import java.util.AbstractCollection;
import java.util.List;
import java.util.ArrayList;
import java.util.Random;
import java.util.Arrays;

public class VantageTree<V> extends AbstractMetricSearchTree<V>{
  public static final int MAXIMUM_LEAF_SIZE = 200;

  public boolean debugStatistics(){ return false; }

  final Metric<V> metric;
  final Random random;
  final Tree tree;
  int leavesBuilt = 0;
  int leafCount = 0;
  int leavesHit = 0;
  int treeBuilt = 0;
  final int totalSize;

  public VantageTree(Metric<V> metric, List<V> items){
  	this.metric = metric;
  	this.random = new Random();
    this.totalSize = items.size();
  	this.tree 	= buildTree(items); 
    this.leafCount = leavesBuilt;
  }

  public Iterator<V> iterator(){ return tree.iterator(); }
  public int size(){ return tree.size(); }
  public List<V> toList(){ return new ArrayList<V>(this); }

  @SuppressWarnings("unchecked")
  public boolean contains(Object x){
    if(x == null) return false;
    // Stupid hack working around lack of <= queries
    Iterator<V> it = allWithinEpsilon((V)x, 0.001).iterator();
    while(it.hasNext()) if(it.next().equals(x)) return true;
    return false;
  }

  public MetricSearchTree<V> allWithinEpsilon(V v, double e){
  	leavesHit = 0;
  	Tree result = this.tree.allWithinEpsilon(v, e);
  	if(debugStatistics()) System.err.println("allWithinEpsilon hit " + leavesHit  + " leaves out of " + leafCount);
  	return result;
  }

  public List<V> nearestN(V v, int n){
    leavesHit = 0;
    List<V> result = this.tree.nearestN(v, n);
    if(debugStatistics()) System.err.println("nearestN hit " + leavesHit  + " leaves out of " + leafCount);
    return result;
  }

  V pickAPivot(List<V> items){
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

  void debugBuilding(){
    if(debugStatistics() && (random.nextInt(1 + totalSize / 100) == 0)) System.err.println("Tree building " + (treeBuilt * 100.0 / totalSize) + "% complete");
  }

  Tree buildTree(List<V> items){
  	if(items.size() <= MAXIMUM_LEAF_SIZE) {
      treeBuilt += items.size();
      debugBuilding();
      return new Leaf(items);
    }
  	else {
  		V pivot = pickAPivot(items);
  		double[] distances = new double[items.size()];
  		int i = 0;
  		for(V v : items) distances[i++] = metric.distance(v, pivot);
  		Arrays.sort(distances);
  		double median = distances[distances.length / 2];
  		double max = distances[distances.length - 1];

      if(max <= 0.0 || median >= max){
        // TODO: Optimise this case more sensibly. 
        treeBuilt += items.size();
        debugBuilding();
        return new Leaf(items);
      }

  		List<V> in = new ArrayList<V>();	
  		List<V> out = new ArrayList<V>();	

      int c = 0;

  		for(V v : items){
        if(v == pivot) c++;
        else if(metric.distance(v, pivot) < median) in.add(v);
  			else out.add(v);
  		}

  		assert(in.size() + out.size() == items.size());

  		Tree result = new Split(pivot, median, max, c, in, out);
      debugBuilding(); 
      return result;
  	}
  }

  private abstract class Tree extends AbstractMetricSearchTree<V>{
    abstract int depth();
    abstract public Tree allWithinEpsilon(V v, double e);

  	abstract void addToQueue(V v, SmallestElements<V> q);
    public List<V> nearestN(V v, int n){
      SmallestElements<V> q = new SmallestElements<V>(n);
      addToQueue(v, q);
      return q.toList();
    }
  }

  private class Leaf extends Tree{
  	private final List<V> items;

    int depth(){ return 0; }

  	Leaf(List<V> items){
  		this.items = items;
  		leavesBuilt++;
  	}

  	public int size(){ return items.size(); }
  	public Iterator<V> iterator(){ return items.iterator(); }	

  	public Tree allWithinEpsilon(V v, double e){
  		leavesHit++;
  		List<V> result = new ArrayList<V>();

  		for(V w: this.items){
  			if(metric.distance(v, w) < e) result.add(w);
  		}
  		return new Leaf(result);
  	}

  	void addToQueue(V v, SmallestElements<V> q){
  		leavesHit++;

  		for(V w: this.items){
  			q.add(w, metric.distance(v, w));
  		}
  	}
  }

  private class Split extends Tree{
  	final V center;
  	final double threshold;
  	final double radius;
  	final Tree in;
  	final Tree out;
    final int count;
  	final int size;

    int depth(){
      int r = in.depth();
      int j = out.depth();
      if(j > r) r = j;
      return r + 1;
    }

  	Split(V center, double threshold, double bound, int count, Tree in, Tree out){
  		this.center = center;
  		this.threshold = threshold;
  		this.radius = bound;
  		this.in = in;
  		this.out = out;
      this.count = count;
  		this.size = this.in.size() + this.count + this.out.size();
  	}

  	Split(V center, double threshold, double bound, int count, List<V> in, List<V> out){
      this(center, threshold, bound,count, buildTree(in), buildTree(out));
  	}

  	public int size(){ return size; }

  	public Iterator<V> iterator(){ return new TreeIterator(this); }

  	public Tree allWithinEpsilon(V v, double e){
  		double r = metric.distance(v, center);

      boolean centerHits = r < e;

  		if(metric.bound(r, this.radius) < e) return this;
  		if(metric.bound(e, this.radius) < r) return new Empty();
  		if(metric.bound(e, this.threshold) < r) return out.allWithinEpsilon(v, e);
  		if(metric.bound(e, r) < this.threshold){
        Tree newIn = in.allWithinEpsilon(v, e);
        if(centerHits) new Split(center, threshold, radius, count, newIn, new Empty());
        else return newIn;
      }

      Tree newIn = in.allWithinEpsilon(v, e);
      Tree newOut = out.allWithinEpsilon(v, e);
     
      if(!centerHits){ 
        if(newIn.isEmpty()) return newOut;
        if(newOut.isEmpty()) return newIn;
      }

  		return new Split(center, threshold, radius, (centerHits ? count : 0), newIn, newOut);
  	}

  	void addToQueue(V v, SmallestElements<V> q){
  		double r = metric.distance(v, center);

      for(int i = 0; i < count; i++) q.add(center, r);
  		if(metric.bound(q.bound(), this.radius) < r) return;
  		if(metric.bound(q.bound(), this.threshold) < r){ out.addToQueue(v, q); }
  		else if(metric.bound(q.bound(), r) < this.threshold){ in.addToQueue(v, q); }
  		else { in.addToQueue(v, q); out.addToQueue(v, q); };
  	}
  }

  class Empty extends Tree{
    int depth(){ return 0; }
    public int size(){ return 0; }
    public Iterator<V> iterator(){ return Collections.<V>emptyList().iterator(); }
  	void addToQueue(V v, SmallestElements<V> q){}
  	public Tree allWithinEpsilon(V v, double e){ return this; }
  }

  static class RepeatingIterator<T> implements Iterator<T>{
    private final T elem;
    private final int count;
    private int i;

    RepeatingIterator(T t, int c){
      this.elem = t;
      this.count = c;
      this.i = 0;
    }

    public boolean hasNext(){ return i < count; }
    public T next(){ i++; return elem; }

  	public void remove(){ throw new UnsupportedOperationException(); }
  }

  class TreeIterator implements Iterator<V>{
    Iterator currentIterator;
    final VantageTree.Tree[] stack;
    int stackDepth;

    TreeIterator(Tree tree){
      stackDepth = 1;
      stack = new VantageTree.Tree[tree.depth() * 2];
      stack[0] = (VantageTree.Tree)tree;
    }

    void advance(){
      while((currentIterator == null || !currentIterator.hasNext()) && stackDepth > 0){
        VantageTree.Tree tree = stack[--stackDepth];
        if(tree instanceof VantageTree.Leaf){
          currentIterator = tree.iterator();
        } else if(tree instanceof VantageTree.Split){
          VantageTree.Split s = (VantageTree.Split)tree;
          currentIterator = new RepeatingIterator(s.center, s.count);
          stack[stackDepth++] = s.in;
          stack[stackDepth++] = s.out;
        }
      }
    }

    public boolean hasNext(){
      advance();
      return currentIterator != null && currentIterator.hasNext();
    }

    @SuppressWarnings("unchecked")
    public V next(){
      advance();
      return (V)currentIterator.next();      
    } 

  	public void remove(){ throw new UnsupportedOperationException(); }
  }

}
