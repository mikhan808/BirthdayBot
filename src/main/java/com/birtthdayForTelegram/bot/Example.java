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
import java.util.Date;
import java.util.Map;
import java.util.Properties;

/**
 * Created by МишаИОля on 15.10.2017.
 */
public class Example extends TelegramLongPollingBot {
    Map<Long,Chat> chatMap;

    @Override
    public void onUpdateReceived(Update update) {
        Message msg = update.getMessage();// Это нам понадобится

            String txt = msg.getText();
            if (txt.equals("Привет"))
                sendMsg(msg, "Привет");
            if (txt.indexOf("#")==0)
                getBirthday(msg,txt.substring(1));
            else if (txt.indexOf("!")==0)
            {
                try
                {
                    int x=Integer.parseInt(txt.substring(1));
                    getBirthday(msg,x);
                }
                catch(Exception e)
                {
                    sendMsg(msg,"после ! должны идти только цифры");
                }
            }
            else if (txt.indexOf("+")==0)
            {
                addNew(msg.getChat());
                sendMsg(msg,"Теперь в 8.00 вам будут приходить сообщения с именниниками");
            }
            else if (txt.indexOf("-")==0)
            {
                deleteID(msg.getChat());
                sendMsg(msg,"Теперь в 8.00 мы вас не будем беспокоить");
            }
            else
            getBirthday(msg);

        //Connection con= DriverManager.getConnection();

    }
    void deleteID(Chat chat)
    {
        try {
            Properties connInfo = new Properties();
            connInfo.put("user", "SYSDBA");
            connInfo.put("password", "masterkey");
            connInfo.put("charSet", "Cp1251");
            Connection con = DriverManager.getConnection("jdbc:firebirdsql://localhost:3050//home/mikhan808/databases/BIRTH (2).FDB", connInfo);
            Statement st = con.createStatement();
            String query ="delete from CHATS where ID =  "+chat.getId();
            st.executeUpdate(query);
            con.commit();
        }
        catch (Exception e)
        {
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
        } catch (TelegramApiException e){
            e.printStackTrace();
        }
    }
    private void sendMsg(Long ChatId, String text) {
        SendMessage s = new SendMessage();
        s.setChatId(ChatId); // Боту может писать не один человек, и поэтому чтобы отправить сообщение, грубо говоря нужно узнать куда его отправлять
        s.setText(text);
        try { //Чтобы не крашнулась программа при вылете Exception
            sendMessage(s);
        } catch (TelegramApiException e){
            e.printStackTrace();
        }
    }
    void addNew(Chat chat)
    {
        try {
            Properties connInfo = new Properties();
            connInfo.put("user", "SYSDBA");
            connInfo.put("password", "masterkey");
            connInfo.put("charSet", "Cp1251");
            Connection con = DriverManager.getConnection("jdbc:firebirdsql://localhost:3050//home/mikhan808/databases/BIRTH (2).FDB", connInfo);
            Statement st = con.createStatement();
            String query ="INSERT INTO CHATS VALUES ( "+chat.getId()+", '"+chat.getFirstName()+" "+chat.getLastName()+"', '"+chat.getUserName()+"' )";
            st.executeUpdate(query);
            con.commit();
        }
        catch (Exception e)
        {
            System.out.println(e.getMessage());
        }
    }
    void getBirthday(Message msg)
    {
        try {
            Properties connInfo=new Properties();
            connInfo.put("user", "SYSDBA");
            connInfo.put("password", "masterkey");
            connInfo.put("charSet", "Cp1251");
            Connection con = DriverManager.getConnection("jdbc:firebirdsql://localhost:3050//home/mikhan808/databases/BIRTH (2).FDB", connInfo);
            Statement st=con.createStatement();
            String query="select * from PEOPLE where extract( month from PEOPLE.BIRTHDAY) = EXTRACT ( month from current_date)\n" +
                    "and extract( day from PEOPLE.BIRTHDAY) = EXTRACT ( day from current_date) ";
            ResultSet rs=st.executeQuery(query);
            boolean first=true;
            while (rs.next())
            {
                if(first)
                {
                    String t="Сегодня празднуют день рождения:";
                    sendMsg(msg,t);
                    first=false;
                }
                String text="";
                String f=rs.getString(2);
                String i=rs.getString(3);
                String o=rs.getString(4);
                String t=rs.getString(5);
                String d=rs.getString(7);
                text=f+" "+i+" "+o;
                if(t!=null)
                {
                    text+=" Телефон:"+t;
                }
                if(d!=null)
                {
                    text+= " "+d;
                }
                sendMsg(msg,text);
            }
            if(first)
            {
                String t="В нашем списке отсутствуют люди отмечающие сегодня день рождения.";
                sendMsg(msg,t);
            }
            rs.close();
            st.close();
            con.close();
        }
        catch (Exception e)
        {
            sendMsg(msg,e.getMessage());
        }
    }
    void getBirthday(Long ChatID)
    {
        try {
            Properties connInfo=new Properties();
            connInfo.put("user", "SYSDBA");
            connInfo.put("password", "masterkey");
            connInfo.put("charSet", "Cp1251");
            Connection con = DriverManager.getConnection("jdbc:firebirdsql://localhost:3050//home/mikhan808/databases/BIRTH (2).FDB", connInfo);
            Statement st=con.createStatement();
            String query="select * from PEOPLE where extract( month from PEOPLE.BIRTHDAY) = EXTRACT ( month from current_date)\n" +
                "and extract( day from PEOPLE.BIRTHDAY) = EXTRACT ( day from current_date) ";
            ResultSet rs=st.executeQuery(query);
            boolean first=true;
            while (rs.next())
            {
                if(first)
                {
                    String t="Сегодня празднуют день рождения:";
                    sendMsg(ChatID,t);
                    first=false;
                }
                String text="";
                String f=rs.getString(2);
                String i=rs.getString(3);
                String o=rs.getString(4);
                String t=rs.getString(5);
                String d=rs.getString(7);
                text=f+" "+i+" "+o;
                if(t!=null)
                {
                    text+=" Телефон:"+t;
                }
                if(d!=null)
                {
                    text+= " "+d;
                }
                sendMsg(ChatID,text);
            }
            if(first)
            {
                String t="В нашем списке отсутствуют люди отмечающие сегодня день рождения.";
                //sendMsg(msg,t);
            }
            rs.close();
            st.close();
            con.close();
        }
        catch (Exception e)
        {
            sendMsg(ChatID,e.getMessage());
        }
    }
    void getBirthday(Message msg,int x)
    {
        try {
            Properties connInfo=new Properties();
            connInfo.put("user", "SYSDBA");
            connInfo.put("password", "masterkey");
            connInfo.put("charSet", "Cp1251");
            Connection con = DriverManager.getConnection("jdbc:firebirdsql://localhost:3050//home/mikhan808/databases/BIRTH (2).FDB", connInfo);
            Statement st=con.createStatement();
            String query="select * from PEOPLE where extract( month from PEOPLE.BIRTHDAY) = EXTRACT ( month from current_date+"+x+")\n" +
                "and extract( day from PEOPLE.BIRTHDAY) = EXTRACT ( day from current_date+"+x+") ";
            ResultSet rs=st.executeQuery(query);
            boolean first=true;
            while (rs.next())
            {
                if(first)
                {
                    String t="через "+x+" "+getDayFormated(x)+" празднуют день рождения:";
                    sendMsg(msg,t);
                    first=false;
                }
                String text="";
                String f=rs.getString(2);
                String i=rs.getString(3);
                String o=rs.getString(4);
                String t=rs.getString(5);
                String d=rs.getString(7);
                text=f+" "+i+" "+o;
                if(t!=null)
                {
                    text+=" Телефон:"+t;
                }
                if(d!=null)
                {
                    text+= " "+d;
                }
                sendMsg(msg,text);
            }
            if(first)
            {
                String t="В нашем списке отсутствуют люди отмечающие день рождения через "+x+" "+getDayFormated(x)+".";
                sendMsg(msg,t);
            }
            rs.close();
            st.close();
            con.close();
        }
        catch (Exception e)
        {
            sendMsg(msg,e.getMessage());
        }
    }
    String getDayFormated(int x)
    {
        int y;
        if (x%100>20)
        y=x%10;
        else y=x%100;
        switch (y)
        {
            case 1:return "день";
            case 2:
            case 3:
            case 4:return "дня";
            default:return "дней";
        }
    }
    void getBirthday(Message msg,String name)
    {
        try {
            Properties connInfo=new Properties();
            connInfo.put("user", "SYSDBA");
            connInfo.put("password", "masterkey");
            connInfo.put("charSet", "Cp1251");
            Connection con = DriverManager.getConnection("jdbc:firebirdsql://localhost:3050//home/mikhan808/databases/BIRTH (2).FDB", connInfo);
            Statement st=con.createStatement();
            String query="select * from PEOPLE where IMYA='"+name+"'";
            ResultSet rs=st.executeQuery(query);
            boolean first=true;
            while (rs.next())
            {
                if(first)
                {
                    String t="Люди с именем "+name+" :";
                    sendMsg(msg,t);
                    first=false;
                }
                String text="";
                String f=rs.getString(2);
                String i=rs.getString(3);
                String o=rs.getString(4);
                String t=rs.getString(5);
                Date d = rs.getDate(6);
                text=f+" "+i+" "+o;
                if (d!=null)
                text+=" "+d;
                if(t!=null)
                {
                    text+=" Телефон:"+t;
                }
                sendMsg(msg,text);
            }
            if(first)
            {
                String t="В нашем списке отсутствуют люди с именем "+name;
                sendMsg(msg,t);
            }
            rs.close();
            st.close();
            con.close();
        }
        catch (Exception e)
        {
            sendMsg(msg,e.getMessage());
        }
    }
}
