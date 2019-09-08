package com.problemeszero.crypto;

import org.bouncycastle.crypto.*;
import org.bouncycastle.util.encoders.Hex;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.*;
import java.util.Base64;

public class RedWaxSec {

    private SecretKeySpec k;
    private byte[] k1;
    private byte[] k2;
    private byte[] iv;

    public RedWaxSec() {

        CryptoServicesRegistrar.setSecureRandom(new SecureRandom());
        this.iv = new byte[16];
        CryptoServicesRegistrar.getSecureRandom().nextBytes(iv);

        byte[] keyBytes = new byte[32];
        CryptoServicesRegistrar.getSecureRandom().nextBytes(keyBytes);
        this.k = new SecretKeySpec(keyBytes, "AES");

        keyBytes = new byte[32];
        CryptoServicesRegistrar.getSecureRandom().nextBytes(keyBytes);
        this.k1 = new SecretKeySpec(keyBytes, "AES").getEncoded();

        generateK2();
    }

    public static SecretKeySpec defineKey(byte[] keyBytes)
    {
        if (keyBytes.length != 16 && keyBytes.length != 24 && keyBytes.length != 32) {
            throw new IllegalArgumentException("keyBytes wrong length for AES key");
        }

        return new SecretKeySpec(keyBytes, "AES");
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
        this.k2 = xorOperation(k.getEncoded(),k1);
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
        Cipher cipher = null;
        byte [] cipherText=null;

        try {
            cipher = Cipher.getInstance("AES/CBC/PKCS7Padding", "BC");
            cipher.init(Cipher.ENCRYPT_MODE, this.k);
            cipherText = cipher.doFinal(plainText);

        } catch (InvalidKeyException  | IllegalBlockSizeException | BadPaddingException |
                NoSuchAlgorithmException | NoSuchProviderException  |  NoSuchPaddingException e){
            e.printStackTrace();
        }
        return new byte[][] {cipher.getIV(), cipherText };
    }


//    public byte[][] cbcEncrypt(byte[] plainText) {
//        FipsOutputEncryptor<FipsAES.Parameters> outputEncryptor = this.fipsSymmetricFactory.createOutputEncryptor(this.k, FipsAES.CBCwithPKCS7.withIV(this.iv));
//        ByteArrayOutputStream bOut = new ByteArrayOutputStream();
//        CipherOutputStream encOut = outputEncryptor.getEncryptingStream(bOut);
//        encOut.update(plainText);
//        try {
//            encOut.close();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        return new byte[][] { this.iv, bOut.toByteArray() };
//    }

       public byte[] cbcDecrypt(byte[] iv, byte[] cipherText ) {
           byte[] plainText = null;
           Cipher cipher;

           try {
               cipher = Cipher.getInstance("AES/CBC/PKCS7Padding", "BC");
               cipher.init(Cipher.DECRYPT_MODE, this.k, new IvParameterSpec(iv));
               plainText = cipher.doFinal(cipherText);

           } catch (InvalidKeyException | IllegalBlockSizeException | BadPaddingException |
                   NoSuchAlgorithmException | NoSuchProviderException |InvalidAlgorithmParameterException | NoSuchPaddingException e) {
               e.printStackTrace();
           }
           return plainText;
       }

//    public byte[] cbcDecrypt(byte[] iv, byte[] cipherText ) {
////        System.err.println(java.util.Arrays.toString(iv));
//
//        FipsInputDecryptor<FipsAES.Parameters> inputDecryptor = this.fipsSymmetricFactory.createInputDecryptor(this.k, FipsAES.CBCwithPKCS7.withIV(iv));
//        ByteArrayOutputStream bOut = new ByteArrayOutputStream();
//        InputStream encIn = inputDecryptor.getDecryptingStream(new ByteArrayInputStream(cipherText));
//        int ch;
//
//        try {
//            while ((ch = encIn.read()) >= 0) {
//                bOut.write(ch);
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        return bOut.toByteArray();
//    }

    public byte[] wrapWithPubKey (PublicKey pubKey, byte[] inputKeyBytes)  {
        Cipher cipher;
        byte[] wrappedKey = null;
        try {
            cipher = Cipher.getInstance("RSA/NONE/NoPadding", "BC");
            cipher.init(Cipher.ENCRYPT_MODE, pubKey);

            wrappedKey = cipher.doFinal(inputKeyBytes);
        } catch (IllegalBlockSizeException | InvalidKeyException | NoSuchAlgorithmException | NoSuchProviderException |
                NoSuchPaddingException | BadPaddingException e ) {
            e.printStackTrace();
        }
        return wrappedKey;
     }

//    public byte[] wrapWithPubKey (PublicKey pubKey, byte[] inputKeyBytes) throws PlainInputProcessingException {
//        AsymmetricRSAPublicKey rsaPubKey = new AsymmetricRSAPublicKey(FipsRSA.ALGORITHM, pubKey.getEncoded());
//        return wrapKey(rsaPubKey,inputKeyBytes);
//    }
//
//    private byte[] wrapKey(AsymmetricRSAPublicKey pubKey, byte[] inputKeyBytes) throws PlainInputProcessingException {
//        FipsRSA.KeyWrapOperatorFactory wrapFact = new FipsRSA.KeyWrapOperatorFactory();
//        FipsKeyWrapperUsingSecureRandom wrapper = (FipsKeyWrapperUsingSecureRandom) wrapFact.createKeyWrapper( pubKey,FipsRSA.WRAP_OAEP)
//                .withSecureRandom(CryptoServicesRegistrar.getSecureRandom());
//
//        return wrapper.wrap(inputKeyBytes, 0, inputKeyBytes.length);
//    }

    public byte[] unwrapWithPrivKey (PrivateKey priKey, byte[] wrappedKeyBytes) {
        Cipher cipher;
        byte[] plainText = null;
        try {
            cipher = Cipher.getInstance("RSA/None/NoPadding", "BC");
            cipher.init(Cipher.DECRYPT_MODE, priKey);
            plainText = cipher.doFinal(wrappedKeyBytes);

		} catch (IllegalBlockSizeException | InvalidKeyException | NoSuchAlgorithmException | NoSuchProviderException |
                NoSuchPaddingException | BadPaddingException e ) {
            e.printStackTrace();
        }
        return plainText;
    }

//    public byte[] unwrapWithPrivKey (PrivateKey priKey, byte[] wrappedKeyBytes) throws InvalidWrappingException {
//        AsymmetricRSAPrivateKey rsaPriKey = new AsymmetricRSAPrivateKey(FipsRSA.ALGORITHM, priKey.getEncoded());
//        return unwrapKey(rsaPriKey,wrappedKeyBytes);
//    }
//
//    private byte[] unwrapKey(AsymmetricRSAPrivateKey privKey, byte[] wrappedKeyBytes) throws InvalidWrappingException {
//        FipsRSA.KeyWrapOperatorFactory wrapFact = new FipsRSA.KeyWrapOperatorFactory();
//        FipsKeyUnwrapperUsingSecureRandom unwrapper = (FipsKeyUnwrapperUsingSecureRandom) wrapFact.createKeyUnwrapper(privKey,FipsRSA.WRAP_OAEP)
//                .withSecureRandom(CryptoServicesRegistrar.getSecureRandom());
//
//        return unwrapper.unwrap(wrappedKeyBytes, 0, wrappedKeyBytes.length);
//    }


}

