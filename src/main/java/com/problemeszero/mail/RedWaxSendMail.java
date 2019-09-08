package com.problemeszero.mail;

import com.problemeszero.redwax.Main;
import com.problemeszero.redwax.RedWaxMessage;
import javafx.beans.property.StringProperty;
import javax.mail.*;
import javax.mail.internet.*;
import java.io.*;
import java.util.Date;
import java.util.Properties;
import static com.problemeszero.redwax.utils.GuiUtils.informationalAlert;

public class RedWaxSendMail {

    private Session sesh;
    private  String   UN, PW, host, port, proto;
    private boolean auth = false;
    public StringProperty authLabel;

    public RedWaxSendMail() {
        System.err.println("Llegint el fitxer de propietats configuration.xml per a smtp");

        host = Main.appProps.getProperty("servidor");
        port = Main.appProps.getProperty("port");
        proto = Main.appProps.getProperty("protocol");
        UN = Main.appProps.getProperty("usuari");
        PW = Main.appProps.getProperty("contrasenya");
    }

    public Properties getProp() {
        return Main.appProps;
    }

    public void setProp(Properties prop) {
        Main.appProps = prop;
    }

//  Nomes per SMTP
    public String auth() throws IOException {
        auth = chk();
        String s="";

        if(!auth) {
            s="ALERTA: SMTP KO. Connexió no satifactòria. Revisau la configuració SMTP";
        } else {
            s="SMTP OK. Credencials correctes";
        }
        System.out.println(s + " - " + UN) ;
//        authLabel.setValue(s);
        return s;
    }

    public boolean isAuth() {
        return auth;
    }

    public String getUN() {
        return UN;
    }

    public String getHost() {
        return host;
    }

    public String getPort() {
        return port;
    }

    public String getProto() {
        return proto;
    }

    public void setUN(String UN) {
        this.UN = UN;
    }

    public void setPW(String PW) {
        this.PW = PW;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public void setPort(String port) {
        this.port = port;
    }

    private  boolean chk() {

        Main.appProps.put("mail.smtp.auth", "true");
        if(host.equals("smtp.gmail.com") || host.equals("ssl0.ovh.net")){
            Main.appProps.put("mail.smtp.starttls.enable", "true");
        }
        Main.appProps.put("mail.smtp.host", host);
        Main.appProps.put("mail.smtp.port", port);

        if(port.equals("465") & !host.equals("smtp.gmail.com")) {
            System.out.println("Connectat a un SMTP diferent de gmail");
            Main.appProps.put("mail.smtp.ssl.enable", "true"); }

        try {
            sesh = Session.getInstance(Main.appProps, new javax.mail.Authenticator() {
                protected PasswordAuthentication getPasswordAuthentication() {return new PasswordAuthentication(UN, PW);}
            });
            Transport transport = sesh.getTransport(proto);
            transport.connect(host, Integer.parseInt(port), UN, PW);
            transport.close();
            System.out.println("Connexio SMTP correcte");
            return true;
        } catch (AuthenticationFailedException e) {
            System.out.println("AuthenticationFailedException - for authentication failures");
//            e.printStackTrace();
            return false;
        } catch (MessagingException e) {
            System.out.println("SMTP connection error - for other failures");
//            e.printStackTrace();
            return false;
        }
    }

    public void mailToBob(RedWaxMessage rwm, RedWaxSMime mAlice) {
        try {
            System.out.println("\n \n>> ?" + rwm.getTo());

            Message m = new MimeMessage(sesh);
            m.setFrom(new InternetAddress(UN));
            m.setRecipients(Message.RecipientType.TO, InternetAddress.parse(rwm.getTo()));
            m.setSubject("REDWAX - " + rwm.getSubject());
            m.setSentDate(new Date());

            //Hem de posar un ID qeu faci referencia al missatge,receptor,asumpte, data
            rwm.setId("ID0000001");
            rwm.setFrom(InternetAddress.toString(m.getFrom()));
            rwm.setSentDate(m.getSentDate());

            //Afegim el mPart com a part del contingut del missatge de correu
            m.setContent(mAlice.getmPart(), mAlice.getmPart().getContentType());
            //Afegim una capçalera que permet identificar les correus
            m.setHeader("Content-ID", "redWax");

            Transport t = sesh.getTransport(proto);
            System.out.println(">> ? smtp(s) ---> ## " + t.getURLName() + " \n>> ?");
            Transport.send(m);
            informationalAlert("Missatge de correu enviat", "El missatge de correu s'ha enviat satifactòriament al destinatari: " + rwm.getTo());

        } catch (MessagingException e) {
            informationalAlert("Alguna cosa no ha anat bé", "Mira el log de l'aplicació per obtenir més informació");
            e.printStackTrace();
        }
    }

    public void mailToAlice(RedWaxMessage rwm, Multipart mPart) throws AuthenticationFailedException, MessagingException{
        Message m = new MimeMessage(sesh);

        m.setFrom(new InternetAddress(UN));
        m.setSubject("REDWAX NRR - " + rwm.getSubject().substring(9));
        m.setRecipients(Message.RecipientType.TO, InternetAddress.parse(rwm.getFrom()));
        m.setSentDate(new Date());
        m.setContent(mPart, mPart.getContentType());
        m.setHeader("Content-ID", "redWax-NRR");
        Transport t = sesh.getTransport(proto);
        informationalAlert("Missatge de correu enviat", "El missatge de correu s'ha enviat satifactòriament al destinatari: " + rwm.getTo());

        System.out.println(">> ? smtp(s) ---> ## " + t.getURLName() + " \n>> ?");

        t.send(m);
    }

}
