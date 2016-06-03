/**
 * 
 */
package com.chinapost.baselib.utils;

import java.io.IOException;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.kobjects.base64.Base64;

/**
 * @author WangYi 
 * 2013-05-23 上午11:45:10 
 * 加密和解密工具类
 */
public class EncryptionAndDecryption {
	
	/**
	 * Description 根据键值进行加密
	 * @param data
	 * @param key加密键byte数组
	 * @return
	 * @throws Exception
	 */
	public static String encrypt(String data, String key) throws Exception {
		key = key.substring(0, 8);
		byte[] bt = encrypt(data.getBytes("UTF-8"), key.getBytes("UTF-8"));
		String strs = Base64.encode(bt);
		return strs;
	}

	/**
	 * Description 根据键值进行解密
	 * @param data
	 * @param key 加密键byte数组
	 * @return
	 * @throws IOException
	 * @throws Exception
	 */
	public static String decrypt(String data, String key) throws IOException,
			Exception {
		if (data == null)
			return null;
		key = key.substring(0, 8);
		byte[] buf = Base64.decode(data);
		byte[] bt = decrypt(buf, key.getBytes("UTF-8"));
		return new String(bt, "UTF-8");
	}

	/**
	 * Description 根据键值进行加密
	 * @param data
	 * @param key加密键byte数组
	 * @return
	 * @throws Exception
	 */
	private static byte[] encrypt(byte[] data, byte[] key) throws Exception {
		byte[] iv = new byte[]{ 1,2,3,4,5,6,7,8 };
		IvParameterSpec zeroIv = new IvParameterSpec(iv);
		SecretKeySpec secretKeySpec = new SecretKeySpec(key, "DES");
		Cipher cipher = Cipher.getInstance("DES/CBC/PKCS5Padding");
		cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, zeroIv);
		return cipher.doFinal(data);
	}

	/**
	 * Description 根据键值进行解密
	 * @param data
	 * @param key 加密键byte数组
	 * @return
	 * @throws Exception
	 */
	private static byte[] decrypt(byte[] data, byte[] key) throws Exception {
		byte[] iv = new byte[] { 1,2,3,4,5,6,7,8 };
		IvParameterSpec zeroIv = new IvParameterSpec(iv);
        SecretKeySpec secretKeySpec = new SecretKeySpec(key, "DES");
        Cipher cipher = Cipher.getInstance("DES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, zeroIv);
        return cipher.doFinal(data);
	}

}
