package com.problemeszero.redwax;

import com.problemeszero.mail.MailEditController;
import com.problemeszero.mail.AliceController;
import com.problemeszero.mail.BobController;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.cell.TextFieldListCell;
import javafx.stage.Stage;
import javafx.util.Callback;
import javafx.util.StringConverter;
import org.bitcoinj.core.*;
import org.bitcoinj.core.listeners.DownloadProgressTracker;
import org.bitcoinj.core.Coin;
import org.bitcoinj.utils.MonetaryFormat;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.util.Duration;
import org.fxmisc.easybind.EasyBind;
import com.problemeszero.redwax.controls.ClickableBitcoinAddress;
import com.problemeszero.redwax.controls.NotificationBarPane;
import com.problemeszero.redwax.utils.*;
import com.problemeszero.redwax.utils.easing.*;
import com.problemeszero.redwax.utils.easing.ElasticInterpolator;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

import static com.problemeszero.redwax.Main.bitcoin;

/**
 * Gets created auto-magically by FXMLLoader via reflection. The widget fields are set to the GUI controls they're named
 * after. This class handles all the updates and event handling for the main UI.
 */
public class MainController {

    public HBox controlsBox;
    public Label balance;
    public Button sendMoneyOutBtn;
    public ClickableBitcoinAddress addressControl;
    public ListView<Transaction> transactionsList;
    public ListView<Proof> pendingProofList;
    @FXML
    Button readButton, mailButton, k1Button;


    private static class Proof implements Serializable {
        byte[] tx, partialMerkleTree;
        Sha256Hash blockHash;

        transient SimpleIntegerProperty depth = new SimpleIntegerProperty();
        transient String filename;

        public void saveTo(String filename) throws IOException {
            try (ObjectOutputStream oos = new ObjectOutputStream(Files.newOutputStream(Paths.get(filename)))) {
                oos.writeObject(this);
            }
        }

