/**
 * 
 */
package com.chinapost.baselib.utils.compress;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import org.apache.tools.zip.ZipOutputStream;

/**
 * @author WangYi
 * 2013-6-14 上午10:55:24
 * 压缩文件
 */
public class CompressBook {
	
	 	/**
	 	 * dirPath 文件夹路径
	 	 * zipFilePath 压缩后文件存放路径
	 	 */
	    public static void compressDir(String dirPath,String zipFilePath) throws Exception {
	        zip(zipFilePath, new File(dirPath));
	    }

	    private static void zip(String zipFileName, File inputFile) throws Exception {
	        ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zipFileName));
	        zip(out, inputFile, "");
	        out.close();
	    }

	    private static void zip(ZipOutputStream out, File f, String base) throws Exception {
	        if (f.isDirectory()) {
	           File[] fl = f.listFiles();
	           out.putNextEntry(new org.apache.tools.zip.ZipEntry(base + "/"));
	           base = base.length() == 0 ? "" : base + "/";
	           for (int i = 0; i < fl.length; i++) {
	           zip(out, fl[i], base + fl[i].getName());
	         }
	        }else {
	           out.putNextEntry(new org.apache.tools.zip.ZipEntry(base));
	           FileInputStream in = new FileInputStream(f);
	           int b;
	           while ( (b = in.read()) != -1) {
	            out.write(b);
	         }
	         in.close();
	       }
	    }
	    
	    public static void main(String [] temp){
	        try {
	           compressDir("c:\\TEMP","c:\\cc.zip");
	        }catch (Exception ex) {
	           ex.printStackTrace();
	       }
	    }



}
