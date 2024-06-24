/*
 * Copyright (c) 2019, The Regents of the University of California
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * 1. Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package edu.umn.cs.spoton;

import com.ibm.wala.ipa.callgraph.CallGraphBuilderCancelException;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import edu.berkeley.cs.jqf.fuzz.ei.ExecutionContext;
import edu.berkeley.cs.jqf.fuzz.ei.ExecutionIndex;
import edu.berkeley.cs.jqf.fuzz.ei.ExecutionIndexingGuidance;
import edu.berkeley.cs.jqf.fuzz.ei.TypedExecutionIndex;
import edu.berkeley.cs.jqf.fuzz.ei.state.JanalaExecutionIndexingState;
import edu.berkeley.cs.jqf.fuzz.guidance.GuidanceException;
import edu.berkeley.cs.jqf.fuzz.guidance.Result;
import edu.berkeley.cs.jqf.fuzz.guidance.TimeoutException;
import edu.berkeley.cs.jqf.fuzz.util.TypedEiCoverage;
import edu.berkeley.cs.jqf.instrument.tracing.events.CallEvent;
import edu.berkeley.cs.jqf.instrument.tracing.events.TraceEvent;
import edu.umn.cs.spoton.analysis.influencing.CodeTarget;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import org.openjdk.jol.info.GraphLayout;
import org.openjdk.jol.vm.VM;

/**
 * A guidance that uses typed execution indices to direct mutation to influencing types
 *
 * @author Soha Hussein
 */
public class SpotOnGuidance extends ExecutionIndexingGuidance {

  public String eiTypeToMutate = null; //used for unit tests

  /**
   * Probability that a standard mutation sets the byte to just zero instead of a random value.
   * Restored it back to 0.1 just like Zest's configuration, to simplify the comparison.
   */
  protected final double MUTATION_ZERO_PROBABILITY = 0.1;

  /**
   * Mean number of type mutations to perform in each round.
   */
  protected final double MEAN_TYPE_MUTATION_COUNT = 4.0;

  //  contains all code targetss encountered during execution.
  static DynamicCodeTargetMap dynamicCodeTargetMap;

  //  File genDetailsFile;
  //  File janalaEiVisitFile = Debug.createNewFile(outputDirectory, "janalaEiVisit", "test#, duration");
  public double janalaEiVisitDur = 0;

  //  File handleEventFile = Debug.createNewFile(outputDirectory, "handleEvent", "test#, duration");
//  public double handleEventDur = 0;

  //  File readFile = Debug.createNewFile(outputDirectory, "readFile", "test#, duration");
  public double readDur = 0;

  public double readGetExecutionIndexDur = 0;
  public double readGetOrGenerateFreshDur = 0;
  public double valuesMapGet = 0;

  public double putTypesInMapDur = 0;

  public double generateNputValuesMap = 0;

  /**
   * Constructs a new EI guidance instance with optional duration, optional trial limit, and
   * possibly deterministic PRNG.
   *
   * @param testName           the name of test to display on the status screen
   * @param duration           the amount of time to run fuzzing for, where {@code null} indicates
   *                           unlimited time.
   * @param trials             the number of trials for which to run fuzzing, where {@code null}
   *                           indicates unlimited trials.
   * @param outputDirectory    the directory where fuzzing results will be written
   * @param sourceOfRandomness a pseudo-random number generator
   * @throws IOException if the output directory could not be prepared
   */
  public SpotOnGuidance(String testName, Duration duration, Long trials,
      File outputDirectory, Random sourceOfRandomness)
      throws IOException {
    super(testName, duration, trials, outputDirectory, sourceOfRandomness);
  }

  /**
   * Creates a new SpotOn guidance used as the entry point of the SpotOnDriver
   *
   * @param testName        the name of test to display on the status screen
   * @param duration        the amount of time to run fuzzing for, where {@code null} indicates
   *                        unlimited time.
   * @param outputDirectory the directory where fuzzing results will be written
   * @throws IOException if the output directory could not be prepared
   */
  public SpotOnGuidance(String testName, Duration duration, File outputDirectory) throws IOException {
    this(testName, duration, null, outputDirectory, new Random());
  }

