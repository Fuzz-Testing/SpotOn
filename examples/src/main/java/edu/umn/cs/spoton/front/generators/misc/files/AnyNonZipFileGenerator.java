package edu.umn.cs.spoton.front.generators.misc.files;

import com.pholser.junit.quickcheck.generator.GenerationStatus;
import com.pholser.junit.quickcheck.generator.Generator;
import com.pholser.junit.quickcheck.internal.GeometricDistribution;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;
import edu.berkeley.cs.jqf.generators.languages.bcel.JavaClassGenerator;
import edu.berkeley.cs.jqf.generators.languages.js.JavaScriptCodeGenerator;
import edu.berkeley.cs.jqf.generators.structured.xml.XmlDocumentGenerator;
import edu.umn.cs.spoton.front.InternalConfig;
import edu.umn.cs.spoton.front.generators.misc.BufferedImageGenerator;
import edu.umn.cs.spoton.front.generators.misc.FreshOrConstantGenerator;
import edu.umn.cs.spoton.front.generators.misc.files.util.FileTypeUtil.FileType;
import edu.umn.cs.spoton.front.generators.misc.files.util.FileTypeUtil.ImageExtensions;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import javax.imageio.ImageIO;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.apache.bcel.classfile.JavaClass;
import org.w3c.dom.Document;

public class AnyNonZipFileGenerator extends Generator<File> {

  private static GeometricDistribution geometricDistribution = new GeometricDistribution();

  public static FileType fileTypeTest;
  public static String filePathTest;
  FileType fileType = fileTypeTest != null ? fileTypeTest : null;
  String filePath = filePathTest != null ? filePathTest : null;
  public String chosenExtension;

//  public static File imageFileCost = Debug.createNewFile(
//      new File(Config.buildDir + File.separator + outputDir),
//      "imageFileCost", "trial,imageFileDur\n");

  public AnyNonZipFileGenerator() {
    super(File.class);
  }

  @Override
  public File generate(SourceOfRandomness r, GenerationStatus status) {
    assert fileType != null : "setFileType is required before generation";
    File localFile = null;
    FileOutputStream output = null;
    try {
      output = new FileOutputStream(filePath);
      switch (fileType) {
        case XML:
          Document xmlDoc = gen().make(XmlDocumentGenerator.class).generate(r, status);
          writeXml(xmlDoc, output);
          break;
        case IMGE:
//          Instant beforeDate = Instant.now();
          BufferedImage imageBuffer = gen().make(BufferedImageGenerator.class).generate(r, status);
          String formate;
          if (filePath.contains(".")) {
            formate = filePath.substring(filePath.lastIndexOf(".") + 1);
          } else {
            assert fileType != null : "unexpected file type that is not part of the filename.";
            formate = r.choose(ImageExtensions.values()).name().replace(".", "");
          }
          localFile = new File(filePath);
          boolean writeResult = ImageIO.write(imageBuffer, formate, localFile);
//          assert writeResult : "failing to write the image file. failing";
//          Instant afterDate = Instant.now();
//          double imageFileDur = Duration.between(beforeDate, afterDate).toNanos() / 1_000_000_000.0;
//          Debug.appendLineToFile(imageFileCost,
//                                 StatisticsManager.testCasesCount + "," + imageFileDur + "\n");
          break;
        case JAVA_CLASS:
          JavaClass javaDoc = gen().make(JavaClassGenerator.class).generate(r, status);
          output.write(javaDoc.getBytes());
          break;
        case JAVA_SCRIPT:
          String javaScriptDoc = gen().make(JavaScriptCodeGenerator.class).generate(r, status);
          output.write(javaScriptDoc.getBytes(StandardCharsets.UTF_8));
          break;
        case TEXT:
          int upperBound = geometricDistribution.sampleWithMean(InternalConfig.getInstance().MEAN_TEXT_FILE_SIZE, r) - 1;
          StringBuilder textDoc = new StringBuilder("");
          while (textDoc.toString().getBytes().length < upperBound) {
            FreshOrConstantGenerator.setUseAnyStringGenerator(true);
            textDoc.append(gen().make(FreshOrConstantGenerator.class).generate(r, status));
          }
          output.write(textDoc.toString().getBytes(StandardCharsets.UTF_8));
          break;
        case CSV:
          String csvDoc = gen().make(CsvGenerator.class).generate(r, status);
          output.write(csvDoc.getBytes(StandardCharsets.UTF_8));
          break;
        default:
          assert false : "unexpected file type in non-zip file creation";
      }
      if (fileType != FileType.IMGE)
        localFile = new File(filePath);
    } catch (IOException | TransformerException e) {
      File outerDir = (new File(filePath.substring(0, filePath.lastIndexOf("/"))));
      System.out.println("outerDir.exists = " + outerDir.exists());
      System.out.println("outerDir.directory = " + outerDir.isDirectory());
      System.out.println("outerDir.getAbsolutePath = " + outerDir.getAbsolutePath());

      assert false : "Unable to create local file (" + filePath + "). Failing.\n"
          + e.toString();
    } finally {
      if (output != null) {
        try {
          output.close();
        } catch (IOException e) {
          assert false :
              "problem closing stream. assumptions violated. failing.\n" + e.toString();
        }
      }
    }
    assert localFile != null : "generated file should not be null. failing.";
    return localFile;
  }

  public void setFileType(FileType fileType) {
    this.fileType = fileType;
  }

  // write doc to output stream
  private static void writeXml(Document doc, OutputStream output) throws TransformerException {

    TransformerFactory transformerFactory = TransformerFactory.newInstance();
    Transformer transformer = transformerFactory.newTransformer();
    DOMSource source = new DOMSource(doc);
    StreamResult result = new StreamResult(output);

    transformer.transform(source, result);
  }

  public void setFilename(String filePath) {
    this.filePath = filePath;
  }
}
