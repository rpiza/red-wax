package com.problemeszero.mail;

import com.google.common.primitives.Bytes;
import com.problemeszero.crypto.RedWaxUtils;
import com.problemeszero.crypto.RedWaxSec;
import com.problemeszero.redwax.Main;

import com.problemeszero.redwax.RedWaxMessage;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;

import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.bouncycastle.cms.CMSException;
import org.bouncycastle.mail.smime.SMIMEException;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.util.encoders.Hex;

import javax.mail.MessagingException;
import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import java.util.*;

import static com.problemeszero.redwax.utils.GuiUtils.informationalAlert;
import static com.problemeszero.time.TimePickerController.selectTimeAlert;

public class MailEditController {

    @FXML private ComboBox<String> cmbTYPE;
    @FXML protected TextField tto;
    @FXML protected TextField tsub;
    @FXML protected TextArea ttext;
    @FXML private Button btn1;
    @FXML private Button btnClose;

//    @FXML private TextField tUN, tPW;
    @FXML private Label connectionLabel;

    public Main.OverlayUI overlayUI;

    public static LocalDateTime deadTime;
    private RedWaxSendMail enviaCorreu = new RedWaxSendMail();


   // public void LayEditController(){
  //  }

    @FXML
    protected void handleConfigButtonAction(ActionEvent actionEvent)  throws Exception {
        //actiontarget.setText("Sign in button pressed");
        //anar a l'altre finestra'
        Properties prop = enviaCorreu.getProp();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("mailconf.fxml"));
            //Carregam les propietats a l'escena mailconf.fxml
            prop.stringPropertyNames().forEach(key -> loader.getNamespace().put(key, prop.getProperty(key)));

            loader.load();
            Parent p = loader.getRoot();

            Stage stage = new Stage();
            stage.setTitle("Configuració SMTP (no persistent)");
            stage.initOwner(btn1.getScene().getWindow());
//            stage.setScene(new Scene((Parent) loader.load()));
            stage.setScene(new Scene(p));

            MailConfController controller = loader.getController();
            controller.setSendMailSmtp(enviaCorreu);

