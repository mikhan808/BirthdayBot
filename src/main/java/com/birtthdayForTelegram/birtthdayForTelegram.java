package com.birtthdayForTelegram;

import com.birtthdayForTelegram.bot.Example;
import com.birtthdayForTelegram.bot.SendToTime;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

/**
 * Created by МишаИОля on 15.10.2017.
 */
public class birtthdayForTelegram {
    public static void main(String[] args) {
        try {
            TelegramBotsApi telegramBotsApi = createTelegramBotsApi();
            try {
                Example bot = new Example();
                Runnable r = new SendToTime(bot);
                Thread t = new Thread(r);
                t.start();
                telegramBotsApi.registerBot(bot);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static TelegramBotsApi createTelegramBotsApi() {
        return createLongPollingTelegramBotsApi();
    }

    private static TelegramBotsApi createLongPollingTelegramBotsApi() {
        try {
            return new TelegramBotsApi(DefaultBotSession.class);
        } catch (TelegramApiException e) {
            e.printStackTrace();
            return null;
        }
    }

}
