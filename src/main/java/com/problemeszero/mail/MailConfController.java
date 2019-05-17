package com.problemeszero.mail;

import javafx.scene.control.Button;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javax.mail.*;
import javax.mail.internet.*;

import java.io.IOException;
import java.util.Properties;

public class MailConfController {

    private final static String GMAIL_HOST = "smtp.gmail.com";
    private final static String GMAIL_PORT = "587";
    private final static String YAHOO_HOST = "ssl0.ovh.net";
    private final static String YAHOO_PORT = "465";

    private final static String DEFAULT_PORT = "80";

    @FXML private GridPane layLOG;
    @FXML private ComboBox<String> cmbHOST;
    @FXML private TextField tUN, tPW;
    @FXML private static final Text UNfail = new Text("Cannot authenticate");
    @FXML private Button btnClose;

    static String UN, PW, host, port;
    static Properties prop = new Properties();

    public void handleCloseButtonAction(ActionEvent actionEvent) {
        // close this window...
        btnClose.getScene().getWindow().hide();

    }

    @FXML
    protected void handleVerifyButton(ActionEvent e) throws IOException{
        auth();
    }

    private void auth() throws IOException{
        boolean auth = chk(UN, PW);
        if(!auth) {
            System.out.print("Not auth");
            layLOG.add(UNfail, 3, 1);
            tUN.clear();
            tPW.clear();
        } else if (auth) {
            System.out.print("Auth");
            //transitionScene("Edit Email", "mailedit.fxml", 640, 710, prevStage);
        } else {
            System.out.print("Not auth");
            layLOG.add(UNfail, 3, 1);
            cmbHOST.setValue(" ");
            tUN.clear();
            tPW.clear();
        }
    }

    private boolean chk(String UN, String PW) {

        prop.put("mail.smtp.auth", "true");
        if(host.equals("smtp.gmail.com") || host.equals("ssl0.ovh.net")){
            prop.put("mail.smtp.starttls.enable", "true");
        }
        prop.put("mail.smtp.host", host);
        prop.put("mail.smtp.port", port);
        if(host.equals("ssl0.ovh.net")) { prop.put("mail.smtp.ssl.enable", "true"); }

        boolean check = true;
        //
        try {
            InternetAddress e = new InternetAddress(UN);
            e.validate();
        } catch (AddressException e) {
            e.getStackTrace();
            check = false;
        }

        if(check) {
//            MailEditController.sesh = Session.getInstance(prop,
//                    new javax.mail.Authenticator() {
//                        protected PasswordAuthentication getPasswordAuthentication() {
//                            return new PasswordAuthentication(UN, PW);
//                        }
//                    });
        }

        return check;
    }
}
