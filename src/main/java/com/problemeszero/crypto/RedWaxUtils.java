package com.problemeszero.crypto;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

import java.security.PrivateKey;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import com.problemeszero.redwax.Main;
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

public class RedWaxUtils  {

    public RedWaxUtils() {}

    public static String writeCertificate(X509Certificate certificate)
            throws IOException
    {
        StringWriter sWrt = new StringWriter();
        JcaPEMWriter pemWriter = new JcaPEMWriter(sWrt);

        pemWriter.writeObject(certificate);

        pemWriter.close();

        return sWrt.toString();
    }

    public static X509Certificate readCertificate(String pemEncoding) throws IOException, CertificateException {
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
            throws IOException
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
            throws IOException, PKCSException
    {
        PEMParser parser = new PEMParser(new StringReader(pemEncoding));

        PKCS8EncryptedPrivateKeyInfo encPrivKeyInfo = (PKCS8EncryptedPrivateKeyInfo)parser.readObject();

        InputDecryptorProvider pkcs8Prov = new JcePKCSPBEInputDecryptorProviderBuilder().setProvider("BCFIPS").build(password);

        JcaPEMKeyConverter   converter = new JcaPEMKeyConverter().setProvider("BCFIPS");

        return converter.getPrivateKey(encPrivKeyInfo.decryptPrivateKeyInfo(pkcs8Prov));
    }

    public static String writeEncryptedKeyOpenSsl(char[] passwd, PrivateKey privateKey)
            throws IOException
    {
        StringWriter sWrt = new StringWriter();
        JcaPEMWriter pemWriter = new JcaPEMWriter(sWrt);

        pemWriter.writeObject(privateKey, new JcePEMEncryptorBuilder("AES-256-CBC").setProvider("BCFIPS").build(passwd));

        pemWriter.close();

        return sWrt.toString();
    }

    public static PrivateKey readEncryptedKeyOpenSsl(char[] passwd, String pemEncoding)
            throws IOException
    {
        PEMParser parser = new PEMParser(new StringReader(pemEncoding));

        PEMEncryptedKeyPair pemEncryptedKeyPair = (PEMEncryptedKeyPair)parser.readObject();

        PEMDecryptorProvider pkcs8Prov = new JcePEMDecryptorProviderBuilder().setProvider("BCFIPS").build(passwd);

        JcaPEMKeyConverter   converter = new JcaPEMKeyConverter().setProvider("BCFIPS");

        return converter.getPrivateKey(pemEncryptedKeyPair.decryptKeyPair(pkcs8Prov).getPrivateKeyInfo());
    }

    public static void stringToFile(String output, Path path) {

        byte[] strToBytes = output.getBytes();
        System.err.println("(stringtoFile) Escriu string al fitxer: " + path);
        try {
            Files.write(path, strToBytes);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

     public static String fileToString(Path path) {

        String output= null;
         System.err.println("(fileToString) Llegeix fitxer cap a string. " + path);
         try {
             output = new String(Files.readAllBytes(path));

         } catch (IOException e) {
             e.printStackTrace();
         }
        return output;
     }

    public static void byteToFile(byte[] output, Path path) throws FileNotFoundException,NullPointerException {

        System.err.println("(byteToFile) Escriu el bytes al fitxer: " + path.toFile().getAbsolutePath());
        FileOutputStream fos= null;
//        try {
        fos = new FileOutputStream(path.toFile().getAbsolutePath());
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//        }

        try {
            fos.write(output);
            //fos.close(); There is no more need for this line since you had created the instance of "fos" inside the try. And this will automatically close the OutputStream

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public static byte[] fileToByte(Path path) {

        System.err.println("(fileToByte) Llegeix el fitxer: " + path.toFile().getAbsolutePath()  + " -- " + path.toString());
        FileInputStream iInputStream = null;
        //File iFile = new File(FC.showOpenDialog(Main.instance.stage).getAbsolutePath());
        File iFile = path.toFile();

        byte[] iByteStream = new byte[(int) iFile.length()];

        try {
            iInputStream = new FileInputStream(iFile);
            iInputStream.read(iByteStream);
            iInputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return iByteStream;
    }

    public static File getRedWaxFile(Path path) {

        System.err.println("(getRedWaxFile) Escriu al fitxer: " + path.toFile().getAbsolutePath());
        return new File(path.toFile().getAbsolutePath());
    }

    public static  void crearDirectoris() {
        String [] carpetes = {"Certificats", "RedWax", "Fitxers", "Temp"};
        for (String i : carpetes) {
            File dir = new File(System.getProperty("user.home"), "RedWax/" + i);
            Main.appProps.setProperty(i,dir.getPath());
            System.err.println("El directori " + i + ", és a la ruta "+ dir.getPath());
            if (!dir.exists()) {
                dir.mkdirs();
            }
        }
    }
}
