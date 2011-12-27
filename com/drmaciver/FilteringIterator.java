package com.drmaciver;

import java.util.Iterator;

abstract class FilteringIterator<T> implements Iterator<T>{
  private boolean advanced = false;
  private boolean hasNext = false;
  private T next;

  private final Iterator<T> source;

  FilteringIterator(Iterator<T> source){ this.source = source; }
 
  abstract boolean acceptElement(T t);

  public T next(){ advance(); advanced = false; return next; }
  public boolean hasNext(){ advance(); return hasNext; }

  public void remove(){ throw new UnsupportedOperationException(); }

  private void advance(){
    if(!advanced) while(source.hasNext() && !(hasNext = acceptElement(next = source.next())));

    advanced = true;
  }
}
