/*
 * Copyright (c) 2017-2018 The Regents of the University of California
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
package edu.berkeley.cs.jqf.fuzz.ei.state;

import edu.berkeley.cs.jqf.fuzz.ei.ExecutionIndex;
import edu.berkeley.cs.jqf.fuzz.ei.TypedExecutionIndex;
import edu.berkeley.cs.jqf.fuzz.util.NonZeroCachingCounter;
import edu.berkeley.cs.jqf.instrument.tracing.events.CallEvent;
import edu.berkeley.cs.jqf.instrument.tracing.events.ReturnEvent;
import edu.berkeley.cs.jqf.instrument.tracing.events.TraceEventVisitor;
import edu.umn.cs.spoton.GuidanceConfig;
import java.util.Arrays;
import java.util.Stack;

/**
 * ExecutionIndexingState implementation for Janala instrumentation framework.
 *
 * @author Soha Hussein
 * @see ExecutionIndex
 */
public class TypedJanalaEiState extends JanalaExecutionIndexingState implements TraceEventVisitor {

  private Stack<String> typeStack = new Stack<>();

  @Override
  public void visitCallEvent(CallEvent c) {

    setLastEventIid(c.getIid());
    this.pushCall(c);
  }


  public void pushCall(CallEvent c) {
    int iid = c.getIid();
    String returnType = getType(c);
    // Increment counter for call-site (note: this is subject to hash collisions)
    int count = stackOfCounters.get(depth).increment(iid);

    // Add to rolling execution index
    rollingIndex[2 * depth] = iid;
    rollingIndex[2 * depth + 1] = count;

    typeStack.push(returnType);

    // Increment depth
    depth++;

    // Ensure that we do not go very deep
    if (depth >= MAX_SUPPORTED_DEPTH) {
      throw new StackOverflowError("Very deep stack; cannot compute execution index");
    }

    // Push a new counter if it does not exist
    if (depth >= stackOfCounters.size()) {
      stackOfCounters.add(new NonZeroCachingCounter(COUNTER_SIZE));
    }

  }

  private String getType(CallEvent c) {
    return c.toString().substring(c.toString().indexOf(")") + 1, c.toString().length() - 2);
  }

  @Override
  public void visitReturnEvent(ReturnEvent r) {
    setLastEventIid(r.getIid());
    this.popReturn(r);
  }


  public void popReturn(ReturnEvent r) {
    // Of course, we still need to pop current
    // method.
    stackOfCounters.get(depth).clear();
    typeStack.pop();
    // Decrement depth
    depth--;
//    assert (depth >= 0);
  }


  public ExecutionIndex getExecutionIndex(int iid) {
    if (!GuidanceConfig.getInstance().spotOnRunning)
      return super.getExecutionIndex(iid);
    else {
      String[] eiTypes = typeStack.toArray(new String[0]);
      // Increment counter for event (note: this is subject to hash collisions)
      int count = stackOfCounters.get(depth).increment(iid);

      // Add to rolling execution index
      rollingIndex[2 * depth] = iid;
      rollingIndex[2 * depth + 1] = count;

      // Snapshot the rolling index
      int size = 2 * (depth + 1); // 2 integers for each depth value
      int[] ei = Arrays.copyOf(rollingIndex, size);
      return new TypedExecutionIndex(ei, eiTypes);
    }
  }
}
