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
package edu.umn.cs.spoton.front.generators.misc;

import com.pholser.junit.quickcheck.generator.GenerationStatus;
import com.pholser.junit.quickcheck.generator.Generator;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;
import edu.berkeley.cs.jqf.generators.string.AlphaStringGenerator;
import edu.berkeley.cs.jqf.generators.string.AsciiStringGenerator;
import edu.umn.cs.spoton.front.InternalConfig;
import edu.umn.cs.spoton.front.InternalConfig.StringGenerator;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;

/**
 * Allows the usage of the permanent and the transient dictionaries, using a flip coin chance. If
 * the choice was to generate a string, then depending on whether the boolean flag,
 * useAnyStringGenerator is set or not. If it is set, we generate using any string generator,
 * otherwise, we use the alpha/character only generator.
 */
public class FreshOrConstantGenerator extends Generator<String> {

  private Generator<String> fallback;

  private static boolean useAnyStringGenerator = true;
  private static HashSet<String> stringTable = deserializeStringTable();

  public FreshOrConstantGenerator() {
    super(String.class);
  }

  public static void setUseAnyStringGenerator(boolean useAnyStringGenerator) {
    FreshOrConstantGenerator.useAnyStringGenerator = useAnyStringGenerator;
  }

  private static HashSet<String> deserializeStringTable() {
    if (InternalConfig.getInstance().STR_OPT_ON) {
      String projectDir = InternalConfig.getInstance().PROJECT_DIR;
      String outputDir = System.getProperty("jqf.RESULTS_OUTPUT_DIR");
      String analysisDirPath = projectDir + "/" + outputDir + "/analysisOutput";
      File strConstFile = new File(analysisDirPath, "StrConstants.txt");
      return readStrConstFile(strConstFile);
    } else
      return new HashSet<>();
  }

  private static HashSet<String> readStrConstFile(File strConstFile) {
    HashSet<String> strConstTable = new HashSet<>();
    try (BufferedReader br = new BufferedReader(new FileReader(strConstFile))) {
      String line;
      while ((line = br.readLine()) != null) {
        strConstTable.add(line);
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
    return strConstTable;
  }

  @Override
  public String generate(SourceOfRandomness r, GenerationStatus status) {
    fallback = gen().make(AlphaStringGenerator.class); //setting default fallback
    String str = "";
    boolean useDictionary =
        !InternalConfig.getInstance().IS_EI_GENERATION_EXP ? (InternalConfig.getInstance().STR_OPT_ON ? r.nextBoolean() : false) : false;
//    using stringTable populated by the analysis
    if (useDictionary && !stringTable.isEmpty())
      str = (String) stringTable.toArray()[r.nextInt(stringTable.size())];
    else {//        System.out.println("generating new strings");
      if (useAnyStringGenerator) {
        StringGenerator strGenType = r.choose(InternalConfig.getInstance().ALL_STRING_GENERATORS);
        switch (strGenType) {
          case ASCII:
            str = gen().make(AsciiStringGenerator.class).generate(r, status);
            break;
          case ALPHA:
            str = gen().make(AlphaStringGenerator.class).generate(r, status);
            break;
          case CODE_POINT:
            str = gen().type(String.class).generate(r, status);
            break;
          case EMPTY: //do nothing, we already have output being the empty string.
            break;
        }
      } else {
        str = fallback.generate(r, status);
      }
    }
    return str;
  }
}
