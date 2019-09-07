package com.problemeszero.mail;

import com.problemeszero.crypto.Smime;
import com.problemeszero.redwax.Main;
import com.problemeszero.redwax.RedWaxMessage;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.util.Callback;
import org.bitcoinj.core.Address;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.InsufficientMoneyException;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.script.ScriptBuilder;
import org.bitcoinj.wallet.KeyChain;
import org.bitcoinj.wallet.SendRequest;
import org.bouncycastle.cms.CMSException;
import org.bouncycastle.mail.smime.SMIMEException;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.util.encoders.Hex;
import javax.mail.*;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.*;

import static com.problemeszero.redwax.utils.GuiUtils.informationalAlert;

public class ReadConfirmationController {

    public ListView<RedWaxMessage> redWaxList;
    private ReceiveMailImap rebreImap = new ReceiveMailImap();
    @FXML Button closeButton;
    @FXML private Label connectionLabelIMAP;

    public void onEnviaK1Clicked(ActionEvent actionEvent) {

        boolean cemIguals = false;
        boolean okSignatura = false;
        String no = "";

        System.err.println("Index del missatge triat: " + redWaxList.getFocusModel().getFocusedIndex());
        if (redWaxList.getFocusModel().getFocusedIndex() == -1)  {
            informationalAlert("Atenció!!!!!", "Has de seleccionar una de les línies");
            return ;
        }

        RedWaxMessage rwm = (RedWaxMessage) redWaxList.getFocusModel().getFocusedItem();
        RedWaxSMime missatgeBob = new RedWaxSMime(rwm.getMailSignedMultiPart());
        //rwm.redWaxToPersistent();

        //comparar el cem enviat per Bob amb l'envat per Alice
        RedWaxMessage rwmAlice = null;
        try {
            FileChooser FC = new FileChooser();
            FC.setTitle("Nom del fitxer \"xml\"");
            FC.setInitialDirectory(new File(Main.appProps.getProperty("RedWax")));
            File file = new File(FC.showOpenDialog(Main.instance.stage).getAbsolutePath());
            JAXBContext jaxbContext = JAXBContext.newInstance(RedWaxMessage.class);

            Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
            rwmAlice = (RedWaxMessage) jaxbUnmarshaller.unmarshal(file);

//            System.err.println(new String(java.util.Arrays.toString(rwm.getCem())));
//            System.err.println(new String(java.util.Arrays.toString(rwmAlice.getCem())));
//            Smime.byteToFile(rwm.getCem(), "Guardar cem Bob", new File(Main.appProps.getProperty("Fitxers")));
//            Smime.byteToFile(rwmAlice.getCem(), "Guardar cem Alice", new File(Main.appProps.getProperty("Fitxers")))

            System.err.println("####################################################################################################################");
            System.err.println("########################## PHASE II: Comprovacions del correu enviat per en Bob i enviament de K1 a la xarxa blockchain");
            System.err.println("####################################################################################################################");

            // Comprovam si el cem generat per n'Alice es el matiex que li ha enviat en Bob

            System.err.println("HashCEM enviat per en Bob   = " + new String(Hex.encode(missatgeBob.getHashCem("SHA256"))));
            System.err.println("HashCEM original de n'Alice = " + new String(Hex.encode(Smime.calculateDigest(rwmAlice.getCem()))));

            if (Arrays.equals(Smime.PartToBAOS(missatgeBob.getCem()),rwmAlice.getCem())){
                cemIguals = true;
            } else  no = "NO ";

            System.err.println("El cem rebut per Alice "+ no +"és igual a l'enviat a ne'n Bob ");
            informationalAlert("Els cem son iguals?","N'Alice determina que el cem enviat per en Bob al missatge NRR "+ no +"és el mateix que li va enviar ella");
        } catch (NullPointerException | JAXBException| GeneralSecurityException | MessagingException | IOException e) {
            informationalAlert("Alguna cosa no ha anat bé", "Mira el log de l'aplicació per obtenir més informació");
            e.printStackTrace();
        }

        //validar la signatura del correu d'en Bob
        try {

           //comprovam que la signatura es correcta
            missatgeBob.verifySignedMultipart();
            okSignatura = missatgeBob.isOkSignatura();
            System.err.println("La validacio de la signatura del correu rebut es: " + okSignatura );
            System.err.println("Certificat de " + missatgeBob.getCert().getSubject() + ", expedit per " + missatgeBob.getCert().getIssuer()+
                    ". Vàlid des de \"" + missatgeBob.getCert().getNotBefore().toLocaleString() + "\" fins a \"" +
                    missatgeBob.getCert().getNotAfter().toLocaleString()+ "\".");

            no = "";
            if (!okSignatura) {no = "NO ";}
            informationalAlert("Validació de la signatura","N'Alice determina que la signatura del missatge NRR enviat per en Bob " + no + "és correcta\n\n" +
                    "Nom del certificat: " + missatgeBob.getCert().getSubject() + "\nExpedit per: " + missatgeBob.getCert().getIssuer() + "\n" +
                    "Vàlid des de " + missatgeBob.getCert().getNotBefore().toLocaleString() + " fins a " + missatgeBob.getCert().getNotAfter().toLocaleString() );

        } catch (GeneralSecurityException | OperatorCreationException | CMSException | MessagingException | SMIMEException e) {
            informationalAlert("Alguna cosa no ha anat bé", "Mira el log de l'aplicació per obtenir més informació");
            e.printStackTrace();
        }

        //enviar K1 a bitcoin
        if (cemIguals) {
            String titol;
            String hText;
            if (okSignatura) {
                titol = "Envia Transacció";
                hText = null;

            } else{
                titol = "Signatura NO vàlida";
                hText = "La validació de la signatura del missatge NRR no és correcta.\n\n" +
                        "Això pot ser degut a canvis realitzats pel servidor SMTP a les capseleres MIME.\n" +
                        "Problema detectat amb els servidors de GMAIL i possiblement amb altres";
            }
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle(titol);
            alert.setHeaderText(hText);
            alert.setContentText("Vols enviar la transacció amb la K1 a la xarxa blockchain?");

            Optional<ButtonType> result = alert.showAndWait();
            if (result.get() == ButtonType.OK) {
                // ... user chose OK
                enviarTx(rwmAlice);
            }
        } else {
            System.err.println("Els CEM no són iguals. No s'envia K1 a la xarxa blockchain");
            informationalAlert("Els CEM no són iguals","No s'envia K1 a la xarxa blockchain" );
        }

        System.err.println("####################################################################################################################");
        System.err.println("####################################################################################################################");

    }

