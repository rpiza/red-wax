package com.problemeszero.mail;

import com.problemeszero.crypto.RedWaxSec;
import com.problemeszero.crypto.RedWaxUtils;
import com.problemeszero.redwax.Main;
import com.problemeszero.redwax.RedWaxMessage;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.util.Callback;
import org.bouncycastle.cms.CMSException;
//import org.bouncycastle.crypto.InvalidWrappingException;
import org.bouncycastle.mail.smime.SMIMEException;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.util.encoders.Hex;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.mail.*;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.*;

import static com.problemeszero.redwax.JsonReader.readJsonArrayFromUrl;
import static com.problemeszero.redwax.JsonReader.readJsonFromUrl;
import static com.problemeszero.redwax.utils.GuiUtils.informationalAlert;

public class BobController {
    public ListView<RedWaxMessage> redWaxList;
    private RedWaxSendMail enviaCorreu = new RedWaxSendMail();
    private RedWaxReceiveMail rebreImap = new RedWaxReceiveMail();
    @FXML
    Button closeButton;
    @FXML private Label connectionLabel;
    @FXML private Label connectionLabelIMAP;

    public void onConfirmClicked(ActionEvent actionEvent) {

        System.err.println("Index del missatge triat: " + redWaxList.getFocusModel().getFocusedIndex());
        if (redWaxList.getFocusModel().getFocusedIndex() == -1)  {
            informationalAlert("Atenció!!!!!", "Pitja el botó \"Carrega\" i després selecciona el missatge que vulguis.");
            return ;
        }

        boolean okSignatura = false;
        String no = "";
        RedWaxMessage rwm = (RedWaxMessage) redWaxList.getFocusModel().getFocusedItem();
       // rwm.redWaxToPersistent();
        RedWaxSMime missatgeAlice = new RedWaxSMime(rwm.getMailSignedMultiPart());

        //validar la signatura del correu rebut
        try {
            missatgeAlice.verifySignedMultipart();
        } catch (GeneralSecurityException | OperatorCreationException | CMSException | SMIMEException | MessagingException e) {
            e.printStackTrace();
        }
        okSignatura = missatgeAlice.isOkSignatura();


        System.err.println("####################################################################################################################");
        System.err.println("########################## Step 2: NRR1 enviat per en Bob");
        System.err.println("####################################################################################################################");
        System.err.println("La validació de la signatura del correu rebut és: " + okSignatura );
        System.err.println("Certificat de " + missatgeAlice.getCert().getSubject() + ", expedit per " + missatgeAlice.getCert().getIssuer()+
                ". Vàlid des de \"" + missatgeAlice.getCert().getNotBefore().toLocaleString() + "\" fins a \"" +
                missatgeAlice.getCert().getNotAfter().toLocaleString()+ "\".");
        no = "";
        if (!okSignatura) {no = "NO ";}
        informationalAlert("Validació de la signatura","En Bob determina que la signatura del missatge enviat per n'Alice " + no + "és correcta\n\n" +
                "Nom del certificat: " + missatgeAlice.getCert().getSubject() +"\nExpedit per: " + missatgeAlice.getCert().getIssuer() + "\n" +
                "Vàlid des de " + missatgeAlice.getCert().getNotBefore().toLocaleString() + " fins a " + missatgeAlice.getCert().getNotAfter().toLocaleString() +".");



        //envia confirmacio
        String titol;
        String hText;
        if (!okSignatura) {
            titol = "Signatura NO vàlida";
            hText = "La validació de la signatura del missatge no és correcta.\n\n" +
                    "Això pot ser degut a canvis realitzats pel servidor IMAP/POP3 a les capseleres MIME.\n" +
                    "Detectat amb els servidors de GMAIL.";

        } else{
                titol = "Envia  la prova de recepció (NRR1)";
                hText = null;
        }
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(titol);
        alert.setHeaderText(hText);
        alert.setContentText("Vols enviar el missatge NRR1 a n'Alice?");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.get() == ButtonType.OK){

            // Calculam el hash del cem que signara en Bob, simplement per mostrar-ho per pantalla
            try {
                System.err.println("HashCEM = " + new String(Hex.encode(missatgeAlice.getHashCem("SHA256"))));
            } catch (GeneralSecurityException | IOException | MessagingException e) {
                e.printStackTrace();
            }


            //Preparam per signar el missatge
            X509Certificate bobCert;
            RedWaxSMime missatgeBob;

            try {
                bobCert = RedWaxUtils.readCertificate(RedWaxUtils.fileToString(elegirFitxer("Introdueix el certificat de'n Bob", new File(Main.appProps.getProperty("Certificats")))));
                PrivateKey priKeyBob = RedWaxUtils.readPrivateKey(RedWaxUtils.fileToString(elegirFitxer("Selecciona la clau Privada de'n Bob", new File(Main.appProps.getProperty("Certificats")))));
//                Signam el missatge
                missatgeBob = missatgeAlice.createSignedMultipart(priKeyBob, bobCert);

//                //Guardar el bodypart de la signatura a un fitxer
//                try {
//                    System.err.println(java.util.Arrays.toString(Smime.PartToBAOS(mPart)));
//                    Smime.byteToFile(Smime.PartToBAOS(bPart), "Guardar signaturaCem",new File(Main.appProps.getProperty("Fitxers")));
//                } catch (IOException | MessagingException e) {
//                   e.printStackTrace();
//                }

                System.err.println("Signatura: " + missatgeBob.getSignaturaBase64());

                //comprovam que la signatura es correcta
                missatgeBob.verifySignedMultipart();
                System.err.println("La validació de la signatura del correu NRR1 d'en Bob és: " + missatgeBob.isOkSignatura());
                System.err.println("Certificat de " + missatgeBob.getCert().getSubject() + ", expedit per " + missatgeBob.getCert().getIssuer() +
                        ". Vàlid des de \"" + missatgeBob.getCert().getNotBefore().toLocaleString() + "\" fins a \"" +
                        missatgeBob.getCert().getNotAfter().toLocaleString() + "\".");

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
                enviaCorreu.mailToAlice(rwm, missatgeBob.getmPart());
                informationalAlert("Enviat missatge NRR1", "En Bob ha enviat el missatge NRR1 a n'Alice\n\n" +
                        "Nom del certificat: " + missatgeBob.getCert().getSubject() + "\nExpedit per: " + missatgeBob.getCert().getIssuer() + "\n" +
                        "Vàlid des de " + missatgeBob.getCert().getNotBefore().toLocaleString() + " fins a " + missatgeBob.getCert().getNotAfter().toLocaleString());
            } catch (AuthenticationFailedException e) {
                informationalAlert("Error d'autenticació SMTP", e.getMessage()+ "\n\nEl missatge de NRR1 NO s'ha enviat!!");
            } catch (ClassCastException | NullPointerException e) {
                informationalAlert("Alguna cosa no ha anat bé", "Mira el log de l'aplicació per obtenir més informació");
                e.printStackTrace();
            } catch (IOException | GeneralSecurityException | CMSException | MessagingException | OperatorCreationException | SMIMEException e) {
                e.printStackTrace();
            }
        }
    }

    public void onRefreshClicked(ActionEvent actionEvent) {
        redWaxList.getItems().clear();
        List<RedWaxMessage> list = new ArrayList<RedWaxMessage>();
        try {
            rebreImap.doit(list, "redWax");
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

    public void onDecryptClicked(ActionEvent actionEvent) {

        System.err.println("Index del missatge triat: " + redWaxList.getFocusModel().getFocusedIndex());
        if (redWaxList.getFocusModel().getFocusedIndex() == -1)  {
            informationalAlert("Atenció!!!!!", "Pitja el botó \"Carrega\" i després selecciona el missatge que vulguis.");
            return ;
        }

        RedWaxMessage rwm = (RedWaxMessage) redWaxList.getFocusModel().getFocusedItem();
//        byte[] hashCem = null;
        byte[] opRe_K1 = null;


        System.err.println("####################################################################################################################");
        System.err.println("########################## PHASE III: En Bob amb el hashCEM del correu seleccionat més l'adreça de n'Alice\n" +
                "########################## cerca a a l'explorer del blockchain una tx on l'OPRETURN contengui el hashCEM.\n" +
                "########################## A més la tx ha de complir el requisit de confirmacions configurat al\n" +
                "########################## fitxer configuration.xml. Per defecte aquest valor\n" +
                "########################## és 0 i la tx es troba immediatament");
        System.err.println("####################################################################################################################");

        RedWaxSMime missatgeAlice = new RedWaxSMime(rwm.getMailSignedMultiPart());
        missatgeAlice.cemToRwm(rwm);
        //Obtenim l'adreça Bitcoin del correu enviat per Alice
        String addrAlice = rwm.getAddrAlice();
        System.err.println("addrAlice: " + addrAlice);

        //obtenir les transaccions de l addrAlice i el valor d' OPRETURN, conectant-nos a l'api de blockchain.info
        RedWaxSec aes = new RedWaxSec();
        try {
            opRe_K1 = obtenirOpreturn(addrAlice,missatgeAlice.getHashCem("SHA256"));

            if (opRe_K1!= null) { aes.setK1(opRe_K1); }
            else {
                informationalAlert("No hem trobat el Hash(cem)!!!" ,"Per a l'adreça " + addrAlice + ",\n" +
                        "no hem trobat cap Tx amb un OPRETURN que contengui\n" +
                        "el hash=" + new String(Hex.encode(missatgeAlice.getHashCem("SHA256"))));
                return; }
        } catch (GeneralSecurityException | MessagingException | IOException e) {
            e.printStackTrace();
        }

        //Carrergam la clau privada de Bob per dexifrar kPrima
        PrivateKey priKeyBob = null;
        try {
            priKeyBob = RedWaxUtils.readPrivateKey(RedWaxUtils.fileToString(elegirFitxer("Selecciona la clau Privada de'n Bob",new File(Main.appProps.getProperty("Certificats")))));
            aes.setK2(aes.unwrapWithPrivKey(priKeyBob,Hex.decode(rwm.getkPrima())));

            System.err.println("Del valor K' recupertat del correu de n'Alice obtenim que\n" +
                    "K2 = " + new String(Hex.encode(aes.getK2())));
            informationalAlert("\nObtinguts els valors de K1 i K2" ,"De l'OPRETURN de l'adreça " + addrAlice + ", hem obtingut\n" +
                    "K1="+new String(Hex.encode(opRe_K1)) + ".\n\nDel valor de K' enviat en el missatge de n'Alice, hem obtingut\n" +
                    "K2=" + new String(Hex.encode(aes.getK2())));
            aes.obtainK();
            String missatge = "Molt bé!!! Has obtingut el missatge certificat!!!";
            try {
                RedWaxUtils.byteToFile(aes.cbcDecrypt(rwm.getCertFile()[0], rwm.getCertFile()[1]), elegirFitxer("Guardar el fitxer desxifrat",new File(Main.appProps.getProperty("Fitxers")),true));
            } catch (FileNotFoundException | NullPointerException e) {
                e.printStackTrace();
                missatge = "Ohhh!! El missatge certificat no s'ha guardat";
            }
            informationalAlert(missatge,"");

            System.err.println("####################################################################################################################");
            System.err.println("####################################################################################################################");
        } catch (ClassCastException | NullPointerException e){
            informationalAlert("Alguna cosa no ha anat bé", "Mira el log de l'aplicació per obtenir més informació");
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
//        } catch (IOException | CertificateException | InvalidWrappingException e) {
//            e.printStackTrace();
//        }
    }

    private byte[] obtenirOpreturn(String addr, byte[] hash){

        JSONObject json = null;
        JSONObject doc = null;
        Long currentBlock = 0L;
        Long txBlock = 0L;
        int depth = Integer.parseInt(Main.appProps.getProperty("depth")); //nombre de confirmacions necessaries per acceptar la tx
        String []  clau  = Main.appProps.getProperty("keys_api").split(",");
        System.err.println("HashCEM en Hex = " + new String(Hex.encode(hash)));
        System.err.println("El nombre de confirmacions necessaries per acceptar la tx és de " + depth);
        //String  clau [][] = { {"txs","hash","outputs","script","block_height"}, {"","txid","vout","scriptpubkey","block_height"}}; // {{blockcypher.com},{blockstream.info}}


        try {
            //
            // S'HA D'ANALITZAR LES RESPOSTES DE LES APIS PER EVITAR XSS
            //
            // json = readJsonFromUrl("https://testnet.blockchain.info/rawaddr/" + addr);
            // json = readJsonFromUrl("http://api.blockcypher.com/v1/btc/test3/addrs/" + addr + "/full");

////////////////**************http://api.blockcypher.com/v1/btc/test3/addrs/******************  ////////////////////////////////////////////////////////
//       //     int w = 0;  //blockcypher.com
//           json = readJsonFromUrl(Main.appProps.getProperty("api_addr") + addr + Main.appProps.getProperty("api_addr_sufix"));
//           System.err.println("URI de cerca de les txs de l'adreça de n'Alice: " + Main.appProps.getProperty("api_addr") + addr + Main.appProps.getProperty("api_addr_sufix"));
//            System.err.println("Claus del json = " + Arrays.toString(clau));
//           JSONArray txs = json.getJSONArray(clau[0]);
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


            // Per obtenir json d'una transaccio: https://testnet.blockchain.info/rawtx/$tx_hash
            //currentBlock = readJsonFromUrl("https://testnet.blockchain.info/q/getblockcount"); //https://testnet.blockchain.info/latestblock"
            //doc = Jsoup.connect("https://testnet.blockchain.info/q/getblockcount").get();

            //Obtenim el valor del darrer bloc de la cadena per calcular el nombre de confirmacions de la nostra transaccio
            //doc = readJsonFromUrl("https://testnet.blockchain.info/latestblock");

//            doc = readJsonFromUrl(Main.appProps.getProperty("api_currentBlock"));
//            currentBlock = Long.valueOf(doc.get("height").toString()); //Long.valueOf(doc.body().text());
//            System.err.println("URI de cerca del darrer bloc la xarxa blockchain: "+ Main.appProps.getProperty("api_currentBlock") + "\n" + "Darrer bloc = " + currentBlock );


//////////////////////////////*****************https://blockstream.info/testnet/api/address/************************************///////////////////////////////////
            // int w = 1;   //blockstream.info
             JSONArray txs = readJsonArrayFromUrl(Main.appProps.getProperty("api_addr") + addr + Main.appProps.getProperty("api_addr_sufix"));
             System.err.println("URI de cerca de les txs de l'adreça de n'Alice: " + Main.appProps.getProperty("api_addr") + addr + Main.appProps.getProperty("api_addr_sufix"));
             System.err.println("Claus del json = " + Arrays.toString(clau));
             ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
            String tx;
            Iterator itT = txs.iterator();
            while (itT.hasNext()) {
                JSONObject level = (JSONObject) itT.next();
                tx = level.get(clau[1]).toString();
                JSONArray out = level.getJSONArray(clau[2]);
                Iterator itO = out.iterator();
                while (itO.hasNext()) {
                    JSONObject output = (JSONObject) itO.next();
                    //System.err.println(output.get(clau[w][3]));
                    if (output.get(clau[3]).toString().startsWith("6a40" + new String(Hex.encode(hash))))  {
                        String op = output.get(clau[3]).toString();
                        System.err.println("Trobat el HashCEM a la tx: "+ tx +  "\namb el valor d'OPRETURN: " + op);
                        System.err.println("Obtenim el valor de K1 = " + op.substring(68));
                        //En el cas de que la tx no estiqui a cap block donam el pes de block = 0
                        if (level.has(clau[4])){
                            JSONObject status = level.getJSONObject(clau[4]);
                            if ("true".equals(status.get("confirmed").toString())){
                               txBlock = status.getLong(clau[5]);}
                        } else {txBlock = 0L;}

                        System.err.println("Block TX del OpReturn = " + txBlock);
                        return Hex.decode(op.substring(68));
//                        Long confirmacions = ((currentBlock - txBlock + 1) <= currentBlock) ? (currentBlock - txBlock + 1) : 0L;
//                        if ((confirmacions>=depth)){ return Hex.decode(op.substring(68)); }
//                        else {
//                            informationalAlert("Confirmacions insuficients","La transacció té "+ confirmacions +" confirmacions.\nPer obtenir validesa són necessaries " + depth + " confirmacions.");
//                            break;
//                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @FXML public void initialize() throws IOException { connectionLabel.setText(enviaCorreu.auth()); connectionLabelIMAP.setText(rebreImap.auth()); }

    private Path elegirFitxer(String title, File dir, Boolean guardaFitxer) {
        FileChooser FC = new FileChooser();
        FC.setTitle(title);
        FC.setInitialDirectory(dir);
        if (guardaFitxer)
            return Paths.get(FC.showSaveDialog(Main.instance.stage).getAbsolutePath());
        else
            return Paths.get(FC.showOpenDialog(Main.instance.stage).getAbsolutePath());
    }
    private Path elegirFitxer(String title, File dir) {
        return elegirFitxer(title, dir, false);
    }
}