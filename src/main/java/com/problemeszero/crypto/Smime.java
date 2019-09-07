package com.problemeszero.crypto;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.cert.*;
import java.util.*;
import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import com.problemeszero.redwax.Main;
import javafx.stage.FileChooser;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.DERSet;
import org.bouncycastle.asn1.cms.Attribute;
import org.bouncycastle.asn1.cms.AttributeTable;
import org.bouncycastle.asn1.cms.CMSAttributes;
import org.bouncycastle.asn1.cms.Time;
import org.bouncycastle.asn1.smime.SMIMECapabilitiesAttribute;
import org.bouncycastle.asn1.smime.SMIMECapability;
import org.bouncycastle.asn1.smime.SMIMECapabilityVector;
import org.bouncycastle.asn1.smime.SMIMEEncryptionKeyPreferenceAttribute;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaCertStore;
import org.bouncycastle.cms.*;
import org.bouncycastle.cms.jcajce.*;
import org.bouncycastle.crypto.CryptoServicesRegistrar;
import org.bouncycastle.crypto.EntropySourceProvider;
import org.bouncycastle.crypto.InvalidWrappingException;
import org.bouncycastle.crypto.PlainInputProcessingException;
import org.bouncycastle.crypto.asymmetric.AsymmetricRSAPrivateKey;
import org.bouncycastle.crypto.asymmetric.AsymmetricRSAPublicKey;
import org.bouncycastle.crypto.fips.FipsDRBG;
import org.bouncycastle.crypto.fips.FipsKeyUnwrapperUsingSecureRandom;
import org.bouncycastle.crypto.fips.FipsKeyWrapperUsingSecureRandom;
import org.bouncycastle.crypto.fips.FipsRSA;
import org.bouncycastle.crypto.util.BasicEntropySourceProvider;
import org.bouncycastle.mail.smime.*;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.util.Store;

public class Smime {

     /**
     * Create a MIME message from using the passed in content.
     */
    public static MimeMessage createMimeMessage(
        String subject,
        Object content,
        String contentType)
        throws MessagingException
    {
        Properties props = System.getProperties();
        Session session = Session.getDefaultInstance(props, null);

        Address fromUser = new InternetAddress("\"Eric H. Echidna\"<eric@bouncycastle.org>");
        Address toUser = new InternetAddress("example@bouncycastle.org");

        MimeMessage message = new MimeMessage(session);

        message.setFrom(fromUser);
        message.setRecipient(Message.RecipientType.TO, toUser);
        message.setSubject(subject);
        message.setContent(content, contentType);
        message.saveChanges();

        return message;
    }

