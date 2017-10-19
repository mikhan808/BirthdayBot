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
  boolean sended;
  public SendToTime(Example bot)
  {
    this.bot=bot;
    time=LocalTime.of(8,0);
    sended = true;
  }
  LocalTime time;
  @Override
  public void run() {
    while (true)
    {
      LocalTime current=LocalTime.now();
      if(sended)
      if(current.getHour()==time.getHour()-1)
        sended=false;
      if(current.getHour()==time.getHour()&&!sended)
      {
        try {
          Properties connInfo=new Properties();
          connInfo.put("user", "SYSDBA");
          connInfo.put("password", "masterkey");
          connInfo.put("charSet", "Cp1251");
          Connection con = DriverManager.getConnection("jdbc:firebirdsql://localhost:3050//home/mikhan808/databases/BIRTH (2).FDB", connInfo);
          Statement st=con.createStatement();
          String query="select * from CHATS";
          List<Long>idChats=new ArrayList<>();
          ResultSet rs=st.executeQuery(query);
          while (rs.next())
          {
            idChats.add(rs.getLong(1));
          }
          rs.close();
          st.close();
          con.close();
          for (Long id:idChats)
          {
            bot.getBirthday(id);
          }
          sended=true;
        }
        catch (Exception e)
        {
          System.out.println(e.getMessage());
        }
      }
      try {
        Thread.sleep(59000);
      }
      catch (Exception e)
      {
        System.out.println(e.getMessage());
      }
    }
  }
}
