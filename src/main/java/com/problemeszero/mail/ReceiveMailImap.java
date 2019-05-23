package com.problemeszero.mail;

import com.problemeszero.crypto.Smime;
import com.problemeszero.redwax.Main;
import com.problemeszero.redwax.RedWaxMessage;
import javafx.beans.binding.StringExpression;

import java.io.*;
import java.util.*;
import javax.mail.*;
import javax.mail.internet.*;

public class ReceiveMailImap {
    private  String   UN, PW, host, port, proto, folderImap;
   // private Properties prop = new Properties();

    public ReceiveMailImap() {
        //carregam el fitxer properties
        //llegim fitxer de propietats configuration.xml

        System.err.println("Llegint el fitxer de propietats configuration.xml per a imap");
        host = Main.appProps.getProperty("imap.servidor");
        port = Main.appProps.getProperty("imap.port");
        proto = Main.appProps.getProperty("imap.protocol");
        UN = Main.appProps.getProperty("imap.usuari");
        PW = Main.appProps.getProperty("imap.contrasenya");
        folderImap = Main.appProps.getProperty("imap.folder");
    }

    //
    // inspired by :
    // http://www.mikedesjardins.net/content/2008/03/using-javamail-to-read-and-extract/
    //

    public void doit(List<RedWaxMessage> rwmList, String contentId) throws MessagingException, IOException {

        Folder folder = null;
        Store store = null;
        try {
       //     Properties props = System.getProperties();
            Main.appProps.setProperty("mail.store.protocol", proto );

            Session session = Session.getDefaultInstance(Main.appProps, null);
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
            for (int i= messages.length-5; i < messages.length; ++i) {
                Message msg = messages[i];
                if (msg.getHeader("Content-ID")!=null)
                if (contentId.equals(msg.getHeader("Content-ID")[0])) {
                    System.out.println("MESSAGE #" + (i + 1) + ":");
                    RedWaxMessage rwm = new RedWaxMessage();
                    /*
                      if we don''t want to fetch messages already processed
                      if (!msg.isSet(Flags.Flag.SEEN)) {
                         String from = "unknown";
                         ...
                      }
                    */
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
                    saveParts(msg.getContent(), rwm);
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

    public void saveParts(Object content, String filename)
            throws IOException, MessagingException
    {
        OutputStream out = null;
        InputStream in = null;
        try {
            if (content instanceof Multipart) {
                Multipart multi = ((Multipart)content);
                int parts = multi.getCount();
                for (int j=0; j < parts; ++j) {
                    MimeBodyPart part = (MimeBodyPart)multi.getBodyPart(j);
                    if (part.getContent() instanceof Multipart) {
                        // part-within-a-part, do some recursion...
                        saveParts(part.getContent(), filename);
                    }
                    else {
                        String extension = "";
                        if (part.isMimeType("text/html")) {
                            extension = "html";
                        }
                        else {
                            if (part.isMimeType("text/plain")) {
                                extension = "txt";
                            }
                            else {
                                //  Try to get the name of the attachment
                                extension = part.getDataHandler().getName();
                            }
                            filename = filename + "." + extension;
                            System.out.println("... " + filename);
                            out = new FileOutputStream(new File(filename));
                            in = part.getInputStream();
                            int k;
                            while ((k = in.read()) != -1) {
                                out.write(k);
                            }
                        }
                    }
                }
            }
        }
        finally {
            if (in != null) { in.close(); }
            if (out != null) { out.flush(); out.close(); }
        }
    }

    public void saveParts(Object content, RedWaxMessage rwm) throws IOException, MessagingException {

        ByteArrayOutputStream out = null;
        InputStream in = null;
        try {
            if (content instanceof Multipart) {
                Multipart multi = ((Multipart)content);

                //Guardar multipart a rwm
                ByteArrayOutputStream mPartBaos = new ByteArrayOutputStream();
                multi.writeTo(mPartBaos);
                if (multi.getContentType().contains("multipart/signed;")){
                    rwm.setMailSignedMultiPart(mPartBaos.toByteArray()); //Guardam el Multipart de tot el correu (CEM + Signatura)
                    //Smime.byteToFile(mPartBaos.toByteArray(), "Guardar cemSignat");
                    //System.err.println(java.util.Arrays.toString(mPartBaos.toByteArray()));
                }
                if (multi.getContentType().contains("multipart/mixed;")){
                    rwm.setCem(mPartBaos.toByteArray());
                    //Smime.byteToFile(mPartBaos.toByteArray(), "Guardar cem imaps");
//                    System.err.println(new String(java.util.Arrays.toString(mPartBaos.toByteArray())));
//                    System.err.println(new String(java.util.Arrays.toString(rwm.getCem())));
                }
                mPartBaos.close();

                int parts = multi.getCount();
                for (int j=0; j < parts; ++j) {
                    MimeBodyPart part = (MimeBodyPart)multi.getBodyPart(j);
                    if (part.getContent() instanceof Multipart) {
                        // part-within-a-part, do some recursion...
                        saveParts(part.getContent(), rwm);
                    }
                    else {
                        if (part.getContentID() != null) {
                            if ("fitxerXifrat".equals(part.getContentID())){
                                System.err.println(j+"-" + part.getContentID() +" " + part.getContentType());
                                out = new ByteArrayOutputStream();
                                in = part.getInputStream();
                                int k;
                                while ((k = in.read()) != -1) {
                                    out.write(k);
                                }
                                System.err.println(new String(Base64.getEncoder().encode(out.toByteArray())));
                                byte [][] cF = {Arrays.copyOfRange(out.toByteArray(),0,16) ,Arrays.copyOfRange(out.toByteArray(),16, out.toByteArray().length) };
                                System.err.println(new String(Base64.getEncoder().encode(cF[0])));
                                System.err.println(new String(Base64.getEncoder().encode(cF[1])));
                                rwm.setCertFile(cF);
                            }

                            if ("deadTime".equals(part.getContentID())){
                                System.err.println(j+"-" + part.getContentID() +" " + part.getContentType());
                                out = new ByteArrayOutputStream();
                                in = part.getInputStream();
                                int k;
                                while ((k = in.read()) != -1) {
                                    out.write(k);
                                }
                               System.err.println(new String(out.toByteArray()));
                               rwm.setDeadTimeMillis(Long.valueOf(new String(out.toByteArray())));
                            }

                            if ("kPrima".equals(part.getContentID())){
                                System.err.println(j+"-" + part.getContentID() +" " + part.getContentType());
                                out = new ByteArrayOutputStream();
                                in = part.getInputStream();
                                int k;
                                while ((k = in.read()) != -1) {
                                    out.write(k);
                                }
                                System.err.println(new String(out.toByteArray()));
                                rwm.setkPrima(out.toByteArray());
                            }

                            if ("addrAlice".equals(part.getContentID())){
                                System.err.println(j+"-" + part.getContentID() +" " + part.getContentType());
                                out = new ByteArrayOutputStream();
                                in = part.getInputStream();
                                int k;
                                while ((k = in.read()) != -1) {
                                    out.write(k);
                                }
                                System.err.println(new String(out.toByteArray()));
                                rwm.setAddrAlice(new String(out.toByteArray()));
                            }
                        }
                        if (("application/pkcs7-signature; name=smime.p7s; "+ (char) 0x0D + (char) 0x0A + (char) 0x09 + "smime-type=signed-data").equals(part.getContentType())) {
                            System.err.println("Signatura del correu");
                            System.err.println(j + "-"  + part.getContentType());
                            out = new ByteArrayOutputStream();
                            in = part.getInputStream();
                            int k;
                            while ((k = in.read()) != -1) {
                                out.write(k);
                            }
                            System.err.println(new String(Base64.getEncoder().encode(out.toByteArray())));
                            rwm.setMailSign(out.toByteArray());

                        }

                    }
                }
            }
        }

        finally {
            if (in != null) { in.close(); }
            if (out != null) { out.flush(); out.close(); }
        }
    }

    public static void main(String args[]) throws Exception {
        ReceiveMailImap rebre = new ReceiveMailImap();
        rebre.doit(null, "redWax");
    }
}
