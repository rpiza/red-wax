package com.problemeszero.mail;

import com.problemeszero.crypto.Smime;
import com.problemeszero.redwax.Main;
import com.problemeszero.redwax.RedWaxMessage;

import java.io.*;
import java.util.*;
import javax.mail.*;
import javax.mail.internet.*;

public class ReceiveMailImap {
    private  String   UN, PW, host, port, proto, folderImap;
    private Session session;
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
//            e.printStackTrace();
            return false;
        } catch (MessagingException e) {
            System.out.println("IMAP/POP3 connection error - for other failures");
//            e.printStackTrace();
            return false;
        }
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
            int total_missatges = ((messages.length > 5) ? 5 : messages.length);

            for (int i= messages.length - total_missatges; i < messages.length; ++i) {
                System.out.println("valor de i = " + i);
                Message msg = messages[i];
                if (msg.getHeader("Content-ID")!=null)
                if (contentId.equals(msg.getHeader("Content-ID")[0])) {
                    System.err.println("####################################################################################################################");
                    System.err.println("########################## Contingut dels missatges recuperats del compte de correu");
                    System.err.println("####################################################################################################################");

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
//                    saveParts(msg.getContent(), rwm);

                    if (msg.getContent() instanceof Multipart) {
                        Multipart multi = ((Multipart) msg.getContent());

                        //Guardar multipart a rwm
    //                   System.err.println(new String(java.util.Arrays.toString(mPartBaos.toByteArray())));
    //                   Smime.byteToFile(mPartBaos.toByteArray(), "Guardar multipart", new File(Main.appProps.getProperty("Fitxers")));

                         System.err.println("Content-Type=" + multi.getContentType());
                         if (multi.getContentType().toLowerCase().contains("multipart/signed;")){
                             rwm.setMailSignedMultiPart(Smime.PartToBAOS(multi)); //Guardam el Multipart de tot el correu (CEM + Signatura)
                         }
    //                   Smime.byteToFile(mPartBaos.toByteArray(), "Guardar cemSignat",new File(Main.appProps.getProperty("Fitxers")));
    //                   System.err.println(java.util.Arrays.toString(mPartBaos.toByteArray()));
                     }


                    System.err.println("####################################################################################################################");
                    System.err.println("####################################################################################################################");
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

//    public void saveParts(Object content, String filename)
//            throws IOException, MessagingException
//    {
//        OutputStream out = null;
//        InputStream in = null;
//        try {
//            if (content instanceof Multipart) {
//                Multipart multi = ((Multipart)content);
//                int parts = multi.getCount();
//                for (int j=0; j < parts; ++j) {
//                    MimeBodyPart part = (MimeBodyPart)multi.getBodyPart(j);
//                    if (part.getContent() instanceof Multipart) {
//                        // part-within-a-part, do some recursion...
//                        saveParts(part.getContent(), filename);
//                    }
//                    else {
//                        String extension = "";
//                        if (part.isMimeType("text/html")) {
//                            extension = "html";
//                        }
//                        else {
//                            if (part.isMimeType("text/plain")) {
//                                extension = "txt";
//                            }
//                            else {
//                                //  Try to get the name of the attachment
//                                extension = part.getDataHandler().getName();
//                            }
//                            filename = filename + "." + extension;
//                            System.out.println("... " + filename);
//                            out = new FileOutputStream(new File(filename));
//                            in = part.getInputStream();
//                            int k;
//                            while ((k = in.read()) != -1) {
//                                out.write(k);
//                            }
//                        }
//                    }
//                }
//            }
//        }
//        finally {
//            if (in != null) { in.close(); }
//            if (out != null) { out.flush(); out.close(); }
//        }
//    }
//
//    public void saveParts(Object content, RedWaxMessage rwm) throws IOException, MessagingException {
//
////        ByteArrayOutputStream out = null;
////        InputStream in = null;
//        try {
//            if (content instanceof Multipart) {
//                Multipart multi = ((Multipart)content);
//
//                //Guardar multipart a rwm
////                System.err.println(new String(java.util.Arrays.toString(mPartBaos.toByteArray())));
////                Smime.byteToFile(mPartBaos.toByteArray(), "Guardar multipart", new File(Main.appProps.getProperty("Fitxers")));
//
//                System.err.println("Content-Type=" + multi.getContentType());
//                if (multi.getContentType().toLowerCase().contains("multipart/signed;")){
//                    rwm.setMailSignedMultiPart(Smime.PartToBAOS(multi)); //Guardam el Multipart de tot el correu (CEM + Signatura)
////                    Smime.byteToFile(mPartBaos.toByteArray(), "Guardar cemSignat",new File(Main.appProps.getProperty("Fitxers")));
////                    System.err.println(java.util.Arrays.toString(mPartBaos.toByteArray()));
//                }
//
//                if (multi.getContentType().toLowerCase().contains("multipart/mixed;")){
//                    rwm.setCem(Smime.PartToBAOS(multi));
////                    Smime.byteToFile(mPartBaos.toByteArray(), "Guardar cem imaps",new File(Main.appProps.getProperty("Fitxers")));
////                    System.err.println(new String(java.util.Arrays.toString(mPartBaos.toByteArray())));
////                    System.err.println(new String(java.util.Arrays.toString(rwm.getCem())));
//                }
//
//                int parts = multi.getCount();
//                System.err.println("Num. parts del missatge multipart = " + parts);
//                for (int j=0; j < parts; ++j) {
//                    MimeBodyPart part = (MimeBodyPart)multi.getBodyPart(j);
//                    if (part.getContent() instanceof Multipart) {
//                        // part-within-a-part, do some recursion...
//                        saveParts(part.getContent(), rwm);
//                    }
//                    else {
//                        if (part.getContentID() != null) {
//                            if ("fitxerXifrat".equals(part.getContentID())){
//                                byte[] out2 = MimeBodyPartToBAOS(part,"Base64");
//                                byte [][] cF = {Arrays.copyOfRange(out2,0,16) ,Arrays.copyOfRange(out2,16, out2.length) };
////                                System.err.println(new String(Base64.getEncoder().encode(cF[0])));
////                                System.err.println(new String(Base64.getEncoder().encode(cF[1])));
//                                rwm.setCertFile(cF);
//                            }
//
//                            if ("deadTime".equals(part.getContentID())){
//                                rwm.setDeadTimeMillis(Long.valueOf(new String(MimeBodyPartToBAOS(part,""))));
//                            }
//
//                            if ("kPrima".equals(part.getContentID())){
//                                rwm.setkPrima(MimeBodyPartToBAOS(part,""));
//                            }
//
//                            if ("addrAlice".equals(part.getContentID())){
//                                rwm.setAddrAlice(new String(MimeBodyPartToBAOS(part,"")));
//                            }
//                        }
////                        System.err.println(j + "-"  + part.getContentType());
////                        System.err.println(java.util.Arrays.toString(part.getContentType().getBytes()));
//                        if (("application/pkcs7-signature; name=smime.p7s; "+ (char) 0x0D + (char) 0x0A + (char) 0x09 + "smime-type=signed-data").equals(part.getContentType().toLowerCase())) {
//                            System.err.println("Signatura del correu");
//                            System.err.println(j + "-" + "Content-Type: " + part.getContentType());
//
//                            rwm.setMailSign(MimeBodyPartToBAOS(part,"Base64"));
//
//                        }
//
//                    }
//                }
//            }
//        }
//
//        finally {
////            if (in != null) { in.close(); }
////            if (out != null) { out.flush(); out.close(); }
//        }
//    }
//
//
//    //Escriu nomes el contingut del MimeBodyPart. NO inclou les capsaleres
//    private byte [] MimeBodyPartToBAOS (MimeBodyPart part, String code) throws MessagingException, IOException {
//
//        ByteArrayOutputStream out = new ByteArrayOutputStream();
//        InputStream in = part.getInputStream();
//        byte [] output;
//        int k;
//        while ((k = in.read()) != -1) {
//            out.write(k);
//        }
//        output = out.toByteArray();
//        in.close();
//        out.close();
////        System.err.println(j+"-" + part.getContentID() +" " + part.getContentType());
////        if (!code.equals("Base64")) System.err.println(new String(output));
////        else System.err.println(new String(Base64.getEncoder().encode(output)));
//        return output;
//    }

    public static void main(String args[]) throws Exception {
        ReceiveMailImap rebre = new ReceiveMailImap();
        rebre.doit(null, "redWax");
    }
}
