package tools;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import org.apache.commons.codec.binary.Base64;  

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class AES {

	public static String encrypt(String content, String password) {  
        try {             
                KeyGenerator kgen = KeyGenerator.getInstance("AES");  
                kgen.init(128, new SecureRandom(password.getBytes()));  
                SecretKey secretKey = kgen.generateKey();  
                byte[] enCodeFormat = secretKey.getEncoded();  
                SecretKeySpec key = new SecretKeySpec(enCodeFormat, "AES");  
                Cipher cipher = Cipher.getInstance("AES");// 创建密码器   
                byte[] byteContent = content.getBytes("utf-8");  
                cipher.init(Cipher.ENCRYPT_MODE, key);// 初始化   
                byte[] result = cipher.doFinal(byteContent);  
                return Base64.encodeBase64String(result); // 加密   
        } catch (NoSuchAlgorithmException e) {  
                e.printStackTrace();  
        } catch (NoSuchPaddingException e) {  
                e.printStackTrace();  
        } catch (InvalidKeyException e) {  
                e.printStackTrace();  
        } catch (UnsupportedEncodingException e) {  
                e.printStackTrace();  
        } catch (IllegalBlockSizeException e) {  
                e.printStackTrace();  
        } catch (BadPaddingException e) {  
                e.printStackTrace();  
        }  
        return null;  
	}
	
	public static void main(String[] args)
	{
		String kind="1";
		
		String key="802100433";
		String strfind= " jgdm='802100433'";
		String pwd="phabro";
		String lastxzqh="";
		String aeskey="phabro";
		
		System.out.println(String.format("https://s.nacao.org.cn/specialResult.html?x=%s&k=%s&s=%s&y=%s",
				encrypt(lastxzqh,aeskey),
				encrypt(kind,aeskey),
				encrypt(strfind,aeskey),
				encrypt(key,aeskey)));
		
//		System.out.println(encrypt(key,pwd));
		
	}
}
