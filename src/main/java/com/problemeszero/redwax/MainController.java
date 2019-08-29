package com.problemeszero.redwax;

import com.problemeszero.crypto.Smime;
import com.problemeszero.mail.MailEditController;
import com.problemeszero.mail.ReadConfirmationController;
import com.problemeszero.mail.ReadMailController;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.cell.TextFieldListCell;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Callback;
import javafx.util.StringConverter;
import org.bitcoinj.core.*;
import org.bitcoinj.core.listeners.DownloadProgressTracker;
import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.params.RegTestParams;
import org.bitcoinj.params.TestNet3Params;
import org.bitcoinj.script.ScriptBuilder;
import org.bitcoinj.utils.MonetaryFormat;
import com.subgraph.orchid.TorClient;
import com.subgraph.orchid.TorInitializationListener;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.util.Duration;
import org.bitcoinj.wallet.SendRequest;
import org.bouncycastle.crypto.CryptoServicesRegistrar;
import org.bouncycastle.jcajce.provider.BouncyCastleFipsProvider;
import org.bouncycastle.util.encoders.Hex;
import org.fxmisc.easybind.EasyBind;
import com.problemeszero.redwax.controls.ClickableBitcoinAddress;
import com.problemeszero.redwax.controls.NotificationBarPane;
import com.problemeszero.redwax.utils.*;
import com.problemeszero.redwax.utils.easing.*;


import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.Security;

import static com.problemeszero.redwax.Main.appProps;
import static com.problemeszero.redwax.Main.bitcoin;
import static com.problemeszero.redwax.utils.GuiUtils.informationalAlert;

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

        //carregam el proveidor desde la JVM - https://docs.oracle.com/cd/E19830-01/819-4712/ablsc/index.html
        //Security.addProvider(new BouncyCastleFipsProvider());
        //CryptoServicesRegistrar.setApprovedOnlyMode(true);
//        System.err.println("Nomes mode aprovat:" + CryptoServicesRegistrar.isInApprovedOnlyMode());

        model.setWallet(bitcoin.wallet());
        addressControl.addressProperty().bind(model.addressProperty());
        balance.textProperty().bind(EasyBind.map(model.balanceProperty(), coin -> MonetaryFormat.BTC.noCode().format(coin).toString()));
        // Don't let the user click send money when the wallet is empty.
        sendMoneyOutBtn.disableProperty().bind(model.balanceProperty().isEqualTo(Coin.ZERO));

        TorClient torClient = Main.bitcoin.peerGroup().getTorClient();
        if (torClient != null) {
            SimpleDoubleProperty torProgress = new SimpleDoubleProperty(-1);
            String torMsg = "Initialising Tor";
            syncItem = Main.instance.notificationBar.pushItem(torMsg, torProgress);
            torClient.addInitializationListener(new TorInitializationListener() {
                @Override
                public void initializationProgress(String message, int percent) {
                    Platform.runLater(() -> {
                        syncItem.label.set(torMsg + ": " + message);
                        torProgress.set(percent / 100.0);
                    });
                }

                @Override
                public void initializationCompleted() {
                    Platform.runLater(() -> {
                        syncItem.cancel();
                        showBitcoinSyncMessage();
                    });
                }
            });
        } else {
            showBitcoinSyncMessage();
        }
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
                        if (value.isPositive()) {
                            return "Entrada de " + MonetaryFormat.BTC.format(value) + " - " + tx.getUpdateTime();
                        } else  if (value.isNegative()) {
                            Address address = tx.getOutput(1).getAddressFromP2PKHScript(Main.params);
                            return "Sortida de " + MonetaryFormat.BTC.format(value)  +" cap a " + address + " - " + tx.getUpdateTime();
                        }
                        return "Pagament amb id " + tx.getHash();
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
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/problemeszero/mail/readconfirmation.fxml"));

            Main.instance.stage = new Stage();
            Main.instance.stage.initOwner(readButton.getScene().getWindow());
            Main.instance.stage.setScene(new Scene((Parent) loader.load()));

            // showAndWait will block execution until the window closes...
            Main.instance.stage.showAndWait();

            ReadConfirmationController controller = loader.getController();

        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

//    private void enviarTx() {
//        //Carregam l'objecte RedWaxMessage
//        RedWaxMessage rwm = new RedWaxMessage();
//        try {
//
//            FileChooser FC = new FileChooser();
//            FC.setTitle("Nom del fitxer");
//            File file = new File(FC.showOpenDialog(Main.instance.mainWindow).getAbsolutePath());
//            JAXBContext jaxbContext = JAXBContext.newInstance(RedWaxMessage.class);
//
//            Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
//            rwm = (RedWaxMessage) jaxbUnmarshaller.unmarshal(file);
//
//            System.err.println("Valor de OPRETURN = " + new String(Hex.encode(rwm.getOpReturn())));
//
//        } catch (JAXBException e) {
//            e.printStackTrace();
//        }
//
//        // Create a tx with an OP_RETURN output
//        Transaction tx = new Transaction(Main.params);
//        tx.addOutput(Coin.ZERO, ScriptBuilder.createOpReturnScript(rwm.getOpReturn()));
//
//        // Send it to the Bitcoin network
//        try {
//            Main.bitcoin.wallet().sendCoins(SendRequest.forTx(tx));
//        } catch (InsufficientMoneyException e) {
//            informationalAlert("Insufficient funds","You need bitcoins in this wallet in order to pay network fees.");
//        }
//        //Treure un Alert amb info de la transaccio
//
//    }

    public void onLlegirClicked(ActionEvent actionEvent) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/problemeszero/mail/readmail.fxml"));

            Main.instance.stage = new Stage();
            Main.instance.stage.initOwner(readButton.getScene().getWindow());
            Main.instance.stage.setScene(new Scene((Parent) loader.load()));

            // showAndWait will block execution until the window closes...
            Main.instance.stage.showAndWait();

            ReadMailController controller = loader.getController();

        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
