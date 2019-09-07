package com.problemeszero.mail;

import com.problemeszero.crypto.Smime;
import com.problemeszero.redwax.RedWaxMessage;
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

import javax.activation.DataHandler;
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
        } catch ( MessagingException | IOException e) {
            e.printStackTrace();
        }
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

    public void addPartToCem (String text, String contendID) throws MessagingException {
        MimeBodyPart messageBodyPart = new MimeBodyPart();
        //AFEGIM UN CR (13) ABANS DEL LF (10) al missatge body del correu
        if (contendID.equals("mailBody")) text=linebreak(text);
        messageBodyPart.setText(text);
        messageBodyPart.setHeader("Content-ID",contendID);
        cem.addBodyPart(messageBodyPart);
    }

    public void addPartToCem (byte[] b, String contendID) throws MessagingException {
        MimeBodyPart messageBodyPart = new MimeBodyPart();
        DataSource dataSource = new ByteArrayDataSource(b, "application/octet-stream");
        messageBodyPart.setDataHandler(new DataHandler(dataSource));
        messageBodyPart.setFileName("Document_Certificat");
        messageBodyPart.setHeader("Content-ID",contendID);
        cem.addBodyPart(messageBodyPart);
    }

    public void cemToRwm(RedWaxMessage r){

        try {
            int parts = cem.getCount();
            System.err.println("Num. parts del missatge multipart = " + parts);
            for (int j = 0; j < parts; ++j) {
                MimeBodyPart part = (MimeBodyPart) cem.getBodyPart(j);
                if (part.getContentID() != null) {
                    if ("fitxerXifrat".equals(part.getContentID())) {
                        byte[] out2 = MimeBodyPartToBAOS(part, "Base64");
                        byte[][] cF = {Arrays.copyOfRange(out2, 0, 16), Arrays.copyOfRange(out2, 16, out2.length)};
//                                System.err.println(new String(Base64.getEncoder().encode(cF[0])));
//                                System.err.println(new String(Base64.getEncoder().encode(cF[1])));
                        r.setCertFile(cF);
                    }

                    if ("deadTime".equals(part.getContentID())) {
                        r.setDeadTimeMillis(Long.valueOf(new String(MimeBodyPartToBAOS(part, ""))));
                    }

                    if ("kPrima".equals(part.getContentID())) {
                        r.setkPrima(MimeBodyPartToBAOS(part, ""));
                    }

                    if ("addrAlice".equals(part.getContentID())) {
                        r.setAddrAlice(new String(MimeBodyPartToBAOS(part, "")));
                    }
                }
            }
        } catch (MessagingException | IOException  e) {
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

     public byte[] getHashCem(String alg) throws GeneralSecurityException,MessagingException,IOException {
        byte[] data = Smime.PartToBAOS(cem);

        MessageDigest hash = MessageDigest.getInstance(alg, "BCFIPS");

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

    //Escriu nomes el contingut del MimeBodyPart. NO inclou les capsaleres
    private byte [] MimeBodyPartToBAOS (MimeBodyPart part, String code) throws MessagingException, IOException {

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        InputStream in = part.getInputStream();
        byte [] output;
        int k;
        while ((k = in.read()) != -1) {
            out.write(k);
        }
        output = out.toByteArray();
        in.close();
        out.close();
//        System.err.println(j+"-" + part.getContentID() +" " + part.getContentType());
//        if (!code.equals("Base64")) System.err.println(new String(output));
//        else System.err.println(new String(Base64.getEncoder().encode(output)));
        return output;
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
