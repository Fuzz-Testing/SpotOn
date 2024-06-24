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
package edu.berkeley.cs.jqf.fuzz.util;

import edu.berkeley.cs.jqf.instrument.tracing.events.BranchEvent;
import edu.umn.cs.spoton.SpotOnGuidance;
import edu.umn.cs.spoton.analysis.influencing.CodeTarget;
import java.util.HashSet;

/**
 * Collects coverage of branches encountered by SpotOn
 *
 * @author Soha Hussein
 */
public class TypedEiCoverage extends Coverage {

  public static HashSet<String> visitedExpandedClasses = new HashSet<>();

  @Override
  public void visitBranchEvent(BranchEvent b) {
    counter.increment1(b.getIid(), b.getArm());
    String className = "L" + b.getContainingClass();
    if (!visitedExpandedClasses.contains(className)) {
      SpotOnGuidance.expandUncoveredComplexConditions(
          janala.instrument.Config.classToInstrumentedScp.get(className));
      visitedExpandedClasses.add(className);
    }
    CodeTarget encounteredSourcePoint = null;
    encounteredSourcePoint = new CodeTarget("L" + b.getContainingClass(),
                                                 b.getContainingMethodName()
                                                     + b.getContainingMethodDesc(),
                                                 b.getLineNumber(), b.getIid());
    SpotOnGuidance.addCoveredBranchCodeTarget(encounteredSourcePoint, b.getArm());
  }
}
