package com.problemeszero.mail;

import com.problemeszero.redwax.Main;
import com.problemeszero.redwax.RedWaxMessage;

import java.io.*;
import java.util.*;
import javax.mail.*;
import javax.mail.internet.MimeBodyPart;

//
// inspired by :
// http://www.mikedesjardins.net/content/2008/03/using-javamail-to-read-and-extract/
//

public class RedWaxReceiveMail {
    private  String   UN, PW, host, port, proto, folderImap;
    private int num_missatges;
    private Session session;
   // private Properties prop = new Properties();

    public RedWaxReceiveMail() {
        //carregam el fitxer properties
        //llegim fitxer de propietats configuration.xml

        System.err.println("Llegint el fitxer de propietats configuration.xml per a imap");
        host = Main.appProps.getProperty("imap.servidor");
        port = Main.appProps.getProperty("imap.port");
        proto = Main.appProps.getProperty("imap.protocol");
        UN = Main.appProps.getProperty("imap.usuari");
        PW = Main.appProps.getProperty("imap.contrasenya");
        folderImap = Main.appProps.getProperty("imap.folder");
        num_missatges = Integer.parseInt(Main.appProps.getProperty("imap.length"));
        session = Session.getDefaultInstance(Main.appProps, null);
    }

    public String auth() throws IOException {
        boolean auth = chk();
        String s="";

        if(!auth) {
            s="ALERTA: "+ proto + " KO. Connexió no satifactòria. Revisau la configuració " + proto;
        } else {
            s= proto + " OK. Credencials correctes";
        }
        System.out.println(s + " - " + UN );
        return s;
    }

    private  boolean chk() {
        try {
            session = Session.getInstance(Main.appProps, new javax.mail.Authenticator() {
                protected PasswordAuthentication getPasswordAuthentication() {return new PasswordAuthentication(UN, PW);}
            });
            Store store = session.getStore(proto);
            store.connect(host, UN, PW);
            System.out.println("Connexio IMAP/POP3 correcte");
            return true;
        } catch (AuthenticationFailedException e) {
            System.out.println("AuthenticationFailedException - for authentication failures");
            return false;
        } catch (MessagingException e) {
            System.out.println("IMAP/POP3 connection error - for other failures");
            return false;
        }
    }

    public void doit(List<RedWaxMessage> rwmList, String contentId) throws MessagingException, IOException {

        Folder folder = null;
        Store store = null;
        try {
       //     Properties props = System.getProperties();
            Main.appProps.setProperty("mail.store.protocol", proto );

            session = Session.getDefaultInstance(Main.appProps, null);
            // session.setDebug(true);
            store = session.getStore(proto);
            store.connect(host,UN, PW);
            folder = store.getFolder(folderImap);
            /* Others GMail folders :
             * [Gmail]/All Mail   This folder contains all of your Gmail messages.
             * [Gmail]/Drafts     Your drafts.
             * [Gmail]/Sent Mail  Messages you sent to other people.
             * [Gmail]/Spam       Messages marked as spam.
             * [Gmail]/Starred    Starred messages.
             * [Gmail]/Trash      Messages deleted from Gmail.
             */
            folder.open(Folder.READ_WRITE);
            Message messages[] = folder.getMessages();
            System.out.println("No of Messages : " + folder.getMessageCount());
            System.out.println("No of Unread Messages : " + folder.getUnreadMessageCount());

            //Per guanyar rapidesa nomes recuperam els darrers 5 missatges de la bustia.
            //En el cas de haver-hi menys de 5 missatges el agafam tots
            int total_missatges = ((messages.length > num_missatges) ? num_missatges : messages.length);

            for (int i= messages.length - total_missatges; i < messages.length; ++i) {
//                System.out.println("valor de i = " + i);
                Message msg = messages[i];
                if (msg.getHeader("Content-ID")!=null)
                if (contentId.equals(msg.getHeader("Content-ID")[0])) {

                    System.out.println("MESSAGE #" + (i + 1) + ":");
                    RedWaxMessage rwm = new RedWaxMessage();

                    String from = "unknown";
                    if (msg.getReplyTo().length >= 1) {
                        from = msg.getReplyTo()[0].toString();
                        rwm.setFrom(msg.getReplyTo()[0].toString());
                    } else if (msg.getFrom().length >= 1) {
                        from = msg.getFrom()[0].toString();
                        rwm.setFrom(msg.getFrom()[0].toString());
                    }
                    String subject = msg.getSubject();
                    rwm.setSubject(msg.getSubject());
                    rwm.setSentDate(msg.getSentDate());
                    System.out.println("Saving ... " + subject + " " + from);
                    // you may want to replace the spaces with "_"
                    // the TEMP directory is used to store the files
                    String filename =  subject;
//                    saveParts(msg.getContent(), rwm);

                    if (msg.getContent() instanceof Multipart) {
                        Multipart multi = ((Multipart) msg.getContent());

                        //Guardar multipart a rwm
//                       System.err.println(new String(java.util.Arrays.toString(mPartBaos.toByteArray())));
//                       Smime.byteToFile(mPartBaos.toByteArray(), "Guardar multipart", new File(Main.appProps.getProperty("Fitxers")));

//                       System.err.println("Content-Type=" + multi.getContentType());
                         if (multi.getContentType().toLowerCase().contains("multipart/signed;")){
                             rwm.setMailSignedMultiPart(PartToBAOS(multi)); //Guardam el Multipart de tot el correu (CEM + Signatura)
                         }
//                       Smime.byteToFile(mPartBaos.toByteArray(), "Guardar cemSignat",new File(Main.appProps.getProperty("Fitxers")));
//                       System.err.println(java.util.Arrays.toString(mPartBaos.toByteArray()));
                     }
//                    System.err.println("####################################################################################################################");
//                    System.err.println("####################################################################################################################");
                    msg.setFlag(Flags.Flag.SEEN, true);
                    rwmList.add(rwm);
                    // to delete the message
                    // msg.setFlag(Flags.Flag.DELETED, true);
                }
            }
        }
        finally {
            if (folder != null) { folder.close(true); }
            if (store != null) { store.close(); }
        }
    }

    //Escriu tot el "part" al byteArray. Incloses les capsaleres.
    public byte [] PartToBAOS(Object p) throws MessagingException,IOException {

        ByteArrayOutputStream bodyPartBaos = new ByteArrayOutputStream();
        byte [] bPB;

        if (p instanceof Multipart) {
            Multipart part = (Multipart) p;
            part.writeTo(bodyPartBaos);
        } else {
            MimeBodyPart part = (MimeBodyPart) p;
            part.writeTo(bodyPartBaos);
        }
        bPB = bodyPartBaos.toByteArray();
        bodyPartBaos.close();

        return bPB;

    }

    public static void main(String args[]) throws Exception {
        RedWaxReceiveMail rebre = new RedWaxReceiveMail();
        rebre.doit(null, "redWax");
    }
}
