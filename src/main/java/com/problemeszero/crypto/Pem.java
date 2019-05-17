package com.problemeszero.crypto;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import com.problemeszero.crypto.EC;
import com.problemeszero.crypto.Cert;
import com.problemeszero.redwax.Main;
import com.problemeszero.redwax.MainController;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.bouncycastle.asn1.nist.NISTObjectIdentifiers;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.openssl.PEMDecryptorProvider;
import org.bouncycastle.openssl.PEMEncryptedKeyPair;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.openssl.jcajce.JcePEMDecryptorProviderBuilder;
import org.bouncycastle.openssl.jcajce.JcePEMEncryptorBuilder;
import org.bouncycastle.operator.InputDecryptorProvider;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.pkcs.PKCS8EncryptedPrivateKeyInfo;
import org.bouncycastle.pkcs.PKCS8EncryptedPrivateKeyInfoBuilder;
import org.bouncycastle.pkcs.PKCSException;
import org.bouncycastle.pkcs.jcajce.JcaPKCS8EncryptedPrivateKeyInfoBuilder;
import org.bouncycastle.pkcs.jcajce.JcePKCSPBEInputDecryptorProviderBuilder;
import org.bouncycastle.pkcs.jcajce.JcePKCSPBEOutputEncryptorBuilder;
import java.security.Security;
import org.bouncycastle.jcajce.provider.BouncyCastleFipsProvider;

import static com.problemeszero.crypto.Cert.*;

public class Pem extends Application {
    static Stage stage;

    public Pem() {}