  /**
   * Constructs a new EI guidance instance with seed input directory and optional duration, optional
   * trial limit, an possibly deterministic PRNG.
   *
   * @param testName           the name of test to display on the status screen
   * @param duration           the amount of time to run fuzzing for, where {@code null} indicates
   *                           unlimited time.
   * @param trials             the number of trials for which to run fuzzing, where {@code null}
   *                           indicates unlimited trials.
   * @param outputDirectory    the directory where fuzzing results will be written
   * @param seedInputDir       the directory containing one or more input files to be used as
   *                           initial inputs
   * @param sourceOfRandomness a pseudo-random number generator
   * @throws IOException if the output directory could not be prepared
   */
  public SpotOnGuidance(String testName, Duration duration, Long trials,
      File outputDirectory, File seedInputDir, Random sourceOfRandomness,
      HashMap<CodeTarget, Map<String, Integer>> codeTargetsToTypesMap)
      throws IOException, ClassHierarchyException, CallGraphBuilderCancelException {
    super(testName, duration, trials, outputDirectory, seedInputDir, sourceOfRandomness);
    dynamicCodeTargetMap = new DynamicCodeTargetMap(codeTargetsToTypesMap);
    for (CodeTarget point : codeTargetsToTypesMap.keySet())
      CodeTargetsCoverage.instance.putUncovered(point, new HashSet<>(
          Arrays.asList(0, 1)));
//    genDetailsFile = Debug.createNewFile(outputDirectory, "genDetails",
//                                         "test#, inputSize, janalaEiVisit, handleEvent, readDur, "
//                                             + "readGetExecutionIndexDur, readGetOrGenerateFreshDur, "
//                                             + "valuesMapGet, generateNputValuesMap, putTypesInMapDur\n");
  }

  //  TODO: This can be combined with the coverage within the Coverage.java, where the uncovered can return false.
  public static void addCoveredBranchCodeTarget(CodeTarget encounteredCodeTarget, int arm) {
    if (CodeTargetsCoverage.instance.unCoveredContainsKey(
        encounteredCodeTarget))
      CodeTargetsCoverage.instance.uncoveredRemoveArm(encounteredCodeTarget,
                                                      arm, startTime);
    else
      CodeTargetsCoverage.instance.addIfNewBranchCoverage(
          encounteredCodeTarget, arm, startTime);
  }

  public static void expandUncoveredComplexConditions(HashSet<CodeTarget> instrumentedScp) {
    CodeTargetsCoverage.instance.expandUncoveredComplexConditions(
        instrumentedScp);
  }

  /**
   * Returns the banner to be displayed on the status screen
   */
  protected String getTitle() {
    return "Semantic Fuzzing with Typed Execution Indexes\n"
        + "---------------------------------------\n";
  }


  /**
   * Spawns a new input from thin air (i.e., actually random)
   */
  @Override
  protected Input<?> createFreshInput() {
    return new MappedTypedInput();
  }

