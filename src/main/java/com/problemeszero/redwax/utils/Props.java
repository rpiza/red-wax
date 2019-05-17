package com.problemeszero.redwax.utils;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

public class Props {

    public static void main(String[] s) {

//        /////////////////////////saving properties into example.properties file/////////////////////////
//        try (OutputStream out = new FileOutputStream("example.properties")) {
//            Properties properties = new Properties();
//            properties.setProperty("name", "javaCodeGeeks");
//            properties.setProperty("article", "JavaProperties");
//            properties.setProperty("version", "1.0");
//            properties.setProperty("ide", "eclipse");
//            properties.store(out, "This is a sample for java properties");
//
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        //////////////////////////////////////////////////////////////////////////////////////////////////
//
//        ///////////////////////////Reading properties////////////////////////////////////////////////////
//        try (InputStream in = new FileInputStream("example.properties")) {
//            Properties prop = new Properties();
//            prop.load(in);
//            System.out.println("####Properties.getProperty usage####");
//            System.out.println(prop.getProperty("name"));
//            System.out.println();
//
//            System.out.println("####Properties.stringPropertyNames usage####");
//            for (String property : prop.stringPropertyNames()) {
//                String value = prop.getProperty(property);
//                System.out.println(property + "=" + value);
//            }
//        } catch (IOException e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//        }
//        System.out.println();

        //////////////////////////////////////////////////////////////////////////////////////////////////

        //////////////////////writing and reading fromxml///////////////////////////////////////////////
//        try (OutputStream out = new FileOutputStream("configuration.xml")) {
//            Properties properties = new Properties();
//
//            properties.setProperty("imap.servidor", "ovh.net");
//            properties.setProperty("imap.port", "465");
//            properties.setProperty("imap.protocol", "smtp");
//            properties.setProperty("imap.usuari", "ramon@problemeszero.com");
//            properties.setProperty("imap.contrasenya", "xxxxxxx");
//            properties.storeToXML(out,
//                    "Fitxer de configuracio del servidor de correu");
//
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
        /////////////////////////////////////////////////////////////////////////////////////////////////

        ///////////////////////////Reading properties from xml////////////////////////////////////////////////////
        try (InputStream in = new FileInputStream("configuration.xml")) {
            Properties prop = new Properties();
       //     prop = System.getProperties();
            prop.loadFromXML(in);

            System.out.println("####Properties.load from xml usage####");
            for (String property : prop.stringPropertyNames()) {
                String value = prop.getProperty(property);
                System.out.println(property + "=" + value);
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        System.out.println();

        /////////////////////////////////////////////////////////////////////////////////////////////////

    }
}

