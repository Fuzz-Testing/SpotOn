package edu.umn.cs.spoton;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import edu.berkeley.cs.jqf.fuzz.ei.TypedExecutionIndex;
import edu.berkeley.cs.jqf.fuzz.ei.ZestGuidance.Input;
import edu.umn.cs.spoton.SpotOnGuidance.MappedTypedInput;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Random;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class SpotOnGuidanceTest {

  private static Random r;
  private SpotOnGuidance g;

  private TypedExecutionIndex e1 = new TypedExecutionIndex(new int[]{1, 1},
                                                           new String[]{"Square", "Circle",
                                                               "triangle"});
  private TypedExecutionIndex e2 = new TypedExecutionIndex(new int[]{1, 1, 3, 4},
                                                           new String[]{"Date", "String",
                                                               "Square"}); // Same EC as e4/e6
  private TypedExecutionIndex e3 = new TypedExecutionIndex(new int[]{1, 1, 2, 1},
                                                           new String[]{"Square"});
  private TypedExecutionIndex e4 = new TypedExecutionIndex(new int[]{1, 2, 3, 4},
                                                           new String[]{
                                                               "Circle"}); // Same EC as e2/e6
  private TypedExecutionIndex e5 = new TypedExecutionIndex(new int[]{1, 1, 5, 5},
                                                           new String[]{"triangle"});
  private TypedExecutionIndex e6 = new TypedExecutionIndex(new int[]{1, 2, 3, 6},
                                                           new String[]{
                                                               "Diamond"}); // Same EC as e2/e4
  private TypedExecutionIndex e7 = new TypedExecutionIndex(new int[]{1, 2, 2, 1},
                                                           new String[]{"Cube"});


  @Before
  public void seedRandom() throws IOException {
    r = new Random(42);
    g = new SpotOnGuidance("test", null, null,
                           Files.createTempDirectory("fuzz-out").toFile(), r);
    GuidanceConfig.getInstance().setRunningEiTypedMutationTest(true);
  }

  @BeforeClass
  public static void createGuidanceInstance() {
    System.setProperty("engine", "spotOn");
    GuidanceConfig.resetInstance();
  }

  @Test
  public void testGetOrFresh() {
    MappedTypedInput input = g.new MappedTypedInput();
    int bytesRead = 0;
    int k1a = input.getOrGenerateFresh(e1, bytesRead++, r);
    int k1b = input.getOrGenerateFresh(e1, bytesRead++, r);
    assertEquals(k1a, k1b);

    int k2a = input.getOrGenerateFresh(e2, bytesRead++, r);
    int k2b = input.getOrGenerateFresh(e2, bytesRead++, r);
    assertEquals(k2a, k2b);

    int k1c = input.getOrGenerateFresh(e1, bytesRead++, r);
    assertEquals(k1a, k1c);
  }

  @Test
  public void testClone() {
    MappedTypedInput input = g.new MappedTypedInput();
    int bytesRead = 0;
    int k1 = input.getOrGenerateFresh(e1, bytesRead++, r);
    int k2 = input.getOrGenerateFresh(e2, bytesRead++, r);
    int k3 = input.getOrGenerateFresh(e3, bytesRead++, r);
    int k4 = input.getOrGenerateFresh(e4, bytesRead++, r);
    int k5 = input.getOrGenerateFresh(e5, bytesRead++, r);

    MappedTypedInput clone = g.new MappedTypedInput(input);
    clone.clearTypes();
    assertEquals(k1, clone.getOrGenerateFresh(e1, bytesRead++, r));
    assertEquals(k2, clone.getOrGenerateFresh(e2, bytesRead++, r));
    assertEquals(k3, clone.getOrGenerateFresh(e3, bytesRead++, r));
    assertEquals(k4, clone.getOrGenerateFresh(e4, bytesRead++, r));
    assertEquals(k5, clone.getOrGenerateFresh(e5, bytesRead++, r));

  }


  @Test
  public void testGc() {
    MappedTypedInput input = g.new MappedTypedInput();
    int bytesRead = 0;
    int k1 = input.getOrGenerateFresh(e1, bytesRead++, r);
    int k2 = input.getOrGenerateFresh(e2, bytesRead++, r);
    int k3 = input.getOrGenerateFresh(e3, bytesRead++, r);
    int k4 = input.getOrGenerateFresh(e4, bytesRead++, r);
    int k5 = input.getOrGenerateFresh(e5, bytesRead++, r);

    MappedTypedInput clone = g.new MappedTypedInput(input);
    clone.clearTypes();
    assertEquals(k1, clone.getOrGenerateFresh(e1, bytesRead++, r));
    assertEquals(k5, clone.getOrGenerateFresh(e5, bytesRead++, r));
    assertEquals(k2, clone.getOrGenerateFresh(e2, bytesRead++, r));

    clone.gc();

    assertNull(clone.getValueInMap(e3));
    assertNull(clone.getValueInMap(e4));
  }

  @Test
  public void testFuzzCircle() {
    MappedTypedInput input = g.new MappedTypedInput();
    int bytesRead = 0;
    int k1 = input.getOrGenerateFresh(e1, bytesRead++, r);
    int k2 = input.getOrGenerateFresh(e2, bytesRead++, r);
    int k3 = input.getOrGenerateFresh(e3, bytesRead++, r);
    int k4 = input.getOrGenerateFresh(e4, bytesRead++, r);
    int k5 = input.getOrGenerateFresh(e5, bytesRead++, r);
    int k6 = input.getOrGenerateFresh(e6, bytesRead++, r);
    int k7 = input.getOrGenerateFresh(e7, bytesRead++, r);

    g.eiTypeToMutate = "Circle";

    Input fuzzedInput = input.fuzz(r);

    assert k1 != ((MappedTypedInput) fuzzedInput).getValueInMap(e1) ||
        k4 != ((MappedTypedInput) fuzzedInput).getValueInMap(e4);

    assertEquals(k2, (int) ((MappedTypedInput) fuzzedInput).getValueInMap(e2));
    assertEquals(k3, (int) ((MappedTypedInput) fuzzedInput).getValueInMap(e3));
    assertEquals(k5, (int) ((MappedTypedInput) fuzzedInput).getValueInMap(e5));
    assertEquals(k6, (int) ((MappedTypedInput) fuzzedInput).getValueInMap(e6));
    assertEquals(k7, (int) ((MappedTypedInput) fuzzedInput).getValueInMap(e7));
  }

  public void testFuzzSquare() {
    MappedTypedInput input = g.new MappedTypedInput();
    int bytesRead = 0;
    int k1 = input.getOrGenerateFresh(e1, bytesRead++, r);
    int k2 = input.getOrGenerateFresh(e2, bytesRead++, r);
    int k3 = input.getOrGenerateFresh(e3, bytesRead++, r);
    int k4 = input.getOrGenerateFresh(e4, bytesRead++, r);
    int k5 = input.getOrGenerateFresh(e5, bytesRead++, r);
    int k6 = input.getOrGenerateFresh(e6, bytesRead++, r);
    int k7 = input.getOrGenerateFresh(e7, bytesRead++, r);

    g.eiTypeToMutate = "Square";
    Input fuzzedInput = input.fuzz(r);

    assert k1 != ((MappedTypedInput) fuzzedInput).getValueInMap(e1) ||
        k3 != ((MappedTypedInput) fuzzedInput).getValueInMap(e3);

    assertEquals(k2, (int) ((MappedTypedInput) fuzzedInput).getValueInMap(e2));
    assertEquals(k4, (int) ((MappedTypedInput) fuzzedInput).getValueInMap(e4));
    assertEquals(k5, (int) ((MappedTypedInput) fuzzedInput).getValueInMap(e5));
    assertEquals(k6, (int) ((MappedTypedInput) fuzzedInput).getValueInMap(e6));
    assertEquals(k7, (int) ((MappedTypedInput) fuzzedInput).getValueInMap(e7));
  }

  public void testFuzzTriangle() {
    MappedTypedInput input = g.new MappedTypedInput();
    int bytesRead = 0;
    int k1 = input.getOrGenerateFresh(e1, bytesRead++, r);
    int k2 = input.getOrGenerateFresh(e2, bytesRead++, r);
    int k3 = input.getOrGenerateFresh(e3, bytesRead++, r);
    int k4 = input.getOrGenerateFresh(e4, bytesRead++, r);
    int k5 = input.getOrGenerateFresh(e5, bytesRead++, r);
    int k6 = input.getOrGenerateFresh(e6, bytesRead++, r);
    int k7 = input.getOrGenerateFresh(e7, bytesRead++, r);

    g.eiTypeToMutate = "Triangle";
    Input fuzzedInput = input.fuzz(r);

    assert k1 != ((MappedTypedInput) fuzzedInput).getValueInMap(e1) ||
        k5 != ((MappedTypedInput) fuzzedInput).getValueInMap(e5);

    assertEquals(k2, (int) ((MappedTypedInput) fuzzedInput).getValueInMap(e2));
    assertEquals(k3, (int) ((MappedTypedInput) fuzzedInput).getValueInMap(e3));
    assertEquals(k4, (int) ((MappedTypedInput) fuzzedInput).getValueInMap(e4));
    assertEquals(k6, (int) ((MappedTypedInput) fuzzedInput).getValueInMap(e6));
    assertEquals(k7, (int) ((MappedTypedInput) fuzzedInput).getValueInMap(e7));
  }

  public void testFuzzDiamond() {
    MappedTypedInput input = g.new MappedTypedInput();
    int bytesRead = 0;
    int k1 = input.getOrGenerateFresh(e1, bytesRead++, r);
    int k2 = input.getOrGenerateFresh(e2, bytesRead++, r);
    int k3 = input.getOrGenerateFresh(e3, bytesRead++, r);
    int k4 = input.getOrGenerateFresh(e4, bytesRead++, r);
    int k5 = input.getOrGenerateFresh(e5, bytesRead++, r);
    int k6 = input.getOrGenerateFresh(e6, bytesRead++, r);
    int k7 = input.getOrGenerateFresh(e7, bytesRead++, r);

    g.eiTypeToMutate = "Diamond";
    Input fuzzedInput = input.fuzz(r);

    assert k6 != ((MappedTypedInput) fuzzedInput).getValueInMap(e6);

    assertEquals(k1, (int) ((MappedTypedInput) fuzzedInput).getValueInMap(e1));
    assertEquals(k2, (int) ((MappedTypedInput) fuzzedInput).getValueInMap(e2));
    assertEquals(k3, (int) ((MappedTypedInput) fuzzedInput).getValueInMap(e3));
    assertEquals(k4, (int) ((MappedTypedInput) fuzzedInput).getValueInMap(e4));
    assertEquals(k5, (int) ((MappedTypedInput) fuzzedInput).getValueInMap(e5));
    assertEquals(k7, (int) ((MappedTypedInput) fuzzedInput).getValueInMap(e7));
  }

  public void testFuzzCube() {
    MappedTypedInput input = g.new MappedTypedInput();
    int bytesRead = 0;
    int k1 = input.getOrGenerateFresh(e1, bytesRead++, r);
    int k2 = input.getOrGenerateFresh(e2, bytesRead++, r);
    int k3 = input.getOrGenerateFresh(e3, bytesRead++, r);
    int k4 = input.getOrGenerateFresh(e4, bytesRead++, r);
    int k5 = input.getOrGenerateFresh(e5, bytesRead++, r);
    int k6 = input.getOrGenerateFresh(e6, bytesRead++, r);
    int k7 = input.getOrGenerateFresh(e7, bytesRead++, r);

    g.eiTypeToMutate = "Cube";
    Input fuzzedInput = input.fuzz(r);

    assert k7 != ((MappedTypedInput) fuzzedInput).getValueInMap(e7);

    assertEquals(k1, (int) ((MappedTypedInput) fuzzedInput).getValueInMap(e1));
    assertEquals(k2, (int) ((MappedTypedInput) fuzzedInput).getValueInMap(e2));
    assertEquals(k3, (int) ((MappedTypedInput) fuzzedInput).getValueInMap(e3));
    assertEquals(k4, (int) ((MappedTypedInput) fuzzedInput).getValueInMap(e4));
    assertEquals(k5, (int) ((MappedTypedInput) fuzzedInput).getValueInMap(e5));
    assertEquals(k6, (int) ((MappedTypedInput) fuzzedInput).getValueInMap(e6));
  }
//
//    @Test
//    public void testExecutionContexts() {
//        assertEquals(new ExecutionContext(e2), new ExecutionContext(e4));
//        assertEquals(new ExecutionContext(e4), new ExecutionContext(e6));
//        assertEquals(new ExecutionContext(e6), new ExecutionContext(e2));
//        assertNotEquals(new ExecutionContext(e1), new ExecutionContext(e2));
//        assertNotEquals(new ExecutionContext(e4), new ExecutionContext(e5));
//        assertNotEquals(new ExecutionContext(e1), new ExecutionContext(e5));
//    }
}