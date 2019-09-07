package com.problemeszero.mail;

import com.google.common.primitives.Bytes;
import com.problemeszero.crypto.AESCrypto2;
import com.problemeszero.crypto.Pem;
import com.problemeszero.crypto.Smime;
import com.problemeszero.redwax.Main;
import com.problemeszero.redwax.RedWaxMessage;
import javafx.beans.property.StringProperty;
import org.bouncycastle.cms.CMSException;
import org.bouncycastle.mail.smime.SMIMEException;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.util.encoders.Hex;
import javax.mail.*;
import javax.mail.internet.*;
import java.io.*;
import java.security.*;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Date;
import java.util.Properties;

import static com.problemeszero.redwax.utils.GuiUtils.informationalAlert;
import static com.problemeszero.time.TimePickerController.selectTimeAlert;


public class SendMailSmtp {

    private Session sesh;
//    String mto, mhead, msub, cTYPE, cTEXT;
    private  String   UN, PW, host, port, proto;
   // private Properties prop = new Properties();
    private boolean auth = false;
    public RedWaxMessage messageObject;
    public StringProperty authLabel;

    public SendMailSmtp() {

        //messageObject conte les diferents part de la transaccio (K,K1,K2,CEM,from,subject, .....)
        messageObject = new RedWaxMessage();
        System.err.println("Llegint el fitxer de propietats configuration.xml per a smtp");

        host = Main.appProps.getProperty("servidor");
        port = Main.appProps.getProperty("port");
        proto = Main.appProps.getProperty("protocol");
        UN = Main.appProps.getProperty("usuari");
        PW = Main.appProps.getProperty("contrasenya");
    }

    public Properties getProp() {
        return Main.appProps;
    }

    public void setProp(Properties prop) {
        Main.appProps = prop;
    }

//  Nomes per SMTP
    public String auth() throws IOException {
        auth = chk();
        String s="";

        if(!auth) {
            s="ALERTA: SMTP KO. Connexió no satifactòria. Revisau la configuració SMTP";
        } else {
            s="SMTP OK. Credencials correctes";
        }
        System.out.println(s + " - " + UN) ;
//        authLabel.setValue(s);
        return s;
    }

    public boolean isAuth() {
        return auth;
    }

    public String getUN() {
        return UN;
    }

    public String getHost() {
        return host;
    }

    public String getPort() {
        return port;
    }

    public String getProto() {
        return proto;
    }

    public void setUN(String UN) {
        this.UN = UN;
    }