    public static ASN1EncodableVector generateSignedAttributes() {

        ASN1EncodableVector signedAttrs = new ASN1EncodableVector();
        SMIMECapabilityVector caps = new SMIMECapabilityVector();
        caps.addCapability(SMIMECapability.aES128_CBC);
        caps.addCapability(SMIMECapability.aES192_CBC);
        caps.addCapability(SMIMECapability.aES256_CBC);
        signedAttrs.add(new SMIMECapabilitiesAttribute(caps));

        return signedAttrs;
    }

//    public static MimeMultipart createSignedMultipart(PrivateKey signingKey, X509Certificate signingCert, MimeBodyPart message)
//            throws GeneralSecurityException, OperatorCreationException, SMIMEException, IOException {
//        List<X509Certificate> certList = new ArrayList<X509Certificate>();
//        certList.add(signingCert);
//        Store certs = new JcaCertStore(certList);
//        ASN1EncodableVector signedAttrs = generateSignedAttributes();
//        signedAttrs.add(new Attribute(CMSAttributes.signingTime, new DERSet(new Time(new Date()))));
//        SMIMESignedGenerator gen = new SMIMESignedGenerator();
//        gen.addSignerInfoGenerator(new JcaSimpleSignerInfoGeneratorBuilder().setProvider("BCFIPS").setSignedAttributeGenerator(new AttributeTable(signedAttrs)).build("SHA384withRSAandMGF1", signingKey, signingCert));
//        gen.addCertificates(certs);
//        return gen.generate(message);
//    }

//    public static boolean verifySignedMultipart(MimeMultipart signedMessage)
//            throws GeneralSecurityException, OperatorCreationException, CMSException, SMIMEException, MessagingException {
//
//        SMIMESigned signedData = new SMIMESigned(signedMessage);
//        Store certStore = signedData.getCertificates();
//        SignerInformationStore signers = signedData.getSignerInfos();
//
//        Collection c = signers.getSigners();
//        Iterator it = c.iterator();
//
//        while (it.hasNext()) {
//            SignerInformation signer = (SignerInformation) it.next();
//            Collection certCollection = certStore.getMatches(signer.getSID());
//            Iterator certIt = certCollection.iterator();
//            X509CertificateHolder cert = (X509CertificateHolder) certIt.next();
//            System.err.println("Certificat de " + cert.getSubject() + ", expedit per " + cert.getIssuer()+
//                    ". Vàlid des de \"" + cert.getNotBefore() + "\" fins a \"" + cert.getNotAfter()+ "\".");
//
//            if (!signer.verify(new JcaSimpleSignerInfoVerifierBuilder().setProvider("BCFIPS").build(cert))) {
//                return false;
//            }
//        }
//        return true;
//    }


//    public static boolean verifySignedMultipart(MimeMultipart signedMessage, Object cert1)
//            throws GeneralSecurityException, OperatorCreationException, CMSException, SMIMEException, MessagingException {
//
//        SMIMESigned signedData = new SMIMESigned(signedMessage);
//        Store certStore = signedData.getCertificates();
//        SignerInformationStore signers = signedData.getSignerInfos();
//        X509CertificateHolder cert = null;
//
//        Collection c = signers.getSigners();
//        Iterator it = c.iterator();
//
//        while (it.hasNext()) {
//            SignerInformation signer = (SignerInformation) it.next();
//            Collection certCollection = certStore.getMatches(signer.getSID());
//            Iterator certIt = certCollection.iterator();
//            cert1 = (X509CertificateHolder) certIt.next();
//            cert = (X509CertificateHolder) cert1;
//            System.err.println("Certificat de " + cert.getSubject() + ", expedit per " + cert.getIssuer()+
//                    ". Vàlid des de \"" + cert.getNotBefore() + "\" fins a \"" + cert.getNotAfter()+ "\".");
//
//            if (!signer.verify(new JcaSimpleSignerInfoVerifierBuilder().setProvider("BCFIPS").build(cert))) {
//                return false;
//            }
//        }
//        return true;
//    }
//    public static MimeBodyPart createSignedEncryptedBodyPart(PrivateKey signingKey, X509Certificate signingCert, X509Certificate encryptionCert, MimeBodyPart message)
//            throws GeneralSecurityException, SMIMEException, CMSException, IOException,OperatorCreationException, MessagingException {
//        SMIMEEnvelopedGenerator gen = new SMIMEEnvelopedGenerator();
//        gen.addRecipientInfoGenerator(new JceKeyTransRecipientInfoGenerator(encryptionCert).setProvider("BCFIPS"));
//        MimeBodyPart bodyPart = new MimeBodyPart();
//        bodyPart.setContent(createSignedMultipart(signingKey, signingCert, message));
//        return gen.generate(bodyPart, new JceCMSContentEncryptorBuilder(CMSAlgorithm.AES256_CBC).setProvider("BCFIPS").build());
//    }
//
//    public static boolean verifySignedEncryptedBodyPart(PrivateKey privateKey, X509Certificate encryptionCert, MimeBodyPart envelopedBodyPart)
//            throws SMIMEException, CMSException, GeneralSecurityException, OperatorCreationException,MessagingException, IOException {
//        SMIMEEnveloped envelopedData = new SMIMEEnveloped(envelopedBodyPart);
//        RecipientInformationStore recipients = envelopedData.getRecipientInfos();
//        Collection c = recipients.getRecipients(new JceKeyTransRecipientId(encryptionCert));
//        Iterator it = c.iterator();
//        if (it.hasNext()) {
//            RecipientInformation recipient = (RecipientInformation)it.next();
//            MimeBodyPart signedPart = SMIMEUtil.toMimeBodyPart(
//                    recipient.getContent(new JceKeyTransEnvelopedRecipient(privateKey).setProvider("BCFIPS")));
//            return verifySignedMultipart((MimeMultipart)signedPart.getContent());
//        }
//        throw new IllegalArgumentException("recipient for certificate not found");
//    }

    public static byte[] calculateDigest(byte[] data) throws GeneralSecurityException {

        MessageDigest hash = MessageDigest.getInstance("SHA256", "BCFIPS");

        return hash.digest(data);
    }
    public static byte[] calculateSha3Digest(byte[] data) throws GeneralSecurityException {

        MessageDigest hash = MessageDigest.getInstance("SHA3-256", "BCFIPS");

        return hash.digest(data);
    }

