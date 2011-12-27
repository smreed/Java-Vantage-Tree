package com.drmaciver;

import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.PriorityQueue;
import java.util.List;
import java.util.Iterator;
import java.util.Collection;

abstract class AbstractMetricSearchTree<V> extends AbstractCollection<V> implements MetricSearchTree<V>{
  private final Metric<V> metric;
  public Metric<V> metric(){ return metric; }

  AbstractMetricSearchTree(Metric<V> metric){ this.metric = metric; }

  public V nearest(V v){
    if(isEmpty()) return null;
    else return nearestN(v, 1).get(0);
  }

  private int lazySize = -1;

  public int size(){
    if(lazySize < 0){
      int size = ownElements().size();
      for(Collection<V> t : subtrees()) size += t.size();
      lazySize = size;
    }
    return lazySize;
  }

  public Iterator<V> iterator(){ return new WholeTreeIterator<V>(this); }
  public Iterator<V> allWithinEpsilon(final V v, final double e){
    return new FilteringIterator<V>(
      new WholeTreeIterator<V>(this){
        @Override Collection<AbstractMetricSearchTree<V>> subtreesFrom(AbstractMetricSearchTree<V> tree){
          return tree.subtreesHitting(v, e);
        }
      }
    ){
      public boolean acceptElement(V w){ return metric().distance(v, w) < e; }
    };
  }

  public List<V> nearestN(final V v, int n){
    final SmallestElements<V> q = new SmallestElements<V>(n);
    final PriorityQueue<ValueWithDistance<AbstractMetricSearchTree<V>>> treesToSearch = new PriorityQueue<ValueWithDistance<AbstractMetricSearchTree<V>>>();

    AbstractTreeIterator<V> searchIterator = new AbstractTreeIterator<V>(){
      @Override Collection<AbstractMetricSearchTree<V>> subtreesFrom(AbstractMetricSearchTree<V> tree){
        return tree.subtreesHitting(v, q.bound());
      }

      void pushTrees(Collection<AbstractMetricSearchTree<V>> trees){
        for(AbstractMetricSearchTree<V> tree : trees){
          treesToSearch.add(new ValueWithDistance(tree, tree.estimateDistanceTo(v)));
        }
      }

      AbstractMetricSearchTree<V> popTree(){
        ValueWithDistance<AbstractMetricSearchTree<V>> vs = treesToSearch.poll();
        if(vs == null) return null;
        if(vs.distance > q.bound()) return null;
        return vs.value;
      }
    };

    searchIterator.pushTrees(Arrays.asList((AbstractMetricSearchTree<V>)this));

    while(searchIterator.hasNext()){
      V w = (V)searchIterator.next();
      q.add(w, metric.distance(v, w));
    }

    return q.toList();
  }

  abstract double estimateDistanceTo(V v);
  abstract Collection<V> ownElements();
  Collection<AbstractMetricSearchTree<V>> subtreesHitting(V v, double e){ return subtrees(); }
  abstract Collection<AbstractMetricSearchTree<V>> subtrees();

  static abstract class AbstractTreeIterator<V> implements Iterator<V>{
    Iterator<V> currentIterator;

    abstract AbstractMetricSearchTree<V> popTree();
    abstract void pushTrees(Collection<AbstractMetricSearchTree<V>> tree);

    Collection<AbstractMetricSearchTree<V>> subtreesFrom(AbstractMetricSearchTree<V> tree){
      return (Collection<AbstractMetricSearchTree<V>>)tree.subtrees();
    }

    void consumeTree(AbstractMetricSearchTree<V> tree){
      currentIterator = tree.ownElements().iterator();
      Collection<AbstractMetricSearchTree<V>> st = subtreesFrom(tree);
      for(Object o : st) assert(st != tree);
      pushTrees(st);
    }

    void advance(){
      while(currentIterator == null || !currentIterator.hasNext()){
        AbstractMetricSearchTree<V> tree = popTree();
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
      System.err.println("Foomp");
      return (V)currentIterator.next();      
    } 

  	public void remove(){ throw new UnsupportedOperationException(); }
  }

  class WholeTreeIterator<V> extends AbstractTreeIterator<V>{
    ArrayList<AbstractMetricSearchTree<V>> stack = new ArrayList<AbstractMetricSearchTree<V>>();

    WholeTreeIterator(AbstractMetricSearchTree<V> tree){
      stack.add(tree); 
    }

    AbstractMetricSearchTree<V> popTree(){
      return stack.isEmpty() ? null : stack.remove(stack.size() - 1);
    }

    void pushTrees(Collection<AbstractMetricSearchTree<V>> treesToAdd){
      stack.addAll(treesToAdd);
    }
  }

}
