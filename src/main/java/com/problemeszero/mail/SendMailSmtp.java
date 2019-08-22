package com.problemeszero.mail;

import com.google.common.primitives.Bytes;
import com.problemeszero.crypto.AESCrypto2;
import com.problemeszero.crypto.Pem;
import com.problemeszero.crypto.Smime;
import com.problemeszero.redwax.Main;
import com.problemeszero.redwax.RedWaxMessage;
import com.problemeszero.redwax.controls.ClickableBitcoinAddress;
import javafx.beans.binding.StringExpression;
import jdk.nashorn.internal.runtime.RewriteException;
import org.bouncycastle.cms.CMSException;
import org.bouncycastle.crypto.CryptoServicesRegistrar;
import org.bouncycastle.crypto.KeyUnwrapperUsingSecureRandom;
import org.bouncycastle.crypto.PlainInputProcessingException;
import org.bouncycastle.crypto.asymmetric.AsymmetricRSAKey;
import org.bouncycastle.crypto.asymmetric.AsymmetricRSAPublicKey;
import org.bouncycastle.crypto.fips.FipsKeyUnwrapperUsingSecureRandom;
import org.bouncycastle.crypto.fips.FipsKeyWrapperUsingSecureRandom;
import org.bouncycastle.crypto.fips.FipsRSA;
import org.bouncycastle.jcajce.provider.BouncyCastleFipsProvider;
import org.bouncycastle.mail.smime.SMIMEException;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.util.encoders.Hex;
import org.spongycastle.crypto.params.AsymmetricKeyParameter;
import org.spongycastle.crypto.util.PublicKeyFactory;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.crypto.Cipher;
import javax.mail.*;
import javax.mail.internet.*;
import javax.mail.util.ByteArrayDataSource;
import java.io.*;
import java.security.*;
import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Base64;
import java.util.Date;
import java.util.Enumeration;
import java.util.Properties;

import static com.problemeszero.time.TimePickerController.selectTimeAlert;
import static java.lang.Thread.sleep;
import static javafx.beans.binding.Bindings.convert;

public class SendMailSmtp {

    private Session sesh;
    String mto, mhead, msub, cTYPE, cTEXT;
    private  String   UN, PW, host, port, proto;
   // private Properties prop = new Properties();
    public RedWaxMessage messageObject;

