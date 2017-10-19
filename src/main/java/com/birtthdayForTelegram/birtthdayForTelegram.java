package com.birtthdayForTelegram;

import com.birtthdayForTelegram.bot.Example;
import com.birtthdayForTelegram.bot.SendToTime;
import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.TelegramBotsApi;
import org.telegram.telegrambots.api.objects.Update;
import org.telegram.telegrambots.exceptions.TelegramApiException;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;

/**
 * Created by МишаИОля on 15.10.2017.
 */
public class birtthdayForTelegram {
    public static void main(String[] args)
    {
        ApiContextInitializer.init(); // Инициализируем апи
        TelegramBotsApi botapi = new TelegramBotsApi();
        try {
            Example bot = new Example();
            Runnable r = new SendToTime(bot);
            Thread t = new Thread(r);
            t.start();
            botapi.registerBot(bot);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}
