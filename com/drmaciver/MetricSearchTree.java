package com.drmaciver;

import java.util.List;
import java.util.Collection;
import java.util.Iterator;

public interface MetricSearchTree<V> extends Collection<V>{
  public V nearest(V v);
  public List<V> nearestN(V v, int n);
  public Iterator<V> allWithinEpsilon(V v, double e);
}
