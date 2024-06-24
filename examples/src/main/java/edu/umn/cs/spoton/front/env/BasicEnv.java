package edu.umn.cs.spoton.front.env;


import edu.umn.cs.spoton.front.InternalConfig;
import edu.umn.cs.spoton.front.generators.misc.files.util.FileManipulation;

//this is acting like a controller
public class BasicEnv implements Environment {

  static BasicEnv instance = new BasicEnv();

  public static BasicEnv getInstance() {
    return instance;
  }

  @Override
  public void init(Object o) {
    FileManipulation.initializeDir(InternalConfig.getInstance().LOCAL_FUZZDIR);
  }

  @Override
  public void reset() {
    clearFUZZ_DIR();
  }

  private static void clearFUZZ_DIR() {
    FileManipulation.clearDir(InternalConfig.getInstance().LOCAL_FUZZDIR);
  }
}
