package com.problemeszero.mail;

import com.problemeszero.redwax.Main;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;

import javafx.stage.Stage;

import java.io.*;
import java.time.LocalDateTime;
import java.util.*;

public class MailEditController {

    @FXML private ComboBox<String> cmbTYPE;
    @FXML protected TextField tto;
    @FXML protected TextField thead;
    @FXML protected TextField tsub;
    @FXML protected TextArea ttext;
    @FXML private Button btn1;
    @FXML private Button btnClose;

    @FXML private TextField tUN, tPW;
    @FXML private Label connectionLabel;

    public Main.OverlayUI overlayUI;

    public static LocalDateTime deadTime;
    private SendMailSmtp enviaCorreu = new SendMailSmtp();


    public void LayEditController(){
    }

    @FXML
    protected void handleConfigButtonAction(ActionEvent actionEvent)  throws Exception {
        //actiontarget.setText("Sign in button pressed");
        //anar a l'altre finestra'
        Properties prop = enviaCorreu.getProp();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("mailconf.fxml"));
            //Carregam les propietats a l'escena mailconf.fxml
            prop.stringPropertyNames().forEach(key -> loader.getNamespace().put(key, prop.getProperty(key)));

            Stage stage = new Stage();
            stage.initOwner(btn1.getScene().getWindow());
            stage.setScene(new Scene((Parent) loader.load()));

            // showAndWait will block execution until the window closes...
            stage.showAndWait();

            MailConfController controller = loader.getController();

        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @FXML protected void handleSendButton(ActionEvent e) throws IOException {

       // enviaCorreu.auth();

        enviaCorreu.mail(tto.getText(), tsub.getText(), ttext.getText());
        if(!tto.getText().isEmpty() || !ttext.getText().isEmpty() || !tsub.getText().isEmpty()){
            FXMLLoader loader = new FXMLLoader(getClass().getResource("mailsent.fxml"));
            //Carregam les propietats a l'escena mailconf.fxml
            Stage stage = new Stage();
            stage.initOwner(btn1.getScene().getWindow());
            stage.setScene(new Scene((Parent) loader.load()));
            // showAndWait will block execution until the window closes...
            stage.showAndWait();
            MailSentController controller = loader.getController();
        }
    }

    @FXML public void initialize() throws IOException { connectionLabel.setText(enviaCorreu.auth()); }

    public void initialize(Object o) throws IOException {
//        //carregam el fitxer properties
//        //llegim fitxer de propietats configuration.xml
//        System.err.println("Llegint el fitxer de propietats configuration.xml");
//        try {
//            InputStream in = new FileInputStream("configuration.xml");
//            prop.loadFromXML(in);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        host = prop.getProperty("servidor");
//        port = prop.getProperty("port");
//        proto = prop.getProperty("protocol");
//        UN = prop.getProperty("usuari");
//        PW = prop.getProperty("contrasenya");
    }

    public void closeClicked(ActionEvent actionEvent) {
        //overlayUI.done();
        btnClose.getScene().getWindow().hide();
    }

}