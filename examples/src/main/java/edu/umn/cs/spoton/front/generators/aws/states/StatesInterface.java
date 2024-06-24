package edu.umn.cs.spoton.front.generators.aws.states;

public interface StatesInterface {

  void add(int testCasesCount, Object item, long time);

  void flush();
}