  /**
   * Overrides the createParameterStream from the ExecutionIndexGuidance as we want to include the
   * number of bytes that have been read as well. This local variable is important to simulate the
   * one that is used by Zest, which we want to compare to at times when running the
   * eiGenerationExperiment
   *
   * @return
   */
  @Override
  protected InputStream createParameterStream() {
    // Return an input stream that uses the EI map
    return new InputStream() {
      int bytesRead = 0;

      @Override
      public int read() throws IOException {
        Instant beforeDate;
        Instant afterDate;
        Instant readBeforeDate = Instant.now();
        // lastEvent must not be null
        if (eiState.getLastEventIid() == -1) {
          throw new GuidanceException("Could not compute execution index; no instrumentation?");
        }

//        assert currentInput instanceof MappedInput : "This guidance should only mutate MappedInput(s)";

        MappedTypedInput mappedInput = (MappedTypedInput) currentInput;

        beforeDate = Instant.now();
        // Get the execution index of the last event
        ExecutionIndex executionIndex = eiState.getExecutionIndex(eiState.getLastEventIid());
        afterDate = Instant.now();
        readGetExecutionIndexDur +=
            Duration.between(beforeDate, afterDate).toNanos() / 1_000_000_000.0;

        beforeDate = Instant.now();

        // Attempt to get a value from the map, or else generate a random value
        int value = mappedInput.getOrGenerateFresh(executionIndex, bytesRead++, random);
//        System.out.println("read (" + bytesRead + ")=" + value);
        afterDate = Instant.now();
        readGetOrGenerateFreshDur +=
            Duration.between(beforeDate, afterDate).toNanos() / 1_000_000_000.0;
        Instant readAfterDate = Instant.now();
        readDur += Duration.between(readBeforeDate, readAfterDate).toNanos() / 1_000_000_000.0;
        return value;
      }
    };
  }

  protected void handleEvent(TraceEvent e) {
    if (!testEntered) {
      // Check if this event enters the test method
      if (e instanceof CallEvent) {
        CallEvent callEvent = (CallEvent) e;
        if (callEvent.getInvokedMethodName().startsWith(entryPoint)) {
          testEntered = true;
        }
      }
    }

    /* If test method has not yet been entered, then ignore code coverage, but compute execution indices.
     otherwise, ignore the execution indices computation, because non is going to be useful for our purposes
     within the test execution; instead, compute the coverages*/
    Instant beforeDate = Instant.now();
    Instant afterDate;
    if (!testEntered) {
      if (eiState instanceof JanalaExecutionIndexingState) {
//        beforeDate = Instant.now();
        // Update execution indexing logic regardless of whether we are in generator or test method
        e.applyVisitor((JanalaExecutionIndexingState) eiState);
//        afterDate = Instant.now();
//        janalaEiVisitDur += Duration.between(beforeDate, afterDate).toNanos() / 1_000_000_000.0;
      }
    } else {
//      beforeDate = Instant.now();
      // Collect totalCoverage
      ((TypedEiCoverage) runCoverage).handleEvent(e);
    }
    afterDate = Instant.now();
    handleEventDur += Duration.between(beforeDate, afterDate).toNanos() / 1_000_000_000.0;

    if (!GuidanceConfig.getInstance().eiGenerationExperiment) {
      beforeDate = Instant.now();
      if (this.singleRunTimeoutMillis > 0 &&
          this.runStart != null && (++this.branchCount) % 10_000 == 0) {
        long elapsed = new Date().getTime() - runStart.getTime();
        if (elapsed > this.singleRunTimeoutMillis) {
          throw new TimeoutException(elapsed, this.singleRunTimeoutMillis);
        }
      }
      afterDate = Instant.now();
      modEventDur += Duration.between(beforeDate, afterDate).toNanos() / 1_000_000_000.0;
    }
  }

  /**
   * Handles the result of a test execution. This is exactly similar to the implementation within
   * ExecutionIndexingGuidance except for two things: 1. in eiGenerationExpirement
   */
  @Override
  public void handleResult(Result result, Throwable error) throws GuidanceException {
    super.handleResult(result, error);
  }

/*  public void appendGenDetails() {

    Debug.appendLineToFile(genDetailsFile,
                           numTrials + "," + currentInput.size() + "," + janalaEiVisitDur
                               + "," + handleEventDur + "," + readDur + ","
                               + readGetExecutionIndexDur + ","
                               + readGetOrGenerateFreshDur + "," + valuesMapGet + ","
                               + generateNputValuesMap
                               + "," + putTypesInMapDur + "\n");

    janalaEiVisitDur = 0;
    handleEventDur = 0;
    readDur = 0;
    readGetExecutionIndexDur = 0;
    readGetOrGenerateFreshDur = 0;
    valuesMapGet = 0;
    generateNputValuesMap = 0;
    putTypesInMapDur = 0;
  }*/


