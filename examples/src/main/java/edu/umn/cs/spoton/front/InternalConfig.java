package edu.umn.cs.spoton.front;

import edu.umn.cs.spoton.front.UserConfig.DynamoDBInfo;
import edu.umn.cs.spoton.front.env.BasicEnv;
import edu.umn.cs.spoton.front.env.DynamoDbEnv;
import edu.umn.cs.spoton.front.env.Environment;
import edu.umn.cs.spoton.front.env.S3Env;
import edu.umn.cs.spoton.front.generators.aws.states.StatisticsManager;
import edu.umn.cs.spoton.front.generators.misc.files.util.FileManipulation;
import edu.umn.cs.spoton.front.generators.misc.files.util.FileTypeUtil;
import edu.umn.cs.spoton.front.generators.misc.files.util.FileTypeUtil.FileType;
import java.io.File;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.imageio.ImageIO;

/**
 * Configuration class for different fuzzing options
 */
public class InternalConfig {

  static InternalConfig instance = new InternalConfig();

  public enum StringGenerator {
    ASCII, ALPHA, CODE_POINT, EMPTY
  }

  // list of all environments for serverless and non-serverless.
  List<Environment> environmentList = new ArrayList<>();

  /************************ CONSTANTS FOR RUNNING GENERATORS: START ******************************/
  public final List<StringGenerator> ALL_STRING_GENERATORS = Arrays.asList(
      StringGenerator.ASCII, StringGenerator.ALPHA, StringGenerator.EMPTY,
      StringGenerator.CODE_POINT);

  public final String ENGINE = System.getProperty("engine");
  public final boolean STR_OPT_ON =
      ENGINE != null && (ENGINE.equals("spotOn") || ENGINE.equals("zestStrOpt"));

  public final String PROJECT_DIR = System.getProperty("jqf.PROJECT_DIR");
  public final String buildDir =
      PROJECT_DIR != null ? PROJECT_DIR + File.separator
          : Paths.get("").toAbsolutePath() + "/target/";

  public final boolean IS_EI_GENERATION_EXP = Boolean.parseBoolean(
      System.getProperty("spoton.RUN_EI_GEN_EXPERIMENT"));

  private FileType[] allowedFileTypes = FileType.values();

  //used to define the range of int values for populating dynamodb and csv
  public final int[] NUMBER_LOWER_UPPER_BOUND = new int[]{-100, 100};

  //the upper bound of bytes in a text file has a mean of 100
  public final int MEAN_TEXT_FILE_SIZE = 100;

  public final String REGION = "us-east-1";

  public final String REMOTE_FUZZDIR = "/FuzzDir";

  //default fuzzing directory to create local and remote s3 objects if left unspecified in the users testcase.
  public final String LOCAL_FUZZDIR = buildDir + REMOTE_FUZZDIR;
  public final String OUTPUT_DIR = System.getProperty("jqf.RESULTS_OUTPUT_DIR");
  public final double PROBABILITY_OF_ERRONEOUS_FILE = 0.003;

  //Defines the mean of items to be populated in the dynamodb and csv
  public final int MEAN_OF_ROWS = 5;

  //Defines the mean of attributes to be created in addition to the primary and the secondary keys for randomness.
  public final int MEAN_ATTRIBUTES = 5;

  //lower bound to geometric distribution, as mutation can make it result in NaN value
  public final int ZERO_LOWERBOUND = 0;

  public final String[] ALLOWED_IMAGE_EXTENSION = Arrays.stream(
      FileTypeUtil.ImageExtensions.values()).map(e -> e.toString()).toArray(String[]::new);
  public final String STATES_DIR;

  /************************ CONSTANTS FOR RUNNING GENERATORS: End *********************************/

  /*********************** APIs for setting tests : START ****************************************/
  boolean isDebugModeOn = true;

  //allow erroneous files, i.e., invalid extension, corrupted files, etc.
  boolean allowErroneousGeneration = false;

  boolean allowNoExtFileTest = false;

