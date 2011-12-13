package com.drmaciver.vantagetree;

import java.lang.Iterable;
import java.util.Iterator;
import java.util.Collection;
import java.util.Collections;
import java.util.AbstractCollection;
import java.util.List;
import java.util.ArrayList;
import java.util.Random;
import java.util.Arrays;

class VantageTree<V>{
	public static final int MAXIMUM_LEAF_SIZE = 10;
  public static final int ITERATIONS_FOR_CANDIDATE_SEARCH = 50;
  public static final int SAMPLE_SIZE_FOR_CANDIDATE_SEARCH = 100;

	public boolean debugStatistics(){ return false; }

	final Metric<V> metric;
	final Random random;
	final Tree tree;
	int leafCount = 0;
	int leavesHit = 0;
  int treeBuilt = 0;
  final int totalSize;

	public VantageTree(Metric<V> metric, List<V> items){
		this.metric = metric;
		this.random = new Random();
    this.totalSize = items.size();
		this.tree 	= buildTree(items); 
	}

	public Collection<V> allWithinEpsilon(V v, double e){
		leavesHit = 0;
		Collection<V> result = this.tree.allWithinEpsilon(v, e);
		if(debugStatistics()) System.err.println("allWithinEpsilon hit " + leavesHit  + " leaves out of " + leafCount);
		return result;
	}

	public List<V> nearestN(V v, int n){
		NNQueue q = new NNQueue(n);
		leavesHit = 0;
		tree.addToQueue(v, q);
		if(debugStatistics()) System.err.println("nearestN hit " + leavesHit  + " leaves out of " + leafCount);
		return q.toList();
	}

  V pickAPivot(List<V> items){
    List<V> sample = new ArrayList<V>();
    for(int run = 0; run < SAMPLE_SIZE_FOR_CANDIDATE_SEARCH; run++) sample.add(items.get(random.nextInt(items.size())));


    V bestCenter = null;
    double bestSpread = 0;

    for(int run = 0; run < ITERATIONS_FOR_CANDIDATE_SEARCH; run++){
      V candidate = items.get(random.nextInt(items.size()));
			double[] distances = new double[sample.size()];
			int i = 0;
			for(V v : sample) distances[i++] = metric.distance(v, candidate);
      Arrays.sort(distances);
      double median = distances[distances.length / 2];
      
      double spread = 0;

      for(double d : distances) spread += Math.pow(d - median, 2);

      if(spread > bestSpread){
        bestCenter = candidate;
        bestSpread = spread;
      }
    }
      
    return bestCenter;
  }

  void debugBuilding(){
    if(debugStatistics() && (random.nextInt(1000) == 0)) System.err.println("Tree building " + (treeBuilt * 100.0 / totalSize) + "% complete");
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

			for(V v : items){
				if(metric.distance(v, pivot) < median) in.add(v);
				else out.add(v);
			}

			assert(in.size() + out.size() == items.size());

			Tree result = new Split(pivot, median, max, in, out);
      debugBuilding(); 
      return result;
		}
	}

	private abstract class Tree extends AbstractCollection<V>{
		abstract Collection<V> allWithinEpsilon(V v, double e);
		abstract void addToQueue(V v, NNQueue q);
	}

	private class Leaf extends Tree{
		private final List<V> items;

		Leaf(List<V> items){
			this.items = items;
			leafCount++;
		}

		public int size(){ return items.size(); }
		public Iterator<V> iterator(){ return items.iterator(); }	

		Collection<V> allWithinEpsilon(V v, double e){
			leavesHit++;
			List<V> result = new ArrayList<V>();

			for(V w: this.items){
				if(metric.distance(v, w) < e) result.add(w);
			}
			return result;
		}

		void addToQueue(V v, NNQueue q){
			leavesHit++;

			for(V w: this.items){
				q.add(w, metric.distance(v, w));
			}
		}
	}

	private class Split extends Tree{
		private final V center;
		private final double threshold;
		private final double radius;
		private final Tree in;
		private final Tree out;
		private final int size;

		Split(V center, double threshold, double bound, List<V> in, List<V> out){
			this.center = center;
			this.threshold = threshold;
			this.radius = bound;
			this.in = buildTree(in);
			this.out = buildTree(out);
			this.size = this.in.size() + this.out.size();
		}

		public int size(){ return size; }

		public Iterator<V> iterator(){ return new ChainedIterator<V>(in.iterator(), out.iterator()); }

		Collection<V> allWithinEpsilon(V v, double e){
			double r = metric.distance(v, center);

			if(metric.bound(r, this.radius) < e) return this;
			if(metric.bound(e, this.radius) < r) return Collections.emptyList();
			if(metric.bound(e, this.threshold) < r) return out.allWithinEpsilon(v, e);
			if(metric.bound(e, r) < this.threshold) return in.allWithinEpsilon(v, e);
			return concat(in.allWithinEpsilon(v, e), out.allWithinEpsilon(v, e));
		}

		void addToQueue(V v, NNQueue q){
			double r = metric.distance(v, center);

			if(metric.bound(q.bound(), this.radius) < r) return;
			if(metric.bound(q.bound(), this.threshold) < r){ out.addToQueue(v, q); }
			else if(metric.bound(q.bound(), r) < this.threshold){ in.addToQueue(v, q); }
			else { in.addToQueue(v, q); out.addToQueue(v, q); };
		}
	}

	private Collection<V> concat(final Collection<V> x, final Collection<V> y){
		if(x.isEmpty()) return y;
		if(y.isEmpty()) return x;
		else return new AbstractCollection<V>(){
			final int size = x.size() + y.size();
			public int size(){ return size; }
			public Iterator<V> iterator(){ return new ChainedIterator<V>(x.iterator(), y.iterator()); }
		};
	}

	private static class ChainedIterator<V> implements Iterator<V>{
		private final Iterator<V> left;
		private final Iterator<V> right;

		ChainedIterator(Iterator<V> left, Iterator<V> right){
			this.left = left;
			this.right = right;
		}

		public boolean hasNext(){ return left.hasNext() || left.hasNext(); }
		public V next(){ return left.hasNext() ? left.next() : right.next(); }
		public void remove(){ throw new UnsupportedOperationException(); }
	}

	static class PWD implements Comparable<PWD>{
		final Object v;
		final double d;

		PWD(Object v, double d){ this.v = v; this.d = d; }

		public int compareTo(PWD that){ return Double.compare(this.d, that.d); }
	}

	class NNQueue{
		private final PWD[] elements;
		private int size = 0;

		NNQueue(int n){ this.elements = new PWD[n]; }

		double bound(){ return size < elements.length ? Double.POSITIVE_INFINITY: elements[size - 1].d; }

		PWD last(){ return elements[elements.length - 1]; }
		void add(V v, double d){
			if(d > bound()) return;

			if(size < elements.length) elements[size++] = new PWD(v, d);
			else elements[elements.length - 1] = new PWD(v, d);
	
			if(size == elements.length) Arrays.sort(elements);	
		}

		List<V> toList(){
			List<V> r = new ArrayList<V>();
			for(PWD pwd : elements) if(pwd != null) r.add((V)pwd.v);
			return r;
		}
	}
}
