package com.problemeszero.mail;

import com.problemeszero.crypto.Smime;
import com.sun.mail.util.LineInputStream;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.DERSet;
import org.bouncycastle.asn1.cms.Attribute;
import org.bouncycastle.asn1.cms.AttributeTable;
import org.bouncycastle.asn1.cms.CMSAttributes;
import org.bouncycastle.asn1.cms.Time;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaCertStore;
import org.bouncycastle.cms.CMSException;
import org.bouncycastle.cms.SignerInformation;
import org.bouncycastle.cms.SignerInformationStore;
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoGeneratorBuilder;
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder;
import org.bouncycastle.mail.smime.SMIMEException;
import org.bouncycastle.mail.smime.SMIMESigned;
import org.bouncycastle.mail.smime.SMIMESignedGenerator;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.util.Store;

import javax.activation.DataSource;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.internet.ContentType;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.*;

public class RedWaxSMime {

    private MimeMultipart mPart;
    private X509CertificateHolder cert;
    private boolean okSignatura;
    private MimeMultipart cem;
    private MimeBodyPart signedPart;


    public RedWaxSMime(){
        mPart = new MimeMultipart();
        cem = new MimeMultipart();
        signedPart = new MimeBodyPart();
    };

    public RedWaxSMime(byte [] message) {

        ContentType cType = new ContentType("multipart", "signed", null);
        cType.setParameter("boundary", obtenir_boundary(message));
        DataSource dataSource = new ByteArrayDataSource(tractar_smtp(message),cType.toString());
//        System.err.println(java.util.Arrays.toString(rwm.getMailSignedMultiPart()));
        mPart = null;
        try {
            //Cream el multipart a partir del datasource
            mPart = new MimeMultipart(dataSource);

            //Obtenim el Cem del multipart signat. Es el primera part del multipart resultat del proces de signatura
            cem =(MimeMultipart) mPart.getBodyPart(0).getContent();
            //Obtenim el bodypart amb la signatura
            signedPart = (MimeBodyPart) mPart.getBodyPart(1);
            // byte [] cem = Smime.PartToBAOS(multi);

//            try {
//                System.err.println(java.util.Arrays.toString(Smime.PartToBAOS(mPart)));
//                Smime.byteToFile(Smime.PartToBAOS(mPart), "Guardar cemSignat",new File(Main.appProps.getProperty("Fitxers")));
//            } catch (IOException | MessagingException e) {
//               e.printStackTrace();
//            }
//        } catch (GeneralSecurityException | OperatorCreationException | CMSException | SMIMEException | MessagingException e) {
        } catch ( MessagingException | IOException e) {
            e.printStackTrace();
        }
    }

    public void addPartToCem(MimeBodyPart bPart) throws MessagingException{
        cem.addBodyPart(bPart);
    }

    public RedWaxSMime(MimeMultipart mPart) {
        this.mPart = mPart;
        try {
            cem = (MimeMultipart) mPart.getBodyPart(0).getContent();
            //Obtenim el bodypart amb la signatura
            signedPart = (MimeBodyPart) mPart.getBodyPart(1);
            // byte [] cem = Smime.PartToBAOS(multi);
        } catch (MessagingException | IOException e) {
            e.printStackTrace();
        }
    }

    //Signar el cem de l'objecte
    public RedWaxSMime createSignedMultipart(PrivateKey signingKey, X509Certificate signingCert)
            throws GeneralSecurityException, OperatorCreationException, SMIMEException, IOException, MessagingException {
        List<X509Certificate> certList = new ArrayList<X509Certificate>();
        certList.add(signingCert);
        Store certs = new JcaCertStore(certList);
        ASN1EncodableVector signedAttrs = Smime.generateSignedAttributes();
        signedAttrs.add(new Attribute(CMSAttributes.signingTime, new DERSet(new Time(new Date()))));
        SMIMESignedGenerator gen = new SMIMESignedGenerator();
        gen.addSignerInfoGenerator(new JcaSimpleSignerInfoGeneratorBuilder().setProvider("BCFIPS").setSignedAttributeGenerator(new AttributeTable(signedAttrs)).build("SHA384withRSAandMGF1", signingKey, signingCert));
        gen.addCertificates(certs);
        MimeBodyPart message = new MimeBodyPart();
        message.setContent(cem);
        return new RedWaxSMime(gen.generate(message));
    }
    //Signa el cem passat com parametre
    public MimeMultipart createSignedMultipart(PrivateKey signingKey, X509Certificate signingCert, MimeBodyPart message)
            throws GeneralSecurityException, OperatorCreationException, SMIMEException, IOException {
        List<X509Certificate> certList = new ArrayList<X509Certificate>();
        certList.add(signingCert);
        Store certs = new JcaCertStore(certList);
        ASN1EncodableVector signedAttrs = Smime.generateSignedAttributes();
        signedAttrs.add(new Attribute(CMSAttributes.signingTime, new DERSet(new Time(new Date()))));
        SMIMESignedGenerator gen = new SMIMESignedGenerator();
        gen.addSignerInfoGenerator(new JcaSimpleSignerInfoGeneratorBuilder().setProvider("BCFIPS").setSignedAttributeGenerator(new AttributeTable(signedAttrs)).build("SHA384withRSAandMGF1", signingKey, signingCert));
        gen.addCertificates(certs);
        return gen.generate(message);
    }

