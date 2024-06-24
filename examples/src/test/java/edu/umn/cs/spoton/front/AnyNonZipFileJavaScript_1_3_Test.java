package edu.umn.cs.spoton.front;

import static edu.berkeley.cs.jqf.generators.languages.js.JavaScriptCodeGenerator.MAX_EXPRESSION_DEPTH;
import static edu.berkeley.cs.jqf.generators.languages.js.JavaScriptCodeGenerator.MAX_STATEMENT_DEPTH;

import com.pholser.junit.quickcheck.From;
import edu.berkeley.cs.jqf.fuzz.Fuzz;
import edu.berkeley.cs.jqf.fuzz.JQF;
import edu.umn.cs.spoton.front.InternalConfig;
import edu.umn.cs.spoton.front.generators.misc.files.FileGenerator;
import edu.umn.cs.spoton.front.generators.misc.files.util.FileManipulation;
import edu.umn.cs.spoton.front.generators.misc.files.util.FileTypeUtil.FileType;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

@RunWith(JQF.class)
public class AnyNonZipFileJavaScript_1_3_Test {


  static long totalSizes = 0;
  static int count = 0;

  @BeforeClass
  public static void setup() {
    InternalConfig.resetInstance();
    FileGenerator.fileTypeTest = FileType.JAVA_SCRIPT;
    FileGenerator.filenameTest = InternalConfig.getInstance().LOCAL_FUZZDIR + "/file.js";
    FileManipulation.initializeDir(InternalConfig.getInstance().LOCAL_FUZZDIR);
    MAX_STATEMENT_DEPTH = 1;
    MAX_EXPRESSION_DEPTH = 3;
  }


  @AfterClass
  public static void printStats() {
    System.out.println("totalFiles = " + count + ", totalSize=" + totalSizes + ",averageSizes="
                           + totalSizes / count);
  }

  @Fuzz
  public void generatorTest(@From(FileGenerator.class) File file) throws IOException {

    System.out.print("MAX_STATEMENT_DEPTH=" + MAX_STATEMENT_DEPTH + ",MAX_EXPRESSION_DEPTH="
                         + MAX_EXPRESSION_DEPTH + ", JS file= " + file.getName());
    long fileSize = Files.size(file.toPath());
    System.out.println(", size= " + fileSize);
    totalSizes += fileSize;
    ++count;
  }

}
