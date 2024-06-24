package edu.umn.cs.spoton;


import static org.junit.Assert.assertArrayEquals;

import com.pholser.junit.quickcheck.runner.JUnitQuickcheck;
import edu.berkeley.cs.jqf.fuzz.ei.ExecutionIndex;
import edu.berkeley.cs.jqf.fuzz.ei.TypedExecutionIndex;
import edu.berkeley.cs.jqf.fuzz.ei.state.TypedJanalaEiState;
import edu.berkeley.cs.jqf.instrument.tracing.events.CallEvent;
import edu.berkeley.cs.jqf.instrument.tracing.events.ReadEvent;
import edu.berkeley.cs.jqf.instrument.tracing.events.ReturnEvent;
import janala.logger.inst.INVOKESTATIC;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(JUnitQuickcheck.class)
public class SpotOnEventsTest {

  private static CallEvent callEvent(int iid, String dec) {
    return new CallEvent(iid, null, 0,
                         new INVOKESTATIC(iid, 0, "Foo", "bar", "()" + dec));
  }

  private static ReturnEvent returnEvent(int iid) {
    return new ReturnEvent(iid, null, 0);
  }

  private static ReadEvent readEvent(int iid) {
    return new ReadEvent(iid, null, 0, 0, "random");
  }


  @BeforeClass
  public static void createGuidanceInstance() {
    System.setProperty("engine", "spotOn");
    GuidanceConfig.resetInstance();
  }

  @Test
  public void testDepth0() {
    TypedJanalaEiState e = new TypedJanalaEiState();
    int[] ei = e.getExecutionIndex(42).getEi();

    int[] expected = {42, 1};
    assertArrayEquals(expected, ei);
  }

  @Test
  public void testDepth1() {
    TypedJanalaEiState e = new TypedJanalaEiState();
    e.pushCall(SpotOnEventsTest.callEvent(4, "String "));
    ExecutionIndex ex = e.getExecutionIndex(42);
    int[] ei = ex.getEi();
    e.popReturn(SpotOnEventsTest.returnEvent(4));

    int[] expectedIndexing = {4, 1, 42, 1};
    String[] expectedCallTypes = {"String"};
    assertArrayEquals(expectedIndexing, ei);
    assert ex instanceof TypedExecutionIndex;
    assertArrayEquals(expectedCallTypes, ((TypedExecutionIndex) ex).getCallTypes());
  }


  @Test
  public void testDepth1withRepeat() {
    TypedJanalaEiState e = new TypedJanalaEiState();
    e.pushCall(SpotOnEventsTest.callEvent(4, "String "));
    e.getExecutionIndex(42);
    e.getExecutionIndex(41);
    e.getExecutionIndex(42);
    ExecutionIndex ex = e.getExecutionIndex(42);

    int[] ei = ex.getEi();
    e.popReturn(SpotOnEventsTest.returnEvent(4));

    int[] expectedIndexing = {4, 1, 42, 3};
    String[] expectedCallTypes = {"String"};

    assertArrayEquals(expectedIndexing, ei);
    assert ex instanceof TypedExecutionIndex;
    assertArrayEquals(expectedCallTypes, ((TypedExecutionIndex) ex).getCallTypes());
  }

  @Test
  public void testDepth2withRepeat() {
    TypedJanalaEiState e = new TypedJanalaEiState();
    int[] ei;
    ExecutionIndex ex;
    e.pushCall(SpotOnEventsTest.callEvent(4, "String "));
    e.popReturn(SpotOnEventsTest.returnEvent(4));

    e.pushCall(SpotOnEventsTest.callEvent(4, "String "));
    {
      e.pushCall(SpotOnEventsTest.callEvent(5, "String5 "));
      e.getExecutionIndex(42);
      e.popReturn(SpotOnEventsTest.returnEvent(5));
    }
    e.popReturn(SpotOnEventsTest.returnEvent(4));

    e.pushCall(SpotOnEventsTest.callEvent(3, "String3 "));
    {
      e.pushCall(SpotOnEventsTest.callEvent(5, "String5 "));
      e.popReturn(SpotOnEventsTest.returnEvent(5));
      e.pushCall(SpotOnEventsTest.callEvent(5, "String5 "));
      e.popReturn(SpotOnEventsTest.returnEvent(5));
      e.pushCall(SpotOnEventsTest.callEvent(5, "String5 "));
      e.popReturn(SpotOnEventsTest.returnEvent(5));
      e.pushCall(SpotOnEventsTest.callEvent(5, "String5 "));
      e.popReturn(SpotOnEventsTest.returnEvent(5));
    }
    e.popReturn(SpotOnEventsTest.returnEvent(3));

    e.pushCall(SpotOnEventsTest.callEvent(4, "String "));
    {
      e.pushCall(SpotOnEventsTest.callEvent(5, "String5 "));
      e.popReturn(SpotOnEventsTest.returnEvent(5));
      e.pushCall(SpotOnEventsTest.callEvent(5, "String5 "));
      e.getExecutionIndex(41);
      e.getExecutionIndex(42);
      ex = e.getExecutionIndex(42);

      ei = ex.getEi();

      e.popReturn(SpotOnEventsTest.returnEvent(5));
    }
    e.popReturn(SpotOnEventsTest.returnEvent(4));

    int[] expectedIndexing = {4, 3, 5, 2, 42, 2};
    String[] expectedCallTypes = {"String", "String5"};
    assertArrayEquals(expectedIndexing, ei);
    System.out.println("ex is of instance type " + ex.getClass().getName());
    assert ex instanceof TypedExecutionIndex;
    assertArrayEquals(expectedCallTypes, ((TypedExecutionIndex) ex).getCallTypes());

  }
}
