package edu.umn.cs.spoton;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;

public class GuidanceConfig {

  static GuidanceConfig instance = new GuidanceConfig();

  final boolean inReproMode = System.getProperty("input") != null;
  final String projectDir = System.getProperty("jqf.PROJECT_DIR");

  String buildDir =
      projectDir != null ? projectDir + File.separator :
          Paths.get("").toAbsolutePath() + "/target/";

  final String outputDir = System.getProperty("jqf.RESULTS_OUTPUT_DIR");

  String engine = System.getProperty("engine");

  public final boolean spotOnRunning;
  public final boolean zestRunning;
  public final boolean zestStrOptRunning;
  public static String dependencyPackage;


  public final boolean eiGenerationExperiment = Boolean.parseBoolean(
      System.getProperty("spoton.RUN_EI_GEN_EXPERIMENT"));

  boolean runningEiTypedMutationTest = false;

  public final String codeTargetDir;
  String benchmarkName;

  public GuidanceConfig() {
    System.out.println("engine in GuidanceConfig=" + engine);
    if (projectDir != null) {
      benchmarkName = projectDir.substring(0, projectDir.lastIndexOf(File.separator));
      benchmarkName =
          benchmarkName.substring(benchmarkName.lastIndexOf(File.separator) + 1);
    }
    if (!inReproMode)
      codeTargetDir = buildDir + File.separator + outputDir + "/cov";
    else
      codeTargetDir = buildDir + File.separator + outputDir + "/repro/cov";

    FileManipulation.initializeDir(codeTargetDir);
    spotOnRunning = engine != null && engine.equals("spotOn");
    zestRunning = engine != null && engine.equals("zest");
    zestStrOptRunning = engine != null && engine.equals("zestStrOpt");
    dependencyPackage =
        System.getProperty("spoton.DEPENDENCY_PACKAGE") != null ? System.getProperty(
            "spoton.DEPENDENCY_PACKAGE") : "";
    dependencyPackage = dependencyPackage.replaceAll("\\.", "/");
  }

  public void setRunningEiTypedMutationTest(boolean runningEiTypedMutationTest) {
    this.runningEiTypedMutationTest = runningEiTypedMutationTest;
  }

  public boolean isRunningEiTypedMutationTest() {
    return runningEiTypedMutationTest;
  }


  public static GuidanceConfig getInstance() {
    return instance;
  }

  public static void resetInstance() {
    instance = new GuidanceConfig();
  }


  public void invokeInfluencingAnalysis(boolean b) {
    try {
      String analysisMavenPath = System.getProperty("analysisMavenPath");
      assert analysisMavenPath
          != null : "cannot run analysis, path of the analysis project has not been specified.";

      String mainClassName = "edu.umn.cs.spoton.analysis.StaticAnalysisMain";

      String dependencyEntryClass = System.getProperty("dependencyEntryClass");
      String jarName = System.getProperty("jarName");

      File target = new File(projectDir);
      String programArguments = b + " " + dependencyPackage + " " + dependencyEntryClass + " " +
          target.getAbsolutePath() + "/" + jarName + " " + "fakeMain()V;" + " "
          + target + "/" + outputDir;

      System.out.println("programArguments  = " + programArguments);
      String[] command = {"mvn", "exec:java", "-Dexec.mainClass=" + mainClassName,
          "-Dexec.args=" + programArguments};

      ProcessBuilder processBuilder = new ProcessBuilder(command)
          .directory(new File(analysisMavenPath))
          .inheritIO(); // Redirect output to the current process

      Process process = processBuilder.start();

      int exitCode = process.waitFor();

      System.out.println("Maven process exited with code: " + exitCode);
    } catch (IOException | InterruptedException e) {
      e.printStackTrace();
    }
  }
}
