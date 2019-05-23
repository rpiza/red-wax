package com.problemeszero.mail;


import com.google.common.primitives.Bytes;
import com.problemeszero.crypto.AESCrypto2;
import com.problemeszero.crypto.Pem;
import com.problemeszero.crypto.Smime;
import com.problemeszero.redwax.Main;
import com.problemeszero.redwax.RedWaxMessage;
import com.problemeszero.redwax.utils.AlertWindowController;
import com.sun.javaws.jnl.InformationDesc;
import com.sun.mail.util.LineInputStream;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.util.Callback;
import org.bouncycastle.cms.CMSException;
import org.bouncycastle.crypto.InvalidWrappingException;
import org.bouncycastle.crypto.asymmetric.AsymmetricRSAPrivateKey;
import org.bouncycastle.crypto.asymmetric.AsymmetricRSAPublicKey;
import org.bouncycastle.crypto.fips.FipsRSA;
import org.bouncycastle.mail.smime.SMIMEException;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.util.encoders.Hex;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Transport;
import javax.mail.internet.*;
import javax.mail.util.ByteArrayDataSource;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.*;

import static com.problemeszero.redwax.JsonReader.readJsonFromUrl;
import static com.problemeszero.redwax.utils.GuiUtils.informationalAlert;

public class ReadMailController {
    public ListView<RedWaxMessage> redWaxList;
    private SendMailSmtp enviaCorreu = new SendMailSmtp();
    private ReceiveMailImap rebreImap = new ReceiveMailImap();
    @FXML
    Button closeButton;
    @FXML private Label connectionLabel;

    public void onConfirmClicked(ActionEvent actionEvent) {

        System.err.println(redWaxList.getFocusModel().getFocusedIndex());
        if (redWaxList.getFocusModel().getFocusedIndex() == -1)  {
            informationalAlert("Atenció!!!!!", "Has de seleccionar una de les línies");
            return ;
        }


        boolean okSignatura = false;
        RedWaxMessage rwm = (RedWaxMessage) redWaxList.getFocusModel().getFocusedItem();
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
            if (okSignatura) informationalAlert("Validació de la signatura","En Bob determina que la signatura del missatge enviat per n'Alice és correcta");
        } catch (GeneralSecurityException |  OperatorCreationException | CMSException | SMIMEException | MessagingException e) {
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

        //enviar confirmacio
        if (okSignatura) {

            //Obtenim el cem del missatge enviat per Alice contingut dins l'objecte rwm
            cType = new ContentType("multipart", "mixed", null);
            cType.setParameter("boundary", obtenir_boundary(rwm.getCem()));
            dataSource = new ByteArrayDataSource(rwm.getCem(),cType.toString());
            MimeBodyPart bodyPart = null;
            MimeMultipart multiPart;
            try {
                multiPart = new MimeMultipart(dataSource);
                bodyPart = new MimeBodyPart();
                bodyPart.setContent(multiPart);
            } catch (MessagingException e) {
                e.printStackTrace();
            }

//            ByteArrayOutputStream mPartBaos = new ByteArrayOutputStream();
//            try {
//                bodyPart.writeTo(mPartBaos);
//                //System.err.println(java.util.Arrays.toString(mPartBaos.toByteArray()));
//               // Smime.byteToFile(mPartBaos.toByteArray(), "Guardar cem recuperat per Bob");
//                mPartBaos.close();
//            } catch (IOException | MessagingException e) {
//                e.printStackTrace();
//            }


            //Preparam per signar el missatge
            X509Certificate bobCert = null;
            try {
                bobCert = Pem.readCertificate(Pem.fileToString("Introdueix el certificat de'n Bob",new File(Main.appProps.getProperty("Certificats"))));
                PrivateKey priKeyBob = Pem.readPrivateKey(Pem.fileToString("Selecciona la clau Privada de'n Bob",new File(Main.appProps.getProperty("Certificats"))));
                //Obtendrem el missatge signat a un MultiPart, que contendra el CEM i el missatge signat
                //Signam el missatge
                mPart = Smime.createSignedMultipart(priKeyBob,bobCert,bodyPart);
                //comprovam que la signatura es correcta
                System.err.println("La validacio de la signatura de la confirmacio es: " + Smime.verifySignedMultipart(mPart));
            } catch (IOException | GeneralSecurityException | CMSException | MessagingException | OperatorCreationException | SMIMEException e) {
                e.printStackTrace();
            }
//            mPartBaos = new ByteArrayOutputStream();
//            try {
//                mPart.writeTo(mPartBaos);
//                //System.err.println(java.util.Arrays.toString(mPartBaos.toByteArray()));
//                Smime.byteToFile(mPartBaos.toByteArray(), "Guardar cemSignat");
//                mPartBaos.close();
//            } catch (IOException | MessagingException e) {
//                e.printStackTrace();
//            }
            //Enviam el missatge
//            try {
//                enviaCorreu.auth();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
            enviaCorreu.mail(rwm,mPart);
            informationalAlert("Enviat missatge NRR", "En Bob ha enviat el missatge NRR a n'Alice");

        }
    }