    private void enviarTx(RedWaxMessage rwm) {

        System.err.println("Valor de OPRETURN = " + new String(Hex.encode(rwm.getOpReturn())));
        // Create a tx with an OP_RETURN output
        Transaction tx = new Transaction(Main.params);
        tx.addOutput(Coin.ZERO, ScriptBuilder.createOpReturnScript(rwm.getOpReturn()));
        //tx.addOutput(Coin.valueOf(1000), ScriptBuilder.createOutputScript(Address.fromBase58(Main.params,rwm.getAddrAlice())));

        // Send it to the Bitcoin network
        try{
//            System.err.println("Current:" + Main.bitcoin.wallet().currentChangeAddress().toString());
//            System.err.println("Authentication:" + Main.bitcoin.wallet().currentAddress(KeyChain.KeyPurpose.AUTHENTICATION).toString());
//            System.err.println("Change:" + Main.bitcoin.wallet().currentAddress(KeyChain.KeyPurpose.CHANGE).toString());
//            System.err.println("Receive:" + Main.bitcoin.wallet().currentAddress(KeyChain.KeyPurpose.RECEIVE_FUNDS).toString());
//            System.err.println("Refund:" + Main.bitcoin.wallet().currentAddress(KeyChain.KeyPurpose.REFUND).toString());
            System.err.println("ChangeAddress actual del wallet:" + Main.bitcoin.wallet().currentAddress(KeyChain.KeyPurpose.CHANGE).toString());
            System.err.println("Addr de n'Alice: " + rwm.getAddrAlice());
            if (!rwm.getAddrAlice().equals(Main.bitcoin.wallet().currentAddress(KeyChain.KeyPurpose.CHANGE).toString())){
                Main.bitcoin.wallet().reuseAddress(KeyChain.KeyPurpose.CHANGE,
                        Main.bitcoin.wallet().findKeyFromPubKeyHash(Address.fromString(Main.params, rwm.getAddrAlice()).getHash(),Address.fromString(Main.params, rwm.getAddrAlice()).getOutputScriptType()));
                System.err.println("Actualitzam ChangeAddress a l'adreça de n'Alice. ChangeAddress: " + Main.bitcoin.wallet().currentAddress(KeyChain.KeyPurpose.CHANGE).toString());
            } else {
                System.err.println("ChangeAddress és correspon amb l'adreça de n'Alice");

            }
            Main.bitcoin.wallet().sendCoins(SendRequest.forTx(tx));
            System.err.println("Hash de la TX = " + tx.getHash());
            //Treure un Alert amb info de la transaccio
            informationalAlert("Enviada la transacio a Bitcoin","OPRETURN=" + new String(Hex.encode(rwm.getOpReturn())) +"\n" +
                    "TX=" + tx.getHash() +"\nAddr="+ rwm.getAddrAlice());
        } catch (InsufficientMoneyException e) {
            informationalAlert("Insufficient funds","You need bitcoins in this wallet in order to pay network fees.");
        }
    }

    public void onRefreshClicked(ActionEvent actionEvent) {
        //Carregam els missatges de correu
        redWaxList.getItems().clear();
        List<RedWaxMessage> list = new ArrayList<RedWaxMessage>();
        try {
            rebreImap.doit(list, "redWax-NRR");
        } catch (AuthenticationFailedException e) {
//             e.printStackTrace();
            informationalAlert("Error d'autenticació IMAP",e.getMessage());
        } catch (MessagingException | IOException e) {
            e.printStackTrace();
        }
        list.forEach((temp) -> {
            redWaxList.getItems().add(temp);
        });

        redWaxList.setCellFactory(new Callback<ListView<RedWaxMessage>, ListCell<RedWaxMessage>>() {
            @Override
            public ListCell<RedWaxMessage> call(ListView<RedWaxMessage> param) {
                return new ListCell<RedWaxMessage>() {
                    @Override
                    protected void updateItem(RedWaxMessage item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) {
                            setText("");
                            setGraphic(null);
                        } else {
                            setText("Remitent: " + item.getFrom() + " - Assumpte: " + item.getSubject() +" - Enviat: " + item.getSentDate());
//                            ProgressBar bar = new ProgressBar();
//                            bar.progressProperty().bind(item.depth.divide(3.0));
//                            setGraphic(bar);
                        }
                    }
                };
            }
        });
    }


    public void onCloseClicked(ActionEvent actionEvent) {
        // close this window...
        closeButton.getScene().getWindow().hide();
    }

    @FXML public void initialize() throws IOException { connectionLabelIMAP.setText(rebreImap.auth()); }

}



