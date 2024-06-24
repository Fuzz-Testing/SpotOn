package edu.umn.cs.spoton.front.generators.misc.files.util;

import com.pholser.junit.quickcheck.random.SourceOfRandomness;
import edu.umn.cs.spoton.front.InternalConfig;
import edu.umn.cs.spoton.front.generators.aws.states.StatisticsManager;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

public class FileTypeUtil {

  public static boolean isRestrictedFileName(String fileName) {
    String[] restrictedFileNames = new String[]{
        "CON", "PRN", "AUX", "NUL", "COM1", "COM2", "COM3", "COM4", "COM5", "COM6", "COM7", "COM8",
        "COM9", "LPT1", "LPT2", "LPT3",
        "LPT4", "LPT5", "LPT6", "LPT7", "LPT8", "LPT9", ".", "..", " "
    };
    HashSet restrictedSet = new HashSet<>(
        Arrays.stream(restrictedFileNames).collect(Collectors.toSet()));
    return restrictedSet.contains(fileName.toUpperCase());
  }

  public static String[] getExtensions(FileType fileType) {
    switch (fileType) {
      case XML:
        return new String[]{".xml"};
      case IMGE:
        return InternalConfig.getInstance().ALLOWED_IMAGE_EXTENSION;
      case JAVA_CLASS:
        return new String[]{".class"};
      case JAVA_SCRIPT:
        return new String[]{".js"};
      case TEXT:
        return new String[]{".txt"};
      case ARCHIVE:
        return new String[]{".zip", ".gz"};
      case CSV:
        return new String[]{".csv"};
    }
    assert false : "unexpected type. failing.";
    return null;
  }

  public static List<String> getAllExtensions() {
    //allow extensions to use the allowImageExtension, that is defined in the configuration file.
    return Arrays.stream(InternalConfig.getInstance().getAllowedFileTypes())
        .map(type -> type == FileType.IMGE ? InternalConfig.getInstance().ALLOWED_IMAGE_EXTENSION
            : getExtensions(type))
        .flatMap(arr -> Arrays.stream(arr)).collect(Collectors.toList());
  }

  public static String makeValidOrNoExtFile(FileType fileType,
      SourceOfRandomness r, boolean erroneousGeneration) {
    String extension = "";
    if (erroneousGeneration) {
//      List<String> extensions = getAllExtensions();
////      extension = r.choose(extensions);
      boolean generateNoExtension =
          InternalConfig.getInstance().isAllowNoExtFileTestaApi()
              || r.nextFloat(0, 1) < InternalConfig.getInstance().PROBABILITY_OF_ERRONEOUS_FILE;
      if (generateNoExtension) {
//        //extension.replace(".", "");
        ++StatisticsManager.noFileExtensionCount;
        return extension;
      }
    }
    extension = makeValidFileName(fileType, r);

    return extension;
  }

  public static String makeValidFileName(FileType fileType, SourceOfRandomness r) {
    String filename;
    String[] extensions = getExtensions(fileType);
//    String[] extensions = new String[]{".png", ".gif", ".jpeg", ".tiff", ".jpg", ".bmp", ".wbmp"};
//    String[] extensions = new String[]{".png", ".jpg"};
    if (extensions.length == 1)
      filename = getExtensions(fileType)[0];

    else
      filename = r.choose(extensions);

    return filename;
  }

  public static FileType fileTypeExtensionToEnum(String ext) {
    List<FileType> filteredType = Arrays.stream(FileType.values()).filter(
        t -> Arrays.stream(FileTypeUtil.getExtensions(t)).filter(type -> type.equals(ext)).collect(
            Collectors.toSet()).size() != 0).collect(Collectors.toList());
    if (filteredType.isEmpty()) {
      if (ext.equals(".gz") || ext.equals(".zip"))
        return FileType.ARCHIVE;
      else {
        return null;
      }
    } else {
      assert filteredType.size() == 1;
      return filteredType.get(0);
    }
  }

  public enum FileType {
    XML,
    IMGE,
    JAVA_CLASS,
    JAVA_SCRIPT,
    TEXT,
    ARCHIVE,
    CSV
  }

  public enum ImageExtensions {
    PNG {
      @Override
      public String toString() {
        return ".png";
      }
    },
    GIF {
      @Override
      public String toString() {
        return ".gif";
      }
    },
    JPEG {
      @Override
      public String toString() {
        return ".jpeg";
      }
    },
    TIFF {
      @Override
      public String toString() {
        return ".tiff";
      }
    },
    JPG {
      @Override
      public String toString() {
        return ".jpg";
      }
    },
    BMP {
      @Override
      public String toString() {
        return ".bmp";
      }
    },
//    WBMP { //currently causing problem with ImageIO.write-- needs to be commented out
//      @Override
//      public String toString() {
//        return ".wbmp";
//      }
//    },
  }


  public static String removeIllegalFileNameChars(String fileName) {
    //ensures that we have a valid fileName, with no slash
//    return fileName.replaceAll("[[.]\"\\\\/]", ""); // commenting this out for now, since we do not want to fuzz the localstack api for now.
    fileName = fileName.replaceAll("[^A-Za-z0-9-_\\s+]", "");
    return fileName.replaceAll("[\\n+]",
                               ""); //adding this because aws seems to complain about that, this can be investigated more in the fuzzing the aws api
  }
}
