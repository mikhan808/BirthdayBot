package com.birtthdayForTelegram.bot;

import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.objects.Chat;
import org.telegram.telegrambots.api.objects.Message;
import org.telegram.telegrambots.api.objects.Update;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.exceptions.TelegramApiException;

import java.sql.*;
import java.util.*;
import java.util.Date;

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
              getBirthdayOfName(msg, txt.substring(1));
            else if (txt.indexOf("!") == 0) {
              try {
                int x = Integer.parseInt(txt.substring(1));
                getBirthday(msg, x);
              } catch (Exception e) {
                sendMsg(msg, "после ! должны идти только цифры");
              }
            } else if (txt.indexOf("+") == 0) {
              updateStatus(chat, Status.TIME_TO_SEND);
              sendMsg(msg, "Введите время в которое вы хотите получать оповещения в формате ЧЧ:ММ (например,08:00)");
            } else if (txt.indexOf("-") == 0) {
              deleteID(msg.getChat());
              sendMsg(msg, "Теперь мы вас не будем беспокоить нашими оповещениями");
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
            } else if (txt.equals("/delete")) {
              updateStatus(chat, Status.DELETE);
              sendMsg(id, "Введите № человека, о дне рождения, которого вы больше не хотите получать уведомления");
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
            sendMsg(msg, "Теперь в " + txt + " вам будут приходить сообщения с именниниками");
            updateStatus(chat, Status.NORMAL);
            break;
          case Status.DELETE:
            try {
              int x = Integer.parseInt(txt.trim());
              deletePeople(chat, x);
              updateStatus(chat, Status.NORMAL);
            } catch (Exception e) {
              sendMsg(msg, "В сообщении должны содержаться только цифры, если вы передумали отправьте /cancel");
            }
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

  List<Long> allChats() {
    List<Long> idChats = new ArrayList<>();
    try {
      Connection con = getConnection();
      Statement st = con.createStatement();
      String query = "select * from CHAT_INFO";
      ResultSet rs = st.executeQuery(query);
      while (rs.next()) {
        idChats.add(rs.getLong(1));
      }
      rs.close();
      st.close();
      con.close();
    } catch (Exception e) {
      System.out.println(e.getMessage());
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
      Connection con = getConnection();
      Statement st = con.createStatement();
      String query = "select * from PEOPLE where FULL_NAME = '" + full_name + "' and BIRTHDAY = '" + d + "'";
      ResultSet rs = st.executeQuery(query);
      if (rs.next()) {
        res = rs.getInt(1);
      }
      rs.close();
      st.close();
      con.close();
    } catch (Exception e) {
      System.out.println(e.getMessage());
    }
    return res;
  }

  void insertBirthday(Chat chat, boolean public_man) {
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
      try {
        st.executeUpdate(query);
      } catch (Exception e) {
        System.out.println(e.getMessage());
      }
      int id = getPeopleID(res[1].replace("'", ""), res[2].replace("'", ""), res[3].replace("'", ""), res[5].replace("'", ""));
      if (public_man) {
        List<Long> chats = allChats();
        query = "INSERT INTO PUBLIC_PEOPLE VALUES (" + id + " )";
        st.executeUpdate(query);
        for (Long c : chats) {
          query = "INSERT INTO VIEW_PEOPLE VALUES (" + c + " , " + id + " )";
          st.executeUpdate(query);
        }
      } else {
        query = "INSERT INTO VIEW_PEOPLE VALUES (" + chat.getId() + " , " + id + " )";
        st.executeUpdate(query);
      }
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
      //con.commit();
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
      //con.commit();
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
      //con.commit();
      st.close();
      con.close();
    } catch (Exception e) {
      System.out.println(e.getMessage());
    }
  }

  String getFullNamePeople(int id) {
    String res = "Никакой Никак Никакович";
    try {
      Connection con = getConnection();
      Statement st = con.createStatement();
      String query = "select FULL_NAME from PEOPLE where ID =  " + id;
      ResultSet rs = st.executeQuery(query);
      if (rs.next())
        res = rs.getString(1);
      st.close();
      con.close();
    } catch (Exception e) {
      System.out.println(e.getMessage());
    }
    return res;
  }

  void deletePeople(Chat chat, int id) {
    try {
      Connection con = getConnection();
      Statement st = con.createStatement();
      String query = "delete from VIEW_PEOPLE where CHAT_ID =  " + chat.getId() + " AND PEOPLE_ID = " + id;
      st.executeUpdate(query);
      st.close();
      con.close();
    } catch (Exception e) {
      System.out.println(e.getMessage());
    }
    sendMsg(chat.getId(), "Мы вам больше не будем присылать сообщения о человеке, которого зовут:\n" + getFullNamePeople(id));
  }

  void deleteID(Chat chat) {
    try {
      Connection con = getConnection();
      Statement st = con.createStatement();
      String query = "delete from CHATS where ID =  " + chat.getId();
      st.executeUpdate(query);
      //con.commit();
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

  void addNew(Message msg) {
    try {
      Chat chat = msg.getChat();
      Connection con = getConnection();
      Statement st = con.createStatement();
      String query = "SELECT * FROM CHATS WHERE ID = " + chat.getId();
      ResultSet rs = st.executeQuery(query);
      if (rs.next()) {
        st.close();
        st = con.createStatement();
        query = "update chats set time_sending='" + msg.getText() + ":00' where id = " + chat.getId();
        st.executeUpdate(query);
        st.close();
        con.close();
      } else {
        st.close();
        st = con.createStatement();
        query = "INSERT INTO CHATS (ID,FULL_NAME,NIKNAME,TIME_SENDING) VALUES ( " + chat.getId() + ", '" + chat.getFirstName() + " " + chat.getLastName() + "', '" + chat.getUserName() + "', '" + msg.getText() + "' )";
        st.executeUpdate(query);
        //con.commit();
        st.close();
        con.close();
      }
    } catch (Exception e) {
      System.out.println(e.getMessage());
    }
  }

  void addNewPublic(Long chat) {
    try {
      Connection con = getConnection();
      Statement st = con.createStatement();
      Connection con1 = getConnection();
      Statement st1 = con1.createStatement();
      ResultSet rs = st.executeQuery("SELECT * FROM PUBLIC_PEOPLE");
      while (rs.next()) {
        st1.executeUpdate("INSERT INTO VIEW_PEOPLE VALUES (" + chat + " , " + rs.getInt(1) + " )");
      }
    } catch (SQLException e) {
      e.printStackTrace();
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
      addNewPublic(chat.getId());
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
      String query = "select * from VIEW_PEOPLE AS V LEFT JOIN PEOPLE P ON P.ID = V.PEOPLE_ID  where V.CHAT_ID = " + msg.getChatId() + " and extract( month from P.BIRTHDAY) = EXTRACT ( month from current_date)\n" +
          "and extract( day from P.BIRTHDAY) = EXTRACT ( day from current_date) ";
      ResultSet rs = st.executeQuery(query);
      boolean first = true;
      while (rs.next()) {
        if (first) {
          String t = "Сегодня празднуют день рождения:";
          sendMsg(msg, t);
          first = false;
        }
        String text = "";
        String id = rs.getString(3);
        String f = rs.getString(4);
        String i = rs.getString(5);
        String o = rs.getString(6);
        String t = rs.getString(7);
        String d = rs.getString(9);
        int v = rs.getInt(10);
        text = f + " " + i;
        if (o != null)
          text += " " + o;
        text += "\n" + v + " " + getYearFormated(v);
        if (t != null) {
          text += "\nТелефон:" + t;
        }
        if (d != null) {
          text += "\n" + d;
        }
        text += "\n№" + id;
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
      String query = "select * from VIEW_PEOPLE AS V LEFT JOIN PEOPLE P ON P.ID = V.PEOPLE_ID  where V.CHAT_ID = " + ChatID + " and where extract( month from P.BIRTHDAY) = EXTRACT ( month from current_date)\n" +
          "and extract( day from P.BIRTHDAY) = EXTRACT ( day from current_date) ";
      ResultSet rs = st.executeQuery(query);
      boolean first = true;
      while (rs.next()) {
        if (first) {
          String t = "Сегодня празднуют день рождения:";
          sendMsg(ChatID, t);
          first = false;
        }
        String text = "";
        String id = rs.getString(3);
        String f = rs.getString(4);
        String i = rs.getString(5);
        String o = rs.getString(6);
        String t = rs.getString(7);
        String d = rs.getString(9);
        int v = rs.getInt(10);
        text = f + " " + i;
        if (o != null)
          text += " " + o;
        text += "\n" + v + " " + getYearFormated(v);
        if (t != null) {
          text += "\nТелефон:" + t;
        }
        if (d != null) {
          text += "\n" + d;
        }
        text += "\n№" + id;
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
      String query = "select * from VIEW_PEOPLE AS V LEFT JOIN PEOPLE P ON P.ID = V.PEOPLE_ID  where V.CHAT_ID = " + msg.getChatId() + " and extract( month from P.BIRTHDAY) = EXTRACT ( month from current_date+" + x + ")\n" +
          "and extract( day from P.BIRTHDAY) = EXTRACT ( day from current_date+" + x + ") ";
      ResultSet rs = st.executeQuery(query);
      boolean first = true;
      while (rs.next()) {
        if (first) {
          String t = "через " + x + " " + getDayFormated(x) + " празднуют день рождения:";
          sendMsg(msg, t);
          first = false;
        }
        String text = "";
        String id = rs.getString(3);
        String f = rs.getString(4);
        String i = rs.getString(5);
        String o = rs.getString(6);
        String t = rs.getString(7);
        String d = rs.getString(9);
        int v = rs.getInt(10);
        text = f + " " + i;
        if (o != null)
          text += " " + o;
        text += "\nНа данный момент возраст:" + v + " " + getYearFormated(v);
        if (t != null) {
          text += "\nТелефон:" + t;
        }
        if (d != null) {
          text += "\n" + d;
        }
        text += "\n№" + id;
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


  void getBirthdayOfName(Message msg, String name) {
    try {
      Properties connInfo = new Properties();
      connInfo.put("user", "SYSDBA");
      connInfo.put("password", "masterkey");
      connInfo.put("charSet", "Cp1251");
      Connection con = DriverManager.getConnection("jdbc:firebirdsql://localhost:3050//home/mikhan808/databases/BIRTH (2).FDB", connInfo);
      Statement st = con.createStatement();
      String query = "select * from VIEW_PEOPLE AS V LEFT JOIN PEOPLE P ON P.ID = V.PEOPLE_ID  where V.CHAT_ID = " + msg.getChatId() + " and UPPER(P.IMYA)='" + name.trim().toUpperCase() + "'";
      ResultSet rs = st.executeQuery(query);
      boolean first = true;
      while (rs.next()) {
        if (first) {
          String t = "Люди с именем " + name + " :";
          sendMsg(msg, t);
          first = false;
        }
        String text = "";
        String id = rs.getString(3);
        String f = rs.getString(4);
        String i = rs.getString(5);
        String o = rs.getString(6);
        String t = rs.getString(7);
        Date d = rs.getDate(8);
        text = f + " " + i;
        if (o != null)
          text += " " + o;
        if (d != null)
          text += " " + d;
        if (t != null) {
          text += " Телефон:" + t;
        }
        text += "\n№" + id;
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
