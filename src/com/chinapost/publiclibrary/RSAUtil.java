package com.chinapost.publiclibrary;



import java.io.ByteArrayOutputStream;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.PublicKey;
import java.security.Security;
import java.security.Signature;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.Cipher;

import org.kobjects.base64.Base64;

import android.text.TextUtils;
import android.util.Log;

/** */
/**
 * <p>
 * RSA公钥/私钥/签名工具包
 * </p>
 * <p>
 * 罗纳德·李维斯特（Ron [R]ivest）、阿迪·萨莫尔（Adi [S]hamir）和伦纳德·阿德曼（Leonard [A]dleman）
 * </p>
 * <p>
 * 字符串格式的密钥在未在特殊说明情况下都为BASE64编码格式<br/>
 * 由于非对称加密速度极其缓慢，一般文件不使用它来加密而是使用对称加密，<br/>
 * 非对称加密算法可以用来对对称加密的密钥加密，这样保证密钥的安全也就保证了数据的安全
 * </p>
 * 
 * @author tangkang
 * @date 2014-12-19
 * @version 1.0
 */
public class RSAUtil {

    /**
     * 加密算法RSA
     */
    public static final String KEY_ALGORITHM = "RSA";

    /**
     * 签名算法
     */
    public static final String SIGNATURE_ALGORITHM = "MD5withRSA";

    /**
     * 获取公钥的key
     */
    private static final String PUBLIC_KEY = "RSAPublicKey";

    /**
     * 获取私钥的key
     */
    private static final String PRIVATE_KEY = "RSAPrivateKey";

    /**
     * RSA key size
     */
    private static final int KEY_SIZE = 1024;
    /**
     * RSA最大加密明文大小
     */
    private static int MAX_ENCRYPT_BLOCK = KEY_SIZE / 8 - 11;// 1024:117

    /**
     * RSA最大解密密文大小
     */
    private static int MAX_DECRYPT_BLOCK = KEY_SIZE / 8;// 1024:128

    private static final String SUN_RSA_SIGN = "BC";// 指定支持的RSA名称

    private static final String SUN_JCE = "BC";//

    public final static String CHAR_SET = "ISO-8859-1";// ok

    /**
     * 公钥
     */
    private static String publicKey; //
    /**
     * 私钥
     */
    private static String privateKey; //

    /**
     * 初始化密钥对(公钥和私钥)
     * 
     * @throws Exception
     */
    public static void initKeys() {
	try {
	    // 产生密钥对
	    Map<String, Object> keyMap = RSAUtil.genKeyPair();
	    // 产生公约
	    publicKey = RSAUtil.getPublicKey(keyMap);
	    // 产生私钥
	    privateKey = RSAUtil.getPrivateKey(keyMap);
	} catch (Exception e) {
	    e.printStackTrace();
	}
    }

    /**
     * <p>
     * 生成密钥对(公钥和私钥)
     * </p>
     * 
     * @return
     * @throws Exception
     */
    public static Map<String, Object> genKeyPair() throws Exception {
	KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance(
		KEY_ALGORITHM, SUN_RSA_SIGN);
	Provider[] providers = Security.getProviders();
	for (int i = 0; i < providers.length; i++) {
	    String name = providers[i].getName();
	    System.out.println("provider business name is:" + name);

	}
	keyPairGen.initialize(KEY_SIZE);
	KeyPair keyPair = keyPairGen.generateKeyPair();
	RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
	RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
	Map<String, Object> keyMap = new HashMap<String, Object>(2);
	keyMap.put(PUBLIC_KEY, publicKey);
	keyMap.put(PRIVATE_KEY, privateKey);
	return keyMap;
    }

