package com.drmaciver;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

class SmallestElements<V>{
  private int fill = 0;
  private final EWS[] heap;

  public SmallestElements(int size){
    heap = new EWS[size];
  }

  double bound(){
    return fill < heap.length ? Double.POSITIVE_INFINITY : heap[0].score;
  }

  void add(V v, double score){
    if(score > bound()) return;

    EWS ews = new EWS(); ews.element = v; ews.score = score;

    if(fill < heap.length){
      heap[fill++] = ews;
      if(fill == heap.length) makeHeapFrom(0);
    } else {
      heap[0] = ews;
      heapify(0);
    }
  }

  @SuppressWarnings("unchecked")
  public List<V> toList(){
    EWS[] h = heap.clone();
    Arrays.sort(h);
    List<V> l = new ArrayList<V>(h.length);
    for(EWS e : h) l.add((V)e.element);
    return l;
  }

  private void makeHeapFrom(int n){
    if(n >= heap.length) return;
    int c1 = n * 2 + 1;
    int c2 = n * 2 + 2;
    makeHeapFrom(c1);
    makeHeapFrom(c2);
    heapify(n);
  }

  private void heapify(int n){
    int c1 = n * 2 + 1;
    int c2 = n * 2 + 2;
   
    if(c1 >= heap.length) return;

    boolean c1g = heap[c1].score > heap[n].score;
    boolean c2g = c2 < heap.length && heap[c2].score > heap[n].score;

    if(!c1g && !c2g) return;

    int c;

    if(c1g && !c2g) c = c1;
    else if(c2g && !c1g) c = c2;
    else if(heap[c2].score > heap[c1].score) c = c2;
    else c = c1;

    EWS x = heap[n];
    heap[n] = heap[c];
    heap[c] = x;
    heapify(c);
  }

  private static class EWS implements Comparable<EWS>{
    Object element;
    double score;
    public int compareTo(EWS that){ return Double.compare(this.score, that.score); }
    public String toString(){ return "EWS(" + element + ", " + score +")"; }
  }
}
