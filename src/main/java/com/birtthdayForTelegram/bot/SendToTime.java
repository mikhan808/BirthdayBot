package com.birtthdayForTelegram.bot;

import java.sql.ResultSet;

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
                    int sendMolitva = rs.getInt("SEND_MOLITVA");
                    int sendBible = rs.getInt("SEND_BIBLE");
                    int sendPlan = rs.getInt("SEND_PLAN");
                    Runnable r = new ThreadSending(id, bot, sendMolitva, sendBible, sendPlan);
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
