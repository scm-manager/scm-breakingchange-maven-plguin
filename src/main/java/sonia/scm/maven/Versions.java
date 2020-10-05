package sonia.scm.maven;

import java.util.Locale;

final class Versions {

  private Versions() {
  }

  public static String findBaseVersion(String version) {
    String v = version.toUpperCase(Locale.ENGLISH).replace("-SNAPSHOT", "");

    String[] parts = v.split("\\.");
    if (parts[2].equals("0")) {
      int i = Integer.parseInt(parts[1]);
      return "2." + (i-1) + ".0";
    } else {
      int i = Integer.parseInt(parts[2]);
      return "2." + parts[1] + "." + (i-1);
    }
  }
}
