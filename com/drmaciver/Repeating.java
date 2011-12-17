package com.drmaciver;

import java.util.Collection;
import java.util.AbstractCollection;
import java.util.Iterator;

class Repeating<T> extends AbstractCollection<T>{
  final T t;
  final int c;

  Repeating(T t, int c){
    this.t = t;
    this.c = c;
  }

  public int size(){ return c; }
  public Iterator<T> iterator(){ return new RepeatingIterator<T>(t, c); }

  static class RepeatingIterator<T> implements Iterator<T>{
    private final T elem;
    private final int count;
    private int i;

    RepeatingIterator(T t, int c){
      this.elem = t;
      this.count = c;
      this.i = 0;
    }

    public boolean hasNext(){ return i < count; }
    public T next(){ i++; return elem; }

  	public void remove(){ throw new UnsupportedOperationException(); }
  }
}
