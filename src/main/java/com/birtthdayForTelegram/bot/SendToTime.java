package com.birtthdayForTelegram.bot;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class SendToTime implements Runnable {
  Example bot;

  public SendToTime(Example bot) {
    this.bot = bot;
  }

  @Override
  public void run() {
    while (true) {
      try {
        String query = "select * from CHATS where NEED_IN_SENDING = 1";
        List<Long> idChats = new ArrayList<>();
        ResultSet rs = Example.getResultSet(query);
        while (rs.next()) {
          idChats.add(rs.getLong(1));
        }
        Example.releaseResources(rs);
        for (Long id : idChats) {
          bot.getBirthday(id);
          try{
            query = "update chats set time_last_sending=current_timestamp where id = "+id;
            Example.executeUpdate(query);
          }
          catch (Exception e)
          {
            System.out.println(e.getMessage());
          }
        }
      } catch (Exception e) {
        System.out.println(e.getMessage());
      }
    try {
      Thread.sleep(59000);
    } catch (Exception e) {
      System.out.println(e.getMessage());
    }
  }
}
}
