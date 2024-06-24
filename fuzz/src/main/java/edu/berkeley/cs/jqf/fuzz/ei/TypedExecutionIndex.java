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
package edu.berkeley.cs.jqf.fuzz.ei;

import static edu.umn.cs.spoton.analysis.StaticAnalysisMain.disregardedJavaTypes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * Representing an execution index with types.
 *
 * @author Soha
 */
public class TypedExecutionIndex extends ExecutionIndex {

  /**
   * A global types map that ensures uniqueness of string types, and ensures sharing of strings when
   * used for types. This avoids unnecessary memory usage of allocating multiple string objects to
   * the same type.
   */
  static HashMap<String, String> globalStringTypesMap = new HashMap<>();
  String[] callTypes;

  public TypedExecutionIndex(int[] ei, String[] types) {
    super(ei);
    this.callTypes = makeTypesUnique(types);
  }

  private String[] makeTypesUnique(String[] types) {
    List<String> filteredTypes = new ArrayList<>();
    for (int i = 0; i < types.length; i++) {
      String type = types[i];
      if (!filteredTypes.contains(types[i])) {
        boolean isJavaDisregardedType = disregardedJavaTypes.stream()
            .anyMatch(t -> type.startsWith(t));
        if (!isJavaDisregardedType) {
          String typeInGlobalMap = globalStringTypesMap.get(type);
          if (typeInGlobalMap != null)
            filteredTypes.add(typeInGlobalMap);
          else {
            filteredTypes.add(type);
            globalStringTypesMap.put(type, type);
          }
        }
      }
    }

    return filteredTypes.stream().toArray(String[]::new);
  }

  public int[] getEi() {
    return ei;
  }

  public String[] getCallTypes() {
    return callTypes;
  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(ei);
  }


  @Override
  public boolean equals(Object other) {
    boolean isEqual = false;
    if (callTypes != null)
      if (Arrays.equals(ei, ((TypedExecutionIndex) other).ei))
        assert Arrays.equals(callTypes,
                             ((TypedExecutionIndex) other).callTypes) : "if pairs are equal then types must be equal";

    return Arrays.equals(ei, ((TypedExecutionIndex) other).ei);
  }

  private boolean isMatchingCallTypes(String[] callTypes1, String[] callTypes2) {
    if (callTypes1.length != callTypes2.length)
      return false;
    for (int i = 0; i < callTypes1.length; i++)
      if (!callTypes1[i].equals(callTypes2[i]))
        return false;

    return true;
  }

  @Override
  public String toString() {

    if (callTypes == null)
      return Arrays.toString(ei);
    else
      return Arrays.toString(ei) + Arrays.toString(callTypes);
  }


  public boolean hasCallType(String typeToMutate) {
    assert callTypes != null : "unexpected null value for typed execution index";
    return Arrays.stream(callTypes).anyMatch(t -> t.equals(typeToMutate));
  }
}
