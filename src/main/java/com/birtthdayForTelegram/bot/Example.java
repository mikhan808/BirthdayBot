package com.birtthdayForTelegram.bot;

import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.objects.Chat;
import org.telegram.telegrambots.api.objects.Message;
import org.telegram.telegrambots.api.objects.Update;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.exceptions.TelegramApiException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.*;

/**
 * Created by МишаИОля on 15.10.2017.
 */
public class Example extends TelegramLongPollingBot {

    @Override
    public void onUpdateReceived(Update update) {
        Message msg = update.getMessage();// Это нам понадобится
        Long id = msg.getChatId();
        Chat chat = msg.getChat();
        Connection con = getConnection();
        int status;
        try {
            Statement st = con.createStatement();
            String query = "SELECT * FROM DIALOGS WHERE CHAT = " + id;
            ResultSet rs = st.executeQuery(query);
            if (rs.next()) {
                status = rs.getInt(2);
            } else {
                addNewChatAndDialog(chat);
                status = Status.NORMAL;
            }
            String txt = msg.getText();
            if (txt.equals("/cancel"))
                updateStatus(chat, Status.NORMAL);
            else
                switch (status) {
                    case Status.NORMAL:
                        if (txt.equals("Привет"))
                            sendMsg(msg, "Привет");
                        if (txt.indexOf("#") == 0)
                            getBirthday(msg, txt.substring(1));
                        else if (txt.indexOf("!") == 0) {
                            try {
                                int x = Integer.parseInt(txt.substring(1));
                                getBirthday(msg, x);
                            } catch (Exception e) {
                                sendMsg(msg, "после ! должны идти только цифры");
                            }
                        } else if (txt.indexOf("+") == 0) {
                            addNew(msg.getChat());
                            sendMsg(msg, "Теперь в 8.00 вам будут приходить сообщения с именниниками");
                        } else if (txt.indexOf("-") == 0) {
                            deleteID(msg.getChat());
                            sendMsg(msg, "Теперь в 8.00 мы вас не будем беспокоить");
                        } else if (txt.equals("/add")) {
                            updateDataRecordAllNull(chat);
                            updateStatus(chat, Status.FAMILIYA);
                            sendMsg(id, "Введите фамилию добавляемого человека");
                        } else if (txt.equals("/help")) {
                            sendMsg(id, "Список команд:\n" +
                                    "/help - список команд\n" +
                                    "#Имя - список людей с указанным именем\n" +
                                    "!X - список людей празднующих день рождения, через X дней\n" +
                                    "/add - добавить человека которого нет в списке\n" +
                                    "/cancel - отменить добавление человека\n" +
                                    "+ - включить функцию автоматического уведомления\n" +
                                    "- - отключить функцию автоматического уведомления\n" +
                                    "?вопрос - так можно задать интересующий вопрос\n" +
                                    "любое другое сообщение - получить список людей празднущих сегодня день рождения");
                        } else if (txt.indexOf("%") == 0) {
                            txt = txt.substring(1);
                            sendAll(txt);
                        } else if (txt.indexOf("?") == 0) {
                            txt = txt.substring(1);
                            sendAdmin(chat, txt);
                        } else if (txt.indexOf("^") == 0) {
                            txt = txt.substring(1);
                            updateStatus(chat, Status.SEND_TO);
                            updateDataRecord(chat, "DESCRIPTION", txt);
                        } else
                            getBirthday(msg);
                        break;
                    case Status.FAMILIYA:
                        updateDataRecord(chat, "FAMILIYA", txt);
                        updateStatus(chat, Status.IMYA);
                        sendMsg(id, "Введите имя добавляемого человека");
                        break;
                    case Status.IMYA:
                        updateDataRecord(chat, "IMYA", txt);
                        updateStatus(chat, Status.OTCHESTVO);
                        sendMsg(id, "Введите отчество добавляемого человека (если вы не знаете отчество человека то отправьте ?)");
                        break;
                    case Status.OTCHESTVO:
                        if (!txt.contains("?"))
                            updateDataRecord(chat, "OTCHESTVO", txt);
                        updateStatus(chat, Status.TELEFON);
                        sendMsg(id, "Введите телефон добавляемого человека (если вы не знаете телефон человека то отправьте ?)");
                        break;
                    case Status.TELEFON:
                        if (!txt.contains("?"))
                            updateDataRecord(chat, "TELEFON", txt);
                        updateStatus(chat, Status.BIRTHDAY);
                        sendMsg(id, "Введите дату рождения добавляемого человека в формате ДД.ММ.ГГГГ (например 01.01.1990)");
                        break;
                    case Status.BIRTHDAY:
                        updateDataRecord(chat, "BIRTHDAY", txt);
                        updateStatus(chat, Status.DESCRIPTION);
                        sendMsg(id, "Введите краткое описание добавляемого человека (например: Старший сын Ивана и Алены живет в г.Москва)");
                        break;
                    case Status.DESCRIPTION:
                        updateDataRecord(chat, "DESCRIPTION", txt);
                        insertBirthday(chat);
                        updateStatus(chat, Status.NORMAL);
                        sendMsg(id, "Человек добавлен");
                        break;
                    case Status.SEND_TO:
                        String[] res = getDataForInsertBirthday(chat);
                        Long id_to = Long.parseLong(res[6].trim());
                        updateStatus(chat,Status.NORMAL);
                        sendMsg(id_to,txt);
                        break;


                }

        } catch (Exception e) {
            sendMsg(msg, "Ошибка: " + e.getMessage());
        }

    }

