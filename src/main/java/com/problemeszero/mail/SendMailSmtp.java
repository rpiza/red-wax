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
import java.util.Properties;

import static com.problemeszero.time.TimePickerController.selectTimeAlert;
import static javafx.beans.binding.Bindings.convert;

public class SendMailSmtp {

    private Session sesh;
    String mto, mhead, msub, cTYPE, cTEXT;
    private  String   UN, PW, host, port, proto;
   // private Properties prop = new Properties();
    public RedWaxMessage messageObject;

    public SendMailSmtp() {
        //carregam el fitxer properties
        //llegim fitxer de propietats configuration.xml
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

    public void auth() throws IOException {
        boolean auth = chk(UN, PW);
        if(!auth) {
            System.out.print("Not auth");
            // ESCRIURE a la finestra si l'autenticacio NO es correcte. retornar un string

        } else {
            System.out.print("Auth");
            // ESCRIURE a la finestra si l'autenticacio es correcte. retonrar un string
        }
    }

    private  boolean chk(String UN, String PW) {

        Main.appProps.put("mail.smtp.auth", "true");
        if(host.equals("smtp.gmail.com") || host.equals("ssl0.ovh.net")){
            Main.appProps.put("mail.smtp.starttls.enable", "true");
        }
        Main.appProps.put("mail.smtp.host", host);
        Main.appProps.put("mail.smtp.port", port);
        if(host.equals("ssl0.ovh.net")) { Main.appProps.put("mail.smtp.ssl.enable", "true"); }

        boolean check = true;
        //
        try {
            InternetAddress e = new InternetAddress(UN);
            e.validate();
        } catch (AddressException e) {
            e.getStackTrace();
            check = false;
        }

        if(check) {
            sesh = Session.getInstance(Main.appProps,new javax.mail.Authenticator() {
                protected PasswordAuthentication getPasswordAuthentication() {return new PasswordAuthentication(UN, PW);}
            });
        }

        return check;
    }


    public void mail(String to, String sub, String cont) {
        X509Certificate bobCert = null;
        X509Certificate aliceCert;

        try {

            System.out.println("\n \n>> ?" + mto);
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

            // Create a multipart message
            Multipart multipart = new MimeMultipart();

            // Set text message part
            multipart.addBodyPart(messageBodyPart);

            AESCrypto2 aes = new AESCrypto2(); //quan incialitzam AESCrypto2, es crear K, K1 i es calcula K2

///////////////Seleccionam el document a xifrar ######################################################
            ////            certFile[0] conte IV i certFile[1] el fitxer xifrat
            byte[][] certFile = aes.cbcEncrypt(Smime.fileToByte("Document a xifrar:", new File(Main.appProps.getProperty("Fitxers"))));
            //byte[][] certFile ={new byte[16], aes.fileToByte("Document a xifrar:")};
            System.err.println("IV: " + new String(Base64.getEncoder().encode(certFile[0])));
            System.err.println("Fitxer: " + new String(Base64.getEncoder().encode(certFile[1])));

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
            selectTimeAlert(messageObject,"Selecciona el periode de validesa","");
            System.err.println("Dead Time: " + new Date(messageObject.getDeadTimeMillis()) + " " + messageObject.getDeadTimeMillis());
            messageBodyPart = new MimeBodyPart();

            messageBodyPart.setText(Long.toString(messageObject.getDeadTimeMillis()));
            messageBodyPart.setHeader("Content-ID","deadTime");
            multipart.addBodyPart(messageBodyPart);


//////////////////////////////////////////////////////////////////////////////////////////////////
//////////////Xifram amb la pubKey de Bob la part de k2 #######################################################################

            try {
//              Carregar la clau Publica de Bob
                //LLANÇAR UN MISSATGE DEMANANT EL CERTIFICAT DE BOB
                bobCert = Pem.readCertificate(Pem.fileToString("Introdueix el certificat de'n Bob", new File(Main.appProps.getProperty("Certificats"))));

                //Cipher enc = Cipher.getInstance("RSA", "BCFIPS");
               // enc.init(Cipher.ENCRYPT_MODE, bobCert.getPublicKey());
               // byte[] kPrima = enc.doFinal(aes.getK2());
              // AsymmetricKeyParameter pubKey = PublicKeyFactory.createKey(bobCert.getPublicKey());
                AsymmetricRSAPublicKey rsaPubKey = new AsymmetricRSAPublicKey(FipsRSA.ALGORITHM, bobCert.getPublicKey().getEncoded());
               // AsymmetricRSAPrivateKey rsaPrivKey =   new AsymmetricRSAPrivateKey(FipsRSA.ALGORITHM, kp.getPrivate().getEncoded());

                byte[] kPrima = Smime.wrapKey(rsaPubKey,aes.getK2());

                System.err.println("k2 (Hex): " + new String(Hex.encode(aes.getK2()))); //k2 en hexadecimal
                System.err.println("kPrima (Hex): " +  new String(Hex.encode(kPrima))); //kprima en hexadecimal

//                System.err.println("k2: " + java.util.Arrays.toString(aes.getK2())); //k2 com string de bytes
//                System.err.println("kPrima: " + java.util.Arrays.toString(kPrima)); //kprima com string de bytes
//                System.err.println("kPrima: " + java.util.Arrays.toString(Hex.decode(Hex.encode(kPrima)))); //kprima com string de bytes
                messageBodyPart = new MimeBodyPart();
                messageBodyPart.setText(new String(Hex.encode(kPrima)));
                messageBodyPart.setHeader("Content-ID","kPrima");
                multipart.addBodyPart(messageBodyPart);
                messageObject.setK2(aes.getK2());
                messageObject.setkPrima(kPrima);
                messageObject.setK1(aes.getK1());
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
            messageObject.setAddrAlice(Main.bitcoin.wallet().currentChangeAddress().toString());

            System.err.println("Addr d'Alice: " + messageObject.getAddrAlice());
            messageBodyPart = new MimeBodyPart();
            messageBodyPart.setText(messageObject.getAddrAlice());
            messageBodyPart.setHeader("Content-ID","addrAlice");
            multipart.addBodyPart(messageBodyPart);
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

            try {
               // Preparam per a signar el missatge, per aixo l'hem de convertir amb un MimeBodyPart
                javax.mail.internet.MimeBodyPart bodyPart = new MimeBodyPart();
                bodyPart.setContent(multipart);

                //Preparam per signar el missatge
                aliceCert = Pem.readCertificate(Pem.fileToString("Introdueix el certificat de n'Alice", new File(Main.appProps.getProperty("Certificats"))));
                PrivateKey priKeyAlice = Pem.readPrivateKey(Pem.fileToString("Selecciona la clau Privada de n'ALice", new File(Main.appProps.getProperty("Certificats"))));
                MimeMultipart mPart; // Obtendrem el missatge signat a un MultiPart, que contendra el CEM i el missatge signat
                //Signam el missatge
                mPart = Smime.createSignedMultipart(priKeyAlice,aliceCert,bodyPart);
//                try {
                    //comprovam que la signatura es correcta
//                    System.err.println("La validacio de la signatura es: ") + Smime.verifySignedMultipart(mPart));
//                } catch (CMSException e) {
//                    e.printStackTrace();
//                }

//                Guardar a un fitxer tot el multipart : cem + signatura
//                ByteArrayOutputStream mPartBaos = new ByteArrayOutputStream();
//                mPart.writeTo(mPartBaos);
//                //System.err.println(java.util.Arrays.toString(mPartBaos.toByteArray()));
//               // aes.byteToFile(mPartBaos.toByteArray(), "Guardar cemSignat");
//                mPartBaos.close();

                //Signar i xifrar el missatge
                //mPart.addBodyPart(Smime.createSignedEncryptedBodyPart(priKeyAlice,aliceCert, bobCert ,bodyPart));

                //Obtenim el Cem del multipart i cream el hash del missatge
                ByteArrayOutputStream bodyPartBaos = new ByteArrayOutputStream();
                Multipart multi=(Multipart) mPart.getBodyPart(0).getContent();
                multi.writeTo(bodyPartBaos);
                //System.err.println(java.util.Arrays.toString(fos.toByteArray()));
                //System.err.println(new String(fos.toByteArray()));
                byte[] hashCem = Smime.calculateDigest(bodyPartBaos.toByteArray()); //SHA2-256 - Tambe pot ser SHA2-384
                //byte[] hashCem = Smime.calculateSha3Digest(fos.toByteArray());  //SHA3-256 - Tambe pot ser SHA2-384
                System.err.println("HashCEM = " + new String(Hex.encode(hashCem)));
//                System.err.println("HashCEM = " + java.util.Arrays.toString(hashCem));

                messageObject.setCem(bodyPartBaos.toByteArray());
                //aes.byteToFile(bodyPartBaos.toByteArray(), "Guardar cem");
                messageObject.setHashCem(hashCem);
                bodyPartBaos.close();

                MimeBodyPart bPart = (MimeBodyPart) mPart.getBodyPart(1);
//                Guardar el bodypart de la signatura a un fitxer
//                bodyPartBaos = new ByteArrayOutputStream();
//                bPart.writeTo(bodyPartBaos);
//                //aes.byteToFile(bodyPartBaos.toByteArray(), "Guardar signaturaCem");
//                bodyPartBaos.close();

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


               // System.err.println(java.util.Arrays.toString(mPart.getContentType().getBytes()));
                m.setContent(mPart,mPart.getContentType());
                m.setHeader("Content-ID","redWax");

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
            messageObject.redWaxToPersistent();

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

//    public byte[] wrapKey(AsymmetricRSAPublicKey pubKey, byte[] inputKeyBytes) throws PlainInputProcessingException{
//        FipsRSA.KeyWrapOperatorFactory wrapFact = new FipsRSA.KeyWrapOperatorFactory();
//        FipsKeyWrapperUsingSecureRandom wrapper = (FipsKeyWrapperUsingSecureRandom) wrapFact.createKeyWrapper( pubKey,FipsRSA.WRAP_OAEP).withSecureRandom(CryptoServicesRegistrar.getSecureRandom());
//        return wrapper.wrap(inputKeyBytes, 0, inputKeyBytes.length);
//    }

}
