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
                ResultSet rs = Example.getResultSet(query);
                while (rs.next()) {
                    Long id = rs.getLong(1);
                    Runnable r = new ThreadSending(id, bot);
                    Thread t = new Thread(r);
                    t.start();

                }
                Example.releaseResources(rs);
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
