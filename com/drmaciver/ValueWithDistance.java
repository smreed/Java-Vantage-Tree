package com.drmaciver;

class ValueWithDistance<V> implements Comparable<ValueWithDistance<?>>{
  public final V value;
  public final double distance;

  ValueWithDistance(V value, double distance){
    this.value = value;
    this.distance = distance;
  }

  public int compareTo(ValueWithDistance<?> that){
    return Double.compare(this.distance, that.distance);
  }
}