    public void setmPartAndSignedPart(RedWaxSMime m){

        if (cem.equals(m.getCem())) {
            System.err.println("Els objectes cem son iguals!!!");
            setmPart(m.getmPart());
            setSignedPart(m.getSignedPart());
        }

    }


    public void verifySignedMultipart()
            throws GeneralSecurityException, OperatorCreationException, CMSException, SMIMEException, MessagingException {

        SMIMESigned signedData = new SMIMESigned(this.mPart);
        Store certStore = signedData.getCertificates();
        SignerInformationStore signers = signedData.getSignerInfos();

        Collection c = signers.getSigners();
        Iterator it = c.iterator();

        while (it.hasNext()) {
            SignerInformation signer = (SignerInformation) it.next();
            Collection certCollection = certStore.getMatches(signer.getSID());
            Iterator certIt = certCollection.iterator();
            this.cert = (X509CertificateHolder) certIt.next();
//            System.err.println("Certificat de " + this.cert.getSubject() + ", expedit per " + this.cert.getIssuer()+
//                    ". Vàlid des de \"" + this.cert.getNotBefore() + "\" fins a \"" + this.cert.getNotAfter()+ "\".");

            if (!signer.verify(new JcaSimpleSignerInfoVerifierBuilder().setProvider("BCFIPS").build(this.cert))) {
                this.okSignatura = false;
            }
        }
        this. okSignatura=true;
    }

     public byte[] getHashCem() throws GeneralSecurityException,MessagingException,IOException {
        byte[] data = Smime.PartToBAOS(cem);

        MessageDigest hash = MessageDigest.getInstance("SHA256", "BCFIPS");

        return hash.digest(data);
    }

    public String getSignaturaBase64() throws MessagingException,IOException{
        // Obtenir la signatura base64. Sense les capsaleres del bodyPart
        ByteArrayOutputStream out = null;
        InputStream in = null;
        out = new ByteArrayOutputStream();
        in = signedPart.getInputStream();
        int k;
        while ((k = in.read()) != -1) {
            out.write(k);
        }
        out.close();
        in.close();
//        System.err.println("Signatura: " + new String(Base64.getEncoder().encode(out.toByteArray())));
        return new String(Base64.getEncoder().encode(out.toByteArray()));
    }

    public byte[] getSignaturaBytes() throws MessagingException,IOException{
        // Obtenir la signatura base64. Sense les capsaleres del bodyPart
        ByteArrayOutputStream out = null;
        InputStream in = null;
        out = new ByteArrayOutputStream();
        in = signedPart.getInputStream();
        int k;
        while ((k = in.read()) != -1) {
            out.write(k);
        }
        out.close();
        in.close();
//        System.err.println("Signatura: " + new String(Base64.getEncoder().encode(out.toByteArray())));
        return out.toByteArray();
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

    //    Aquesta funcio corregeix les modificacions introduides pel servidor SMTP de Gmail al Content-type del missatge.
//    Aquestes modificacions fan que la validacio de la signatura segui erronea.
//    Pot ser que altres servidors indroduexien altres modificacions o que els servidor de gmail en generi de noves amb el temps.
    private byte [] tractar_smtp (byte [] content){
        int i,j;
        String bigStr = new String(content, StandardCharsets.UTF_8);
        byte [] small1 = {98, 111, 117, 110, 100, 97, 114, 121, 61, 34, 45, 45, 45, 45}; //boundary="----
        byte [] small2 = {32, 115, 109, 105, 109, 101, 45, 116, 121, 112, 101, 61, 115, 105, 103, 110, 101, 100, 45, 100, 97, 116, 97};// smime-type=signed-data
        String smallStr1 = new String(small1, StandardCharsets.UTF_8);
        String smallStr2 = new String(small2, StandardCharsets.UTF_8);

        i = bigStr.indexOf(smallStr1);
//        System.err.println(java.util.Arrays.toString(bigStr.substring(i-3,i).getBytes()));
        if (!bigStr.substring(i-3,i).equals("\r\n\t")) {
            bigStr = bigStr.substring(0, i) + (char) 0x0D + (char) 0x0A + (char) 0x09 + bigStr.substring(i);
            System.err.println("Corregim modificació SMTP: \"boundary=\"----\"");
        }
        j = bigStr.indexOf(smallStr2);
//        System.err.println(java.util.Arrays.toString(bigStr.substring(j-2,j).getBytes()));
        if (bigStr.substring(j-2,j).equals("\r\n")) {
            bigStr = bigStr.substring(0, j - 2) + bigStr.substring(j - 2);
            System.err.println("Corregim modificació SMPT: \" smime-type=signed-data\"");
        }

        return bigStr.getBytes();
    }

    public MimeMultipart getmPart() {
        return mPart;
    }

    private void setmPart(MimeMultipart mPart) {
        this.mPart = mPart;
    }

    public X509CertificateHolder getCert() {
        return cert;
    }

    public boolean isOkSignatura() {
        return okSignatura;
    }

    public MimeMultipart getCem() {
        return cem;
    }

    public MimeBodyPart getSignedPart() {
        return signedPart;
    }

    private void setSignedPart(MimeBodyPart signedPart) {
        this.signedPart = signedPart;
    }
}