    /** */
    /**
     * <p>
     * 用私钥对信息生成数字签名
     * </p>
     * 
     * @param data
     *            已加密数据
     * @param privateKey
     *            私钥(BASE64编码)
     * 
     * @return
     * @throws Exception
     */
    public static String sign(byte[] data, String privateKey) throws Exception {
	byte[] keyBytes = decryptBASE64(privateKey);
	PKCS8EncodedKeySpec pkcs8KeySpec = new PKCS8EncodedKeySpec(keyBytes);
	KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORITHM,
		SUN_RSA_SIGN);
	PrivateKey privateK = keyFactory.generatePrivate(pkcs8KeySpec);
	Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM,
		SUN_RSA_SIGN);
	signature.initSign(privateK);
	signature.update(data);
	return encryptBASE64(signature.sign());
    }

    /** */
    /**
     * <p>
     * 校验数字签名
     * </p>
     * 
     * @param data
     *            已加密数据
     * @param publicKey
     *            公钥(BASE64编码)
     * @param sign
     *            数字签名
     * 
     * @return
     * @throws Exception
     * 
     */
    public static boolean verify(byte[] data, String publicKey, String sign)
	    throws Exception {
	byte[] keyBytes = decryptBASE64(publicKey);
	X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
	KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORITHM,
		SUN_RSA_SIGN);
	PublicKey publicK = keyFactory.generatePublic(keySpec);
	Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM,
		SUN_RSA_SIGN);
	signature.initVerify(publicK);
	signature.update(data);
	return signature.verify(decryptBASE64(sign));
    }

    /** */
    /**
     * <P>
     * 私钥解密
     * </p>
     * 
     * @param encryptedData
     *            已加密数据
     * @param privateKey
     *            私钥(BASE64编码)
     * @return
     * @throws Exception
     */
    public static byte[] decryptByPrivateKey(byte[] encryptedData,
	    String privateKey) throws Exception {
	byte[] keyBytes = decryptBASE64(privateKey);
	PKCS8EncodedKeySpec pkcs8KeySpec = new PKCS8EncodedKeySpec(keyBytes);
	KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORITHM,
		SUN_RSA_SIGN);
	Key privateK = keyFactory.generatePrivate(pkcs8KeySpec);
	Cipher cipher = Cipher.getInstance(keyFactory.getAlgorithm(), SUN_JCE);
	cipher.init(Cipher.DECRYPT_MODE, privateK);
	int inputLen = encryptedData.length;
	ByteArrayOutputStream out = new ByteArrayOutputStream();
	int offSet = 0;
	byte[] cache;
	int i = 0;
	// 对数据分段解密
	while (inputLen - offSet > 0) {
	    System.out.println(encryptedData + "====" + offSet + "======"
		    + MAX_DECRYPT_BLOCK);
	    if (inputLen - offSet > MAX_DECRYPT_BLOCK) {
		cache = cipher
			.doFinal(encryptedData, offSet, MAX_DECRYPT_BLOCK);
	    } else {
		cache = cipher
			.doFinal(encryptedData, offSet, inputLen - offSet);
	    }
	    out.write(cache, 0, cache.length);
	    i++;
	    offSet = i * MAX_DECRYPT_BLOCK;
	}
	byte[] decryptedData = out.toByteArray();
	out.close();
	return decryptedData;
    }

    /** */
    /**
     * <p>
     * 公钥解密
     * </p>
     * 
     * @param encryptedData
     *            已加密数据
     * @param publicKey
     *            公钥(BASE64编码)
     * @return
     * @throws Exception
     */
    public static byte[] decryptByPublicKey(byte[] encryptedData,
	    String publicKey) throws Exception {
	byte[] keyBytes = decryptBASE64(publicKey);
	X509EncodedKeySpec x509KeySpec = new X509EncodedKeySpec(keyBytes);
	KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORITHM,
		SUN_RSA_SIGN);
	Key publicK = keyFactory.generatePublic(x509KeySpec);
	Cipher cipher = Cipher.getInstance(keyFactory.getAlgorithm(), SUN_JCE);
	cipher.init(Cipher.DECRYPT_MODE, publicK);
	int inputLen = encryptedData.length;
	ByteArrayOutputStream out = new ByteArrayOutputStream();
	int offSet = 0;
	byte[] cache;
	int i = 0;
	// 对数据分段解密
	while (inputLen - offSet > 0) {
	    if (inputLen - offSet > MAX_DECRYPT_BLOCK) {
		cache = cipher
			.doFinal(encryptedData, offSet, MAX_DECRYPT_BLOCK);
	    } else {
		cache = cipher
			.doFinal(encryptedData, offSet, inputLen - offSet);
	    }
	    out.write(cache, 0, cache.length);
	    i++;
	    offSet = i * MAX_DECRYPT_BLOCK;
	}
	byte[] decryptedData = out.toByteArray();
	out.close();
	return decryptedData;
    }

    /** */
    /**
     * <p>
     * 公钥加密
     * </p>
     * 
     * @param data
     *            源数据
     * @param publicKey
     *            公钥(BASE64编码)
     * @return
     * @throws Exception
     */
    public static byte[] encryptByPublicKey(byte[] data, String publicKey)
	    throws Exception {
	byte[] keyBytes = decryptBASE64(publicKey);
	X509EncodedKeySpec x509KeySpec = new X509EncodedKeySpec(keyBytes);
	KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORITHM,
		SUN_RSA_SIGN);
	System.out.println(keyFactory.getProvider().getName());
	Key publicK = keyFactory.generatePublic(x509KeySpec);
	// 对数据加密
	Cipher cipher = Cipher.getInstance(keyFactory.getAlgorithm(), SUN_JCE);
	System.out.println(cipher.getProvider().getName());
	cipher.init(Cipher.ENCRYPT_MODE, publicK);
	int inputLen = data.length;
	ByteArrayOutputStream out = new ByteArrayOutputStream();
	int offSet = 0;
	byte[] cache;
	int i = 0;
	// 对数据分段加密
	while (inputLen - offSet > 0) {
	    if (inputLen - offSet > MAX_ENCRYPT_BLOCK) {
		cache = cipher.doFinal(data, offSet, MAX_ENCRYPT_BLOCK);
	    } else {
		cache = cipher.doFinal(data, offSet, inputLen - offSet);
	    }
	    out.write(cache, 0, cache.length);
	    i++;
	    offSet = i * MAX_ENCRYPT_BLOCK;
	}
	byte[] encryptedData = out.toByteArray();
	out.close();
	return encryptedData;
    }

    /** */
    /**
     * <p>
     * 私钥加密
     * </p>
     * 
     * @param data
     *            源数据
     * @param privateKey
     *            私钥(BASE64编码)
     * @return
     * @throws Exception
     */
    public static byte[] encryptByPrivateKey(byte[] data, String privateKey)
	    throws Exception {
	byte[] keyBytes = decryptBASE64(privateKey);
	PKCS8EncodedKeySpec pkcs8KeySpec = new PKCS8EncodedKeySpec(keyBytes);
	KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORITHM,
		SUN_RSA_SIGN);
	Key privateK = keyFactory.generatePrivate(pkcs8KeySpec);
	Cipher cipher = Cipher.getInstance(keyFactory.getAlgorithm(), SUN_JCE);
	cipher.init(Cipher.ENCRYPT_MODE, privateK);
	int inputLen = data.length;
	ByteArrayOutputStream out = new ByteArrayOutputStream();
	int offSet = 0;
	byte[] cache;
	int i = 0;
	// 对数据分段加密
	while (inputLen - offSet > 0) {
	    if (inputLen - offSet > MAX_ENCRYPT_BLOCK) {
		cache = cipher.doFinal(data, offSet, MAX_ENCRYPT_BLOCK);
	    } else {
		cache = cipher.doFinal(data, offSet, inputLen - offSet);
	    }
	    out.write(cache, 0, cache.length);
	    i++;
	    offSet = i * MAX_ENCRYPT_BLOCK;
	}
	byte[] encryptedData = out.toByteArray();
	out.close();
	return encryptedData;
    }

    /** */
    /**
     * <p>
     * 获取私钥
     * </p>
     * 
     * @param keyMap
     *            密钥对
     * @return
     * @throws Exception
     */
    public static String getPrivateKey(Map<String, Object> keyMap)
	    throws Exception {
	Key key = (Key) keyMap.get(PRIVATE_KEY);
	return encryptBASE64(key.getEncoded());
    }

    /** */
    /**
     * <p>
     * 获取公钥
     * </p>
     * 
     * @param keyMap
     *            密钥对
     * @return
     * @throws Exception
     */
    public static String getPublicKey(Map<String, Object> keyMap)
	    throws Exception {
	Key key = (Key) keyMap.get(PUBLIC_KEY);
	return encryptBASE64(key.getEncoded());
    }

    public static void clearKeys() {
	publicKey = "";
	privateKey = "";
    }

    /**
     * get公约
     * 
     * @return
     */
    public static String getPublicKey() {
	if (TextUtils.isEmpty(publicKey))
	    initKeys();
	return publicKey;
    }

    /**
     * get私钥
     * 
     * @return
     */
    public static String getPrivateKey() {
	if (TextUtils.isEmpty(privateKey))
	    initKeys();
	return privateKey;
    }

    /**
     * BASE64解密
     * 
     * @param key
     * @return
     * @throws Exception
     */
    public static byte[] decryptBASE64(String key) throws Exception {
	return Base64.decode(key);
    }

    /**
     * BASE64加密
     * 
     * @param key
     * @return
     * @throws Exception
     */
    public static String encryptBASE64(byte[] key) throws Exception {
	return Base64.encode(key);
    }

    // =================================test=========================================

    public static void main(String[] args) throws Exception { 
   	 
  	  RSAUtil rSAUtil=new RSAUtil();
  	//  String key="MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDmEOLRXHiYiHlhJ0q2Q8IS6GGE2jmohHdQd7mW6EsjZquZE/gxeCVLlQFIXpsM8R6ikY5L+uD0u6ydL6J7sSgtjbjTUMuICdT9LbHs07PX7OMVtyCqsZnG+A6iH6EKdVAbWCNdDfXGmF7qmpcsoJeGpGq+kmCE6/xIRfqVgBVovQIDAQAB";
  	 // String data="aa";
  	  
//  	byte[] rs=rSAUtil.encryptByPublicKey(data.getBytes(),key);
//  	  String result = new String(rs);
//  	  System.out.println(result);
    }
    /*
     * public static void main(String[] args) throws Exception { test(); //
     * testSign(); }
     * 
     * static void test() throws Exception { System.err.println("公钥加密——私钥解密");
     * String source = "这是一行没有任何意义的文字，你看完了等于没看，不是吗？";
     * System.out.println("\r加密前文字：\r\n" + source);
     * System.out.println(source.length()); byte[] data = source.getBytes();
     * byte[] encodedData = RSAUtil.encryptByPublicKey(data, getPublicKey());
     * String string = new String(encodedData,RSAUtil.CHAR_SET);
     * System.out.println("加密后文字：\r\n" + string); byte[] decodedData =
     * RSAUtil.decryptByPrivateKey(string.getBytes(RSAUtil.CHAR_SET),
     * getPrivateKey()); String target = new String(decodedData);
     * System.out.println("解密后文字: \r\n" + target); }
     * 
     * 
     * static void testSign() throws Exception {
     * System.err.println("私钥加密——公钥解密"); String source = "这是一行测试RSA数字签名的无意义文字";
     * System.out.println("原文字：\r\n" + source); byte[] data = source.getBytes();
     * byte[] encodedData = RSAUtil.encryptByPrivateKey(data, privateKey);
     * System.out.println("加密后：\r\n" + new String(encodedData)); byte[]
     * decodedData = RSAUtil.decryptByPublicKey(encodedData, publicKey); String
     * target = new String(decodedData); System.out.println("解密后: \r\n" +
     * target); System.err.println("私钥签名——公钥验证签名"); String sign =
     * RSAUtil.sign(encodedData, privateKey); System.err.println("签名:\r" +
     * sign); boolean status = RSAUtil.verify(encodedData, publicKey, sign);
     * System.err.println("验证结果:\r" + status); }
     */
    // =================================test=========================================

}
