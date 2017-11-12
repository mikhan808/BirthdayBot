package com.birtthdayForTelegram.bot;

/**
 * Created by МишаИОля on 12.11.2017.
 */
public class ThreadSending implements Runnable {

    Long id;
    Example bot;

    public ThreadSending(Long id, Example bot) {
        this.id = id;
        this.bot = bot;
    }

    @Override
    public void run() {
        bot.getBirthday(id);
        try {
            String query = "update chats set time_last_sending=current_timestamp where id = " + id;
            Example.executeUpdate(query);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
}
