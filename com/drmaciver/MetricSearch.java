package com.drmaciver;

import java.util.List;
import java.util.Collection;

public interface MetricSearch<V> extends Collection<V>{
  public V nearest(V v);
  public List<V> nearestN(V v, int n);
  public Collection<V> allWithinEpsilon(V v, double e);
}
