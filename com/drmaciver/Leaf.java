package com.drmaciver;

import java.util.Collection;
import java.util.Collections;
import java.util.Arrays;
import java.util.List;
import java.util.Iterator;

abstract class Leaf<V> extends AbstractMetricSearchTree<V>{
  Collection<AbstractMetricSearchTree<V>> subtreesHitting(V v, double e){ return Collections.emptyList(); }
  Collection<AbstractMetricSearchTree<V>> subtrees(){ return Collections.emptyList(); }
  public Iterator<V> iterator(){ return ownElements().iterator(); }
  public int size(){ return ownElements().size(); }
  public double estimateDistanceTo(V v){ return 0; }

  private Leaf(Metric<V> m){
    super(m); 
  }

  public static <V> Leaf<V> build(Metric<V> m, Collection<V> items){
    switch(items.size()){
      case 0: return new EmptyLeaf<V>(m);
      case 1: return new SingleLeaf(m, items.iterator().next());
      default: return new ArrayLeaf(m, items.toArray());
    }
  }

  private static class ArrayLeaf<V> extends Leaf<V>{
    private final Object[] objects;

    ArrayLeaf(Metric<V> metric, Object[] vs){ super(metric); this.objects = vs; }
    Collection<V> ownElements(){ return (List<V>)Arrays.asList(objects); }
  }

  private static class SingleLeaf<V> extends Leaf<V>{
    private final V v;

    SingleLeaf(Metric<V> metric, V v){ super(metric); this.v = v; }

    Collection<V> ownElements(){ return new Repeating(v, 1); }
  }

  private static class EmptyLeaf<V> extends Leaf<V>{
    EmptyLeaf(Metric<V> metric){ super(metric); }

    Collection<V> ownElements(){ return Collections.emptyList(); }
    public int size(){ return 0; }
  }
}