    @Override
    public void start(Stage primaryStage) {
        stage = primaryStage;
        try {
            crearCert();
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (OperatorCreationException e) {
            e.printStackTrace();
        } catch (PKCSException e) {
            e.printStackTrace();
        }
        primaryStage.setTitle("JavaFX App");

        FileChooser fileChooser = new FileChooser();

        Button button = new Button("Select File");
        button.setOnAction(e -> {
            fileChooser.showOpenDialog(primaryStage);
        });

        VBox vBox = new VBox(button);
        Scene scene = new Scene(vBox, 960, 600);

        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static String writeCertificate(X509Certificate certificate)
            throws IOException
    {
        StringWriter sWrt = new StringWriter();
        JcaPEMWriter pemWriter = new JcaPEMWriter(sWrt);

        pemWriter.writeObject(certificate);

        pemWriter.close();

        return sWrt.toString();
    }

    public static X509Certificate readCertificate(String pemEncoding)
            throws IOException, CertificateException
    {
        PEMParser parser = new PEMParser(new StringReader(pemEncoding));

        X509CertificateHolder certHolder = (X509CertificateHolder)parser.readObject();

        return new JcaX509CertificateConverter().getCertificate(certHolder);
    }

    public static String writePrivateKey(PrivateKey privateKey)
            throws IOException
    {
        StringWriter sWrt = new StringWriter();
        JcaPEMWriter pemWriter = new JcaPEMWriter(sWrt);

        pemWriter.writeObject(privateKey);

        pemWriter.close();

        return sWrt.toString();
    }

    public static PrivateKey readPrivateKey(String pemEncoding)
            throws IOException, CertificateException
    {
        PEMParser parser = new PEMParser(new StringReader(pemEncoding));

        PEMKeyPair pemKeyPair = (PEMKeyPair)parser.readObject();

        return new JcaPEMKeyConverter().getPrivateKey(pemKeyPair.getPrivateKeyInfo());
    }

    public static String writeEncryptedKey(char[] passwd, PrivateKey privateKey)
            throws IOException, OperatorCreationException
    {
        StringWriter sWrt = new StringWriter();
        JcaPEMWriter pemWriter = new JcaPEMWriter(sWrt);

        PKCS8EncryptedPrivateKeyInfoBuilder pkcs8Builder = new JcaPKCS8EncryptedPrivateKeyInfoBuilder(privateKey);

        pemWriter.writeObject(pkcs8Builder.build(new JcePKCSPBEOutputEncryptorBuilder(NISTObjectIdentifiers.id_aes256_CBC).setProvider("BCFIPS").build(passwd)));

        pemWriter.close();

        return sWrt.toString();
    }

    public static PrivateKey readEncryptedKey(char[] password, String pemEncoding)
            throws IOException, OperatorCreationException, PKCSException
    {
        PEMParser parser = new PEMParser(new StringReader(pemEncoding));

        PKCS8EncryptedPrivateKeyInfo encPrivKeyInfo = (PKCS8EncryptedPrivateKeyInfo)parser.readObject();

        InputDecryptorProvider pkcs8Prov = new JcePKCSPBEInputDecryptorProviderBuilder().setProvider("BCFIPS").build(password);

        JcaPEMKeyConverter   converter = new JcaPEMKeyConverter().setProvider("BCFIPS");

        return converter.getPrivateKey(encPrivKeyInfo.decryptPrivateKeyInfo(pkcs8Prov));
    }

    public static String writeEncryptedKeyOpenSsl(char[] passwd, PrivateKey privateKey)
            throws IOException, OperatorCreationException
    {
        StringWriter sWrt = new StringWriter();
        JcaPEMWriter pemWriter = new JcaPEMWriter(sWrt);

        pemWriter.writeObject(privateKey, new JcePEMEncryptorBuilder("AES-256-CBC").setProvider("BCFIPS").build(passwd));

        pemWriter.close();

        return sWrt.toString();
    }

    public static PrivateKey readEncryptedKeyOpenSsl(char[] passwd, String pemEncoding)
            throws IOException, OperatorCreationException
    {
        PEMParser parser = new PEMParser(new StringReader(pemEncoding));

        PEMEncryptedKeyPair pemEncryptedKeyPair = (PEMEncryptedKeyPair)parser.readObject();

        PEMDecryptorProvider pkcs8Prov = new JcePEMDecryptorProviderBuilder().setProvider("BCFIPS").build(passwd);

        JcaPEMKeyConverter   converter = new JcaPEMKeyConverter().setProvider("BCFIPS");

        return converter.getPrivateKey(pemEncryptedKeyPair.decryptKeyPair(pkcs8Prov).getPrivateKeyInfo());
    }

    public static void stringToFile(String output, String title, File dir) {

//        FileChooser1 FC = new FileChooser1();
        FileChooser FC = new FileChooser();
        FC.setTitle(title);
        FC.setInitialDirectory(dir);

//        Path path = Paths.get(FC.chooseFile());
 //       Path path = Paths.get(FC.showSaveDialog(stage).getAbsolutePath());
        Path path = Paths.get(FC.showOpenDialog(Main.instance.stage).getAbsolutePath());
        byte[] strToBytes = output.getBytes();
        try {
            Files.write(path, strToBytes);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

     public static String fileToString(String title, File dir) {

        FileChooser FC = new FileChooser();
        FC.setTitle(title);
        FC.setInitialDirectory(dir);
        String output= null;

         try {
//             output = new String(Files.readAllBytes(Paths.get(FC.chooseFile())));
             output = new String(Files.readAllBytes(Paths.get(FC.showOpenDialog(Main.instance.stage).getAbsolutePath())));

         } catch (IOException e) {
             e.printStackTrace();
         }
        return output;
     }



    public static void main(String[] args)
            throws GeneralSecurityException, IOException, OperatorCreationException, PKCSException {
        launch(args);
    }
    public static void crearCert()  throws GeneralSecurityException, IOException, OperatorCreationException, PKCSException{
        Security.addProvider(new BouncyCastleFipsProvider());

        KeyPair keyPair = Rsa.generateKeyPair();

        KeyPair taKeyPair = Rsa.generateKeyPair();
        KeyPair caKeyPair = Rsa.generateKeyPair();
        KeyPair eeKeyPair = Rsa.generateKeyPair();

        X509Certificate taCert = makeV1RsaCertificate(taKeyPair.getPrivate(), taKeyPair.getPublic());
        X509Certificate caCert = makeV3CACertificate(taCert, taKeyPair.getPrivate(), caKeyPair.getPublic());
        X509Certificate eeCert = makeV3Certificate(caCert, caKeyPair.getPrivate(), eeKeyPair.getPublic(),"Alice");

        X509Certificate certificate = makeV3Certificate(caCert, caKeyPair.getPrivate(), keyPair.getPublic(), "Bob");//makeV1Certificate(keyPair.getPrivate(), keyPair.getPublic());

        char[] keyPass = "keyPassword".toCharArray();

        File dir= new File("/home/set/RedWax/Certificats");

        String pemCertificate = writeCertificate(taCert);
        System.err.println(pemCertificate);
        stringToFile(pemCertificate,"",dir);
        pemCertificate = writeCertificate(caCert);
        stringToFile(pemCertificate,"",dir);
        pemCertificate = writeCertificate(eeCert);
        stringToFile(pemCertificate,"",dir);
        pemCertificate = writeCertificate(certificate);
        stringToFile(pemCertificate, "" ,dir) ;


//        System.err.println(fileToString());
//        //System.err.println("XXXXXXXXXXXX" + certificate.equals(readCertificate(fileToString())));

        System.err.println(certificate.equals(readCertificate(pemCertificate)));

        String pemPrivateKey = writePrivateKey(taKeyPair.getPrivate());
        stringToFile(pemPrivateKey,"",dir);
        pemPrivateKey = writePrivateKey(caKeyPair.getPrivate());
        stringToFile(pemPrivateKey,"",dir);
        pemPrivateKey = writePrivateKey(eeKeyPair.getPrivate());
        stringToFile(pemPrivateKey,"",dir);

        pemPrivateKey = writePrivateKey(keyPair.getPrivate());
        stringToFile(pemPrivateKey,"", dir);


        System.err.println(pemPrivateKey);
        System.err.println(keyPair.getPrivate().equals(readPrivateKey(pemPrivateKey)));

        String encPrivKey = writeEncryptedKey(keyPass, keyPair.getPrivate());

        System.err.println(encPrivKey);
        System.err.println(keyPair.getPrivate().equals(readEncryptedKey(keyPass, encPrivKey)));

        String openSslEncPrivKey = writeEncryptedKeyOpenSsl(keyPass, keyPair.getPrivate());

        System.err.println(openSslEncPrivKey);
        System.err.println(keyPair.getPrivate().equals(readEncryptedKeyOpenSsl(keyPass, openSslEncPrivKey)));
    }
}
