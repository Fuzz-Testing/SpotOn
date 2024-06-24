package edu.umn.cs.spoton;

import static edu.umn.cs.spoton.analysis.StaticAnalysisMain.disregardedJavaTypes;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;

import edu.umn.cs.spoton.analysis.influencing.CodeTarget;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * Maintains the coverage of code targets.
 *
 * @author Soha Hussein
 */
public class CodeTargetsCoverage {


  static CodeTargetsCoverage instance;

  static {
    try {
      instance = new CodeTargetsCoverage();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static void resetInstance() throws IOException {
    instance = new CodeTargetsCoverage();
  }
//  static boolean inReproMode = System.getProperty("input") != null;
//  static String buildDir =
//      System.getProperty("serverfuzz.PROJECT_DIR") != null ? System.getProperty(
//          "serverfuzz.PROJECT_DIR") + File.separator :
//          Paths.get("").toAbsolutePath() + "/target/";
//  static String outputDir = System.getProperty("jqf.RESULTS_OUTPUT_DIR");

  // used in typed EI where the static analysis tells us which scp needs coverage.
  //  contains all code targets encountered during execution.
  HashMap<CodeTarget, Set<Integer>> uncoveredCodeTargetsToArms = new HashMap<CodeTarget, Set<Integer>>();

  //  used in zest mode where we have no analysis to know in advance the needed to cover scp
  HashMap<CodeTarget, Set<Integer>> coveredCodeTargetsToArms = new HashMap<CodeTarget, Set<Integer>>();

  //used to collect edges of invocations.
  HashMap<CodeTarget, Set<String>> coveredTargetsToDynamicallyDispatchMap = new HashMap<CodeTarget, Set<String>>();


  String scpFullFileName;
  String allCoveragesFileName;

  public CodeTargetsCoverage() throws IOException {
    GuidanceConfig guidanceConfig = GuidanceConfig.getInstance();
    scpFullFileName = guidanceConfig.codeTargetDir + File.separator + guidanceConfig.engine + "_"
        + guidanceConfig.benchmarkName + "_ScpCov.txt";
    allCoveragesFileName =
        guidanceConfig.codeTargetDir + File.separator + guidanceConfig.engine + "_"
            + guidanceConfig.benchmarkName + "_AllCov.txt";

    new File(scpFullFileName).createNewFile();
    Files.write(new File(scpFullFileName).toPath(),
                "".getBytes(StandardCharsets.UTF_8),
                StandardOpenOption.CREATE, TRUNCATE_EXISTING);
    new File(allCoveragesFileName).createNewFile();
    Files.write(new File(allCoveragesFileName).toPath(),
                "".getBytes(StandardCharsets.UTF_8),
                StandardOpenOption.CREATE, TRUNCATE_EXISTING);
  }

  public static CodeTargetsCoverage getInstance() {
    return instance;
  }

  public void expandUncoveredComplexConditions(HashSet<CodeTarget> instrumentationScp) {
    HashSet<CodeTarget> matchingAnalysisScps = new HashSet<>();
    //mark all matches that might need expansion
    for (CodeTarget expandedScp : instrumentationScp) {
      CodeTarget expandedNoIdScp = new CodeTarget(expandedScp.toString());
      if (uncoveredCodeTargetsToArms.containsKey(expandedNoIdScp))
        matchingAnalysisScps.add(expandedScp);
    }

    //expand/append the iid to the scps and the arms
    for (CodeTarget matchingScp : matchingAnalysisScps) {
      CodeTarget expandedNoIdScp = new CodeTarget(matchingScp.toString());
      //a match will exist for all scp with the same hash including -1 for the iid.
      // since the static analysis does not know that id, it appends -1. It is only
      // when the class is loaded that we figure out these iid for branches, and
      // update existing scps to include those iids, and to be expanded if
      // multiple matches were found.
      uncoveredCodeTargetsToArms.remove(expandedNoIdScp);
      putUncovered(matchingScp, new HashSet<>(Arrays.asList(0, 1)));
    }
  }

  public Set<Integer> putUncovered(CodeTarget codeTarget, Set<Integer> arms) {
    return uncoveredCodeTargetsToArms.put(codeTarget, arms);
  }

  public boolean uncoveredContains(CodeTarget codeTarget) {
    return this.uncoveredCodeTargetsToArms.containsKey(codeTarget);
  }

  public Set<Integer> removeUncovered(CodeTarget codeTarget) throws IOException {
    return this.uncoveredCodeTargetsToArms.remove(codeTarget);
  }

  public boolean unCoveredContainsKey(CodeTarget codeTarget) {
    return this.uncoveredCodeTargetsToArms.containsKey(codeTarget);
  }

  public Set<Integer> uncoveredGetArms(CodeTarget codeTarget) {
    return this.uncoveredCodeTargetsToArms.get(codeTarget);
  }

  public Set<Integer> uncoveredRemoveArm(CodeTarget codeTarget, int arm, Date startTime) {
    Set<Integer> uncoveredArms = uncoveredGetArms(codeTarget);
    if (uncoveredArms.contains(arm)) {
      System.out.println(
          "encountering new codeTarget coverage : " + codeTarget);
      long durationToCoverage = ((new Date()).getTime() - startTime.getTime()) / 1000;
      uncoveredArms.remove(arm);

      Set<Integer> armsInCoverage = coveredCodeTargetsToArms.getOrDefault(codeTarget,
                                                                          new HashSet<>());
      assert armsInCoverage == null || !armsInCoverage.contains(arm)
          : "it cannot be in the covered map if we have defined it in the uncovered static analysis map";

      armsInCoverage.add(arm);
      coveredCodeTargetsToArms.put(codeTarget,
                                   armsInCoverage); // this ensures that next time around we hit it we do not mistakenly count it in addIfNewCoverage as a new coverage.
      try {
        String text = codeTarget.toString() + "_Arm_" + arm + "," + durationToCoverage + "\n";
        Files.write(Paths.get(scpFullFileName),
                    text.getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        Files.write(Paths.get(allCoveragesFileName),
                    text.getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        if (uncoveredArms.isEmpty())
          return removeUncovered(codeTarget);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
    return uncoveredArms;
  }


  public void addIfNewBranchCoverage(CodeTarget codeTarget, int arm, Date startTime) {

    boolean isJavaDisregardedType = disregardedJavaTypes.stream()
        .anyMatch(t -> codeTarget.getFullClassName().startsWith(t));
    String rootPackageName = GuidanceConfig.dependencyPackage;
    boolean methodInUserPackage =
        !isJavaDisregardedType && codeTarget.getFullClassName()
            .startsWith("L" + rootPackageName);

//    if (!methodInUserPackage)
//      return;
    Set<Integer> coveredArms = coveredCodeTargetsToArms.getOrDefault(codeTarget,
                                                                     new HashSet<>());
    if (coveredArms == null || !coveredArms.contains(arm)) {
      System.out.println(
          "encountering new codeTarget coverage : " + codeTarget);
      double durationToCoverage = ((new Date()).getTime() - startTime.getTime()) / 1000.0;
      coveredArms.add(arm);
      coveredCodeTargetsToArms.put(codeTarget, coveredArms);
      try {
        String text = codeTarget.toString() + "_Arm_" + arm + "," + durationToCoverage + "\n";
        Files.write(Paths.get(allCoveragesFileName),
                    text.getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        if (methodInUserPackage)
          Files.write(Paths.get(scpFullFileName),
                      text.getBytes(StandardCharsets.UTF_8),
                      StandardOpenOption.CREATE, StandardOpenOption.APPEND);

      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
  }


  public void addIfNewCallCoverage(CodeTarget codeTarget, String invokedMethod,
      Date startTime) {

    boolean isJavaDisregardedType = disregardedJavaTypes.stream()
        .anyMatch(t -> codeTarget.getFullClassName().startsWith(t));
    String rootPackageName = GuidanceConfig.dependencyPackage;
    if(rootPackageName==null || rootPackageName.isEmpty())
      System.err.println("Warning empty dependency package. This result in unsound scp_cov information.");
    boolean methodInUserPackage =
        !isJavaDisregardedType && (codeTarget.getFullClassName()
            .startsWith("L" + rootPackageName)
            || invokedMethod.startsWith(rootPackageName));

//    if (!methodInUserPackage)
//      return;
    Set<String> coveredDispatches = coveredTargetsToDynamicallyDispatchMap.getOrDefault(codeTarget,
                                                                                        new HashSet<>());
    if (coveredDispatches == null || !coveredDispatches.contains(invokedMethod)) {
      System.out.println(
          "encountering new Dynamic Dispatch coverage: " + codeTarget + "->" + invokedMethod);
      double durationToCoverage = ((new Date()).getTime() - startTime.getTime()) / 1000.0;
      coveredDispatches.add(invokedMethod);
      coveredTargetsToDynamicallyDispatchMap.put(codeTarget, coveredDispatches);
      try {
        String text =
            codeTarget.toString() + "->" + invokedMethod + "," + durationToCoverage + "\n";
        Files.write(Paths.get(allCoveragesFileName),
                    text.getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
//        System.out.println("methodInUserPackage = " + methodInUserPackage +", rootPackageName = "+ rootPackageName+" for scp" + text);
        if (methodInUserPackage)
          Files.write(Paths.get(scpFullFileName),
                      text.getBytes(StandardCharsets.UTF_8),
                      StandardOpenOption.CREATE, StandardOpenOption.APPEND);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
  }

  public boolean uncoveredIsEmpty() {
    if (uncoveredCodeTargetsToArms.isEmpty()) {
//      System.out.println("!!!!!!!!!!!!!UncoveredCodeTarget is EMPTY !!!!!!!!!!!!");
      return true;
    }
    return false;
  }


}