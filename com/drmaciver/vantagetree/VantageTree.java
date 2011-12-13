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

	public boolean debugStatistics = false;

	final Metric<V> metric;
	final Random random;
	final Tree tree;
	int leafCount = 0;
	int leavesHit = 0;

	public VantageTree(Metric<V> metric, List<V> items){
		this.metric = metric;
		this.random = new Random();
		this.tree 	= buildTree(items); 
	}

	public Collection<V> allWithinEpsilon(V v, double e){
		leavesHit = 0;
		Collection<V> result = this.tree.allWithinEpsilon(v, e);
		if(debugStatistics) System.err.println("Hit " + leavesHit  + " leaves out of " + leafCount);
		return result;
	}

	Tree buildTree(List<V> items){
		if(items.size() <= MAXIMUM_LEAF_SIZE) return new Leaf(items);
		else {
			V pivot = items.get(random.nextInt(items.size()));
			double[] distances = new double[items.size()];
			int i = 0;
			for(V v : items) distances[i++] = metric.distance(v, pivot);
			Arrays.sort(distances);
			double median = distances[distances.length / 2];
			double max = distances[distances.length - 1];

			List<V> in = new ArrayList<V>();	
			List<V> out = new ArrayList<V>();	

			for(V v : items){
				if(metric.distance(v, pivot) < median) in.add(v);
				else out.add(v);
			}

			assert(in.size() + out.size() == items.size());

			return new Split(pivot, median, max, in, out);
		}
	}

	private abstract class Tree extends AbstractCollection<V>{
		abstract Collection<V> allWithinEpsilon(V v, double e);
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
}