    public void setPW(String PW) {
        this.PW = PW;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public void setPort(String port) {
        this.port = port;
    }

    private  boolean chk() {

        Main.appProps.put("mail.smtp.auth", "true");
        if(host.equals("smtp.gmail.com") || host.equals("ssl0.ovh.net")){
            Main.appProps.put("mail.smtp.starttls.enable", "true");
        }
        Main.appProps.put("mail.smtp.host", host);
        Main.appProps.put("mail.smtp.port", port);

        if(port.equals("465") & !host.equals("smtp.gmail.com")) {
            System.out.println("Connectat a un SMTP diferent de gmail");
            Main.appProps.put("mail.smtp.ssl.enable", "true"); }

        try {
            sesh = Session.getInstance(Main.appProps, new javax.mail.Authenticator() {
                protected PasswordAuthentication getPasswordAuthentication() {return new PasswordAuthentication(UN, PW);}
            });
            Transport transport = sesh.getTransport(proto);
            transport.connect(host, Integer.parseInt(port), UN, PW);
            transport.close();
            System.out.println("Connexio SMTP correcte");
            return true;
        } catch (AuthenticationFailedException e) {
            System.out.println("AuthenticationFailedException - for authentication failures");
//            e.printStackTrace();
            return false;
        } catch (MessagingException e) {
            System.out.println("SMTP connection error - for other failures");
//            e.printStackTrace();
            return false;
        }
    }

    public void mail(String to, String sub, String cont) {
        X509Certificate bobCert = null;
        X509Certificate aliceCert;

        try {
            System.out.println("\n \n>> ?" + cont);
            System.out.println("\n \n>> ?" + to);

            Message m = new MimeMessage(sesh);
            m.setFrom(new InternetAddress(UN));
            m.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
            m.setSubject("REDWAX - " + sub);
            m.setSentDate(new Date());

            //Hem de posar un ID qeu faci referencia al missatge,receptor,asumpte, data
            messageObject.setId("ID0000001");
            messageObject.setFrom(InternetAddress.toString(m.getFrom()));
            messageObject.setTo(InternetAddress.toString(m.getReplyTo()));
            messageObject.setSentDate(m.getSentDate());

            // Create a multipart message on hi afegim les diferents parts del missatge (COS del correu, fitxer xifrat, deadTime, la clau K2 signada amb PubKey de'n Bob, addrAlice)
            RedWaxSMime missatgeAlice = new RedWaxSMime();

            // Set text message part
            missatgeAlice.addPartToCem(cont,"mailBody");
            System.err.println("####################################################################################################################");
            System.err.println("########################## PHASE I - Delivery");
            System.err.println("####################################################################################################################");
            System.err.println("########################## Step 1: Alice envia el missatge certificat a n'en Bob");
            System.err.println("####################################################################################################################");

            System.err.println("Es genera K = K1 XOR K2");
            AESCrypto2 aes = new AESCrypto2(); //quan incialitzam AESCrypto2, es crea K, K1 i es calcula K2 mitjançant XOR
            System.err.println("####################################################################################################################");
///////////////Seleccionam el document a xifrar ######################################################
            ////            certFile[0] conte IV i certFile[1] el fitxer xifrat
            System.err.println("Obtenim el fitxer a xifrar M");
            byte[][] certFile = aes.cbcEncrypt(Smime.fileToByte("Document a xifrar:", new File(Main.appProps.getProperty("Fitxers"))));
            //byte[][] certFile ={new byte[16], aes.fileToByte("Document a xifrar:")};
            System.err.println("Es genera C(M)");
            System.err.println("IV: " + new String(Base64.getEncoder().encode(certFile[0])));
            System.err.println("Fitxer: " + new String(Base64.getEncoder().encode(certFile[1])));
            System.err.println("####################################################################################################################");
            //Concatenam el fitxer amb IV
            certFile[1] = Bytes.concat(certFile[0],certFile[1]);  //Adjuntam l'IV al fitxer

            // Part two is attachment
            missatgeAlice.addPartToCem(certFile[1],"fitxerXifrat");
            messageObject.setCertFile(certFile);

/////////////////////////////////////////////////////////////////////////////////////////////////
//            aes.byteToFile(certFile[1]);
//            aes.byteToFile(aes.cbcDecrypt(certFile[0], certFile[1]));
//            aes.generateK2();
//            aes.obtainK();
/////////////////////////////////////////////////////////////////////////////////////////////////

            // Introduir la marca de temps que volem donar per publicar la transaccio al blockchain
            // Carregam la finestra
            System.err.println("S'especifica el Deadline Time");
            selectTimeAlert(messageObject,"Selecciona el periode de validesa","");
            System.err.println("Deadline Time: " + new Date(messageObject.getDeadTimeMillis()) + " " + messageObject.getDeadTimeMillis());
            System.err.println("####################################################################################################################");
            //Part three is deadTime
            missatgeAlice.addPartToCem(Long.toString(messageObject.getDeadTimeMillis()),"deadTime");


//////////////////////////////////////////////////////////////////////////////////////////////////
//////////////Xifram amb la pubKey de Bob la part de k2 #######################################################################

            try {
//              Carregar la clau Publica de Bob
                System.err.println("Xifram K2 amb la clau publica d'en Bob");
                bobCert = Pem.readCertificate(Pem.fileToString("Introdueix el certificat de'n Bob", new File(Main.appProps.getProperty("Certificats"))));
                byte[] kPrima = aes.signPubKey(bobCert.getPublicKey(),aes.getK2());
                System.err.println("k2 (Hex): " + new String(Hex.encode(aes.getK2()))); //k2 en hexadecimal
                System.err.println("kPrima (Hex): " +  new String(Hex.encode(kPrima))); //kprima en hexadecimal
//                System.err.println("k2: " + java.util.Arrays.toString(aes.getK2())); //k2 com string de bytes
//                System.err.println("kPrima: " + java.util.Arrays.toString(kPrima)); //kprima com string de bytes
//                System.err.println("kPrima: " + java.util.Arrays.toString(Hex.decode(Hex.encode(kPrima)))); //kprima com string de bytes
                //Part four is K2 signed wiht Bob's PubKey
                missatgeAlice.addPartToCem(new String(Hex.encode(kPrima)),"kPrima");
                messageObject.setK2(aes.getK2());
                messageObject.setkPrima(kPrima);
                messageObject.setK1(aes.getK1());
                System.err.println("####################################################################################################################");

            } catch (GeneralSecurityException | IOException e) {
                e.printStackTrace();
            }
///////////////////////////////////////////////////////////////////////////////////////////
//////////////// Obtenim l'adreca de Bitcoin de n'Alice ##############################################################
            //Hem hagut de fer la classe ClickableBitcoinAddress static. La variable address i el metode addressProperty tambe
            System.err.println("Obtenim l'adreça de n'Alice:");
            messageObject.setAddrAlice(Main.bitcoin.wallet().currentChangeAddress().toString());

            System.err.println("Addr d'Alice: " + messageObject.getAddrAlice());
            //Part five is Alice address
            missatgeAlice.addPartToCem(messageObject.getAddrAlice(),"addrAlice");
            System.err.println("####################################################################################################################");

//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//            System.out.println("Multipart Content-Type= " +  multipart.getContentType());
            messageObject.setCemCT(missatgeAlice.getCem().getContentType());

//            try {
//                Smime.byteToFile(Smime.PartToBAOS(multipart), "Guardar multipart", new File(Main.appProps.getProperty("Fitxers")));
//            } catch (IOException e) {
//                e.printStackTrace();
//            }

            // Preparam per a signar el missatge, per aixo convertim multiPart amb un MimeBodyPart, que es el que signarem
            try {
                //Preparam per signar el missatge
                System.err.println("N'Alice signa el missatge de correu");
                aliceCert = Pem.readCertificate(Pem.fileToString("Introdueix el certificat de n'Alice", new File(Main.appProps.getProperty("Certificats"))));
                PrivateKey priKeyAlice = Pem.readPrivateKey(Pem.fileToString("Selecciona la clau Privada de n'ALice", new File(Main.appProps.getProperty("Certificats"))));
                //Signam el missatge
                missatgeAlice.setmPartAndSignedPart(missatgeAlice.createSignedMultipart(priKeyAlice,aliceCert));

                //comprovam que la signatura es correcta
                missatgeAlice.verifySignedMultipart();
                System.err.println("La validacio de la signatura es: " + missatgeAlice.isOkSignatura());
                System.err.println("Certificat de " + missatgeAlice.getCert().getSubject() + ", expedit per " + missatgeAlice.getCert().getIssuer() +
                        ". Vàlid des de " + missatgeAlice.getCert().getNotBefore() +  " fins a"  + missatgeAlice.getCert().getNotAfter()+ ".");

                System.err.println("HashCEM = " + new String(Hex.encode(missatgeAlice.getHashCem("SHA256"))));
                //System.err.println("HashCEM = " + java.util.Arrays.toString(hashCem));

                messageObject.setCem(Smime.PartToBAOS(missatgeAlice.getCem()));
//                Smime.byteToFile(cem, "Guardar cem", new File(Main.appProps.getProperty("Fitxers")));
                messageObject.setHashCem(missatgeAlice.getHashCem("SHA256"));
                System.err.println("Signatura: "+ missatgeAlice.getSignaturaBase64());
                messageObject.setMailSign(missatgeAlice.getSignaturaBytes());

                //Afegim el mPart com a part del contingut del missatge de correu
                m.setContent(missatgeAlice.getmPart(),missatgeAlice.getmPart().getContentType());
                //Afegim una capçalera que permet identificar les correus
                m.setHeader("Content-ID","redWax");

                System.err.println("####################################################################################################################");
                System.err.println("####################################################################################################################");

            } catch (GeneralSecurityException | SMIMEException | IOException | OperatorCreationException | CMSException e) {
                e.printStackTrace();
            }

            Transport t = sesh.getTransport(proto);
            System.out.println(">> ? smtp(s) ---> ## " + t.getURLName() + " \n>> ?");
            Transport.send(m);
            informationalAlert("Missatge de correu enviat", "El missatge de correu s'ha enviat satifactòriament al destinatari: " + messageObject.getTo());
            //Guardam a un xml l'objecte redwaxmessage

            System.err.println("################### Contingut del fitxer xml, que dona persistència a les dades del missatge enviat per n'Alice");
            System.err.println("####################################################################################################################");
            messageObject.redWaxToPersistent();
            System.err.println("####################################################################################################################");

        } catch (NullPointerException | ClassCastException e) {
            informationalAlert("Alguna cosa no ha anat bé", "Mira el log de l'aplicació per obtenir més informació");
            e.printStackTrace();
        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }

    }

    public void mail(RedWaxMessage rwm, Multipart mPart) throws AuthenticationFailedException, MessagingException{
        Message m = new MimeMessage(sesh);

        m.setFrom(new InternetAddress(UN));
        m.setSubject("REDWAX NRR - " + rwm.getSubject().substring(9));
        m.setRecipients(Message.RecipientType.TO, InternetAddress.parse(rwm.getFrom()));
        m.setSentDate(new Date());
        m.setContent(mPart, mPart.getContentType());
        m.setHeader("Content-ID", "redWax-NRR");
        Transport t = sesh.getTransport(proto);

        System.out.println(">> ? smtp(s) ---> ## " + t.getURLName() + " \n>> ?");

        t.send(m);
    }


//    public byte[] wrapKey(AsymmetricRSAPublicKey pubKey, byte[] inputKeyBytes) throws PlainInputProcessingException{
//        FipsRSA.KeyWrapOperatorFactory wrapFact = new FipsRSA.KeyWrapOperatorFactory();
//        FipsKeyWrapperUsingSecureRandom wrapper = (FipsKeyWrapperUsingSecureRandom) wrapFact.createKeyWrapper( pubKey,FipsRSA.WRAP_OAEP).withSecureRandom(CryptoServicesRegistrar.getSecureRandom());
//        return wrapper.wrap(inputKeyBytes, 0, inputKeyBytes.length);
//    }


}
