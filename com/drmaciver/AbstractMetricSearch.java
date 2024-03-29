package com.drmaciver;

import java.util.AbstractCollection;

abstract class AbstractMetricSearch<V> extends AbstractCollection<V> implements MetricSearch<V>{
  public V nearest(V v){
    if(isEmpty()) return null;
    else return nearestN(v, 1).get(0);
  }


}
