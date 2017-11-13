package com.birtthdayForTelegram.bot;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;

public class Log {
  private static FileWriter writer;

  public static void add(String msg) {
    try {
      if (writer == null)
        writer = new FileWriter("log.txt", true);
      writer.write(LocalDateTime.now() + ":" + msg + "\n");
      writer.flush();
    } catch (IOException ex) {

      System.out.println(ex.getMessage());
    }
  }

  public static void error(String msg) {
    System.out.println(msg);
    add(msg);
  }
}