    public void onRefreshClicked(ActionEvent actionEvent) {
        redWaxList.getItems().clear();
        List<RedWaxMessage> list = new ArrayList<RedWaxMessage>();
        try {
            rebreImap.doit(list, "redWax");
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
        return line.substring(2,line.length());

    }


    public void onDecryptClicked(ActionEvent actionEvent) {

        System.err.println(redWaxList.getFocusModel().getFocusedIndex());
        if (redWaxList.getFocusModel().getFocusedIndex() == -1)  {
            informationalAlert("Atenció!!!!!", "Has de seleccionar una de les línies");
            return ;
        }

        RedWaxMessage rwm = (RedWaxMessage) redWaxList.getFocusModel().getFocusedItem();
        byte[] hashCem = null;

        //Obtenim l'adreça Bitcoin del correu enviat per Alice
        String addrAlice = rwm.getAddrAlice();
        //obtenim el hashCem

        try {
            hashCem = Smime.calculateDigest(rwm.getCem());
            System.err.println("HashCEM = " + new String(Hex.encode(hashCem)));
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
        }

        //obtenir les transaccions de l addrAlice i el valor d' OPRETURN, conectant-nos a l'api de blockchain.info
        AESCrypto2 aes = new AESCrypto2();

        byte[] opRe = obtenirOpreturn(addrAlice,hashCem);

        if (opRe!= null) { aes.setK1(opRe); }
        else {
            informationalAlert("No hem trobat el Hash(cem)!!!" ,"Per a l'adreça " + addrAlice + ",\n" +
                    "no hem trobat cap Tx amb un OPRETURN que contengui\n" +
                    "el hash=" + new String(Hex.encode(hashCem)));
            return; }

        //Carrergam la clau privada de Bob per dexifrar kPrima
        PrivateKey priKeyBob = null;
        Cipher dec = null;
        try {
            priKeyBob = Pem.readPrivateKey(Pem.fileToString("Selecciona la clau Privada de'n Bob",new File(Main.appProps.getProperty("Certificats"))));
            AsymmetricRSAPrivateKey rsaPriKey = new AsymmetricRSAPrivateKey(FipsRSA.ALGORITHM, priKeyBob.getEncoded());
//            dec = Cipher.getInstance("RSA", "BCFIPS");
//            dec.init(Cipher.DECRYPT_MODE, priKeyBob);
//            aes.setK2(dec.doFinal(Hex.decode(rwm.getkPrima())));
            aes.setK2(Smime.unwrapKey(rsaPriKey,Hex.decode(rwm.getkPrima())));


        } catch (IOException | CertificateException | InvalidWrappingException e) {
            e.printStackTrace();
        }
        informationalAlert("\nObtinguts els valors de K1 i K2" ,"De l'OPRETURN de l'adreça " + addrAlice + ", hem obtingut\n" +
                "K1="+new String(Hex.encode(opRe)) + ".\n\nDel valor de K' enviat en el missatge de n'Alice, hem obtingut\n" +
                "K2=" + new String(Hex.encode(aes.getK2())));
        aes.obtainK();
        Smime.byteToFile(aes.cbcDecrypt(rwm.getCertFile()[0], rwm.getCertFile()[1]),"Guardar el fitxer desxifrat",new File(Main.appProps.getProperty("Fitxers")));
        informationalAlert("Obtingut el fitxer desxifrat","");
    }

    private byte[] obtenirOpreturn(String addr, byte[] hash){

        JSONObject json = null;
        //JSONObject currentBlock = null;
        Document doc = null;
        Long currentBlock;
        Long txBlock = 0L;
        int depth = Integer.parseInt(Main.appProps.getProperty("depth")); //nombre de confirmacions necessaries per acceptar la tx

        try {
            //
            // S'HA D'ANALITZAR LES RESPOSTES DE LES APIS PER EVITAR XSS
            //
           // json = readJsonFromUrl("https://testnet.blockchain.info/rawaddr/" + addr);
            json = readJsonFromUrl(Main.appProps.getProperty("api_addr") + addr);
            // Per obtenir json d'una transaccio: https://testnet.blockchain.info/rawtx/$tx_hash
            //currentBlock = readJsonFromUrl("https://testnet.blockchain.info/q/getblockcount"); //https://testnet.blockchain.info/latestblock"
            //doc = Jsoup.connect("https://testnet.blockchain.info/q/getblockcount").get();
            doc = Jsoup.connect(Main.appProps.getProperty("api_currentBlock")).get();

        } catch (IOException e) {
            e.printStackTrace();
        }
        System.err.println("******************************************");
        //System.err.println(json.toString());
        System.err.println("Hash en Hex = " + new String(Hex.encode(hash)));
        //Obtenir el darrer block de la cadena
        System.err.println("Block Actual = " + doc.body().text());
        System.err.println("******************************************");
        currentBlock = Long.valueOf(doc.body().text());

        //System.err.println("Block Actual = " + currentBlock.toString());

        JSONArray txs = json.getJSONArray("txs");
        Iterator itT = txs.iterator();
        while (itT.hasNext()) {
            JSONObject level = (JSONObject) itT.next();
            JSONArray out = level.getJSONArray("out");
            Iterator itO = out.iterator();
            while (itO.hasNext()) {
                JSONObject output = (JSONObject) itO.next();
                //System.err.println(output.get("script"));
                if (output.get("script").toString().startsWith("6a40" + new String(Hex.encode(hash))))  {
                    System.err.println("Valor d'OPRETURN = " + output.get("script"));
                    System.err.println("Valor de K1 = " + output.get("script").toString().substring(68));
                    //En el cas de que la tx no estiqui a cap block donam el pes de block = 0
                    if (level.has("block_height")){
                        txBlock = level.getLong("block_height");
                    } else {txBlock = 0L;}

                    System.err.println("Block TX del OpReturn = " + txBlock);
                    Long confirmacions = ((currentBlock - txBlock + 1) <= currentBlock) ? (currentBlock - txBlock + 1) : 0L;
                    if ((confirmacions>=depth)){ return Hex.decode(output.get("script").toString().substring(68)); }
                    else {
                        informationalAlert("Confirmacions insuficients","La transacció té "+ confirmacions +" confirmacions.\nPer obtenir validesa són necessaries " + depth + " confirmacions.");
                        break;
                    }
                }
            }
        }
     return null;
    }

    @FXML public void initialize() throws IOException { connectionLabel.setText(enviaCorreu.auth()); }
}



