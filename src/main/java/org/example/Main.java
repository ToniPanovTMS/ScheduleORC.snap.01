package org.example;
import javafx.beans.property.adapter.JavaBeanBooleanPropertyBuilder;

import java.sql.*;
import java.io.*;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

public class Main {
    static String url = "jdbc:mysql://localhost:3306/data?serverTimezone=Europe/Moscow&useSSL=false";
    static String user = "asitus";
    static String password = "3Hl8mg7i";
    static Calendar cal = Calendar.getInstance();
    static int WEEK_OF_YEAR = cal.get(Calendar.WEEK_OF_YEAR);
    static String separator = File.separator;
    static HashMap<String,Integer> phoneNambers=new HashMap< String,Integer>();
    static String text_for_log_file;
    static int indexWeek=0;
    static String[] Name_file = new String[]{"ch-queue-mon-until1500.sh",
            "ch-queue-mon-after1500.sh",
            "ch-queue-tue-until1500.sh",
            "ch-queue-tue-after1500.sh",
            "ch-queue-wed-until1500.sh",
            "ch-queue-wed-after1500.sh",
            "ch-queue-thu-until1500.sh",
            "ch-queue-thu-after1500.sh",
            "ch-queue-fri-until1500.sh",
            "ch-queue-fri-after1500.sh",
            "ch-queue-sat-until1500.sh"};

    public static void main(String[] args) throws IOException, SQLException {
        inputPhoneNambers();
        LocalDate date =LocalDate.now();
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        if(dayOfWeek.name().equals("SUNDAY")){
            indexWeek=1;
        }
        for(String name:Name_file){
            start(name);
        }
        generate_log_file();
        System.out.println("Файлы созданы");
    }
    static void start(String name_file){
        switch (name_file) {
            case "ch-queue-mon-until1500.sh": {
                outputInfoFromDB(name_file,"U","Понидельник","U","Суббота",-1);
                break;
            }
            case "ch-queue-mon-after1500.sh": {
                outputInfoFromDB(name_file,"A","Понидельник","U","Понидельник",0);
                break;
            }
            case "ch-queue-tue-until1500.sh": {
                outputInfoFromDB(name_file,"U","Вторник","A","Понидельник",0);
                break;
            }
            case "ch-queue-tue-after1500.sh": {
                outputInfoFromDB(name_file,"A","Вторник","U","Вторник",0);
                break;
            }
            case "ch-queue-wed-until1500.sh": {
                outputInfoFromDB(name_file,"U","Среда","A","Вторник",0);
                break;
            }
            case "ch-queue-wed-after1500.sh": {
                outputInfoFromDB(name_file,"A","Среда","U","Среда",0);
                break;
            }
            case "ch-queue-thu-until1500.sh": {
                outputInfoFromDB(name_file,"U","Четверг","A","Среда",0);
                break;
            }
            case "ch-queue-thu-after1500.sh": {
                outputInfoFromDB(name_file,"A","Четверг","U","Четверг",0);
                break;
            }
            case "ch-queue-fri-until1500.sh": {
                outputInfoFromDB(name_file,"U","Пятница","A","Четверг",0);
                break;
            }
            case "ch-queue-fri-after1500.sh": {
                outputInfoFromDB(name_file,"A","Пятница","U","Пятница",0);
                break;
            }
            case "ch-queue-sat-until1500.sh": {
                outputInfoFromDB(name_file,"U","Суббота","A","Пятница",0);
                break;
            }
        }
    }
    static void outputInfoFromDB (String name_file,String until_afterInstal,String dayInstal,String until_afterDelete,String dayDelete,int indexSat) {
        List<String> delete_list = new ArrayList<String>();
        List<String> instal_list = new ArrayList<String>();
        text_for_log_file=text_for_log_file+name_file+"\n";
        try (Connection conn = DriverManager.getConnection(url, user, password)) {
            Statement stmt = conn.createStatement();
            ResultSet rsInstal = stmt.executeQuery("SELECT * FROM schedule Where week_of_the_year='" + ((WEEK_OF_YEAR + indexWeek +0)+ " " + cal.get(Calendar.YEAR)) + "' and until_after LIKE '" + until_afterInstal + "%' and day='" + dayInstal + "';");
            while (rsInstal.next()) {
                instal_list.add(String.valueOf(phoneNambers.get(rsInstal.getString("name"))));
                text_for_log_file=text_for_log_file +rsInstal.getString("name")+" "+phoneNambers.get(rsInstal.getString("name"))+"\n";
            }
            ResultSet rsDelete = stmt.executeQuery("SELECT * FROM schedule Where week_of_the_year='" + ((WEEK_OF_YEAR + indexWeek +indexSat)+ " " + cal.get(Calendar.YEAR)) + "' and until_after LIKE '" + until_afterDelete + "%' and day='" + dayDelete + "';");
            while (rsDelete.next()) {
                delete_list.add(String.valueOf(phoneNambers.get(rsDelete.getString("name"))));
            }
        } catch (SQLException e) {
            System.out.println("Error: " + e.getMessage());
        }
        generate_file(delete_list,instal_list,name_file);
    }
    static void inputPhoneNambers(){
        try (Connection conn = DriverManager.getConnection(url, user, password)) {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM phons_namber");
            while (rs.next()) {
                phoneNambers.put(rs.getString("name"),rs.getInt("namber_phone"));
            }
        } catch (SQLException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
    static void generate_file(List<String> delete_list,List<String> install_list,String name_file){
        String text ="#!/bin/bash"+"\n";
        for (String line : delete_list) {
            text=text+"/bin/mysql -u root -pVyiu28giOCd9 <<EOF\n" +
                    "USE asterisk\n" +
                    "DELETE FROM queues_details WHERE data = 'Local/"+line+"@from-queue/n,0' AND id = '1729'\n" +
                    "EOF"+"\n";
        }
        for (String line : install_list) {
            text=text+"/bin/mysql -u root -pVyiu28giOCd9 <<EOF\n" +
                    "USE asterisk\n" +
                    "INSERT INTO queues_details (id, keyword, data, flags) VALUES ('1729', 'member', 'Local/"+line+"@from-queue/n,0', '0')\n" +
                    "EOF"+"\n";
        }
        text+="/var/lib/asterisk/bin/amportal a r";
        try(FileWriter writer = new FileWriter("data"+separator+"script"+separator+name_file, false))
        {
            writer.write(text);
        }
        catch(IOException ex) {
            System.out.println(ex.getMessage());
        }
    }
    static void generate_log_file(){
        try(FileWriter writer = new FileWriter("data"+separator+"log"+separator+"log.txt", false))
        {
            writer.write(text_for_log_file);
        }
        catch(IOException ex){
            System.out.println(ex.getMessage());
        }
    }
}



