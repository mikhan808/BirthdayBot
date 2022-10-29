package com.birtthdayForTelegram.bot;

/**
 * Created by МишаИОля on 12.11.2017.
 */
public class ThreadSending implements Runnable {

    Long id;
    Example bot;
    int sendMolitva;
    int sendBible;
    int sendPlan;

    public ThreadSending(Long id, Example bot, int sendMolitva, int sendBible, int sendPlan) {
        this.id = id;
        this.bot = bot;
        this.sendMolitva = sendMolitva;
        this.sendBible = sendBible;
        this.sendPlan = sendPlan;
    }

    @Override
    public void run() {
        if (sendPlan == 1)
            bot.getTodayPlan(id, sendBible == 1);
        if (sendMolitva == 1)
            bot.getMolitvaForSchedule(id);
        bot.getBirthdaysForSchedule(id);
        try {
            String query = "update chats set time_last_sending=current_timestamp where id = " + id;
            Example.executeUpdate(query);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
}
