package com.birtthdayForTelegram.bot;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.sql.*;
import java.util.Date;
import java.util.*;

/**
 * Created by МишаИОля on 15.10.2017.
 */

public class Example extends TelegramLongPollingBot {
    public final static String PASSWORD = "****";

    public static Connection getConnection() {
        try {
            Properties connInfo = new Properties();
            connInfo.put("user", "SYSDBA");
            connInfo.put("password", "masterkey");
            connInfo.put("charSet", "Cp1251");
            return DriverManager.getConnection("jdbc:firebirdsql:127.0.0.1/3026:D:\\databases\\BIRTH.FDB", connInfo);
        } catch (Exception e) {
            Log.error(e.getMessage());
            return null;
        }
    }

    @Override
    public void onUpdateReceived(Update update) {
        Message msg = update.getMessage();
        Long id = msg.getChatId();
        Chat chat = msg.getChat();
        int status;
        try {
            String query = "SELECT * FROM DIALOGS WHERE CHAT = " + id;
            ResultSet rs = getResultSet(query);
            if (rs != null) {
                if (rs.next()) {
                    status = rs.getInt(2);
                } else {
                    if (Objects.equals(msg.getText(), PASSWORD)) {
                        addNewChatAndDialog(chat);
                        status = Status.NORMAL;
                    } else {
                        sendMsg(id, "Введите пароль");
                        releaseResources(rs);
                        return;
                    }
                }
            } else return;
            releaseResources(rs);
            String txt = msg.getText();
            if (txt != null) {
                if (txt.equals("/cancel"))
                    updateStatus(chat, Status.NORMAL);
                else
                    switch (status) {
                        case Status.NORMAL:
                            if (txt.equals("Привет"))
                                sendMsg(id, "Привет");
                            if (txt.indexOf("*") == 0)
                                getBirthdayOfName(id, txt.substring(1));
                            else if (txt.indexOf("/id") == 0) {
                                try {
                                    int x = Integer.parseInt(txt.substring("/id".length()));
                                    sendMsg(id, getFullNamePeople(x));
                                } catch (Exception e) {
                                    sendMsg(id, "после /id должны идти только цифры");
                                }

                            } else if (txt.indexOf("!") == 0) {
                                try {
                                    int x = Integer.parseInt(txt.substring(1));
                                    getBirthdaysInFewDays(id, x);
                                } catch (Exception e) {
                                    sendMsg(id, "после ! должны идти только цифры");
                                }
                            } else if (txt.indexOf("+") == 0) {
                                updateStatus(chat, Status.TIME_TO_SEND);
                                sendMsg(id, "Введите время в которое вы хотите получать оповещения в формате ЧЧ:ММ (например,08:00)");
                            } else if (txt.indexOf("-") == 0) {
                                deleteID(msg.getChat());
                                sendMsg(id, "Теперь мы вас не будем беспокоить нашими оповещениями");
                            } else if (txt.equals("/add")) {
                                updateDataRecordAllNull(chat);
                                updateStatus(chat, Status.FAMILIYA);
                                sendMsg(id, "Введите фамилию добавляемого человека");
                            } else if (txt.equals("/help")) {
                                sendMsg(id, "Список команд:\n" +
                                        "/help - список команд\n" +
                                        "/add - добавить человека которого нет в списке\n" +
                                        "/cancel - отменить добавление человека\n" +
                                        "/addphoto - Добавить фотографию человеку\n" +
                                        "/edit - Исправить неверные сведения\n" +
                                        "*Имя - список людей с указанным именем\n" +
                                        "!X - список людей празднующих день рождения, через X дней\n" +
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
                            } else if (txt.indexOf("$") == 0) {
                                txt = txt.substring(1);
                                int x = Integer.parseInt(txt);
                                sendPhotoForPeopleID(chat, x);
                            } else if (txt.equals("/addphoto")) {
                                updateStatus(chat, Status.ADD_PHOTO_1);
                                sendMsg(id, "Введите № человека, фото, которого вы хотите добавить");
                            } else if (txt.equals("/edit")) {
                                updateDataRecordAllNull(chat);
                                updateStatus(chat, Status.EDIT);
                                sendMsg(id, "Введите № человека, информацию о котором вы хотите исправить");
                            } else if (txt.equals("/condition")) {
                                updateStatus(chat, Status.CONDITION);
                                sendMsg(id, "Введите условие по которому будет выполнен SQL запрос");
                            } else if (txt.equals("/molitva")) {
                                getTodayMolitva(id);
                            } else if (txt.equals("/plan")) {
                                getTodayPlan(id, false);
                            } else
                                getTodayBirthdays(id);
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
                            sendMsg(id, "Введите краткое описание добавляемого человека (например: Старший сын Ивана и Алены живет в г.Москва) или," +
                                    " если вы ничего не знаете о человеке, отправьте ?");
                            break;
                        case Status.DESCRIPTION:
                            if (txt.trim().indexOf("?") != 0)
                                updateDataRecord(chat, "DESCRIPTION", txt);
                            insertBirthday(chat);
                            updateStatus(chat, Status.NORMAL);
                            sendMsg(id, "Человек добавлен");
                            break;
                        case Status.SEND_TO:
                            String[] res = getDataForInsertBirthday(chat);
                            Long id_to = Long.parseLong(res[6].trim().replace("'", ""));
                            updateStatus(chat, Status.NORMAL);
                            sendMsg(id_to, txt);
                            break;
                        case Status.TIME_TO_SEND:
                            addNew(msg);
                            sendMsg(chat.getId(), "Теперь в " + txt + " вам будут приходить сообщения с именниниками");
                            updateStatus(chat, Status.NORMAL);
                            break;
                        case Status.ADD_PHOTO_1:
                            updateDataRecord(chat, "DESCRIPTION", txt);
                            updateStatus(chat, Status.ADD_PHOTO_2);
                            sendMsg(id, "Отправьте фото этого человека");
                            break;
                        case Status.EDIT:
                            try {
                                Integer.parseInt(txt);
                                updateDataRecord(chat, "DESCRIPTION", txt);
                                updateStatus(chat, Status.EDIT_2);
                                StringBuilder sb = new StringBuilder();
                                sb.append("Введите цифру поля,которое Вы хотите исправить\n");
                                sb.append("1 - Фамилия\n");
                                sb.append("2 - Имя\n");
                                sb.append("3 - Отчество\n");
                                sb.append("4 - Телефон\n");
                                sb.append("5 - День рождения\n");
                                sb.append("6 - Описание");
                                sendMsg(id, sb.toString());
                            } catch (NumberFormatException e) {
                                sendMsg(id, "Номер может быть только числом. Пожалуйста повторите ввод");
                            }
                            break;
                        case Status.EDIT_2:
                            try {
                                int pole = Integer.parseInt(txt) + Status.EDIT;
                                updateStatus(chat, pole);
                                String s_pole = "";
                                switch (pole) {
                                    case Status.EDIT_FAMILIYA:
                                        s_pole = "фамилию";
                                        break;
                                    case Status.EDIT_IMYA:
                                        s_pole = "имя";
                                        break;
                                    case Status.EDIT_OTCHESTVO:
                                        s_pole = "отчество";
                                        break;
                                    case Status.EDIT_TELEFON:
                                        s_pole = "телефон";
                                        break;
                                    case Status.EDIT_BIRTHDAY:
                                        s_pole = "дату рождения в формате ДД.ММ.ГГГГ (например 01.01.1990)";
                                        break;
                                    case Status.EDIT_DESCRIPTION:
                                        s_pole = "описание";
                                        break;

                                }
                                sendMsg(id, "Введите " + s_pole);
                            } catch (NumberFormatException e) {
                                sendMsg(id, "Номер может быть только числом. Пожалуйста повторите ввод");
                            }
                            break;
                        case Status.EDIT_FAMILIYA:
                            updateDataRecordPeopleInfo(chat, "FAMILIYA", txt);
                            updateStatus(chat, Status.NORMAL);
                            sendMsg(id, "Исправлено");
                            break;
                        case Status.EDIT_IMYA:
                            updateDataRecordPeopleInfo(chat, "IMYA", txt);
                            updateStatus(chat, Status.NORMAL);
                            sendMsg(id, "Исправлено");
                            break;
                        case Status.EDIT_OTCHESTVO:
                            updateDataRecordPeopleInfo(chat, "OTCHESTVO", txt);
                            updateStatus(chat, Status.NORMAL);
                            sendMsg(id, "Исправлено");
                            break;
                        case Status.EDIT_TELEFON:
                            updateDataRecordPeopleInfo(chat, "TELEFON", txt);
                            updateStatus(chat, Status.NORMAL);
                            sendMsg(id, "Исправлено");
                            break;
                        case Status.EDIT_BIRTHDAY:
                            updateDataRecordPeopleInfo(chat, "BIRTHDAY", txt);
                            updateStatus(chat, Status.NORMAL);
                            sendMsg(id, "Исправлено");
                            break;
                        case Status.EDIT_DESCRIPTION:
                            updateDataRecordPeopleInfo(chat, "DESCRIPTION", txt);
                            updateStatus(chat, Status.NORMAL);
                            sendMsg(id, "Исправлено");
                            break;
                        case Status.CONDITION:
                            getBirthdayOfCondition(id, txt);
                            updateStatus(chat, Status.NORMAL);
                            break;

                    }
            } else if (status == Status.ADD_PHOTO_2) {
                String[] res = getDataForInsertBirthday(chat);
                int x = Integer.parseInt(res[6].replace("'", ""));
                List<PhotoSize> list = msg.getPhoto();
                if (list != null && list.size() > 0) {
                    sendMsg(chat.getId(), "Отправьте фото еще раз без сжатия(Для этого выберите не \"отправить фото\", а \"отправить файл\")");
                } else {
                    Document d = msg.getDocument();
                    if (d != null) {
                        updateStatus(chat, Status.NORMAL);
                        addPhotoForPeopleId(x, d.getFileId());
                        sendMsg(chat.getId(), "Фото добавлено");
                        String text = chat.getFirstName()+" "+chat.getLastName()+","+chat.getUserName()+" добавил фото человека:\n"+getFullNamePeople(x);
                        sendAdmin(text);
                    }
                }


            }

        } catch (Exception e) {
            Log.error(e.getMessage());
            sendMsg(chat.getId(), "Ошибка: " + e.getMessage());
        }


    }

    public static Statement getStatement() {
        try {
            return getConnection().createStatement();
        } catch (Exception e) {
            Log.error(e.getMessage());
            return null;
        }
    }

    public static ResultSet getResultSet(String query) {
        Log.add("Executing:" + query);
        try {
            return getStatement().executeQuery(query);
        } catch (Exception e) {
            Log.error(e.getMessage());
            return null;
        }
    }

    public static void executeUpdate(String query) {
        Log.add("Executing:" + query);
        Statement st = null;
        try {
            st = getStatement();
            st.executeUpdate(query);
        } catch (Exception e) {
            Log.error(e.getMessage());
        } finally {
            releaseResources(st);
        }
    }

    public static PreparedStatement getPreparedStatement(String sql) {
        try {
            return getConnection().prepareStatement(sql);
        } catch (Exception e) {
            Log.error(e.getMessage());
            return null;
        }
    }


    public static void executeUpdate(String query, List<Object> params) {
        Log.add("Executing:" + query);
        PreparedStatement st = null;
        try {
            st = getPreparedStatement(query);
            for (int i = 0; i < params.size(); i++) {
                st.setObject(i + 1, params.get(i));
            }
            st.executeUpdate();
        } catch (Exception e) {
            Log.error(e.getMessage());
        } finally {
            releaseResources(st);
        }
    }

    void addPhotoForPeopleId(int id, String fileId) {
        String query = "UPDATE PEOPLE SET PHOTO = ? WHERE ID = " + id;
        try {
            GetFile request = new GetFile(fileId);
            org.telegram.telegrambots.meta.api.objects.File tfile = execute(request);
            File file = downloadFile(tfile);
            List<Object> list = new ArrayList<>();
            list.add(new FileInputStream(file));
            executeUpdate(query, list);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void sendPhotoForPeopleID(Chat chat, int id) {
        String query = "SELECT PHOTO,FULL_NAME FROM PEOPLE WHERE ID = " + id;
        ResultSet rs = getResultSet(query);
        try {
            if (rs.next()) {
                Blob b = rs.getBlob(1);
                String name = rs.getString(2);
                if (b != null) {
                    InputStream stream = b.getBinaryStream();
                    sendPhoto(chat.getId(), stream, name);
                }
            }
        } catch (SQLException e) {
            Log.error(e.getMessage());
            e.printStackTrace();
        } finally {
            releaseResources(rs);
        }
    }

    public static void releaseResources(Statement st) {
        try {
            Connection con = st.getConnection();
            con.close();
        } catch (SQLException e) {
            Log.error(e.getMessage());
        }
    }

    public static void releaseResources(ResultSet rs) {
        try {
            Statement st = rs.getStatement();
            releaseResources(st);
        } catch (SQLException e) {
            Log.error(e.getMessage());
        }
    }

    void sendAdmin(Chat chat, String txt) {
        try {
            txt = "ID=" + chat.getId() + "\n" +
                    "Имя:" + chat.getFirstName() + " " + chat.getLastName() + "\n" + txt;
            sendAdmin(txt);
        } catch (Exception e) {
            Log.error(e.getMessage());
        }
    }
    void sendAdmin( String txt) {
        try {
            String query = "select * from CHAT_INFO where NICKNAME = 'mikhan808'";
            Long id = (long) 0;
            ResultSet rs = getResultSet(query);
            while (rs.next()) {
                id = rs.getLong(1);
            }
            releaseResources(rs);
            sendMsg(id, txt);
        } catch (Exception e) {
            Log.error(e.getMessage());
        }
    }

    List<Long> allChats() {
        List<Long> idChats = new ArrayList<>();
        try {
            String query = "select * from CHAT_INFO";
            ResultSet rs = getResultSet(query);
            while (rs.next()) {
                idChats.add(rs.getLong(1));
            }
            releaseResources(rs);
        } catch (Exception e) {
            Log.error(e.getMessage());
        }
        return idChats;
    }

    void sendAll(String txt) {
        try {
            List<Long> idChats = allChats();
            for (Long id : idChats) {
                sendMsg(id, txt);
            }
        } catch (Exception e) {
            Log.error(e.getMessage());
        }
    }

    String[] getDataForInsertBirthday(Chat chat) {
        String[] res = new String[8];
        for (int i = 0; i < 8; i++)
            res[i] = "NULL";
        try {
            String query = "SELECT * FROM DIALOGS_DATA WHERE CHAT = " + chat.getId();
            ResultSet rs = getResultSet(query);
            if (rs.next()) {
                for (int i = 2; i < 8; i++) {
                    String temp = rs.getString(i);
                    if (temp != null)
                        res[i - 1] = "'" + temp + "'";
                }
            }
            releaseResources(rs);
            return res;
        } catch (Exception e) {
            Log.error(e.getMessage());
            return res;
        }
    }

    int getPeopleID(String f, String i, String d) {
        return getPeopleID(f, i, null, d);
    }

    int getPeopleID(String f, String i, String o, String d) {
        if (o == null || o.trim().equalsIgnoreCase("NULL") || o.trim().equals(""))
            return getPeopleID(f + " " + i, d);
        else return getPeopleID(f + " " + i + " " + o, d);
    }

    int getPeopleID(String full_name, String d) {
        int res = -1;
        try {
            String query = "select * from PEOPLE where FULL_NAME = '" + full_name + "' and BIRTHDAY = '" + d + "'";
            ResultSet rs = getResultSet(query);
            if (rs.next()) {
                res = rs.getInt(1);
            }
            releaseResources(rs);
        } catch (Exception e) {
            Log.error(e.getMessage());
        }
        return res;
    }

    void insertBirthday(Chat chat) {
        try {
            String[] res = getDataForInsertBirthday(chat);
            StringBuilder query = new StringBuilder("INSERT INTO PEOPLE VALUES (\n");
            for (int i = 0; i < res.length; i++) {
                query.append(" ").append(res[i]);
                if (i < res.length - 1)
                    query.append(",");
            }
            query.append(")");
            try {
                executeUpdate(query.toString());
            } catch (Exception e) {
                Log.error(e.getMessage());
            }
            int id;
            if (res[3] != null)
                id = getPeopleID(res[1].replace("'", ""), res[2].replace("'", ""), res[3].replace("'", ""), res[5].replace("'", ""));
            else id = getPeopleID(res[1].replace("'", ""), res[2].replace("'", ""), res[5].replace("'", ""));
            String txt = chat.getFirstName() + " " + chat.getLastName() + "," + chat.getUserName() + " добавил человека:\n" + getFullNamePeople(id);
            sendAdmin(txt);
        } catch (Exception e) {
            Log.error(e.getMessage());
        }
    }

    void updateDataRecordPeopleInfo(Chat chat, String field, String data) {
        String[] ss = getDataForInsertBirthday(chat);
        int x = Integer.parseInt(ss[6].replace("'", ""));
        try {
            String query = "UPDATE PEOPLE\n" +
                    "SET " + field + " = '" + data + "'\n" +
                    "where id =  " + x;
            executeUpdate(query);
        } catch (Exception e) {
            Log.error(e.getMessage());
        }
    }

    void updateDataRecord(Chat chat, String field, String data) {
        try {
            String query = "UPDATE DIALOGS_DATA\n" +
                    "SET " + field + " = '" + data + "'\n" +
                    "where CHAT =  " + chat.getId();
            executeUpdate(query);
        } catch (Exception e) {
            Log.error(e.getMessage());
        }
    }

    void updateDataRecordAllNull(Chat chat) {
        try {
            String query = "UPDATE DIALOGS_DATA\n" +
                    "SET FAMILIYA = NULL,\n" +
                    "IMYA = NULL,\n" +
                    "OTCHESTVO = NULL,\n" +
                    "TELEFON = NULL,\n" +
                    "BIRTHDAY = NULL,\n" +
                    "DESCRIPTION = NULL\n" +
                    "where CHAT =  " + chat.getId();
            executeUpdate(query);
        } catch (Exception e) {
            Log.error(e.getMessage());
        }
    }


    void updateStatus(Chat chat, int status) {
        try {
            String query = "UPDATE DIALOGS\n" +
                    "SET STATUS = " + status + "\n" +
                    "where CHAT =  " + chat.getId();
            executeUpdate(query);
        } catch (Exception e) {
            Log.error(e.getMessage());
        }
    }

    String getFullNamePeople(int id) {
        String res = "Никакой Никак Никакович";
        try {
            String query = "select UPPER (FULL_NAME) from PEOPLE where ID =  " + id;
            ResultSet rs = getResultSet(query);
            if (rs.next())
                res = rs.getString(1);
            releaseResources(rs);
        } catch (Exception e) {
            Log.error(e.getMessage());
        }
        return res;
    }

    void deletePeople(Chat chat, int id) {
        try {
            String query = "delete from VIEW_PEOPLE where CHAT_ID =  " + chat.getId() + " AND PEOPLE_ID = " + id;
            executeUpdate(query);
        } catch (Exception e) {
            Log.error(e.getMessage());
        }
        sendMsg(chat.getId(), "Мы вам больше не будем присылать сообщения о человеке, которого зовут:\n" + getFullNamePeople(id));
    }

    void deleteID(Chat chat) {
        try {
            String query = "delete from CHATS where ID =  " + chat.getId();
            executeUpdate(query);
        } catch (Exception e) {
            Log.error(e.getMessage());
        }
    }

    @Override
    public String getBotUsername() {
        return "Напоминалка";
    }

    @Override
    public String getBotToken() {
        return "****";
    }

    public void sendPhoto(Long chatId, File file, String text) {
        sendPhoto(chatId, new InputFile(file), text);
    }

    public void sendPhoto(Long chatId, InputStream is, String text) {
        InputFile file = new InputFile();
        file.setMedia(is, text);
        sendPhoto(chatId, file, text);
    }

    public void sendPhoto(Long chatId, InputFile inputFile, String text) {
        SendPhoto s = new SendPhoto();
        s.setChatId(chatId.toString());
        s.setPhoto(inputFile);
        if (text != null)
            s.setCaption(text);
        try {
            execute(s);
        } catch (TelegramApiException e) {
            Log.error(e.getMessage());
            e.printStackTrace();
        }
    }

    private void sendMsg(Long ChatId, String text) {
        if (text.length() > 4096) {
            String text1 = text.substring(0, 4095);
            String text2 = text.substring(4095);
            sendMsg(ChatId, text1);
            sendMsg(ChatId, text2);
        } else {
            SendMessage s = new SendMessage();
            s.setChatId(ChatId); // Боту может писать не один человек, и поэтому чтобы отправить сообщение, грубо говоря нужно узнать куда его отправлять
            s.setText(text);
            try {
                execute(s);
            } catch (TelegramApiException e) {
                Log.error(e.getMessage());
                e.printStackTrace();
            }
        }
    }

    void addNew(Message msg) {
        try {
            Chat chat = msg.getChat();
            String query = "SELECT * FROM CHATS WHERE ID = " + chat.getId();
            ResultSet rs = getResultSet(query);
            if (rs.next()) {
                query = "update chats set time_sending='" + msg.getText() + ":00' where id = " + chat.getId();
                executeUpdate(query);
            } else {
                query = "INSERT INTO CHATS (ID,FULL_NAME,NIKNAME,TIME_SENDING) VALUES ( " + chat.getId() + ", '" + chat.getFirstName() + " " + chat.getLastName() + "', '" + chat.getUserName() + "', '" + msg.getText() + "' )";
                executeUpdate(query);
            }
            releaseResources(rs);
        } catch (Exception e) {
            Log.error(e.getMessage());
        }
    }

    void addNewPublic(Long chat) {
        try {
            ResultSet rs = getResultSet("SELECT * FROM PUBLIC_PEOPLE");
            while (rs.next()) {
                executeUpdate("INSERT INTO VIEW_PEOPLE VALUES (" + chat + " , " + rs.getInt(1) + " )");
            }
            releaseResources(rs);
        } catch (SQLException e) {
            Log.error(e.getMessage());
        }
    }

    void addNewChatAndDialog(Chat chat) {
        try {
            String query = "INSERT INTO CHAT_INFO VALUES ( " + chat.getId() + ", '" + chat.getFirstName() + " " + chat.getLastName() + "', '" + chat.getUserName() + "' )";
            executeUpdate(query);
            query = "INSERT INTO DIALOGS VALUES ( " + chat.getId() + ", 0 )";
            executeUpdate(query);
            query = "INSERT INTO DIALOGS_DATA(CHAT )  VALUES ( " + chat.getId() + ")";
            executeUpdate(query);
            addNewPublic(chat.getId());
            query = "INSERT INTO GROUPS (ID , NAME , PASSWORD , PRIVATE,OWNER_ID)  VALUES (null , '"+chat.getFirstName() + " " + chat.getLastName()+"','*****' , 1,"+chat.getId()+")";
            executeUpdate(query);
            query = "SELECT ID FROM GROUPS WHERE OWNER_ID ="+chat.getId()+" and name = '"+chat.getFirstName() + " " + chat.getLastName()+"' and private = 1";
            ResultSet rs1 = getResultSet(query);
            if(rs1.next()) {
                executeUpdate("INSERT INTO GROUP_CHATS (ID_GROUP, ID_CHAT)  VALUES (" + rs1.getLong(1) + ", " + chat.getId() + " )");
            }
            releaseResources(rs1);
            String txt = "К боту Дни Рождения присоединился пользователь:" + chat.getFirstName() + " " + chat.getLastName() + "', '" + chat.getUserName();
            sendAdmin(txt);
        } catch (Exception e) {
            Log.error(e.getMessage());
        }
    }

    String buildQueryBirthdaysOfCondition(String condition) {
        return buildQueryBirthdays(condition, null, 0);
    }

    String buildQueryPlan(int days) {
        String condition = "mesyac = EXTRACT ( month from " + buildDate(days) + " )\n" +
                "and den = EXTRACT ( day from " + buildDate(days) + " ) ";
        return "select mesto1, mesto2, mesto3 from plan_bible where " + condition;
    }

    String buildBibleTextQuery(String mesto) {
        String sql = "select book, glava, stih, text from bible_view where book='";
        String[] parts = mesto.split(":");
        sql += parts[0] + "' and ";
        parts = parts[1].split(",");
        if (parts.length > 1) {
            sql += "glava=" + parts[0];
            parts = parts[1].split("-");
            if (parts.length > 1) {
                sql += " and stih>=" + parts[0] + " and stih<=" + parts[1];
            }

        } else {
            parts = parts[0].split("-");
            if (parts.length > 1) {
                sql += "glava>=" + parts[0] + " and glava<=" + parts[1];
            } else sql += "glava=" + parts[0];
        }
        sql += "\norder by 1,2,3";
        return sql;

    }

    String buildQueryMolitva(int days) {

        String condition = "number_date = EXTRACT ( day from " + buildDate(days) + " ) ";
        return "select semya from molitva where " + condition;
    }

    String buildQueryBirthdays() {
        return buildQueryBirthdays(null, null, 0);
    }

    String buildQueryBirthdays(int days) {
        return buildQueryBirthdays(null, null, days);
    }

    String buildQueryBirthdays(String name) {
        return buildQueryBirthdays(null, name, 0);
    }

    String buildQueryBirthdays(String condition, String name, int days) {
        if (condition == null) {
            condition = "extract( month from BIRTHDAY) = EXTRACT ( month from " + buildDate(days) + " )\n" +
                    "and extract( day from BIRTHDAY) = EXTRACT ( day from " + buildDate(days) + " ) ";
            if (name != null)
                condition = "IMYA='" + name + "'";
        }
        return "select * from ZHIVYE where " + condition;
    }

    String buildDate(int days) {
        if (days == 0)
            return "current_date";
        return "current_date+" + days;
    }

    void getTodayPlan(Long chat, boolean sendBible) {
        String query = buildQueryPlan(0);
        String firstText = "Сегодня читаем:";
        String emptyMsg = "Ошибка";
        sendInfoAboutPlan(chat, query, firstText, emptyMsg, sendBible);
    }

    void getTodayMolitva(Long chat) {
        String query = buildQueryMolitva(0);
        String firstText = "Сегодня молитва за следующие семьи:";
        String emptyMsg = "Сегодня свободная тема молитвы";
        sendInfoAboutMolitva(chat, query, firstText, emptyMsg);
    }

    void getMolitvaForSchedule(Long chat) {
        String query = buildQueryMolitva(0);
        String firstText = "Сегодня молитва за следующие семьи:";
        String emptyMsg = null;
        sendInfoAboutMolitva(chat, query, firstText, emptyMsg);
    }

    void getTodayBirthdays(Long chat) {
        String query = buildQueryBirthdays();
        String firstText = "Сегодня празднуют день рождения:";
        String vosrastText = "";
        String emptyMsg = "В нашем списке отсутствуют люди отмечающие сегодня день рождения.";
        sendInfoAboutPeople(chat, query, firstText, vosrastText, emptyMsg, true, false);
    }

    void getBirthdaysForSchedule(Long chat) {
        String query = buildQueryBirthdays();
        String firstText = "Сегодня празднуют день рождения:";
        String vosrastText = "";
        String emptyMsg = null;
        sendInfoAboutPeople(chat, query, firstText, vosrastText, emptyMsg, false, false);
    }

    void getBirthdaysInFewDays(Long chat, int x) {
        String query = buildQueryBirthdays(x);
        String firstText = "через " + x + " " + getDayFormated(x) + " празднуют день рождения:";
        String vosrastText = "На данный момент возраст:";
        String emptyMsg = "В нашем списке отсутствуют люди отмечающие день рождения через " + x + " " + getDayFormated(x) + ".";
        sendInfoAboutPeople(chat, query, firstText, vosrastText, emptyMsg, true, true);
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

    String getYearFormated(int x) {
        int y;
        if (x % 100 > 20)
            y = x % 10;
        else y = x % 100;
        switch (y) {
            case 1:
                return "год";
            case 2:
            case 3:
            case 4:
                return "года";
            default:
                return "лет";
        }
    }

    void sendInfoAboutMolitva(Long chat, String query, String firstMsg, String emptyMsg) {
        try {
            ResultSet rs = getResultSet(query);
            boolean first = true;
            while (rs.next()) {
                if (first) {
                    sendMsg(chat, firstMsg);
                    first = false;
                }
                String text = rs.getString(1);
                sendMsg(chat, text);
            }
            if (first) {
                if (emptyMsg != null)
                    sendMsg(chat, emptyMsg);
            }
            releaseResources(rs);
        } catch (Exception e) {
            Log.error(e.getMessage());
            sendMsg(chat, e.getMessage());
        }
    }

    void sendInfoAboutMesto(Long chat, String mesto, String emptyMsg) {
        try {
            ResultSet rs = getResultSet(buildBibleTextQuery(mesto));
            boolean first = true;
            String text = "";
            while (rs.next()) {
                if (first) {
                    first = false;
                }
                String m1 = rs.getString(1);
                String m2 = rs.getString(2);
                String m3 = rs.getString(3);
                String m4 = rs.getString(4);
                text += m1 + " " + m2 + ":" + m3 + " " + m4 + "\n";
            }
            if (first) {
                if (emptyMsg != null)
                    sendMsg(chat, emptyMsg);
            } else sendMsg(chat, text);
            releaseResources(rs);
        } catch (Exception e) {
            Log.error(e.getMessage());
            sendMsg(chat, e.getMessage());
        }
    }

    void sendInfoAboutPlan(Long chat, String query, String firstMsg, String emptyMsg, boolean sendBible) {
        try {
            ResultSet rs = getResultSet(query);
            while (rs.next()) {
                sendMsg(chat, firstMsg);
                for (int i = 1; i <= 3; i++) {
                    sendMsg(chat, rs.getString(i));
                }
                if (sendBible)
                    for (int i = 1; i <= 3; i++) {
                        sendInfoAboutMesto(chat, rs.getString(i), emptyMsg);
                    }
            }
            releaseResources(rs);
        } catch (Exception e) {
            Log.error(e.getMessage());
            sendMsg(chat, e.getMessage());
        }
    }

    void sendInfoAboutPeople(Long chat, String query, String firstMsg, String vozrastText, String emptyMsg, boolean useId, boolean showBirthday) {
        try {
            ResultSet rs = getResultSet(query);
            boolean first = true;
            while (rs.next()) {
                if (first) {
                    sendMsg(chat, firstMsg);
                    first = false;
                }
                String id = rs.getString(1);
                String f = rs.getString(2);
                String i = rs.getString(3);
                String o = rs.getString(4);
                String t = rs.getString(5);
                String d = rs.getString(7);
                Date date = rs.getDate(6);
                int v = rs.getInt(8);
                Blob photo = rs.getBlob(10);
                String text = f + " " + i;
                if (o != null)
                    text += " " + o;
                if (showBirthday) {
                    if (date != null)
                        text += " " + date;
                }
                text += "\n" + vozrastText + v + " " + getYearFormated(v);
                if (t != null) {
                    text += "\nТелефон:" + t;
                }
                if (d != null) {
                    text += "\n" + d;
                }
                if (useId) {
                    text += "\n№" + id;
                }
                sendMsg(chat, text);
                if (photo != null) {
                    InputStream is = photo.getBinaryStream();
                    sendPhoto(chat, is, ":-)");
                }
            }
            if (first) {
                if (emptyMsg != null)
                    sendMsg(chat, emptyMsg);
            }
            releaseResources(rs);
        } catch (Exception e) {
            Log.error(e.getMessage());
            sendMsg(chat, e.getMessage());
        }
    }

    void getBirthdayOfCondition(Long chat, String condition) {
        String query = buildQueryBirthdaysOfCondition(condition);
        String firstText = "Люди с заданным условием \"" + condition + "\" :";
        String vosrastText = "На данный момент возраст:";
        String emptyMsg = "В нашем списке отсутствуют люди с заданным условием \"" + condition + "\"";
        sendInfoAboutPeople(chat, query, firstText, vosrastText, emptyMsg, true, true);
    }

    void getBirthdayOfName(Long chat, String name) {
        String query = buildQueryBirthdays(name);
        String firstText = "Люди с именем " + name + " :";
        String vosrastText = "На данный момент возраст:";
        String emptyMsg = "В нашем списке отсутствуют люди с именем " + name;
        sendInfoAboutPeople(chat, query, firstText, vosrastText, emptyMsg, true, true);
    }
}