    public static byte[] fileToByte(String title, File initialDir) {

        FileChooser FC = new FileChooser();
        FC.setTitle(title);
        //FC.setInitialDirectory(new File("."));
        FC.setInitialDirectory(initialDir);
        FileInputStream iInputStream = null;
        File iFile = new File(FC.showOpenDialog(Main.instance.stage).getAbsolutePath());

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


    public static void byteToFile(byte[] output, String title, File dir) throws FileNotFoundException,NullPointerException {

        //FileChooser1 FC = new FileChooser1();
        FileChooser FC = new FileChooser();
        FC.setTitle(title);
//        FC.titleProperty().setValue(title);
        FC.setInitialDirectory(dir);

        FileOutputStream fos= null;
//        try {
            fos = new FileOutputStream(FC.showSaveDialog(Main.instance.stage).getAbsolutePath());
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


    //Escriu tot el "part" al byteArray. Incloses les capsaleres.
    public static byte [] PartToBAOS(Object p) throws MessagingException,IOException {

        ByteArrayOutputStream bodyPartBaos = new ByteArrayOutputStream();
        byte [] bPB;

        if (p instanceof Multipart) {
            Multipart part = (Multipart) p;
            part.writeTo(bodyPartBaos);
        } else {
            MimeBodyPart part = (MimeBodyPart) p;
            part.writeTo(bodyPartBaos);
        }
        bPB = bodyPartBaos.toByteArray();
        bodyPartBaos.close();

        return bPB;

    }

    public static SecureRandom buildDrbg(){

        EntropySourceProvider entSource = new BasicEntropySourceProvider(new SecureRandom(), true);
        FipsDRBG.Builder drgbBldr = FipsDRBG.SHA512_HMAC.fromEntropySource(entSource).setSecurityStrength(256).setEntropyBitsRequired(256);

        return drgbBldr.build("varibale per inicialitzar".getBytes(), false);
    }

    public static SecureRandom buildDrbgForKeys(){

        EntropySourceProvider entSource = new BasicEntropySourceProvider(new SecureRandom(), true);
        FipsDRBG.Builder drgbBldr = FipsDRBG.SHA512_HMAC.fromEntropySource(entSource).setSecurityStrength(256).setEntropyBitsRequired(256)
                .setPersonalizationString("una altra variable per inicialitzar".getBytes());

        return drgbBldr.build("varibale per inicialitzar".getBytes(), true);
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

//    Aquesta funcio corregeix les modificacions introduides pel servidor SMTP de Gmail al Content-type del missatge.
//    Aquestes modificacions fan que la validacio de la signatura segui erronea.
//    Pot ser que altres servidors indroduexien altres modificacions o que els servidor de gmail en generi de noves amb el temps.
//    public static byte [] tractar_smtp (byte [] content){
//        int i,j;
//        String bigStr = new String(content, StandardCharsets.UTF_8);
//        byte [] small1 = {98, 111, 117, 110, 100, 97, 114, 121, 61, 34, 45, 45, 45, 45}; //boundary="----
//        byte [] small2 = {32, 115, 109, 105, 109, 101, 45, 116, 121, 112, 101, 61, 115, 105, 103, 110, 101, 100, 45, 100, 97, 116, 97};// smime-type=signed-data
//        String smallStr1 = new String(small1, StandardCharsets.UTF_8);
//        String smallStr2 = new String(small2, StandardCharsets.UTF_8);
//
//        i = bigStr.indexOf(smallStr1);
////        System.err.println(java.util.Arrays.toString(bigStr.substring(i-3,i).getBytes()));
//        if (!bigStr.substring(i-3,i).equals("\r\n\t")) {
//            bigStr = bigStr.substring(0, i) + (char) 0x0D + (char) 0x0A + (char) 0x09 + bigStr.substring(i);
//            System.err.println("Corregim modificació SMTP: \"boundary=\"----\"");
//        }
//        j = bigStr.indexOf(smallStr2);
////        System.err.println(java.util.Arrays.toString(bigStr.substring(j-2,j).getBytes()));
//        if (bigStr.substring(j-2,j).equals("\r\n")) {
//            bigStr = bigStr.substring(0, j - 2) + bigStr.substring(j - 2);
//            System.err.println("Corregim modificació SMPT: \" smime-type=signed-data\"");
//        }
//
//        return bigStr.getBytes();
//    }

//    public static byte[] wrapKey(AsymmetricRSAPublicKey pubKey, byte[] inputKeyBytes) throws PlainInputProcessingException {
//        FipsRSA.KeyWrapOperatorFactory wrapFact = new FipsRSA.KeyWrapOperatorFactory();
//        FipsKeyWrapperUsingSecureRandom wrapper = (FipsKeyWrapperUsingSecureRandom) wrapFact.createKeyWrapper( pubKey,FipsRSA.WRAP_OAEP)
//                .withSecureRandom(CryptoServicesRegistrar.getSecureRandom());
//
//        return wrapper.wrap(inputKeyBytes, 0, inputKeyBytes.length);
//    }
//
//    public static byte[] unwrapKey(AsymmetricRSAPrivateKey privKey, byte[] wrappedKeyBytes) throws InvalidWrappingException {
//        FipsRSA.KeyWrapOperatorFactory wrapFact = new FipsRSA.KeyWrapOperatorFactory();
//        FipsKeyUnwrapperUsingSecureRandom unwrapper = (FipsKeyUnwrapperUsingSecureRandom) wrapFact.createKeyUnwrapper(privKey,FipsRSA.WRAP_OAEP)
//                .withSecureRandom(CryptoServicesRegistrar.getSecureRandom());
//
//        return unwrapper.unwrap(wrappedKeyBytes, 0, wrappedKeyBytes.length);
//    }

}