  /**
   * A candidate test input represented as a map from execution indices to integer values.
   *
   * <p>When a quickcheck-like generator requests a new ``random'' byte,
   * the current execution index is used to retrieve the input from this input map (a fresh value is
   * generated and stored in the map if the key is not mapped).</p>
   *
   * <p>Inputs should not be publicly mutable. The only way to mutate
   * an input is via the {@link #fuzz} method which produces a new input object with some values
   * mutated.</p>
   */
  public class MappedTypedInput extends MappedInput {

    /**
     * A map from types to executionIndex elements.
     */
    protected TypesMap map;
    public LinearInput linearInput;

    /**
     * Create an empty input map.
     */
    public MappedTypedInput() {
      super();
      map = new TypesMap();
      linearInput = GuidanceConfig.getInstance().eiGenerationExperiment ? new LinearInput() : null;
    }

    public Integer getValueInMap(ExecutionIndex e) {
      return valuesMap.get(e);
    }

    /**
     * Create a copy of an existing input map.
     *
     * @param toClone the input map to clone
     */
    public MappedTypedInput(MappedTypedInput toClone) {
      super(toClone);
      map = toClone.map;
//      toClone.map.cloneMap();
      if (GuidanceConfig.getInstance().eiGenerationExperiment)
        linearInput = new LinearInput(toClone.linearInput);
    }

    @Override
    public long computeVmMemory(boolean isDeepComputation) {
      /**
       * The deep memory of FCI can be roughly approximated as
       * 1. the shallow size of the linear input, since the values are going to be deeply counted in the valuesMap in super
       * 2. the deep size of the string types within the typesMap (keys)
       * 3. the shallow size of the ei arrays within the types map, since its deep computation will be computed within super
       * 4. the deep size of the supper which will include the valuesMap and the orderedKeys, the first is deep the latter is shallow
       */
      if (isDeepComputation) {
        long linearInputShallowMem = linearInput.computeVmMemory(false);
        long typesMapKeyDeepMem = GraphLayout.parseInstance(map.getTypesInMap()).totalSize();
        long typesMapValueShallowMem = VM.current().sizeOf(map.getValuesInMap());
        long superDeepMem = super.computeVmMemory(true);
        return linearInputShallowMem + typesMapKeyDeepMem + typesMapValueShallowMem + superDeepMem;
      } else
        return VM.current().sizeOf(map.getTypesInMap()) + linearInput.computeVmMemory(false)
            + super.computeVmMemory(false);
    }

//TODO:NEED TO CLEAN UP THE CODE COMMENTS

    public int getOrGenerateFresh(ExecutionIndex key, int linearKey, Random random)
        throws IllegalStateException {

      Instant beforeDate;
      Instant afterDate;
      if (executed) {
        throw new IllegalStateException("Cannot generate fresh values after execution");
      }

      // If we reached a limit, then just return EOF
      if (orderedKeys.size() >= MAX_INPUT_SIZE) {
        return -1;
      }

      Integer val;
      beforeDate = Instant.now();
      val = valuesMap.get(key);
      afterDate = Instant.now();
      valuesMapGet += Duration.between(beforeDate, afterDate).toNanos() / 1_000_000_000.0;

      // If not, generate a new value
      if (val == null || GuidanceConfig.getInstance().eiGenerationExperiment) {
        beforeDate = Instant.now();
        // If we could not splice or were unsuccessful, try to generate a new input
        if (GENERATE_EOF_WHEN_OUT)
          return -1;
        else {
          if (GuidanceConfig.getInstance().eiGenerationExperiment)
            val = linearInput.getOrGenerateFresh(linearKey, random);
          else
            // Just generate a random input
            val = random.nextInt(256);

          // Put the new value into the map
          valuesMap.put(key, val);
        }
        afterDate = Instant.now();
        generateNputValuesMap +=
            Duration.between(beforeDate, afterDate).toNanos() / 1_000_000_000.0;

      }
      // Mark this key as visited
      orderedKeys.add(key);
      beforeDate = Instant.now();
      putInTypesMap(key, map);
      afterDate = Instant.now();
      putTypesInMapDur += Duration.between(beforeDate, afterDate).toNanos() / 1_000_000_000.0;

      return val;
    }


