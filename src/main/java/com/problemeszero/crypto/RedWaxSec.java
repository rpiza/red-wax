package com.problemeszero.crypto;

import com.problemeszero.redwax.Main;
import javafx.stage.FileChooser;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.cms.*;
import org.bouncycastle.cms.jcajce.JceCMSContentEncryptorBuilder;
import org.bouncycastle.cms.jcajce.JceKeyTransEnvelopedRecipient;
import org.bouncycastle.cms.jcajce.JceKeyTransRecipientId;
import org.bouncycastle.cms.jcajce.JceKeyTransRecipientInfoGenerator;
import org.bouncycastle.crypto.*;
import org.bouncycastle.crypto.asymmetric.AsymmetricRSAPrivateKey;
import org.bouncycastle.crypto.asymmetric.AsymmetricRSAPublicKey;
import org.bouncycastle.crypto.fips.*;
import org.bouncycastle.crypto.util.BasicEntropySourceProvider;
import org.bouncycastle.operator.jcajce.JcaAlgorithmParametersConverter;
import org.bouncycastle.util.encoders.Hex;

import javax.crypto.spec.OAEPParameterSpec;
import java.io.*;
import java.lang.reflect.Array;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.Iterator;


public class RedWaxSec {

    private SymmetricSecretKey k;
    private byte[] k1;
    private byte[] k2;
    private FipsSymmetricOperatorFactory<FipsAES.Parameters> fipsSymmetricFactory;
    private byte[] iv;

    public RedWaxSec() {
//        CryptoServicesRegistrar.setApprovedOnlyMode(true);
//        System.err.println("Nomes mode aprovat:" + CryptoServicesRegistrar.isInApprovedOnlyMode());

        CryptoServicesRegistrar.setSecureRandom(Smime.buildDrbgForKeys());
        this.iv = new byte[16];

        CryptoServicesRegistrar.getSecureRandom().nextBytes(iv);
        FipsSymmetricKeyGenerator<SymmetricSecretKey> keyGen = new FipsAES.KeyGenerator(256, CryptoServicesRegistrar.getSecureRandom());
        this.k = keyGen.generateKey();
        this.k1 = keyGen.generateKey().getKeyBytes();
        generateK2();

        this.fipsSymmetricFactory = new FipsAES.OperatorFactory();
    }

    public static SymmetricSecretKey defineKey(byte[] keyBytes)
    {
        if (keyBytes.length != 16 && keyBytes.length != 24 && keyBytes.length != 32) {
            throw new IllegalArgumentException("keyBytes wrong length for AES key");
        }

        return new SymmetricSecretKey(FipsAES.ALGORITHM,keyBytes);
    }

    public byte[] getK1() {
        return k1;
    }

    public void setK1(byte[] k1) {
        this.k1 = k1;
//        System.err.println("K1 = " + java.util.Arrays.toString(k1));
//        System.err.println("K1 (base64) = " + new String(Base64.getEncoder().encode(k1)));
//        System.err.println("K1 (Hex) = " + new String(Hex.encode(k1)));
    }

    public byte[] getK2() {
        return k2;
    }

    public void setK2(byte[] k2) {
        this.k2 = k2;
//        System.err.println("K2 = " + java.util.Arrays.toString(k2));
//        System.err.println("K2 (base64) = " + new String(Base64.getEncoder().encode(k2)));
//        System.err.println("K2 (Hex) = " + new String(Hex.encode(k2)));
    }

    public void generateK2(){
        this.k2 = xorOperation(k.getKeyBytes(),k1);
    }

    public void obtainK(){
        this.k = defineKey(xorOperation(k2,k1));
    }

    public byte[] xorOperation(byte[] x , byte[] y){
        //FALTA!!!!
        //comprovar que x i y son iguals

        int i;
        byte [] z = new byte[x.length];
        for (i=0; i< (x.length); i++)
        {
            z[i] = (byte) ((x[i]  ^  y[i]) & 0x000000ff);
        }
//        System.err.println(java.util.Arrays.toString(x));
//        System.err.println(java.util.Arrays.toString(y));
//        System.err.println(java.util.Arrays.toString(z));
        return z;
    }


