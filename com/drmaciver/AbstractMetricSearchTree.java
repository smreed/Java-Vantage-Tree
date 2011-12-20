package com.drmaciver;

import java.util.AbstractCollection;

abstract class AbstractMetricSearchTree<V> extends AbstractCollection<V> implements MetricSearchTree<V>{
  public V nearest(V v){
    if(isEmpty()) return null;
    else return nearestN(v, 1).get(0);
  }


}