    /**
     * Associates the execution index to all non-primitive the call stack types.
     *
     * @param ei
     * @param typesMap
     */
    private void putInTypesMap(ExecutionIndex ei, TypesMap typesMap) {
//      assert ei.equals(orderedKeys.get(orderedKeys.size()
//                                           - 1)) : "both ei and orderedKeys -1 should match up. Violation detected. Failing";
//      assert ei instanceof TypedExecutionIndex : "ei should be typed, but found untyped ei";
      String[] eiTypes = ((TypedExecutionIndex) ei).getCallTypes();
      if (eiTypes != null && eiTypes.length > 0) {
        for (String type : eiTypes)
          typesMap.add(type, orderedKeys.size() - 1);
      }
    }

    /**
     * Trims the input map of all keys that were never actually requested since its construction.
     *
     * <p>Although this operation mutates the underlying object, the effect should
     * not be externally visible (at least as long as the test executions are== deterministic).</p>
     */
    @Override
    public void gc() {
//      System.out.println("size of valuesMap before = " + valuesMap.size());
      LinkedHashMap<ExecutionIndex, Integer> newMap = new LinkedHashMap<>();

      for (ExecutionIndex key : orderedKeys)
        newMap.put(key, valuesMap.get(key));

      valuesMap = newMap;
//      assert
//          valuesMap.size()
//              == orderedKeys.size() : "valuesMap and orderedKeys must be of same size";
      if (GuidanceConfig.getInstance().eiGenerationExperiment)
        this.linearInput.gc();
      // Set the `executed` flag
      executed = true;
//      System.out.println("size of valuesMap after = " + valuesMap.size());
    }

    /**
     * Return a new input derived from this one with some values mutated.
     *
     * <p>This method performs one or both of random mutations
     * and splicing.</p>
     *
     * <p>Random mutations are done by performing M
     * mutation operations each on a random contiguous sequence of N bytes, where M and N are
     * sampled from a geometric distribution with mean {@link #MEAN_MUTATION_COUNT} and
     * {@link #MEAN_MUTATION_SIZE} respectively.</p>
     *
     * <p>Splicing is performed by first randomly choosing a location and
     * its corresponding execution context in this input's value map, and then copying a contiguous
     * sequence of up to Z bytes from another input, starting with a location that also maps the
     * same execution context. Here, Z is sampled from a uniform distribution from 0 to
     * {@link #MAX_SPLICE_SIZE}.</p>
     *
     * @param random       the PRNG
     * @param ecToInputLoc map of execution contexts to input-location pairs
     * @return a newly fuzzed input
     */
    protected MappedInput fuzz(Random random,
        Map<ExecutionContext, ArrayList<InputLocation>> ecToInputLoc) {

      //if not alwaysDoTypeMutation then it is 80-20 chance of it working
      // stop type fuzzing if our uncoveredCodeTargets is empty.
      boolean doTypeMutation = !GuidanceConfig.getInstance().eiGenerationExperiment && (
          !GuidanceConfig.getInstance().isRunningEiTypedMutationTest() ? random.nextFloat() >= 0.5
              && !CodeTargetsCoverage.instance.uncoveredIsEmpty()
              && !this.map.typesMap.isEmpty()
              : true);
      MappedTypedInput newInput = new MappedTypedInput(this);

      if (doTypeMutation && !GuidanceConfig.getInstance().isRunningEiTypedMutationTest()
          && CodeTargetsCoverage.instance.uncoveredIsEmpty()) {
        System.out.println(
            "!!!!!!!!!!!!!UncoveredCodeTarget is EMPTY !!!!!!!!!!!! -- reverting to zest fuzzing\n ");
        return randomFuzz(newInput);
      }

      if (doTypeMutation) {
        // Derive new input from this object as source
        MappedInput mutatedInput = typedRandomMutation(newInput);
        if (mutatedInput
            == null) { //revert to regularly fuzzing if we exceeded the number of attempts to find a matching type between the static analysis and the EI.
          System.out.println("searching for type to mutate failed. Reverting to Zest fuzzing");
          return randomFuzz(newInput);
        } else
          return mutatedInput;
      } else
        System.out.println("Reverting to Zest fuzzing");
      return randomFuzz(newInput);
    }

