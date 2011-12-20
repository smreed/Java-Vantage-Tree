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

  final Metric<V> metric;
  final Tree tree;
  final int totalSize;

  public VantageTree(Metric<V> metric, List<V> items){
  	this.metric = metric;
    this.totalSize = items.size();
  	this.tree 	= buildTree(items); 
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
  	return this.tree.allWithinEpsilon(v, e);
  }

  public List<V> nearestN(V v, int n){
    List<V> result = this.tree.nearestN(v, n);
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

  Tree buildTree(List<V> items){
  	if(items.size() <= MAXIMUM_LEAF_SIZE) {
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
      return result;
  	}
  }

  private abstract class Tree extends AbstractMetricSearchTree<V>{
    abstract int depth();
    abstract public Tree allWithinEpsilon(V v, double e);

    abstract Collection<V> ownElements();
    Collection<Tree> subtreesHitting(V v, double e){ return subtrees(); }
    abstract Collection<Tree> subtrees();

    public List<V> nearestN(final V v, int n){
      final SmallestElements<V> q = new SmallestElements<V>(n);
      final PriorityQueue<ValueWithDistance<Split>> treesToSearch = new PriorityQueue<ValueWithDistance<Split>>();

      AbstractTreeIterator searchIterator = new AbstractTreeIterator(){
        @Override Collection<VantageTree.Tree> subtreesFrom(VantageTree.Tree tree){
          return tree.subtreesHitting(v, q.bound());
        }

        void pushTrees(Collection<VantageTree.Tree> trees){
          for(VantageTree.Tree tree : trees){
            if(tree instanceof VantageTree.Split){
              Split split = (Split)tree;
              treesToSearch.add(new ValueWithDistance(tree, metric.unbound(metric.distance(v, split.center), split.radius)));
            } else consumeTree(tree);
          }
        }

        VantageTree.Tree popTree(){
          ValueWithDistance<Split> vs = treesToSearch.poll();
          if(vs == null) return null;
          if(vs.distance > q.bound()) return null;
          return vs.value;
        }
      };

      searchIterator.pushTrees(Arrays.asList((VantageTree.Tree)this));

      while(searchIterator.hasNext()){
        V w = (V)searchIterator.next();
        q.add(w, metric.distance(v, w));
      }

      return q.toList();
    }
  }

  private class Leaf extends Tree{
  	private final List<V> items;

    int depth(){ return 0; }

  	Leaf(List<V> items){
  		this.items = items;
  	}

    Collection<V> ownElements(){ return items; }
    Collection<Tree> subtrees(){ return Collections.emptyList(); }

  	public int size(){ return items.size(); }
  	public Iterator<V> iterator(){ return items.iterator(); }	

  	public Tree allWithinEpsilon(V v, double e){
  		List<V> result = new ArrayList<V>();

  		for(V w: this.items){
  			if(metric.distance(v, w) < e) result.add(w);
  		}
  		return new Leaf(result);
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

    Collection<V> ownElements(){ return new Repeating(center, count); }
    Collection<Tree> subtrees(){ return Arrays.asList(in, out); }
    Collection<Tree> subtreesHitting(V v, double e){
  		double r = metric.distance(v, center);

  		if(metric.bound(r, this.radius) < e) return Arrays.asList(in, out);
  		if(metric.bound(e, this.radius) < r) return Collections.emptyList();
  		if(metric.bound(e, this.threshold) < r) return Arrays.asList(out);
  		if(metric.bound(e, this.threshold) < r) return Arrays.asList(in);
  		return Arrays.asList(in, out);
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
  }

  class Empty extends Tree{
    int depth(){ return 0; }
    public int size(){ return 0; }
    public Iterator<V> iterator(){ return Collections.<V>emptyList().iterator(); }
  	public Tree allWithinEpsilon(V v, double e){ return this; }

    Collection<V> ownElements(){ return Collections.emptyList(); }
    Collection<Tree> subtrees(){ return Collections.emptyList(); }
  }

  abstract class AbstractTreeIterator implements Iterator<V>{
    Iterator currentIterator;

    abstract VantageTree.Tree popTree();
    abstract void pushTrees(Collection<VantageTree.Tree> tree);

    Collection<VantageTree.Tree> subtreesFrom(VantageTree.Tree tree){
      return (Collection<VantageTree.Tree>)tree.subtrees();
    }

    void consumeTree(VantageTree.Tree tree){
      currentIterator = tree.ownElements().iterator();
      pushTrees(subtreesFrom(tree));
    }

    void advance(){
      while(currentIterator == null || !currentIterator.hasNext()){
        VantageTree.Tree tree = popTree();
        if(tree == null) break;
        consumeTree(tree);
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

  class TreeIterator extends AbstractTreeIterator{
    VantageTree.Tree[] stack;
    int stackDepth;

    TreeIterator(Tree tree){
      stackDepth = 1;
      stack = new VantageTree.Tree[1 + tree.depth() * 2];
      stack[0] = (VantageTree.Tree)tree;
    }

    VantageTree.Tree popTree(){
      return stackDepth <= 0 ? null : stack[--stackDepth];
    }

    void pushTrees(Collection<VantageTree.Tree> treesToAdd){
      if(stackDepth + treesToAdd.size() > stack.length){
        VantageTree.Tree[] newStack = new VantageTree.Tree[stack.length * 2];
        System.arraycopy(stack, 0, newStack, 0, stackDepth);
        stack = newStack;
      }
      for(VantageTree.Tree s : treesToAdd) stack[stackDepth++] = s;
    }
  }
}
