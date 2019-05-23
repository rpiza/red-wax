package com.problemeszero.redwax;

import com.google.common.primitives.Bytes;

import javafx.beans.binding.StringExpression;

import javafx.stage.FileChooser;

import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import java.io.File;
import java.io.Serializable;
import java.util.Date;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

@XmlRootElement
public class RedWaxMessage implements Serializable {
    private String from;
    private String to;
    private String subject;
    private Date sentDate;
    private byte[] mailSign; //conte el MimeBodyPart del la signatura
    private byte[] mailSignedMultiPart; //conte el MultiPart del (MultiPart) Cem + (MimeBodyPart) Signatura
    private byte[] K1;
    private byte[] K2;
    private byte[][] certFile; //Conte el document certificat xifrat.
    private long deadTimeMillis;
    private byte[] kPrima;
    private String addrAlice;
    private byte[] cem; //Conte el Multipart del missatge. Sense signatura
    private byte[] hashCem;
    private byte[] opReturn;
    private String id;

    public RedWaxMessage() {}

    public String getFrom() {
        return from;
    }

    @XmlElement
    public void setFrom(String from) {
        this.from = from;
    }

    public String getTo() {
        return to;
    }

    @XmlElement
    public void setTo(String to) {
        this.to = to;
    }

    public String getSubject() {
        return subject;
    }

    @XmlElement
    public void setSubject(String subject) {
        this.subject = subject;
    }

    public Date getSentDate() {
        return sentDate;
    }

    @XmlElement
    public void setSentDate(Date sentDate) {
        this.sentDate = sentDate;
    }

    public byte[] getMailSign() {
        return mailSign;
    }

    @XmlElement
    public void setMailSign(byte[] mailSign) {
        this.mailSign = mailSign;
    }

    public byte[] getMailSignedMultiPart() {
        return mailSignedMultiPart;
    }
    @XmlElement
    public void setMailSignedMultiPart(byte[] mailSignedMultiPart) {
        this.mailSignedMultiPart = mailSignedMultiPart;
    }

    public byte[] getK1() {
        return K1;
    }

    @XmlElement
    public void setK1(byte[] k1) {
        K1 = k1;
    }

    public byte[] getK2() {
        return K2;
    }

    @XmlElement
    public void setK2(byte[] k2) {
        K2 = k2;
    }

    public byte[][] getCertFile() {
        return certFile;
    }

    @XmlElement
    public void setCertFile(byte[][] certFile) {
        this.certFile = certFile;
    }

    public long getDeadTimeMillis() {
        return deadTimeMillis;
    }

    @XmlElement
    public void setDeadTimeMillis(long deadTimeMillis) {
        this.deadTimeMillis = deadTimeMillis;
    }

    public byte[] getkPrima() {
        return kPrima;
    }

    @XmlElement
    public void setkPrima(byte[] kPrima) {
        this.kPrima = kPrima;
    }

    public String getAddrAlice() {
        return addrAlice;
    }

    @XmlElement
    public void setAddrAlice(String addrAlice) {
        this.addrAlice = addrAlice;
    }

    public byte[] getCem() {
        return cem;
    }

    @XmlElement
    public void setCem(byte[] cem) {
        this.cem = cem;
    }

    public byte[] getHashCem() {
        return hashCem;
    }

    @XmlElement
    public void setHashCem(byte[] hashCem) {
        this.hashCem = hashCem;
    }

    public byte[] getOpReturn() {
        if ((this.getHashCem()!= null) && (this.getK1()!= null)) {
            return Bytes.concat(this.getHashCem(),this.getK1());
        }
        return null;
    }

    @XmlElement
    public void setOpReturn(byte[] opReturn) {
        this.opReturn = opReturn;
    }

    public String getId() {
        return id;
    }

    @XmlAttribute
    public void setId(String id) {
        this.id = id;
    }

    public void redWaxToPersistent() {
        try {

            FileChooser FC = new FileChooser();
            FC.setTitle("Nom del fitxer");
            FC.setInitialDirectory(new File(Main.appProps.getProperty("RedWax")));
            File file = new File(FC.showSaveDialog(Main.instance.stage).getAbsolutePath());

            JAXBContext jaxbContext = JAXBContext.newInstance(RedWaxMessage.class);
            Marshaller jaxbMarshaller = jaxbContext.createMarshaller();

            // output pretty printed
            jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

            jaxbMarshaller.marshal(this, file);
            jaxbMarshaller.marshal(this, System.out);

        } catch (JAXBException e) {
            e.printStackTrace();
        }
    }

//    public RedWaxMessage persistentToRedWax() {
//
//        RedWaxMessage redWaxMessage = null;
//        try {
//
//            FileChooser FC = new FileChooser();
//            FC.setTitle("Nom del fitxer");
//            File file = new File(FC.showOpenDialog(Main.instance.mainWindow).getAbsolutePath());
//            JAXBContext jaxbContext = JAXBContext.newInstance(RedWaxMessage.class);
//
//            Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
//            redWaxMessage = (RedWaxMessage) jaxbUnmarshaller.unmarshal(file);
//            System.out.println(redWaxMessage  +"XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX");
//
//        } catch (JAXBException e) {
//            e.printStackTrace();
//        }
//        return redWaxMessage;
//    }


}
