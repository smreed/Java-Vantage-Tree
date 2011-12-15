package com.drmaciver;

import java.util.Random;
import java.util.List;
import java.util.ArrayList;

abstract class RecursiveSampler<V>{
  private final Random random = new Random();

  public int sampleSize(){ return 100; }
  public int startingCandidates(){ return 128; }

  public abstract double score(V candidate, List<V> sample);

  V pickBestCandidate(List<V> everything){
    List<V> candidates = pickASample(everything, startingCandidates());
  
    while(candidates.size() > 1) candidates = reduceCandidates(everything, candidates);

    return candidates.get(0); 
  }


  List<V> reduceCandidates(List<V> everything, List<V> candidates){
    List<V> testCase = pickASample(everything, sampleSize());
    SmallestElements<V> se = new SmallestElements<V>(candidates.size() / 2);
    for(V c : candidates) se.add(c, score(c, testCase));
    return se.toList();
  }

  List<V> pickASample(List<V> everything, int size){
    if(everything.size() <= size) return everything;
    else {
      List<V> sample = new ArrayList<V>(size);
      for(int i = 0; i < size; i++) sample.add(everything.get(random.nextInt(everything.size())));
      return sample;
    }
  }
}