        public static Proof readFrom(String filename) throws IOException {
            try (ObjectInputStream ois = new ObjectInputStream(Files.newInputStream(Paths.get(filename)))) {
                return (Proof) ois.readObject();
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private BitcoinUIModel model = new BitcoinUIModel();
    private NotificationBarPane.Item syncItem;

    // Called by FXMLLoader.
    public void initialize() {
        addressControl.setOpacity(0.0);
    }

    public void onBitcoinSetup() {

        DateFormat dateFormat = new SimpleDateFormat("dd/MM/yy HH:mm:ss");

        model.setWallet(bitcoin.wallet());
        addressControl.addressProperty().bind(model.addressProperty());
        balance.textProperty().bind(EasyBind.map(model.balanceProperty(), coin -> MonetaryFormat.BTC.noCode().format(coin).toString()));
        // Don't let the user click send money when the wallet is empty.
        sendMoneyOutBtn.disableProperty().bind(model.balanceProperty().isEqualTo(Coin.ZERO));

//        TorClient torClient = Main.bitcoin.peerGroup().getTorClient();
//        if (torClient != null) {
//            SimpleDoubleProperty torProgress = new SimpleDoubleProperty(-1);
//            String torMsg = "Initialising Tor";
//            syncItem = Main.instance.notificationBar.pushItem(torMsg, torProgress);
//            torClient.addInitializationListener(new TorInitializationListener() {
//                @Override
//                public void initializationProgress(String message, int percent) {
//                    Platform.runLater(() -> {
//                        syncItem.label.set(torMsg + ": " + message);
//                        torProgress.set(percent / 100.0);
//                    });
//                }
//
//                @Override
//                public void initializationCompleted() {
//                    Platform.runLater(() -> {
//                        syncItem.cancel();
//                        showBitcoinSyncMessage();
//                    });
//                }
//            });
//        } else {
            showBitcoinSyncMessage();
//        }
        model.syncProgressProperty().addListener(x -> {
            if (model.syncProgressProperty().get() >= 1.0) {
                readyToGoAnimation();
                if (syncItem != null) {
                    syncItem.cancel();
                    syncItem = null;
                }
            } else if (syncItem == null) {
                showBitcoinSyncMessage();
            }
        });

        Bindings.bindContent(transactionsList.getItems(), model.getTransactions());
        transactionsList.setCellFactory(new Callback<ListView<Transaction>, ListCell<Transaction>>() {
            @Override
            public ListCell<Transaction> call(ListView<Transaction> param) {
                return new TextFieldListCell<Transaction>(new StringConverter<Transaction>() {
                    @Override
                    public String toString(Transaction tx) {
                        Coin value = tx.getValue(Main.bitcoin.wallet());
                        Address addrOutput;
                        Address addrInput;
                         try {
                                addrOutput = tx.getOutput(0).getScriptPubKey().getToAddress(Main.params);
                                addrInput = tx.getInput(0).getConnectedOutput().getScriptPubKey().getToAddress(Main.params);
                         } catch (Exception e){
                             addrOutput = null;
                             addrInput = null;

                         }
                        if (value.isPositive()) {
                            System.err.println(dateFormat.format(tx.getUpdateTime()) + " - Entrada  de " + MonetaryFormat.BTC.format(value) +" cap a " + addrOutput + " - " + tx.getTxId());
                            return dateFormat.format(tx.getUpdateTime()) + " - Entrada  de " + MonetaryFormat.BTC.format(value) + " - tx: " + tx.getTxId();
                        } else  if (value.isNegative()) {
                            System.err.println(dateFormat.format(tx.getUpdateTime()) + " - Sortida de " + MonetaryFormat.BTC.format(value)  +" de " + addrInput + " - " + tx.getTxId());
                            return dateFormat.format(tx.getUpdateTime()) + " - Sortida de " + MonetaryFormat.BTC.format(value)  + " - tx: " + tx.getTxId();
                        }
                        System.err.println("Pagament amb id " + tx.getTxId());
                        return "Pagament amb id " + tx.getTxId();
                    }

                    @Override
                    public Transaction fromString(String string) {
                        return null;
                    }
                });
            }
        });
    }

    private void showBitcoinSyncMessage() {
        syncItem = Main.instance.notificationBar.pushItem("Synchronising with the Bitcoin network", model.syncProgressProperty());
    }

    public void sendMoneyOut(ActionEvent event) {
        // Hide this UI and show the send money UI. This UI won't be clickable until the user dismisses send_money.
        Main.instance.overlayUI("send_money.fxml");
    }

    public void settingsClicked(ActionEvent event) {
        Main.OverlayUI<WalletSettingsController> screen = Main.instance.overlayUI("wallet_settings.fxml");
        screen.controller.initialize(null);
    }

    public void restoreFromSeedAnimation() {
        // Buttons slide out ...
        TranslateTransition leave = new TranslateTransition(Duration.millis(1200), controlsBox);
        leave.setByY(80.0);
        leave.play();
    }

    public void readyToGoAnimation() {
        // Buttons slide in and clickable address appears simultaneously.
        TranslateTransition arrive = new TranslateTransition(Duration.millis(1200), controlsBox);
        arrive.setInterpolator(new ElasticInterpolator(EasingMode.EASE_OUT, 1, 2));
        arrive.setToY(0.0);
        FadeTransition reveal = new FadeTransition(Duration.millis(1200), addressControl);
        reveal.setToValue(1.0);
        ParallelTransition group = new ParallelTransition(arrive, reveal);
        group.setDelay(NotificationBarPane.ANIM_OUT_DURATION);
        group.setCycleCount(1);
        group.play();
    }

    public DownloadProgressTracker progressBarUpdater() {
        return model.getDownloadProgressTracker();
    }

    public void onEnviarClicked(ActionEvent actionEvent) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/problemeszero/mail/mailedit.fxml"));

            Main.instance.stage = new Stage();
            Main.instance.stage.setTitle("FASE I - Step 1: N'Aice envia document certificat a ne'n Bob");
            Main.instance.stage.initOwner(mailButton.getScene().getWindow());
            Main.instance.stage.setScene(new Scene((Parent) loader.load()));

            // showAndWait will block execution until the window closes...
            Main.instance.stage.showAndWait();

            MailEditController controller = loader.getController();
//            controller.initialize(null);

        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

//
//        Main.instance.mainWindow.setOpacity(0.99);
//        Main.OverlayUI<MailEditController> screen = Main.instance.overlayUI("../mail/mailedit.fxml");
//        //Proves rpm. no ha funcionat
//        screen.controller.initialize(null);
    }

    public void onEnviarK1Clicked(ActionEvent actionEvent) {
        // Ask the user for the document to timestamp
//        File doc = new FileChooser().showOpenDialog(Main.instance.mainWindow);
//        if (doc == null) return; // User cancelled

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/problemeszero/mail/alice.fxml"));

            Main.instance.stage = new Stage();
            Main.instance.stage.setTitle("FASE II: N'Aice valida el NRR de'n Bob i publica la K1");
            Main.instance.stage.initOwner(readButton.getScene().getWindow());
            Main.instance.stage.setScene(new Scene((Parent) loader.load()));

            // showAndWait will block execution until the window closes...
            Main.instance.stage.showAndWait();

            AliceController controller = loader.getController();

        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }


    public void onLlegirClicked(ActionEvent actionEvent) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/problemeszero/mail/bob.fxml"));

            Main.instance.stage = new Stage();
            Main.instance.stage.setTitle("FASE I - Step 2: En Bob decideix si envia el NRR -- FASE III: En Bob obtè la K1 i desxifra el document");
            Main.instance.stage.initOwner(readButton.getScene().getWindow());
            Main.instance.stage.setScene(new Scene((Parent) loader.load()));

            // showAndWait will block execution until the window closes...
            Main.instance.stage.showAndWait();

            BobController controller = loader.getController();

        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
