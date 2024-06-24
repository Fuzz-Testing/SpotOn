package edu.umn.cs.spoton;

import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class MutatedTypesStates {

  final static String TYPE_TO_MUTATE_STATS = "_mutatedTypes.txt";

  static String typeToMutateFullFileName;

  static {
    typeToMutateFullFileName =
        GuidanceConfig.getInstance().codeTargetDir + "/" + File.separator
            + GuidanceConfig.getInstance().benchmarkName + TYPE_TO_MUTATE_STATS;
    try {
      System.out.println("typeToMutateFullFileName " + typeToMutateFullFileName);
      new File(typeToMutateFullFileName).createNewFile();
      Files.write(new File(typeToMutateFullFileName).toPath(),
                  "".getBytes(StandardCharsets.UTF_8),
                  StandardOpenOption.CREATE, TRUNCATE_EXISTING);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static void add(long testCasesCount, String type) {

    try {
      String text = testCasesCount + "," + type + "\n";
      Files.write(Paths.get(typeToMutateFullFileName),
                  text.getBytes(StandardCharsets.UTF_8),
                  StandardOpenOption.CREATE, StandardOpenOption.APPEND);

    } catch (IOException e) {
      throw new RuntimeException(e);
    }

  }
}
