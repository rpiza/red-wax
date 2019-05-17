package com.problemeszero.time;

import com.problemeszero.redwax.RedWaxMessage;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.time.LocalDateTime;

import java.time.ZoneId;
import java.util.function.BiConsumer;

public class TimePickerController {

    public Label messageLabel;
    public Label detailsLabel;
    public Button okButton;
    public Button cancelButton;
    public Button actionButton;
    public DateTimePicker timeBox;

    /** Initialize this alert dialog for information about a crash. */
    public void crashAlert(Stage stage, String crashMessage) {
        messageLabel.setText("Unfortunately, we screwed up and the app crashed. Sorry about that!");
        detailsLabel.setText(crashMessage);

        cancelButton.setVisible(false);
        actionButton.setVisible(false);
        okButton.setOnAction(actionEvent -> stage.close());
    }

    /** Initialize this alert for general information: OK button only, nothing happens on dismissal. */
    public void selectTime(Stage stage, RedWaxMessage rwd, String message, String details) {
        messageLabel.setText(message );
        detailsLabel.setText(details);
        detailsLabel.setVisible(false);
        cancelButton.setVisible(false);
        actionButton.setVisible(false);
        timeBox.setDateTimeValue(LocalDateTime.now());
        //System.err.println(timeBox.getDateTimeValue());
        okButton.setOnAction(actionEvent -> { rwd.setDeadTimeMillis(timeBox.getDateTimeValue().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()); stage.close(); });
        //deadTime = timeBox.getDateTimeValue();
    }

    public static void runAlert(BiConsumer<Stage, TimePickerController> setup) {
        try {
            // JavaFX2 doesn't actually have a standard alert template. Instead the Scene Builder app will create FXML
            // files for an alert window for you, and then you customise it as you see fit. I guess it makes sense in
            // an odd sort of way.
            Stage dialogStage = new Stage();
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            FXMLLoader loader = new FXMLLoader(TimePickerController.class.getResource("timepicker.fxml"));
            Pane pane = loader.load();
            TimePickerController controller = loader.getController();
            setup.accept(dialogStage, controller);
            dialogStage.setScene(new Scene(pane));
            dialogStage.showAndWait();
        } catch (IOException e) {
            // We crashed whilst trying to show the alert dialog (this should never happen). Give up!
            throw new RuntimeException(e);
        }
    }

    public static void selectTimeAlert(RedWaxMessage rwd, String message, String details, Object... args) {
        String formattedDetails = String.format(details, args);
        Runnable r = () -> runAlert((stage, controller) -> controller.selectTime(stage,rwd, message, formattedDetails));
        if (Platform.isFxApplicationThread())
            r.run();
        else
            Platform.runLater(r);
    }


    public void okClicked(ActionEvent actionEvent) {
       // System.err.println(timeBox.getDateTimeValue());
    }
}