    //    make random fuzzing using the orderedKeys DS
    private MappedInput randomFuzz(MappedTypedInput newInput) {
      if (GuidanceConfig.getInstance().eiGenerationExperiment) {
        newInput.linearInput = (LinearInput) linearInput.fuzz(random);
        return newInput;
      }

      int numMutations = sampleGeometric(random, MEAN_MUTATION_COUNT);
      newInput.desc += ",havoc:" + numMutations;
      boolean setToZero =
          random.nextDouble() < 0.1; // one out of 10 times
      if (setToZero) {
        newInput.desc += "=0";
      }
      for (int mutation = 1; mutation <= numMutations; mutation++) {

        assert newInput.valuesMap.size()
            == orderedKeys.size() : "At this point both of these DS should match up. Violation detected. Failing";

        // Select a random offset and size
        int offset = random.nextInt(newInput.valuesMap.size());
        int mutationSize = sampleGeometric(random, MEAN_MUTATION_SIZE);
        // infoLog("[%d] Mutating %d bytes at offset %d", mutation, mutationSize, offset);

        newInput.desc += String.format("(%d@%d)", mutationSize, offset);
        for (int i = offset; i < offset + mutationSize && i < newInput.valuesMap.size(); i++) {
          ExecutionIndex e = orderedKeys.get(i);
          // infoLog("Mutating: %s", e.getKey());
          // Apply a random mutation
          int mutatedValue = setToZero ? 0 : random.nextInt(256);
//          assert newInput.valuesMap.containsKey(
//              e) : "execution index should exist in valuesMap. Failing";
          newInput.valuesMap.put(e, mutatedValue);
        }
      }
      newInput.clearTypes();
      return newInput;
    }

    private MappedInput typedRandomMutation(MappedTypedInput newInput) {
      int numMutations = sampleGeometric(random, MEAN_TYPE_MUTATION_COUNT);
      newInput.desc += ",havoc:" + numMutations;

      int eiIndex;

      String typeToMutate = eiTypeToMutate == null ? findTypeToMutate(newInput) : eiTypeToMutate;

      if (typeToMutate == null) //attempts for finding a dynamic type failed.
        return null;

      for (int mutation = 1; mutation <= numMutations; mutation++) {
        eiIndex = newInput.map.selectIndexForType(random, typeToMutate);
//        assert eiIndex != -1 : "eiIndex should be matched up to a dynamic type at this point.";
//        System.out.println("Type to mutate is : " + typeToMutate);
        MutatedTypesStates.add(numTrials, typeToMutate);

        int mutationSize = sampleGeometric(random, MEAN_MUTATION_SIZE);

        infoLog("[%d] Mutating %d bytes at offset %d", mutation, mutationSize, eiIndex);
        boolean setToZero =
            random.nextDouble() < MUTATION_ZERO_PROBABILITY; // one out of 10 times
        if (setToZero) {
          newInput.desc += "=0";
        }
        for (int m = eiIndex; m < eiIndex + mutationSize
            && m < orderedKeys.size(); m++) {
          int mutatedValue = setToZero ? 0 : random.nextInt(256);
          ExecutionIndex eiToMutate = orderedKeys.get(m);
//          assert eiToMutate instanceof TypedExecutionIndex : "expected typed ei, but found untyped.";
          if (((TypedExecutionIndex) eiToMutate).hasCallType(typeToMutate))
            newInput.valuesMap.put(eiToMutate, mutatedValue);
        }
        newInput.desc += String.format("%d", eiIndex);
      }
      newInput.clearTypes(); //clear the typesMap which is going to be populated entirely when orderedKeys are populated during getOrGenerateFresh
      return newInput;
    }

