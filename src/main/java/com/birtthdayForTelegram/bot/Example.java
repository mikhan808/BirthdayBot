package com.birtthdayForTelegram.bot;

import org.telegram.telegrambots.api.methods.GetFile;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.methods.send.SendPhoto;
import org.telegram.telegrambots.api.objects.*;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.exceptions.TelegramApiException;

import java.io.*;
import java.io.File;
import java.sql.*;
import java.util.*;
import java.util.Date;

/**
 * Created by МишаИОля on 15.10.2017.
 */
public class Example extends TelegramLongPollingBot {

    @Override
    public void onUpdateReceived(Update update) {
        Message msg = update.getMessage();
        Long id = msg.getChatId();
        Chat chat = msg.getChat();
        int status;
        try {
            String query = "SELECT * FROM DIALOGS WHERE CHAT = " + id;
            ResultSet rs = getResultSet(query);
            if (rs.next()) {
                status = rs.getInt(2);
            } else {
                addNewChatAndDialog(chat);
                status = Status.NORMAL;
            }
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
                            if (txt.indexOf("#") == 0)
                                getBirthdayOfName(id, txt.substring(1));
                            else if (txt.indexOf("!") == 0) {
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
                                        "#Имя - список людей с указанным именем\n" +
                                        "!X - список людей празднующих день рождения, через X дней\n" +
                                        "/add - добавить человека которого нет в списке\n" +
                                        "/cancel - отменить добавление человека\n" +
                                        "/delete - оменить уведомления об определенном человеке(например которого вы не знаете)\n" +
                                        "/addPhoto - Добавить фотографию человеку\n" +
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
                            } else if (txt.equals("/delete")) {
                                updateStatus(chat, Status.DELETE);
                                sendMsg(id, "Введите № человека, о дне рождения, которого вы больше не хотите получать уведомления");
                            } else if (txt.equals("/addPhoto")) {
                                updateStatus(chat, Status.ADD_PHOTO_1);
                                sendMsg(id, "Введите № человека, фото, которого вы хотите добавить");
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
                            updateStatus(chat, Status.PUBLIC_MAN);
                            sendMsg(id, "Если Вы не хотите чтоб другие пользователи получали информацию о добавленном человеке отправьте 0," +
                                    " если же Вы не считаете это тайной отправьте 1");
                            break;
                        case Status.PUBLIC_MAN:
                            boolean pm = (txt.trim().indexOf("0") != 0);
                            insertBirthday(chat, pm);
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
                        case Status.DELETE:
                            try {
                                int x = Integer.parseInt(txt.trim());
                                deletePeople(chat, x);
                                updateStatus(chat, Status.NORMAL);
                            } catch (Exception e) {
                                Log.error(e.getMessage());
                                sendMsg(chat.getId(), "В сообщении должны содержаться только цифры, если вы передумали отправьте /cancel");
                            }
                            break;
                        case Status.ADD_PHOTO_1:
                            updateDataRecord(chat, "DESCRIPTION", txt);
                            updateStatus(chat, Status.ADD_PHOTO_2);
                            sendMsg(id, "Отправьте фото этого человека");
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
                    }
                }


            }

        } catch (Exception e) {
            Log.error(e.getMessage());
            sendMsg(chat.getId(), "Ошибка: " + e.getMessage());
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
            Log.error(e.getMessage());
            return null;
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

    void addPhotoForPeopleId(int id, String file_id) {
        String query = "UPDATE PEOPLE SET PHOTO = ? WHERE ID = " + id;
        try {
            GetFile getFile = new GetFile();
            getFile.setFileId(file_id);
            org.telegram.telegrambots.api.objects.File tfile = getFile(getFile);
            File file = downloadFile(tfile);
            List<Object> list = new ArrayList<>();
            list.add(new FileInputStream(file));
            executeUpdate(query, list);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (TelegramApiException e) {
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
            String query = "select * from CHAT_INFO where NICKNAME = 'mikhan808'";
            Long id = (long) 0;
            ResultSet rs = getResultSet(query);
            while (rs.next()) {
                id = rs.getLong(1);
            }
            releaseResources(rs);
            txt = "ID=" + chat.getId() + "\n" +
                    "Имя:" + chat.getFirstName() + " " + chat.getLastName() + "\n" + txt;
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
        if (o == null || o.trim().toUpperCase().equals("NULL") || o.trim().equals(""))
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

    void insertBirthday(Chat chat, boolean public_man) {
        try {
            String[] res = getDataForInsertBirthday(chat);
            String query = "INSERT INTO PEOPLE VALUES (\n";
            for (int i = 0; i < res.length; i++) {
                query += " " + res[i];
                if (i < res.length - 1)
                    query += ",";
            }
            query += ")";
            try {
                executeUpdate(query);
            } catch (Exception e) {
                Log.error(e.getMessage());
            }
            int id;
            if (res[3] != null)
                id = getPeopleID(res[1].replace("'", ""), res[2].replace("'", ""), res[3].replace("'", ""), res[5].replace("'", ""));
            else id = getPeopleID(res[1].replace("'", ""), res[2].replace("'", ""), res[5].replace("'", ""));
            if (public_man) {
                List<Long> chats = allChats();
                query = "INSERT INTO PUBLIC_PEOPLE VALUES (" + id + " )";
                executeUpdate(query);
                for (Long c : chats) {
                    query = "INSERT INTO VIEW_PEOPLE VALUES (" + c + " , " + id + " )";
                    executeUpdate(query);
                }
            } else {
                query = "INSERT INTO VIEW_PEOPLE VALUES (" + chat.getId() + " , " + id + " )";
                executeUpdate(query);
            }
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
            String query = "select FULL_NAME from PEOPLE where ID =  " + id;
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
        return "Дни рождения";
    }

    @Override
    public String getBotToken() {
        return "456313115:AAF-YfszfkM3HYSDdD1VfzSbMH9a7M1vHYg";
    }

    private void sendPhoto(Long id, InputStream stream, String photoName) {
        SendPhoto s = new SendPhoto();
        s.setChatId(id);
        try {
            s.setNewPhoto(photoName, stream);
            sendPhoto(s);
        } catch (TelegramApiException e) {
            Log.error(e.getMessage());
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
            Log.error(e.getMessage());
            e.printStackTrace();
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
        } catch (Exception e) {
            Log.error(e.getMessage());
        }
    }

    void getTodayBirthdays(Long chat) {
        String query = "select * from VIEW_PEOPLE AS V LEFT JOIN PEOPLE P ON P.ID = V.PEOPLE_ID  where V.CHAT_ID = " + chat + " and extract( month from P.BIRTHDAY) = EXTRACT ( month from current_date)\n" +
                "and extract( day from P.BIRTHDAY) = EXTRACT ( day from current_date) ";
        String firstText = "Сегодня празднуют день рождения:";
        String vosrastText = "";
        String emptyMsg = "В нашем списке отсутствуют люди отмечающие сегодня день рождения.";
        sendInfoAboutPeople(chat, query, firstText, vosrastText, emptyMsg, true, false);
    }

    void getBirthdaysForSchedule(Long chat) {
        String query = "select * from VIEW_PEOPLE AS V LEFT JOIN PEOPLE P ON P.ID = V.PEOPLE_ID  where V.CHAT_ID = " + chat + " and extract( month from P.BIRTHDAY) = EXTRACT ( month from current_date)\n" +
                "and extract( day from P.BIRTHDAY) = EXTRACT ( day from current_date) ";
        String firstText = "Сегодня празднуют день рождения:";
        String vosrastText = "";
        String emptyMsg = null;
        sendInfoAboutPeople(chat, query, firstText, vosrastText, emptyMsg, false, false);
    }

    void getBirthdaysInFewDays(Long chat, int x) {
        String query = "select * from VIEW_PEOPLE AS V LEFT JOIN PEOPLE P ON P.ID = V.PEOPLE_ID  where V.CHAT_ID = " + chat + " and extract( month from P.BIRTHDAY) = EXTRACT ( month from current_date+" + x + ")\n" +
                "and extract( day from P.BIRTHDAY) = EXTRACT ( day from current_date+" + x + ") ";
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

    void sendInfoAboutPeople(Long chat, String query, String firstMsg, String vozrastText, String emptyMsg, boolean useId, boolean showBirthday) {
        try {
            ResultSet rs = getResultSet(query);
            boolean first = true;
            while (rs.next()) {
                if (first) {
                    sendMsg(chat, firstMsg);
                    first = false;
                }
                String id = rs.getString(3);
                String f = rs.getString(4);
                String i = rs.getString(5);
                String o = rs.getString(6);
                String t = rs.getString(7);
                String d = rs.getString(9);
                Date date = rs.getDate(8);
                int v = rs.getInt(10);
                Blob photo = rs.getBlob(12);
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
                    sendPhoto(chat, is, "");
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


    void getBirthdayOfName(Long chat, String name) {
        String query = "select * from VIEW_PEOPLE AS V LEFT JOIN PEOPLE P ON P.ID = V.PEOPLE_ID  where V.CHAT_ID = " + chat + " and UPPER(P.IMYA)='" + name.trim().toUpperCase() + "'";
        String firstText = "Люди с именем " + name + " :";
        String vosrastText = "На данный момент возраст:";
        String emptyMsg = "В нашем списке отсутствуют люди с именем " + name;
        sendInfoAboutPeople(chat, query, firstText, vosrastText, emptyMsg, true, true);
    }
}