  boolean allowDifferentExtFileTest = false;

  final String localStackPort = System.getProperty("localstack");
  String localStackEndpoint =
      localStackPort == null ? "http://localhost:4566" : "http://localhost:" + localStackPort;

  public String getLocalStackEndPointApi() {
    return localStackEndpoint;
  }

  public void setLocalStackEndpoint(String localStackEndpoint) {
    this.localStackEndpoint = localStackEndpoint;
  }

  public boolean isAllowErroneousGeneration() {
    return allowErroneousGeneration;
  }

  public void setAllowErroneousGeneration(boolean allowErroneousGeneration) {
    this.allowErroneousGeneration = allowErroneousGeneration;
  }

  //default is to allow all file types to be generated, unless configured to generate specific file types.

  public boolean isAllowNoExtFileTestaApi() {
    return allowNoExtFileTest;
  }

  public void setAllowNoExtFileTestApi(boolean allowNoExtFileTest) {
    this.allowNoExtFileTest = allowNoExtFileTest;
  }

  public boolean isAllowDifferentExtFileTest() {
    return allowDifferentExtFileTest;
  }

  public void setAllowDifferentExtFileTest(boolean allowDifferentExtFileTest) {
    this.allowDifferentExtFileTest = allowDifferentExtFileTest;
  }

  public boolean isDebugModeOn() {
    return isDebugModeOn;
  }

  public void setDebugModeOn(boolean debugModeOn) {
    isDebugModeOn = debugModeOn;
  }

  public void setAllowedFileTypes(
      FileType[] allowedFileTypes) {
    this.allowedFileTypes = allowedFileTypes;
  }

  public FileType[] getAllowedFileTypes() {
    return allowedFileTypes;
  }

  /*********************** APIs for setting tests : END ****************************************/


  private InternalConfig() {
    ImageIO.setUseCache(false);
    String inputProperty = System.getProperty("input");
    boolean inReproMode = inputProperty != null;

    if (!inReproMode)
      STATES_DIR = buildDir + File.separator + OUTPUT_DIR + "/aws-states";
    else
      STATES_DIR = buildDir + File.separator + OUTPUT_DIR + "/repro/aws-states";

    System.out.println("Remote FuzzDir = " + REMOTE_FUZZDIR);
    System.out.println("Local FuzzDir = " + LOCAL_FUZZDIR);
    System.out.println("buildDir = " + buildDir);
    System.out.println("statesDir = " + STATES_DIR);
    System.out.println("using strOptimization = " + STR_OPT_ON);
    FileManipulation.initializeDir(STATES_DIR);
  }

  public static InternalConfig getInstance() {
    return instance;
  }

  public static void resetInstance() {
    instance = new InternalConfig();
  }

  void setupEnv(ArrayList<String> bucketNames, ArrayList<DynamoDBInfo> dynamoDBInfos) {
    S3Env s3Env = S3Env.getInstance();
    DynamoDbEnv dynamoDbEnv = DynamoDbEnv.getInstance();
    if (bucketNames.isEmpty()) { //add only the basic environment
      BasicEnv basicEnv = BasicEnv.getInstance();
      basicEnv.init(null);
      environmentList.add(basicEnv);
    } else { //add both basic and s3 environments
      s3Env.init(bucketNames);
      BasicEnv basicEnv = BasicEnv.getInstance();
      environmentList.add(basicEnv);
      environmentList.add(s3Env);
    }
    if (!dynamoDBInfos.isEmpty()) { // add dynamodb environment
      dynamoDbEnv.init(dynamoDBInfos);
      environmentList.add(dynamoDbEnv);
    }
  }

  public Environment getDynamoDBEnv() {
    for (Environment env : environmentList)
      if (env instanceof DynamoDbEnv)
        return env;
    return null;
  }

  void resetEnv() {
    StatisticsManager.makeStep();
    for (Environment env : environmentList)
      env.reset();
    System.out.println("reset done at:" + LocalDateTime.now());
  }
}
