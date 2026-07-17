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
    int sendMolodezh;
    int sendBirthday;

    public ThreadSending(Long id, Example bot, int sendMolitva, int sendBible, int sendPlan,int sendMolodezh,int sendBirthday) {
        this.id = id;
        this.bot = bot;
        this.sendMolitva = sendMolitva;
        this.sendBible = sendBible;
        this.sendPlan = sendPlan;
        this.sendMolodezh = sendMolodezh;
        this.sendBirthday = sendBirthday;
    }

    @Override
    public void run() {
        if (sendPlan == 1)
            bot.getTodayPlan(id, sendBible == 1);
        if (sendMolitva == 1)
            bot.getMolitvaForSchedule(id);
        if (sendMolodezh == 1)
            bot.getMolodezhMolitvaForSchedule(id);
        if (sendBirthday == 1)
            bot.getBirthdaysForSchedule(id);
        try {
            String query = "update chats set time_last_sending=current_timestamp where id = " + id;
            Example.executeUpdate(query);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
}
