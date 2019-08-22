package com.problemeszero.mail;

import com.problemeszero.crypto.AESCrypto2;
import com.problemeszero.crypto.Pem;
import com.problemeszero.crypto.Smime;
import com.problemeszero.redwax.Main;
import com.problemeszero.redwax.RedWaxMessage;
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
import javax.activation.DataSource;
import javax.crypto.Cipher;
import javax.mail.*;
import javax.mail.internet.*;
import javax.mail.util.ByteArrayDataSource;
import java.io.*;
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
    @FXML private Label connectionLabelIMAP;

    public void onConfirmClicked(ActionEvent actionEvent) {

        System.err.println("Index del missatge triat: " + redWaxList.getFocusModel().getFocusedIndex());
        if (redWaxList.getFocusModel().getFocusedIndex() == -1)  {
            informationalAlert("Atenció!!!!!", "Has de seleccionar una de les línies");
            return ;
        }


        boolean okSignatura = false;
        String no = "";
        RedWaxMessage rwm = (RedWaxMessage) redWaxList.getFocusModel().getFocusedItem();
       // rwm.redWaxToPersistent();

        //validar la signatura del correu rebut
        ContentType cType = new ContentType("multipart", "signed", null);
        cType.setParameter("boundary", obtenir_boundary(rwm.getMailSignedMultiPart()));

        DataSource dataSource = new ByteArrayDataSource(Smime.tractar_smtp(rwm.getMailSignedMultiPart()),cType.toString());
//        System.err.println(java.util.Arrays.toString(rwm.getMailSignedMultiPart()));
        MimeMultipart mPart = null;
        try {
            mPart = new MimeMultipart(dataSource);

//            try {
//                System.err.println(java.util.Arrays.toString(Smime.PartToBAOS(mPart)));
//                Smime.byteToFile(Smime.PartToBAOS(mPart), "Guardar cemSignat",new File(Main.appProps.getProperty("Fitxers")));
//            } catch (IOException | MessagingException e) {
//               e.printStackTrace();
//            }

            //comprovam que la signatura es correcta
            okSignatura = Smime.verifySignedMultipart(mPart);

        } catch (GeneralSecurityException |  OperatorCreationException | CMSException | SMIMEException | MessagingException e) {
            e.printStackTrace();
        }
        System.err.println("####################################################################################################################");
        System.err.println("########################## Step 2: NRR enviat per en Bob");
        System.err.println("####################################################################################################################");
        System.err.println("La validació de la signatura del correu rebut és: " + okSignatura );
        no = "";
        if (!okSignatura) {no = "NO ";}
        informationalAlert("Validació de la signatura","En Bob determina que la signatura del missatge enviat per n'Alice " + no + "és correcta");

//        try {
//            System.err.println(java.util.Arrays.toString(Smime.PartToBAOS(mPart)));
//            Smime.byteToFile(Smime.PartToBAOS(mPart), "Guardar cemSignat",new File(Main.appProps.getProperty("Fitxers")));
//        } catch (IOException | MessagingException e) {
//            e.printStackTrace();
//        }

        //envia confirmacio
        String titol;
        String hText;
        if (!okSignatura) {
            titol = "Signatura NO vàlida";
            hText = "La validació de la signatura del missatge no és correcta.\n\n" +
                    "Això pot ser degut a canvis realitzats pel servidor SMTP a les capseleres MIME.\n" +
                    "Detectat amb els servidors de GMAIL.";

        } else{
                titol = "Envia no rebuig";
                hText = null;
        }
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(titol);
        alert.setHeaderText(hText);
        alert.setContentText("Vols enviar el missatge de no rebuig a n'Alice?");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.get() == ButtonType.OK){
            // ... user chose OK

//       if (okSignatura) {

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


//            try {
//               System.err.println(java.util.Arrays.toString(Smime.PartToBAOS(bodyPart)));
//               Smime.byteToFile(Smime.PartToBAOS(bodyPart), "Guardar cem recuperat per Bob",new File(Main.appProps.getProperty("Fitxers")));
//            } catch (IOException | MessagingException e) {
//                e.printStackTrace();
//            }

            // Calculam el hash del cem que signara en Bob, simplement per mostrar-ho per pantalla
            try {
                System.err.println("HashCEM = " + new String(Hex.encode(Smime.calculateDigest(rwm.getCem()))));
            } catch (GeneralSecurityException e) {
                e.printStackTrace();
            }


            //Preparam per signar el missatge
            X509Certificate bobCert = null;
            try {
                bobCert = Pem.readCertificate(Pem.fileToString("Introdueix el certificat de'n Bob",new File(Main.appProps.getProperty("Certificats"))));
                PrivateKey priKeyBob = Pem.readPrivateKey(Pem.fileToString("Selecciona la clau Privada de'n Bob",new File(Main.appProps.getProperty("Certificats"))));
                //Obtendrem el missatge signat a un MultiPart, que contendra el CEM i el missatge signat
                //Signam el missatge
                mPart = Smime.createSignedMultipart(priKeyBob,bobCert,bodyPart);

                //Obtenim el MimeBodyPart de la signatura del missatge. Es la segona part del multipart resultat del proces de sigantura
                MimeBodyPart bPart = (MimeBodyPart) mPart.getBodyPart(1);

//                //Guardar el bodypart de la signatura a un fitxer
//                try {
//                    System.err.println(java.util.Arrays.toString(Smime.PartToBAOS(mPart)));
//                    Smime.byteToFile(Smime.PartToBAOS(bPart), "Guardar signaturaCem",new File(Main.appProps.getProperty("Fitxers")));
//                } catch (IOException | MessagingException e) {
//                   e.printStackTrace();
//                }


                // Obtenir la signatura base64. Sense les capsaleres del bodyPart
                ByteArrayOutputStream out = null;
                InputStream in = null;
                out = new ByteArrayOutputStream();
                in = bPart.getInputStream();
                int k;
                while ((k = in.read()) != -1) {
                    out.write(k);
                }
                out.close();
                in.close();
                System.err.println("Signatura: "+ new String(Base64.getEncoder().encode(out.toByteArray())));


                //comprovam que la signatura es correcta
                System.err.println("La validació de la signatura del correu NRR d'en Bob és: " + Smime.verifySignedMultipart(mPart));
            } catch (IOException | GeneralSecurityException | CMSException | MessagingException | OperatorCreationException | SMIMEException e) {
                e.printStackTrace();
            }
            System.err.println("####################################################################################################################");
            System.err.println("####################################################################################################################");

//            try {
//               System.err.println(java.util.Arrays.toString(Smime.PartToBAOS(mPart)));
//               Smime.byteToFile(Smime.PartToBAOS(mPart), "Guardar cem recuperat per Bob",new File(Main.appProps.getProperty("Fitxers")));
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

        System.err.println("Index del missatge triat: " + redWaxList.getFocusModel().getFocusedIndex());
        if (redWaxList.getFocusModel().getFocusedIndex() == -1)  {
            informationalAlert("Atenció!!!!!", "Has de seleccionar una de les línies");
            return ;
        }

        RedWaxMessage rwm = (RedWaxMessage) redWaxList.getFocusModel().getFocusedItem();
        byte[] hashCem = null;


        System.err.println("####################################################################################################################");
        System.err.println("########################## PHASE III: En Bob amb el hashCEM del correu seleccionat més l'adreça de n'Alice\n" +
                "########################## cerca a a l'explorer del blockchain una tx on l'OPRETURN contengui el hashCEM.\n" +
                "########################## A més la tx ha de complir el requisit de confirmacions configurat al\n" +
                "########################## fitxer configuration.xml. Per defecte aquest valor\n" +
                "########################## és 0 i la tx es troba immediatament");
        System.err.println("####################################################################################################################");

        //Obtenim l'adreça Bitcoin del correu enviat per Alice
        String addrAlice = rwm.getAddrAlice();
        System.err.println("addrAlice: " + addrAlice);

        //obtenim el hashCem
        try {
            hashCem = Smime.calculateDigest(rwm.getCem());
            //System.err.println("HashCEM = " + new String(Hex.encode(hashCem)));
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
        System.err.println("Del valor K' recupertat del correu de n'Alice obtenim que\n" +
                "K2 = " + new String(Hex.encode(aes.getK2())));
        informationalAlert("\nObtinguts els valors de K1 i K2" ,"De l'OPRETURN de l'adreça " + addrAlice + ", hem obtingut\n" +
                "K1="+new String(Hex.encode(opRe)) + ".\n\nDel valor de K' enviat en el missatge de n'Alice, hem obtingut\n" +
                "K2=" + new String(Hex.encode(aes.getK2())));
        aes.obtainK();
        Smime.byteToFile(aes.cbcDecrypt(rwm.getCertFile()[0], rwm.getCertFile()[1]),"Guardar el fitxer desxifrat",new File(Main.appProps.getProperty("Fitxers")));
        informationalAlert("Obtingut el fitxer desxifrat","");

        System.err.println("####################################################################################################################");
        System.err.println("####################################################################################################################");

    }

    private byte[] obtenirOpreturn(String addr, byte[] hash){

        JSONObject json = null;
        JSONObject doc = null;
        Long currentBlock = 0L;
        Long txBlock = 0L;
        int depth = Integer.parseInt(Main.appProps.getProperty("depth")); //nombre de confirmacions necessaries per acceptar la tx

        System.err.println("HashCEM en Hex = " + new String(Hex.encode(hash)));
        System.err.println("El nombre de confirmacions necessaries per acceptar la tx és de " + depth);

        try {
            //
            // S'HA D'ANALITZAR LES RESPOSTES DE LES APIS PER EVITAR XSS
            //
//            json = readJsonFromUrl("https://testnet.blockchain.info/rawaddr/" + addr);
//            json = readJsonFromUrl("http://api.blockcypher.com/v1/btc/test3/addrs/" + addr + "/full");
          json = readJsonFromUrl(Main.appProps.getProperty("api_addr") + addr + Main.appProps.getProperty("api_addr_sufix"));
          System.err.println("URI de cerca de les txs de l'adreça de n'Alice: " + Main.appProps.getProperty("api_addr") + addr + Main.appProps.getProperty("api_addr_sufix"));
            // Per obtenir json d'una transaccio: https://testnet.blockchain.info/rawtx/$tx_hash
            //currentBlock = readJsonFromUrl("https://testnet.blockchain.info/q/getblockcount"); //https://testnet.blockchain.info/latestblock"
            //doc = Jsoup.connect("https://testnet.blockchain.info/q/getblockcount").get();

            //Obtenim el valor del darrer bloc de la cadena per calcular el nombre de confirmacions de la nostra transaccio
//            doc = readJsonFromUrl("https://testnet.blockchain.info/latestblock");
            doc = readJsonFromUrl(Main.appProps.getProperty("api_currentBlock"));
            currentBlock = Long.valueOf(doc.get("height").toString()); //Long.valueOf(doc.body().text());
            System.err.println("URI de cerca del darrer bloc la xarxa blockchain: "+ Main.appProps.getProperty("api_currentBlock") + "\n" + "Darrer bloc = " + currentBlock );

        } catch (IOException e) {
            e.printStackTrace();
        }
//        System.err.println("******************************************");
//        System.err.println(json.toString());
//        System.err.println("HashCEM en Hex = " + new String(Hex.encode(hash)));
        //Obtenir el darrer block de la cadena
//        System.err.println("Block Actual = " + doc.body().text());
//        System.err.println("URI de cerca del darrer bloc la xarxa blockchain: "+ Main.appProps.getProperty("api_currentBlock") + "\n" + "Darrer bloc = " + currentBlock );
//        System.err.println("******************************************");
        //System.err.println("Block Actual = " + currentBlock.toString());

        String tx;
        JSONArray txs = json.getJSONArray("txs");
        Iterator itT = txs.iterator();
        while (itT.hasNext()) {
            JSONObject level = (JSONObject) itT.next();
            tx = level.get("hash").toString();
            JSONArray out = level.getJSONArray("outputs");
            Iterator itO = out.iterator();
            while (itO.hasNext()) {
                JSONObject output = (JSONObject) itO.next();
                //System.err.println(output.get("script"));
                if (output.get("script").toString().startsWith("6a40" + new String(Hex.encode(hash))))  {
                    System.err.println("Trobat el HashCEM a la tx: "+ tx +  "\namb el valor d'OPRETURN: " + output.get("script"));
                    System.err.println("Obtenim el valor de K1 = " + output.get("script").toString().substring(68));
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

    @FXML public void initialize() throws IOException { connectionLabel.setText(enviaCorreu.auth()); connectionLabelIMAP.setText(auth_bustia()); }

    public String auth_bustia() throws IOException {
        String   UN, PW, host, port, proto;

        host = Main.appProps.getProperty("imap.servidor");
        port = Main.appProps.getProperty("imap.port");
        proto = Main.appProps.getProperty("imap.protocol");
        UN = Main.appProps.getProperty("imap.usuari");
        PW = Main.appProps.getProperty("imap.contrasenya");

        boolean auth = chk_bustia(UN, PW, host,port,proto);
        String s="";

        if(!auth) {
            s="ALERTA: "+ proto + " KO. Connexió no satifactòria. Revisau la configuració " + proto;
        } else {
            s= proto + " OK. Credencials correctes";
        }
        System.out.println(s + " - " + UN );
        return s;
    }

    private  boolean chk_bustia(String UN, String PW, String host, String port, String proto) {
        Session sesh;
        try {
            sesh = Session.getInstance(Main.appProps, new javax.mail.Authenticator() {
                protected PasswordAuthentication getPasswordAuthentication() {return new PasswordAuthentication(UN, PW);}
            });
            Store store = sesh.getStore(proto);
            store.connect(host, UN, PW);
            System.out.println("Connexio IMAP/POP3 correcte");
            return true;
        } catch (AuthenticationFailedException e) {
            System.out.println("AuthenticationFailedException - for authentication failures");
//            e.printStackTrace();
            return false;
        } catch (MessagingException e) {
            System.out.println("IMAP/POP3 connection error - for other failures");
//            e.printStackTrace();
            return false;
        }
    }

}
