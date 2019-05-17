package com.problemeszero.mail;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;

import java.io.IOException;

public class MailSentController {

    @FXML private Button btnClose;

    @FXML
    protected void handleBackButton(ActionEvent e) throws IOException {
       /** tto.setText(MailEditController.mto);
        ttext.setText(MailEditController.cTEXT);
        tsub.setText(MailEditController.msub);
        thead.setText(MailEditController.mhead);**/
        //transitionScene("Edit Email", "com/problemeszero/mail/mailedit.fxml", 640, 710);
        btnClose.getScene().getWindow().hide();
    }
}
