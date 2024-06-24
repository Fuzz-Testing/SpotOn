package edu.umn.cs.spoton.front;

import edu.berkeley.cs.jqf.fuzz.guidance.GuidanceException;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class Debug {

  static int fileNum = 0;

  public static File createNewFile(File outputDirectory, String filename, String header) {
    File file = new File(outputDirectory, filename + "_" + InternalConfig.getInstance().ENGINE + "_" + fileNum++);
    file.delete();
    appendLineToFile(file, header);
    return file;
  }


  public static void appendLineToFile(File file, String line) throws GuidanceException {
    try (PrintWriter out = new PrintWriter(new FileWriter(file, true))) {
      out.print(line);
      out.close();
    } catch (IOException e) {
      throw new GuidanceException(e);
    }
  }

  public static void printCallStack() {
    StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();

    System.out.println("Call Stack:");

    for (StackTraceElement element : stackTrace) {
      System.out.println(element.toString());
    }
  }

  public static void saveInputStreamToFile(InputStream inputStream, Path path) throws IOException {

    // Using java.nio.file.Files.copy to copy the input stream to the file
    Files.copy(inputStream, path, StandardCopyOption.REPLACE_EXISTING);

    // Alternatively, you can use FileOutputStream to manually copy the data
    // try (FileOutputStream outputStream = new FileOutputStream(filePath)) {
    //     byte[] buffer = new byte[4096];
    //     int bytesRead;
    //     while ((bytesRead = inputStream.read(buffer)) != -1) {
    //         outputStream.write(buffer, 0, bytesRead);
    //     }
    // }
  }

}

