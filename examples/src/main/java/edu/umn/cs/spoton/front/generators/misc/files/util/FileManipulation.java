package edu.umn.cs.spoton.front.generators.misc.files.util;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import javax.imageio.ImageIO;

/**
 * A utility class to manipulate the creation of files and directories
 */
public class FileManipulation {

  public static void initializeDir(String dir) {
    ImageIO.setUseCache(false);
    File theDir = new File(dir);
    if (!theDir.exists())
      theDir.mkdirs();
    else
      clearDir(dir);
  }


  public static void clearDir(String dir) {
    File fuzzDir = new File(dir);
    File[] contents = fuzzDir.listFiles();
    if (contents != null) {
      for (File f : contents) {
        f.delete();
      }
    }
  }

  public static void writeToFile(String file, String value) {
    try {
      Files.write(Paths.get(file), value.getBytes(StandardCharsets.UTF_8),
                  StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }


  public static void copyDir(String src, String dest, boolean overwrite) {
    try {
      Files.walk(Paths.get(src)).forEach(a -> {
        Path b = Paths.get(dest, a.toString().substring(src.length()));
        try {
          if (!a.toString().equals(src))
            Files.copy(a, b, overwrite ? new CopyOption[]{StandardCopyOption.REPLACE_EXISTING} : new CopyOption[]{});
        } catch (IOException e) {
          e.printStackTrace();
        }
      });
    } catch (IOException e) {
      //permission issue
      e.printStackTrace();
      assert false;
    }
  }
}