    public byte[][] cbcEncrypt(byte[] plainText) {
        FipsOutputEncryptor<FipsAES.Parameters> outputEncryptor = this.fipsSymmetricFactory.createOutputEncryptor(this.k, FipsAES.CBCwithPKCS7.withIV(this.iv));
        ByteArrayOutputStream bOut = new ByteArrayOutputStream();
        CipherOutputStream encOut = outputEncryptor.getEncryptingStream(bOut);
        encOut.update(plainText);
        try {
            encOut.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new byte[][] { this.iv, bOut.toByteArray() };
    }

    public byte[] cbcDecrypt(byte[] iv, byte[] cipherText ) {
//        System.err.println(java.util.Arrays.toString(iv));

        FipsInputDecryptor<FipsAES.Parameters> inputDecryptor = this.fipsSymmetricFactory.createInputDecryptor(this.k, FipsAES.CBCwithPKCS7.withIV(iv));
        ByteArrayOutputStream bOut = new ByteArrayOutputStream();
        InputStream encIn = inputDecryptor.getDecryptingStream(new ByteArrayInputStream(cipherText));
        int ch;

        try {
            while ((ch = encIn.read()) >= 0) {
                bOut.write(ch);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return bOut.toByteArray();
    }

    public byte[] wrapWithPubKey (PublicKey pubKey, byte[] inputKeyBytes) throws PlainInputProcessingException {
        AsymmetricRSAPublicKey rsaPubKey = new AsymmetricRSAPublicKey(FipsRSA.ALGORITHM, pubKey.getEncoded());
        return wrapKey(rsaPubKey,inputKeyBytes);
    }

    private byte[] wrapKey(AsymmetricRSAPublicKey pubKey, byte[] inputKeyBytes) throws PlainInputProcessingException {
        FipsRSA.KeyWrapOperatorFactory wrapFact = new FipsRSA.KeyWrapOperatorFactory();
        FipsKeyWrapperUsingSecureRandom wrapper = (FipsKeyWrapperUsingSecureRandom) wrapFact.createKeyWrapper( pubKey,FipsRSA.WRAP_OAEP)
                .withSecureRandom(CryptoServicesRegistrar.getSecureRandom());

        return wrapper.wrap(inputKeyBytes, 0, inputKeyBytes.length);
    }

    public byte[] unwrapWithPrivKey (PrivateKey priKey, byte[] wrappedKeyBytes) throws InvalidWrappingException {
        AsymmetricRSAPrivateKey rsaPriKey = new AsymmetricRSAPrivateKey(FipsRSA.ALGORITHM, priKey.getEncoded());
        return unwrapKey(rsaPriKey,wrappedKeyBytes);
    }

    private byte[] unwrapKey(AsymmetricRSAPrivateKey privKey, byte[] wrappedKeyBytes) throws InvalidWrappingException {
        FipsRSA.KeyWrapOperatorFactory wrapFact = new FipsRSA.KeyWrapOperatorFactory();
        FipsKeyUnwrapperUsingSecureRandom unwrapper = (FipsKeyUnwrapperUsingSecureRandom) wrapFact.createKeyUnwrapper(privKey,FipsRSA.WRAP_OAEP)
                .withSecureRandom(CryptoServicesRegistrar.getSecureRandom());

        return unwrapper.unwrap(wrappedKeyBytes, 0, wrappedKeyBytes.length);
    }

//    public static SecureRandom buildDrbg(){
//
//        EntropySourceProvider entSource = new BasicEntropySourceProvider(new SecureRandom(), true);
//        FipsDRBG.Builder drgbBldr = FipsDRBG.SHA512_HMAC.fromEntropySource(entSource).setSecurityStrength(256).setEntropyBitsRequired(256);
//        return drgbBldr.build("varibale per inicialitzar".getBytes(), false);
//    }
//
//    public static SecureRandom buildDrbgForKeys(){
//
//        EntropySourceProvider entSource = new BasicEntropySourceProvider(new SecureRandom(), true);
//        FipsDRBG.Builder drgbBldr = FipsDRBG.SHA512_HMAC.fromEntropySource(entSource).setSecurityStrength(256).setEntropyBitsRequired(256)
//                .setPersonalizationString("una altra variable per inicialitzar".getBytes());
//        return drgbBldr.build("varibale per inicialitzar".getBytes(), true);
//    }


}
