package com.problemeszero.mail;


import com.problemeszero.crypto.Pem;
import com.problemeszero.crypto.Smime;
import com.problemeszero.redwax.Main;
import com.problemeszero.redwax.MainController;
import com.problemeszero.redwax.RedWaxMessage;
import com.sun.mail.util.LineInputStream;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.stage.FileChooser;
import javafx.util.Callback;
import org.bitcoinj.core.Address;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.InsufficientMoneyException;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.script.ScriptBuilder;
import org.bitcoinj.wallet.SendRequest;
import org.bouncycastle.cms.CMSException;
import org.bouncycastle.mail.smime.SMIMEException;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.util.encoders.Hex;

import javax.activation.DataSource;
import javax.mail.MessagingException;
import javax.mail.internet.ContentType;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.problemeszero.redwax.utils.GuiUtils.informationalAlert;

public class ReadConfirmationController {


    public ListView<RedWaxMessage> redWaxList;
    private SendMailSmtp enviaCorreu = new SendMailSmtp();
    private ReceiveMailImap rebreImap = new ReceiveMailImap();
    @FXML
    Button closeButton;
    public void onEnviaK1Clicked(ActionEvent actionEvent) {

        boolean cemIguals = false;
        boolean okSignatura = false;

        System.err.println(redWaxList.getFocusModel().getFocusedIndex());
        if (redWaxList.getFocusModel().getFocusedIndex() == -1)  {
            informationalAlert("Atenció!!!!!", "Has de seleccionar una de les línies");
            return ;
        }

        RedWaxMessage rwm = (RedWaxMessage) redWaxList.getFocusModel().getFocusedItem();
        //rwm.redWaxToPersistent();

        //comparar el cem enviat per Bob amb l'envat per Alice
        RedWaxMessage rwmAlice = null;
        try {

        FileChooser FC = new FileChooser();
        FC.setTitle("Nom del fitxer redwax.xml");
        FC.setInitialDirectory(new File(Main.appProps.getProperty("RedWax")));
        File file = new File(FC.showOpenDialog(Main.instance.stage).getAbsolutePath());
        JAXBContext jaxbContext = JAXBContext.newInstance(RedWaxMessage.class);

        Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
        rwmAlice = (RedWaxMessage) jaxbUnmarshaller.unmarshal(file);

        if (Arrays.equals(rwm.getCem(),rwmAlice.getCem())){
            cemIguals = true;
            System.err.println(" El cem rebut per n'Alice és igual a l'enviat a ne'n Bob " );
            informationalAlert("Els cem son iguals?","N'Alice determina que el cem enviat per en Bob al missatge NRR és el mateix que li va enviar ella");
        } else  System.err.println(" El cem rebut per Alice NO és igual a l'enviat a ne'n Bob ");

        } catch (JAXBException e) {
            e.printStackTrace();
        }

        //validar la signatura del correu rebut
        ContentType cType = new ContentType("multipart", "signed", null);
        cType.setParameter("boundary", obtenir_boundary(rwm.getMailSignedMultiPart()));

        DataSource dataSource = new ByteArrayDataSource(rwm.getMailSignedMultiPart(),cType.toString());

        MimeMultipart mPart = null;
        try {
            mPart = new MimeMultipart(dataSource);

            //comprovam que la signatura es correcta
            okSignatura = Smime.verifySignedMultipart(mPart);
             System.err.println(" La validacio de la signatura del correu rebut es: " + okSignatura );
            if (okSignatura) informationalAlert("Validació de la signatura","N'Alice determina que la signatura del missatge NRR enviat per en Bob és correcte");
        } catch (GeneralSecurityException | OperatorCreationException | CMSException | MessagingException | SMIMEException e) {
            e.printStackTrace();
        }

//        ByteArrayOutputStream mPartBaos = new ByteArrayOutputStream();
//        try {
//            mPart.writeTo(mPartBaos);
//        } catch (IOException e) {
//            e.printStackTrace();
//        } catch (MessagingException e) {
//            e.printStackTrace();
//        }
//        Smime.byteToFile(mPartBaos.toByteArray(), "Guardar cemSignat");

        //enviar K1 a bitcoin
        if (okSignatura && cemIguals){

            //Hem de treure una finestra per donar eleccio a Alice a envia la Tx o no
            //Enviam la transaccio
            enviarTx(rwmAlice);


        }
    }


    private void enviarTx(RedWaxMessage rwm) {
        //Carregam l'objecte RedWaxMessage
        //RedWaxMessage rwm = new RedWaxMessage();
//        try {
//
//            FileChooser FC = new FileChooser();
//            FC.setTitle("Nom del fitxer redwax.xml");
//            FC.setInitialDirectory(new File(Main.appProps.getProperty("RedWax")));
//            File file = new File(FC.showOpenDialog(Main.instance.stage).getAbsolutePath());
//            JAXBContext jaxbContext = JAXBContext.newInstance(RedWaxMessage.class);
//
//            Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
//            rwm = (RedWaxMessage) jaxbUnmarshaller.unmarshal(file);
//            System.err.println("Valor de OPRETURN = " + new String(Hex.encode(rwm.getOpReturn())));
//
//        } catch (JAXBException e) {
//            e.printStackTrace();
//        }

        System.err.println("Valor de OPRETURN = " + new String(Hex.encode(rwm.getOpReturn())));
        // Create a tx with an OP_RETURN output
        Transaction tx = new Transaction(Main.params);
        tx.addOutput(Coin.ZERO, ScriptBuilder.createOpReturnScript(rwm.getOpReturn()));
        tx.addOutput(Coin.valueOf(1000), ScriptBuilder.createOutputScript(Address.fromBase58(Main.params,rwm.getAddrAlice())));

        // Send it to the Bitcoin network
        try{
            Main.bitcoin.wallet().sendCoins(SendRequest.forTx(tx));
            System.err.println("Hash de la TX = " + tx.getHash());
            System.err.println("Addr = " + rwm.getAddrAlice());
            //Treure un Alert amb info de la transaccio
            informationalAlert("Enviada la transacio a Bitcoin","OPRETURN=" + new String(Hex.encode(rwm.getOpReturn())) +"\n" +
                    "TX=" + tx.getHash() +"\nAddr="+ rwm.getAddrAlice());
        } catch (InsufficientMoneyException e) {
            informationalAlert("Insufficient funds","You need bitcoins in this wallet in order to pay network fees.");
        }
    }

    public void onRefreshClicked(ActionEvent actionEvent) {
        redWaxList.getItems().clear();
        List<RedWaxMessage> list = new ArrayList<RedWaxMessage>();
        try {
            rebreImap.doit(list, "redWax-NRR");
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
                            setText("correu de " + item.getFrom() + " - Assupte:" + item.getSubject() +" - Enviat: " + item.getSentDate());
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

    private String obtenir_boundary(byte[] m){

        ByteArrayInputStream in = new ByteArrayInputStream(m);
        LineInputStream lin = new LineInputStream(in);
        String line = null;
        try {
            line = lin.readLine();
            lin.close();
            in.close();
//            System.err.println(line);
        } catch (IOException e) {
            e.printStackTrace();
        }
        //boundary = line. Es la marca dins dels diferents multiparts
        //Llevam els dos primers -- del boundary.
        return line.substring(2,line.length());

    }

}