    public static Connection getConnection() {
        try {
            Properties connInfo = new Properties();
            connInfo.put("user", "SYSDBA");
            connInfo.put("password", "masterkey");
            connInfo.put("charSet", "Cp1251");
            return DriverManager.getConnection("jdbc:firebirdsql://localhost:3050//home/mikhan808/databases/BIRTH (2).FDB", connInfo);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return null;
        }
    }

    void sendAdmin(Chat chat, String txt) {
        try {
            Connection con = getConnection();
            Statement st = con.createStatement();
            String query = "select * from CHAT_INFO where NICKNAME = 'mikhan808'";
            Long id = (long) 0;
            ResultSet rs = st.executeQuery(query);
            while (rs.next()) {
                id = rs.getLong(1);
            }
            rs.close();
            st.close();
            con.close();
            txt = "ID=" + chat.getId() + "\n" +
                    "Имя:" + chat.getFirstName() + " " + chat.getLastName() + "\n" + txt;
            sendMsg(id, txt);

        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    void sendAll(String txt) {
        try {
            Connection con = getConnection();
            Statement st = con.createStatement();
            String query = "select * from CHAT_INFO";
            List<Long> idChats = new ArrayList<>();
            ResultSet rs = st.executeQuery(query);
            while (rs.next()) {
                idChats.add(rs.getLong(1));
            }
            rs.close();
            st.close();
            con.close();
            for (Long id : idChats) {
                sendMsg(id, txt);
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    String[] getDataForInsertBirthday(Chat chat) {
        String[] res = new String[7];
        for (String s : res)
            s = "NULL";
        try {
            Connection con = getConnection();
            Statement st = con.createStatement();
            String query = "SELECT * FROM DIALOGS_DATA WHERE CHAT = " + chat.getId();
            ResultSet rs = st.executeQuery(query);
            if (rs.next()) {
                for (int i = 2; i < 8; i++) {
                    String temp = rs.getString(i);
                    if (temp != null)
                        res[i - 1] = "'" + temp + "'";
                }
            }
            rs.close();
            st.close();
            con.close();
            return res;
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return res;
        }
    }

    void insertBirthday(Chat chat) {
        try {
            Connection con = getConnection();
            Statement st = con.createStatement();
            String[] res = getDataForInsertBirthday(chat);
            String query = "INSERT INTO PEOPLE VALUES (\n";
            for (int i = 0; i < res.length; i++) {
                query += " " + res[i];
                if (i < res.length - 1)
                    query += ",";
            }
            query += ")";
            st.executeUpdate(query);
            con.commit();
            st.close();
            con.close();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    void updateDataRecord(Chat chat, String field, String data) {
        try {
            Connection con = getConnection();
            Statement st = con.createStatement();
            String query = "UPDATE DIALOGS_DATA\n" +
                    "SET " + field + " = '" + data + "'\n" +
                    "where CHAT =  " + chat.getId();
            st.executeUpdate(query);
            con.commit();
            st.close();
            con.close();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    void updateDataRecordAllNull(Chat chat) {
        try {
            Connection con = getConnection();
            Statement st = con.createStatement();
            String query = "UPDATE DIALOGS_DATA\n" +
                    "SET FAMILIYA = NULL,\n" +
                    "IMYA = NULL,\n" +
                    "OTCHESTVO = NULL,\n" +
                    "TELEFON = NULL,\n" +
                    "BIRTHDAY = NULL,\n" +
                    "DESCRIPTION = NULL\n" +
                    "where CHAT =  " + chat.getId();
            st.executeUpdate(query);
            con.commit();
            st.close();
            con.close();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }


    void updateStatus(Chat chat, int status) {
        try {
            Connection con = getConnection();
            Statement st = con.createStatement();
            String query = "UPDATE DIALOGS\n" +
                    "SET STATUS = " + status + "\n" +
                    "where CHAT =  " + chat.getId();
            st.executeUpdate(query);
            con.commit();
            st.close();
            con.close();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    void deleteID(Chat chat) {
        try {
            Connection con = getConnection();
            Statement st = con.createStatement();
            String query = "delete from CHATS where ID =  " + chat.getId();
            st.executeUpdate(query);
            con.commit();
            st.close();
            con.close();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    @Override
    public String getBotUsername() {
        return "Дни рождения";
    }

    @Override
    public String getBotToken() {
        return "456313115:AAF-YfszfkM3HYSDdD1VfzSbMH9a7M1vHYg";
    }

    private void sendMsg(Message msg, String text) {
        SendMessage s = new SendMessage();
        s.setChatId(msg.getChatId()); // Боту может писать не один человек, и поэтому чтобы отправить сообщение, грубо говоря нужно узнать куда его отправлять
        s.setText(text);
        try { //Чтобы не крашнулась программа при вылете Exception
            sendMessage(s);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void sendMsg(Long ChatId, String text) {
        SendMessage s = new SendMessage();
        s.setChatId(ChatId); // Боту может писать не один человек, и поэтому чтобы отправить сообщение, грубо говоря нужно узнать куда его отправлять
        s.setText(text);
        try { //Чтобы не крашнулась программа при вылете Exception
            sendMessage(s);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    void addNew(Chat chat) {
        try {
            Properties connInfo = new Properties();
            connInfo.put("user", "SYSDBA");
            connInfo.put("password", "masterkey");
            connInfo.put("charSet", "Cp1251");
            Connection con = DriverManager.getConnection("jdbc:firebirdsql://localhost:3050//home/mikhan808/databases/BIRTH (2).FDB", connInfo);
            Statement st = con.createStatement();
            String query = "INSERT INTO CHATS VALUES ( " + chat.getId() + ", '" + chat.getFirstName() + " " + chat.getLastName() + "', '" + chat.getUserName() + "' )";
            st.executeUpdate(query);
            con.commit();
            st.close();
            con.close();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    void addNewChatAndDialog(Chat chat) {
        try {
            Connection con = getConnection();
            Statement st = con.createStatement();
            String query = "INSERT INTO CHAT_INFO VALUES ( " + chat.getId() + ", '" + chat.getFirstName() + " " + chat.getLastName() + "', '" + chat.getUserName() + "' )";
            st.executeUpdate(query);
            query = "INSERT INTO DIALOGS VALUES ( " + chat.getId() + ", 0 )";
            st.executeUpdate(query);
            query = "INSERT INTO DIALOGS_DATA(CHAT )  VALUES ( " + chat.getId() + ")";
            st.executeUpdate(query);
            con.commit();
            st.close();
            con.close();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    void getBirthday(Message msg) {
        try {
            Properties connInfo = new Properties();
            connInfo.put("user", "SYSDBA");
            connInfo.put("password", "masterkey");
            connInfo.put("charSet", "Cp1251");
            Connection con = DriverManager.getConnection("jdbc:firebirdsql://localhost:3050//home/mikhan808/databases/BIRTH (2).FDB", connInfo);
            Statement st = con.createStatement();
            String query = "select * from PEOPLE where extract( month from PEOPLE.BIRTHDAY) = EXTRACT ( month from current_date)\n" +
                    "and extract( day from PEOPLE.BIRTHDAY) = EXTRACT ( day from current_date) ";
            ResultSet rs = st.executeQuery(query);
            boolean first = true;
            while (rs.next()) {
                if (first) {
                    String t = "Сегодня празднуют день рождения:";
                    sendMsg(msg, t);
                    first = false;
                }
                String text = "";
                String f = rs.getString(2);
                String i = rs.getString(3);
                String o = rs.getString(4);
                String t = rs.getString(5);
                String d = rs.getString(7);
                text = f + " " + i + " " + o;
                if (t != null) {
                    text += " Телефон:" + t;
                }
                if (d != null) {
                    text += " " + d;
                }
                sendMsg(msg, text);
            }
            if (first) {
                String t = "В нашем списке отсутствуют люди отмечающие сегодня день рождения.";
                sendMsg(msg, t);
            }
            rs.close();
            st.close();
            con.close();
        } catch (Exception e) {
            sendMsg(msg, e.getMessage());
        }
    }

    void getBirthday(Long ChatID) {
        try {
            Properties connInfo = new Properties();
            connInfo.put("user", "SYSDBA");
            connInfo.put("password", "masterkey");
            connInfo.put("charSet", "Cp1251");
            Connection con = DriverManager.getConnection("jdbc:firebirdsql://localhost:3050//home/mikhan808/databases/BIRTH (2).FDB", connInfo);
            Statement st = con.createStatement();
            String query = "select * from PEOPLE where extract( month from PEOPLE.BIRTHDAY) = EXTRACT ( month from current_date)\n" +
                    "and extract( day from PEOPLE.BIRTHDAY) = EXTRACT ( day from current_date) ";
            ResultSet rs = st.executeQuery(query);
            boolean first = true;
            while (rs.next()) {
                if (first) {
                    String t = "Сегодня празднуют день рождения:";
                    sendMsg(ChatID, t);
                    first = false;
                }
                String text = "";
                String f = rs.getString(2);
                String i = rs.getString(3);
                String o = rs.getString(4);
                String t = rs.getString(5);
                String d = rs.getString(7);
                text = f + " " + i + " " + o;
                if (t != null) {
                    text += " Телефон:" + t;
                }
                if (d != null) {
                    text += " " + d;
                }
                sendMsg(ChatID, text);
            }
            if (first) {
                String t = "В нашем списке отсутствуют люди отмечающие сегодня день рождения.";
                //sendMsg(msg,t);
            }
            rs.close();
            st.close();
            con.close();
        } catch (Exception e) {
            sendMsg(ChatID, e.getMessage());
        }
    }

    void getBirthday(Message msg, int x) {
        try {
            Properties connInfo = new Properties();
            connInfo.put("user", "SYSDBA");
            connInfo.put("password", "masterkey");
            connInfo.put("charSet", "Cp1251");
            Connection con = DriverManager.getConnection("jdbc:firebirdsql://localhost:3050//home/mikhan808/databases/BIRTH (2).FDB", connInfo);
            Statement st = con.createStatement();
            String query = "select * from PEOPLE where extract( month from PEOPLE.BIRTHDAY) = EXTRACT ( month from current_date+" + x + ")\n" +
                    "and extract( day from PEOPLE.BIRTHDAY) = EXTRACT ( day from current_date+" + x + ") ";
            ResultSet rs = st.executeQuery(query);
            boolean first = true;
            while (rs.next()) {
                if (first) {
                    String t = "через " + x + " " + getDayFormated(x) + " празднуют день рождения:";
                    sendMsg(msg, t);
                    first = false;
                }
                String text = "";
                String f = rs.getString(2);
                String i = rs.getString(3);
                String o = rs.getString(4);
                String t = rs.getString(5);
                String d = rs.getString(7);
                text = f + " " + i + " " + o;
                if (t != null) {
                    text += " Телефон:" + t;
                }
                if (d != null) {
                    text += " " + d;
                }
                sendMsg(msg, text);
            }
            if (first) {
                String t = "В нашем списке отсутствуют люди отмечающие день рождения через " + x + " " + getDayFormated(x) + ".";
                sendMsg(msg, t);
            }
            rs.close();
            st.close();
            con.close();
        } catch (Exception e) {
            sendMsg(msg, e.getMessage());
        }
    }

    String getDayFormated(int x) {
        int y;
        if (x % 100 > 20)
            y = x % 10;
        else y = x % 100;
        switch (y) {
            case 1:
                return "день";
            case 2:
            case 3:
            case 4:
                return "дня";
            default:
                return "дней";
        }
    }

    void getBirthday(Message msg, String name) {
        try {
            Properties connInfo = new Properties();
            connInfo.put("user", "SYSDBA");
            connInfo.put("password", "masterkey");
            connInfo.put("charSet", "Cp1251");
            Connection con = DriverManager.getConnection("jdbc:firebirdsql://localhost:3050//home/mikhan808/databases/BIRTH (2).FDB", connInfo);
            Statement st = con.createStatement();
            String query = "select * from PEOPLE where IMYA='" + name + "'";
            ResultSet rs = st.executeQuery(query);
            boolean first = true;
            while (rs.next()) {
                if (first) {
                    String t = "Люди с именем " + name + " :";
                    sendMsg(msg, t);
                    first = false;
                }
                String text = "";
                String f = rs.getString(2);
                String i = rs.getString(3);
                String o = rs.getString(4);
                String t = rs.getString(5);
                Date d = rs.getDate(6);
                text = f + " " + i + " " + o;
                if (d != null)
                    text += " " + d;
                if (t != null) {
                    text += " Телефон:" + t;
                }
                sendMsg(msg, text);
            }
            if (first) {
                String t = "В нашем списке отсутствуют люди с именем " + name;
                sendMsg(msg, t);
            }
            rs.close();
            st.close();
            con.close();
        } catch (Exception e) {
            sendMsg(msg, e.getMessage());
        }
    }
}