            // showAndWait will block execution until the window closes...
            stage.showAndWait();



        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @FXML protected void handleSendButton(ActionEvent e) throws IOException {
        // Create a multipart message on hi afegim les diferents parts del missatge (COS del correu, fitxer xifrat, deadTime,
        // la clau K2 signada amb PubKey de'n Bob, addrAlice)
        RedWaxSMime missatgeAlice = new RedWaxSMime();
        RedWaxMessage rwmAlice = new RedWaxMessage();

        if (!enviaCorreu.isAuth()) {
            informationalAlert( "Error en l'autenticació SMTP", "Revisa la configuració del compte SMTP.\n" +
                "No és possible connectar adequadement amb el servidor SMTP: " + enviaCorreu.getHost());
        } else {
            if (!tto.getText().isEmpty()) {
                System.err.println("####################################################################################################################");
                System.err.println("########################## PHASE I - Delivery");
                System.err.println("####################################################################################################################");
                System.err.println("########################## Step 1: Alice envia el missatge certificat a n'en Bob");
                System.err.println("####################################################################################################################");
                 try {
                    try {
                        X509Certificate bobCert;
                        X509Certificate aliceCert;

//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//////// ///////////////////////////////////// Cream les diferents parts dels missatge  //////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
                        // Set text message part
                        missatgeAlice.addPartToCem(ttext.getText(),"mailBody");
                        System.err.println("####################################################################################################################");
                        ///////////////Seleccionam el document a xifrar ######################################################
                        ////            certFile[0] conte IV i certFile[1] el fitxer xifrat
                        System.err.println("Es genera K = K1 XOR K2");
                        RedWaxSec aes = new RedWaxSec(); //quan incialitzam RedWaxSec, es crea K, K1 i es calcula K2 mitjançant XOR
                        System.err.println("Obtenim el fitxer a xifrar M");
                        byte[][] certFile = aes.cbcEncrypt(RedWaxUtils.fileToByte(elegirFitxer("Document a xifrar:", new File(Main.appProps.getProperty("Fitxers")))));
                        //byte[][] certFile ={new byte[16], aes.fileToByte("Document a xifrar:")};
                        System.err.println("Es genera C(M)");
                        System.err.println("IV: " + new String(Base64.getEncoder().encode(certFile[0])));
                        System.err.println("Fitxer: " + new String(Base64.getEncoder().encode(certFile[1])));
                        //Concatenam el fitxer amb IV
                        certFile[1] = Bytes.concat(certFile[0],certFile[1]);  //Adjuntam l'IV al fitxer
                        // Part two is attachment
                        missatgeAlice.addPartToCem(certFile[1],"fitxerXifrat");
                        rwmAlice.setCertFile(certFile);
                        System.err.println("####################################################################################################################");

        /////////////////////////////////////////////////////////////////////////////////////////////////
        //            aes.byteToFile(certFile[1]);
        //            aes.byteToFile(aes.cbcDecrypt(certFile[0], certFile[1]));
        //            aes.generateK2();
        //            aes.obtainK();
        /////////////////////////////////////////////////////////////////////////////////////////////////

                        // Introduir la marca de temps que volem donar per publicar la transaccio al blockchain
                        // Carregam la finestra
                        System.err.println("S'especifica el Deadline Time");
                        selectTimeAlert(rwmAlice,"Selecciona el periode de validesa","");
                        System.err.println("Deadline Time: " + new Date(rwmAlice.getDeadTimeMillis()) + " " + rwmAlice.getDeadTimeMillis());
                        //Part three is deadTime
                        missatgeAlice.addPartToCem(Long.toString(rwmAlice.getDeadTimeMillis()),"deadTime");
                        System.err.println("####################################################################################################################");

        //////////////////////////////////////////////////////////////////////////////////////////////////
        //////////////Xifram amb la pubKey de Bob la part de k2 #######################################################################

                        //Carregar la clau Publica de Bob
                        System.err.println("Xifram K2 amb la clau publica d'en Bob");
                        bobCert = RedWaxUtils.readCertificate(RedWaxUtils.fileToString(elegirFitxer("Introdueix el certificat de'n Bob", new File(Main.appProps.getProperty("Certificats")))));
                        byte[] kPrima = aes.wrapWithPubKey(bobCert.getPublicKey(),aes.getK2());
                        System.err.println("k2 (Hex): " + new String(Hex.encode(aes.getK2()))); //k2 en hexadecimal
                        System.err.println("kPrima (Hex): " +  new String(Hex.encode(kPrima))); //kprima en hexadecimal
                        //Part four is K2 signed wiht Bob's PubKey
                        missatgeAlice.addPartToCem(new String(Hex.encode(kPrima)),"kPrima");
                        rwmAlice.setK2(aes.getK2());
                        rwmAlice.setkPrima(kPrima);
                        rwmAlice.setK1(aes.getK1());
                        System.err.println("####################################################################################################################");

    ///////////////////////////////////////////////////////////////////////////////////////////
    //////////////// Obtenim l'adreca de Bitcoin de n'Alice ##############################################################
                        //Hem hagut de fer la classe ClickableBitcoinAddress static. La variable address i el metode addressProperty tambe
                        rwmAlice.setAddrAlice(Main.getUTXOAddress().toString());
                        //rwmAlice.setAddrAlice(Main.bitcoin.wallet().currentChangeAddress().toString());
                        System.err.println("Addr d'Alice: " + rwmAlice.getAddrAlice());
                        //Part five is Alice address
                        missatgeAlice.addPartToCem(rwmAlice.getAddrAlice(),"addrAlice");
                        System.err.println("####################################################################################################################");
        //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
                        rwmAlice.setCemCT(missatgeAlice.getCem().getContentType());

                        // Preparam per a signar el missatge. A missatgeAlice tenim el multipart que forma el CEM.
                        // Signam el cem i actualitzam el missatgeAlice amb el mPart i el signedPart de l'objecte RedWaxSMime retornat per la funcio de signatura

                        //Preparam per signar el missatge
                        System.err.println("N'Alice signa el missatge de correu");
                        aliceCert = RedWaxUtils.readCertificate(RedWaxUtils.fileToString(elegirFitxer("Introdueix el certificat de n'Alice", new File(Main.appProps.getProperty("Certificats")))));
                        PrivateKey priKeyAlice = RedWaxUtils.readPrivateKey(RedWaxUtils.fileToString(elegirFitxer("Selecciona la clau Privada de n'ALice", new File(Main.appProps.getProperty("Certificats")))));
                        //Signam el missatge i del RedWaxSMime retornat actualitzam les parts de missatgeAlice
                        missatgeAlice.setmPartAndSignedPart(missatgeAlice.createSignedMultipart(priKeyAlice,aliceCert));

                        //comprovam que la signatura es correcta
                        missatgeAlice.verifySignedMultipart();
                        System.err.println("La validacio de la signatura es: " + missatgeAlice.isOkSignatura());
                        System.err.println("Certificat de " + missatgeAlice.getCert().getSubject() + ", expedit per " + missatgeAlice.getCert().getIssuer() +
                                ". Vàlid des de " + missatgeAlice.getCert().getNotBefore() +  " fins a"  + missatgeAlice.getCert().getNotAfter()+ ".");

                        //Calculam el hash del cem
                        System.err.println("HashCEM = " + new String(Hex.encode(missatgeAlice.getHashCem("SHA256"))));
                        rwmAlice.setCem(missatgeAlice.PartToBAOS(missatgeAlice.getCem()));
                        rwmAlice.setHashCem(missatgeAlice.getHashCem("SHA256"));
                        System.err.println("Signatura: "+ missatgeAlice.getSignaturaBase64());
                        rwmAlice.setMailSign(missatgeAlice.getSignaturaBytes());

                     } catch (GeneralSecurityException  | SMIMEException | IOException | OperatorCreationException | CMSException ex) {
                            ex.printStackTrace();
                     }
                        rwmAlice.setTo(tto.getText());
                        rwmAlice.setSubject(tsub.getText());

                        enviaCorreu.mailToBob(rwmAlice,missatgeAlice);
                        informationalAlert("Enviat missatge certificat", "N'Alice ha enviat el missatge a ne'n Bob\n\n" +
                             "Nom del certificat: " + missatgeAlice.getCert().getSubject() + "\nExpedit per: " + missatgeAlice.getCert().getIssuer() + "\n" +
                             "Vàlid des de " + missatgeAlice.getCert().getNotBefore().toLocaleString() + " fins a " + missatgeAlice.getCert().getNotAfter().toLocaleString());

                        System.err.println("################### Contingut del fitxer xml, que dona persistència a les dades del missatge enviat per n'Alice");
                        System.err.println("####################################################################################################################");
                        rwmAlice.redWaxToPersistent(RedWaxUtils.getRedWaxFile(elegirFitxer("Nom del fitxer \"xml\"",new File(Main.appProps.getProperty("RedWax")),true)));
                        System.err.println("####################################################################################################################");
                        System.err.println("####################################################################################################################");

                 } catch (NullPointerException | ClassCastException ex) {
                    informationalAlert("Alguna cosa no ha anat bé", "Mira el log de l'aplicació per obtenir més informació");
                    ex.printStackTrace();
                } catch (MessagingException ex) {
                    throw new RuntimeException(ex);
                }

            } else informationalAlert( "No és pot enviar el missatge", "Has d'afegir almanco un destinatari!!!");
        }
    }

    @FXML public void initialize() throws IOException {
        connectionLabel.setText(enviaCorreu.auth());
//        connectionLabel.textProperty().bind(enviaCorreu.authLabel);
    }

    public void closeClicked(ActionEvent actionEvent) {
        //overlayUI.done();
        btnClose.getScene().getWindow().hide();
    }

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