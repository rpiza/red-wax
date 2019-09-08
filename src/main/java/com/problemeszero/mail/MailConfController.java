package com.problemeszero.mail;

import javafx.scene.control.Button;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.text.Text;

import java.io.IOException;
import java.util.Properties;

import static com.problemeszero.redwax.utils.GuiUtils.informationalAlert;

public class MailConfController {

    @FXML private GridPane layLOG;
    @FXML private ComboBox<String> cmbHOST;
    @FXML protected TextField tUN, tPW, tServidor, tPort;
    @FXML private static final Text UNfail = new Text("Cannot authenticate");
    @FXML private Button btnClose;
    private RedWaxSendMail smtpConf;

    private String UN, PW, host, port;

    public void handleCloseButtonAction(ActionEvent actionEvent) {
        // close this window...
        btnClose.getScene().getWindow().hide();

    }

    @FXML
    protected void handleVerifyButton(ActionEvent e) throws IOException{
        smtpConf.setUN(tUN.getText());
        smtpConf.setPW(tPW.getText());
        smtpConf.setHost(tServidor.getText());
        smtpConf.setPort(tPort.getText());
        smtpConf.auth();
        if (smtpConf.isAuth()) {
            informationalAlert( "Connexió SMPT satisfactòria!!!!","");
        } else {
            informationalAlert( "Error en l'autenticació SMTP", "Revisa la configuració del compte SMTP.\n" +
                "No és possible connectar adequadement amb el servidor SMTP: " + smtpConf.getHost());
        }

    }

    public void setSendMailSmtp(RedWaxSendMail smtpConf){
        this.smtpConf=smtpConf;

    }

    @FXML public void initialize() throws IOException{


    }

    public void handleLoadConfButton(ActionEvent actionEvent) {
        Properties prop = smtpConf.getProp();
        tUN.setText(prop.getProperty("usuari"));
        tPW.setText(prop.getProperty("contrasenya"));
        tServidor.setText(prop.getProperty("servidor"));
        tPort.setText(prop.getProperty("port"));

    }

//    private void auth() throws IOException{
//        boolean auth = chk(UN, PW);
//        if(!auth) {
//            System.out.print("Not auth");
//            layLOG.add(UNfail, 3, 1);
//            tUN.clear();
//            tPW.clear();
//        } else if (auth) {
//            System.out.print("Auth");
//            //transitionScene("Edit Email", "mailedit.fxml", 640, 710, prevStage);
//        } else {
//            System.out.print("Not auth");
//            layLOG.add(UNfail, 3, 1);
//            cmbHOST.setValue(" ");
//            tUN.clear();
//            tPW.clear();
//        }
//    }

//    private boolean chk(String UN, String PW) {
//
//        prop.put("mail.smtp.auth", "true");
//        if(host.equals("smtp.gmail.com") || host.equals("ssl0.ovh.net")){
//            prop.put("mail.smtp.starttls.enable", "true");
//        }
//        prop.put("mail.smtp.host", host);
//        prop.put("mail.smtp.port", port);
//        if(host.equals("ssl0.ovh.net")) { prop.put("mail.smtp.ssl.enable", "true"); }
//
//        boolean check = true;
//        //
//        try {
//            InternetAddress e = new InternetAddress(UN);
//            e.validate();
//        } catch (AddressException e) {
//            e.getStackTrace();
//            check = false;
//        }
//
//        if(check) {
////            MailEditController.sesh = Session.getInstance(prop,
////                    new javax.mail.Authenticator() {
////                        protected PasswordAuthentication getPasswordAuthentication() {
////                            return new PasswordAuthentication(UN, PW);
////                        }
////                    });
//        }
//
//        return check;
//    }
}
