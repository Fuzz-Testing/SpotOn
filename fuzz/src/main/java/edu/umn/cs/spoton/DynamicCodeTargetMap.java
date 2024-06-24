package edu.umn.cs.spoton;

import static java.lang.Math.ceil;
import static java.lang.Math.log;

import edu.umn.cs.spoton.analysis.influencing.CodeTarget;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;


public class DynamicCodeTargetMap {

  protected final int GEOMETRIC_COST_FACTOR = 10;

  /**
   * types within the code targetss mapped to their priorities. As types are not mapped to
   * their execution indexing counterparts, we increment the prioirty of the particular type,
   * indicating that it is less likely be selected than others that have not been incremented. 88/
   */
  LinkedHashMap<String, Float> typeToPriorityDepthMap = new LinkedHashMap<String, Float>();

  HashMap<String, Float> originalDepthsMap;

  public DynamicCodeTargetMap(
      HashMap<CodeTarget, Map<String, Integer>> codeTargetsToTypesMap) {

    assert codeTargetsToTypesMap.size()
        > 0 : "no codeTargets found; Analysis could find influencing types.";

    List<Map<String, Integer>> typesMap = new ArrayList<>(codeTargetsToTypesMap.values());

    typesMap.sort((m1, m2) -> Integer.compare(m2.size(), m1.size())); //descending order

    // we add to typeToPriorityDepthMap giving higher preferences to prioritize set by deeper influencing analysis.
    for (int i = 0; i < typesMap.size();
        i++) {
      Map<String, Integer> codeTargetTypeInfluenceMap = typesMap.get(i);
      codeTargetTypeInfluenceMap.forEach((e, v) -> {
        if (!typeToPriorityDepthMap.containsKey(e))
          typeToPriorityDepthMap.put(e, Float.valueOf(v));
      });
    }

    originalDepthsMap = new HashMap<>(typeToPriorityDepthMap);
  }

  /**
   * decrease types depth if unsuccessful match has occurred making it less likely to be picked up,
   * but does not mess up with other pre-determined types.
   *
   * @param type
   */
  public void handleSelectionEffect(String type,
      boolean dynamicTypeMatchFound) { // (3/4) -- 1/(3/4) = 4/3)
//    typeToPriorityDepthMap.put(type, typeToPriorityDepthMap.get(type) + 1);
    Float currentDepth = typeToPriorityDepthMap.get(type);
    if (dynamicTypeMatchFound) { //placing lower bound
      currentDepth *= 3 / 4.0f;
      Float originalDist = originalDepthsMap.get(type);
      currentDepth = currentDepth < originalDist ? originalDist : currentDepth;
    } else {
      currentDepth *= 4 / 3.0f;
      currentDepth = currentDepth >= 1000 ? 1000 : currentDepth; //placing upper bound
    }
    typeToPriorityDepthMap.put(type, currentDepth);
  }

  public String selectMutationType(Random random) {
    ArrayList<String> sortedTypes = new ArrayList<>(typeToPriorityDepthMap.keySet());
    sortedTypes.sort((t1, t2) -> Float.compare(typeToPriorityDepthMap.get(t1),
                                               typeToPriorityDepthMap.get(t2)));
    HashMap<String, Float> normalizedTypeToPriorityDepthMap = new HashMap<>();
    for (Map.Entry e : typeToPriorityDepthMap.entrySet())
      normalizedTypeToPriorityDepthMap.put((String) e.getKey(), 1.0f
          / (float) e.getValue()); //normalizedTypeToPriorityDepthMap not normalized yet

    float totalPriorities = ((float) (normalizedTypeToPriorityDepthMap.values().stream()
        .mapToDouble(f -> f.floatValue()).sum()));
    for (String type : sortedTypes) {
      Float normalizedPriority = normalizedTypeToPriorityDepthMap.get(type)
          / totalPriorities;
      normalizedTypeToPriorityDepthMap.put(type, normalizedPriority);
    }
    float selection = random.nextFloat();
    for (String type : sortedTypes) {
      Float typeNormalizedPriority = (Float) normalizedTypeToPriorityDepthMap.get(type);
      selection -= typeNormalizedPriority;
      if (selection <= 0)
        return type;
    }
    assert false : "looks like there is something wrong in the normalization";
    return null;
  }

  private String findTypeToMutate(ArrayList<String> typeToDepth, Random random) {

    int typePosition = 0;
    do
      //choosing a typePosition using geometric distribution within the size of the type.
      typePosition = weightedSampleGeometric(random, typeToDepth.size());
//      System.out.println("printing mutation typePosition:" + typePosition);
    while (typePosition >= typeToDepth.size());

    return typeToDepth.get(typePosition).toString();
  }


  /*
  * (a) p
    (b) (1-p)^(n-1) p		n = length

    f = (a)/(b)

    f = p / (1-p)^(n-1) p
      = 1 / (1-p)^(n-1)
      =  (1-p)^-(n-1)
      =  (1-p)^(-n+1)
      =  (1-p)^(1-n)

    f^(1/(1-n))  =  ((1-p)^(1-n))^(1/(1-n))
    f^(1/(1-n))  =  1-p
    1-f^(1/(1-n))  =  p
  * */
  private int weightedSampleGeometric(Random random, int n) {
    assert n >= 1 : "unexpected size to select from. Failing.";
    if (n == 1)
      return 0;

    double exponent = (1.0d / (1 - n));
    double p = (1 - Math.pow(GEOMETRIC_COST_FACTOR, exponent));
    double uniform = random.nextDouble();
    return (int) ceil(log(1 - uniform) / log(1 - p)) - 1;
  }
}
