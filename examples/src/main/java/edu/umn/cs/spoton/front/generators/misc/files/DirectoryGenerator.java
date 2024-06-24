package edu.umn.cs.spoton.front.generators.misc.files;

import com.pholser.junit.quickcheck.generator.GenerationStatus;
import com.pholser.junit.quickcheck.generator.Generator;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;
import edu.umn.cs.spoton.front.InternalConfig;
import java.time.Instant;

public class DirectoryGenerator extends Generator<String> {

  public DirectoryGenerator() {
    super(String.class);
  }

  @Override
  public String generate(SourceOfRandomness r, GenerationStatus status) {
    Instant beforeDate;
    Instant afterDate;
    beforeDate = Instant.now();
    int numOfFiles = r.nextInt(0, 3);
    for (int i = 0; i < numOfFiles; i++)
      gen().make(FileWithAttributesGenerator.class)
          .generate(r, status); //has the side effect of populating LOCAL_FUZZDIR
    afterDate = Instant.now();
    return InternalConfig.getInstance().LOCAL_FUZZDIR; //we are always generating files in a specific directory.
  }
}