    public SendMailSmtp() {

        //messageObject conte les diferents part de la transaccio (K,K1,K2,CEM,from,subject, .....)
        messageObject = new RedWaxMessage();
        System.err.println("Llegint el fitxer de propietats configuration.xml per a smtp");
//        try {
//            InputStream in = new FileInputStream("configuration.xml");
//            prop.loadFromXML(in);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
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


    public String auth() throws IOException {
        boolean auth = chk(UN, PW);
        String s="";

        if(!auth) {
            s="ALERTA: SMTP KO. Connexió no satifactòria. Revisau la configuració SMTP";
        } else {
            s="SMTP OK. Credencials correctes";
        }
        System.out.println(s + " - " + UN) ;
        return s;
    }


    private  boolean chk(String UN, String PW) {

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

//            System.out.println("\n \n>> ?" + cont);
//            System.out.println(java.util.Arrays.toString(cont.getBytes()));

            //AFEGIM UN CR (13) ABANS DEL LF (10) al missatge body del correu
            System.out.println("El valor de line_break es: " + Main.appProps.getProperty("line_break"));
            if ( Boolean.valueOf(Main.appProps.getProperty("line_break"))) { cont =linebreak(cont);}
//            System.out.println(java.util.Arrays.toString(cont.getBytes()));
            System.out.println("\n \n>> ?" + cont);
            System.out.println("\n \n>> ?" + to);

            Message m = new MimeMessage(sesh);
            m.setFrom(new InternetAddress(UN));
            m.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
            m.setSubject(sub);
            m.setSentDate(new Date());

            //Hem de posar un ID qeu faci referencia al missatge,receptor,asumpte, data
            messageObject.setId("ID0000001");
            messageObject.setFrom(InternetAddress.toString(m.getFrom()));
            messageObject.setTo(InternetAddress.toString(m.getReplyTo()));
            messageObject.setSentDate(m.getSentDate());

            // Create the message part
            BodyPart messageBodyPart = new MimeBodyPart();

            // Now set the actual message
            messageBodyPart.setText(cont);
            messageBodyPart.setHeader("Content-ID","mailBody");

            // Create a multipart message on hi afegim les diferents parts del missatge (COS del correu, fitxer xifrat, deadTime, la clau K2 signada amb PubKey de'n Bob, addrAlice)
            Multipart multipart = new MimeMultipart();

            // Set text message part
            multipart.addBodyPart(messageBodyPart);
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
            messageBodyPart = new MimeBodyPart();
            DataSource dataSource = new ByteArrayDataSource(certFile[1], "application/octet-stream");
            messageBodyPart.setDataHandler(new DataHandler(dataSource));
            messageBodyPart.setFileName("Document_Certificat");
            // messageBodyPart.setHeader("Content-Type", "image/jpeg");
            messageBodyPart.setHeader("Content-ID","fitxerXifrat");
            // messageBodyPart.setDisposition(MimeBodyPart.INLINE);
            // messageBodyPart.setFileName("logo inline image");
            multipart.addBodyPart(messageBodyPart);
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
            messageBodyPart = new MimeBodyPart();
            messageBodyPart.setText(Long.toString(messageObject.getDeadTimeMillis()));
            messageBodyPart.setHeader("Content-ID","deadTime");
            multipart.addBodyPart(messageBodyPart);


//////////////////////////////////////////////////////////////////////////////////////////////////
//////////////Xifram amb la pubKey de Bob la part de k2 #######################################################################

            try {
//              Carregar la clau Publica de Bob
                //LLANÇAR UN MISSATGE DEMANANT EL CERTIFICAT DE BOB
                System.err.println("Xifram K2 amb la clau publica d'en Bob");
                bobCert = Pem.readCertificate(Pem.fileToString("Introdueix el certificat de'n Bob", new File(Main.appProps.getProperty("Certificats"))));

              // AsymmetricKeyParameter pubKey = PublicKeyFactory.createKey(bobCert.getPublicKey());
                AsymmetricRSAPublicKey rsaPubKey = new AsymmetricRSAPublicKey(FipsRSA.ALGORITHM, bobCert.getPublicKey().getEncoded());
               // AsymmetricRSAPrivateKey rsaPrivKey =   new AsymmetricRSAPrivateKey(FipsRSA.ALGORITHM, kp.getPrivate().getEncoded());

                byte[] kPrima = Smime.wrapKey(rsaPubKey,aes.getK2());
                System.err.println("k2 (Hex): " + new String(Hex.encode(aes.getK2()))); //k2 en hexadecimal
                System.err.println("kPrima (Hex): " +  new String(Hex.encode(kPrima))); //kprima en hexadecimal
//                System.err.println("k2: " + java.util.Arrays.toString(aes.getK2())); //k2 com string de bytes
//                System.err.println("kPrima: " + java.util.Arrays.toString(kPrima)); //kprima com string de bytes
//                System.err.println("kPrima: " + java.util.Arrays.toString(Hex.decode(Hex.encode(kPrima)))); //kprima com string de bytes
                //Part four is K2 signed wiht Bob's PubKey
                messageBodyPart = new MimeBodyPart();
                messageBodyPart.setText(new String(Hex.encode(kPrima)));
                messageBodyPart.setHeader("Content-ID","kPrima");
                multipart.addBodyPart(messageBodyPart);
                messageObject.setK2(aes.getK2());
                messageObject.setkPrima(kPrima);
                messageObject.setK1(aes.getK1());
                System.err.println("####################################################################################################################");
/////////////////////////////////////////////////////////////////////////////////////////////
////              Carrergam la clau privada de Bob
//                //Hex.decode(new String(Hex.encode(kPrima)));
//                PrivateKey priKeyBob = Pem.readPrivateKey(Pem.fileToString("Selecciona la clau Privada de'n Bob", new File(Main.appProps.getProperty("Certificats"))));
//                Cipher dec = Cipher.getInstance("RSA", "BCFIPS");
//                dec.init(Cipher.DECRYPT_MODE, priKeyBob);
//                byte[] pla = dec.doFinal(Hex.decode(Hex.encode(kPrima)));
//                System.err.println("k2: " + java.util.Arrays.toString(pla)); //k2 com string de bytes
//                System.err.println("k2: " + new String(Hex.encode(pla))); //k2 en hexadecimal
////////////////////////////////////////////////////////////////////////////////////////////
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
            messageBodyPart = new MimeBodyPart();
            messageBodyPart.setText(messageObject.getAddrAlice());
            messageBodyPart.setHeader("Content-ID","addrAlice");
            multipart.addBodyPart(messageBodyPart);
            System.err.println("####################################################################################################################");

//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//            System.out.println("Multipart Content-Type= " +  multipart.getContentType());
            messageObject.setCemCT(multipart.getContentType());

//            try {
//                Smime.byteToFile(Smime.PartToBAOS(multipart), "Guardar multipart", new File(Main.appProps.getProperty("Fitxers")));
//            } catch (IOException e) {
//                e.printStackTrace();
//            }

            // Preparam per a signar el missatge, per aixo convertim multiPart amb un MimeBodyPart, que es el que signarem
            try {
//                ByteArrayOutputStream bodyPartBaos = new ByteArrayOutputStream();
                javax.mail.internet.MimeBodyPart bodyPart = new MimeBodyPart();
                bodyPart.setContent(multipart);

//                Smime.byteToFile(Smime.PartToBAOS(bodyPart), "Guardar bodypartCem",new File(Main.appProps.getProperty("Fitxers")));

                //Preparam per signar el missatge
                System.err.println("N'Alice signa el missatge de correu");
                aliceCert = Pem.readCertificate(Pem.fileToString("Introdueix el certificat de n'Alice", new File(Main.appProps.getProperty("Certificats"))));
                PrivateKey priKeyAlice = Pem.readPrivateKey(Pem.fileToString("Selecciona la clau Privada de n'ALice", new File(Main.appProps.getProperty("Certificats"))));
                //MimeMultipart mPart;
                // Obtendrem el missatge signat a un MultiPart, que contendra el CEM i el missatge signat
                //Signam el missatge
                MimeMultipart mPart = Smime.createSignedMultipart(priKeyAlice,aliceCert,bodyPart);
//
                //comprovam que la signatura es correcta
                try {
                    System.err.println("La validacio de la signatura es: " + Smime.verifySignedMultipart(mPart));
                } catch (CMSException e) {
                    e.printStackTrace();
                }

                //Guardar a un fitxer tot el multipart : cem + signatura
                //mPart  conte dos MimeBobyPart: 1r que es el cem i el 2n que es el missatge signat

//                System.err.println(java.util.Arrays.toString(Smime.PartToBAOS(mPart)));
//                Smime.byteToFile(Smime.PartToBAOS(mPart), "Guardar cemSignat", new File(Main.appProps.getProperty("Fitxers")));

                //Obtenim el Cem del multipart signat. Es el primera part del multipart resultat del proces de signatura
                Multipart multi=(Multipart) mPart.getBodyPart(0).getContent();
                byte [] cem = Smime.PartToBAOS(multi);

                //cream el hash del missatge cem
                byte[] hashCem = Smime.calculateDigest(cem); //SHA2-256 - Tambe pot ser SHA2-384
                System.err.println("HashCEM = " + new String(Hex.encode(hashCem)));
                //System.err.println("HashCEM = " + java.util.Arrays.toString(hashCem));
                messageObject.setCem(cem);
//                Smime.byteToFile(cem, "Guardar cem", new File(Main.appProps.getProperty("Fitxers")));
                messageObject.setHashCem(hashCem);

                //Obtenim el MimeBodyPart de la signatura del missatge. Es la segona part del multipart resultat del proces de sigantura
                MimeBodyPart bPart = (MimeBodyPart) mPart.getBodyPart(1);

                //Guardar el bodypart de la signatura a un fitxer
//                Smime.byteToFile(Smime.PartToBAOS(bPart), "Guardar signaturaCem",new File(Main.appProps.getProperty("Fitxers")));

                // Obtenir la signatura base64. Sense les capsaleres del bodyPart
                ByteArrayOutputStream out = null;
                InputStream in = null;
                out = new ByteArrayOutputStream();
                in = bPart.getInputStream();
                int k;
                while ((k = in.read()) != -1) {
                    out.write(k);
                }
                System.err.println("Signatura: "+ new String(Base64.getEncoder().encode(out.toByteArray())));
                messageObject.setMailSign(out.toByteArray());
                out.close();
                in.close();

                //System.err.println(mPart.getContentType());
                //Afegim el mPart com a part del contingut del missatge de correu
                m.setContent(mPart,mPart.getContentType());
                //Afegim una capçalera que permet identificar les correus
                m.setHeader("Content-ID","redWax");

                //parseSignedMultipart(m.getContent());
                System.err.println("####################################################################################################################");
                System.err.println("####################################################################################################################");

            } catch (GeneralSecurityException | SMIMEException | IOException | OperatorCreationException e) {
                e.printStackTrace();
            }

//            System.out.println("\n \n \n \t >> ??????? " + m.getContentType());
//            System.out.println("\n \n \n \t >> ??????? " + m.getDataHandler());
//            System.out.println("\n \n \n \t >> ??????? " + m.getSubject());

            Transport t = sesh.getTransport(proto);
            System.out.println(">> ? smtp(s) ---> ## " + t.getURLName() + " \n>> ?");
            Transport.send(m);
            //Guardam a un xml l'objecte redwaxmessage

            System.err.println("################### Contingut del fitxer xml, que dona persistència a les dades del missatge enviat per n'Alice");
            System.err.println("####################################################################################################################");
            messageObject.redWaxToPersistent();
            System.err.println("####################################################################################################################");

        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }

    }

    public void mail(RedWaxMessage rwm, Multipart mPart) {
        Message m = new MimeMessage(sesh);

        try {
            m.setFrom(new InternetAddress(UN));
            m.setSubject("RE:" + rwm.getSubject());
            m.setRecipients(Message.RecipientType.TO, InternetAddress.parse(rwm.getFrom()));
            m.setSentDate(new Date());
            m.setContent(mPart, mPart.getContentType());
            m.setHeader("Content-ID", "redWax-NRR");
            Transport t = sesh.getTransport(proto);

            System.out.println(">> ? smtp(s) ---> ## " + t.getURLName() + " \n>> ?");

            t.send(m);
        } catch (MessagingException  e) {
            e.printStackTrace();
        }

    }
    //Afegeix un CR al final de linia.
    //A l'hora de comparar els cem a ReadConfirmationController dona error.
    //Sense haver investigat molt, sembla ser que els servidors de correu afegeixen un \r quan troben un \n, quedant \r\n
    //Amb aquesta funcio afegim de serie el \r i aixi la comparacio dels cem no falla
    private String linebreak(String string){
        int j= 0;
        int i=string.indexOf(10);
        String str="";
        while (i>0) {
              System.err.println("Corregim modificació SMPT: Afegim un CR abans de cada NL");
//            System.out.println("i=" + i);
//            System.out.println(string.substring(j, i));
//            System.out.println("length="+string.substring(j, i).length());
            str =  str + string.substring(j, i) + (char) 0x0D + (char) 0x0A;
//            System.out.println("length="+ str.length());
            j=i+1;
            i=string.indexOf(10,j);
        }
        str = str + string.substring(j);
        return str;
    }

//    public byte[] wrapKey(AsymmetricRSAPublicKey pubKey, byte[] inputKeyBytes) throws PlainInputProcessingException{
//        FipsRSA.KeyWrapOperatorFactory wrapFact = new FipsRSA.KeyWrapOperatorFactory();
//        FipsKeyWrapperUsingSecureRandom wrapper = (FipsKeyWrapperUsingSecureRandom) wrapFact.createKeyWrapper( pubKey,FipsRSA.WRAP_OAEP).withSecureRandom(CryptoServicesRegistrar.getSecureRandom());
//        return wrapper.wrap(inputKeyBytes, 0, inputKeyBytes.length);
//    }

    private void parseSignedMultipart(Object m){
        ReceiveMailImap parse = new ReceiveMailImap();
        RedWaxMessage r = new RedWaxMessage();
        try {
            parse.saveParts(m,r);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (MessagingException e) {
            e.printStackTrace();
        }

    }

}