    private String findTypeToMutate(MappedTypedInput newInput) {
      int eiIndex = -1;
      int typeAttempts = 0;
      String typeToMutate = null;
      while (eiIndex == -1 && typeAttempts < 10) {
        typeToMutate = dynamicCodeTargetMap.selectMutationType(random);
//        System.out.println("check type for mutation: " + typeToMutate);
        eiIndex = newInput.map.selectIndexForType(random, typeToMutate);
        if (eiIndex == -1)
          System.out.println("type " + typeToMutate + " cannot be matched with ei points");
        if (!GuidanceConfig.getInstance().isRunningEiTypedMutationTest())
          dynamicCodeTargetMap.handleSelectionEffect(typeToMutate, eiIndex != -1);
        typeAttempts++;
      }
      if (eiIndex == -1)
        return null;
      else
        return typeToMutate;
    }

    public void clearTypes() {
      map = new TypesMap();
    }

  }

  //mute that function since we are not doing splicing
  @Override
  protected void mapEcToInputLoc(Input input) {
  }

  class TypesMap {

    protected HashMap<String, List<int[]>> typesMap;

    private TypesMap(HashMap<String, List<int[]>> typesMap) {
      this.typesMap = typesMap;
    }

    private TypesMap() {
      this.typesMap = new HashMap<>();
    }

    public void add(String typeName, int index) {

      boolean isTypeInMap = typesMap.containsKey(typeName);

      if (isTypeInMap) {
//        if it is in the map, then we need to find the last range, and see if we can append to it
        List<int[]> typeRanges = typesMap.get(typeName);
        int[] lastRange = typeRanges.get(typeRanges.size() - 1);
        if (lastRange[1] + 1 == index)
          lastRange[1] += 1; // expand the range by 1
        else { //create a new range.
//          assert index
//              > lastRange[1] : "unexpected index, we should always see increments, might be in different ranges, but we should not go back.";
          typeRanges.add(new int[]{index, index});
        }
      } else {
//        if type is not in the map, then insert a new type where the beginning and end ranges are the same.
        List<int[]> typeRanges = new ArrayList<>();
        typeRanges.add(new int[]{index, index});
        typesMap.put(typeName, typeRanges);
      }

    }

    public Set<String> getTypesInMap() {
      return typesMap.keySet();
    }

    public Collection<List<int[]>> getValuesInMap() {
      return typesMap.values();
    }

    public TypesMap cloneMap() {
      HashMap<String, List<int[]>> newTypesMap = new HashMap<>();

      for (String type : typesMap.keySet()) {
        List<int[]> ranges = typesMap.get(type);
        List<int[]> newRanges = new ArrayList<>(ranges.size());

//      cloning ranges.
        for (int i = 0; i < ranges.size(); i++)
          newRanges.add(ranges.get(i).clone());

        newTypesMap.put(type, newRanges);
      }
      return new TypesMap(newTypesMap);
    }

    public int selectIndexForType(Random r, String typeToMutate) {
      List<int[]> typeRanges = typesMap.get(typeToMutate);
      if (typeRanges == null)
        return -1;
      else {
        int[] selectedRange = typeRanges.get(r.nextInt(typeRanges.size()));
        //since r.nextInt is not inclusive of the max range, we need to increment it by 1
        int selectedEiIndex = r.nextInt(selectedRange[1] + 1 - selectedRange[0]);
        return selectedRange[0] + selectedEiIndex;
      }
    }
  }
}
