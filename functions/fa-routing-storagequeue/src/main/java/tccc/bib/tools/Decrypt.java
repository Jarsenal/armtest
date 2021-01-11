package tccc.bib.tools;

import javax.crypto.spec.SecretKeySpec;
import javax.crypto.Cipher;
import java.util.Base64;
import java.util.AbstractList;
import java.util.AbstractMap;

public class Decrypt {

    public static AbstractMap<String, Object> decryptItems(
            AbstractMap<String, Object> routeConfig,
            AbstractList<String> _fields, 
            String key) throws Exception {
        
            for (String _field : _fields) {
                routeConfig.put(_field, decrypt(
                    (String)routeConfig.get(_field),
                    key
                ));
            }
            return routeConfig;
    }

    public static String decrypt(String strEncrypted, String key) 
        throws Exception{
        String strData="";
        
        try {
            SecretKeySpec skeyspec=new SecretKeySpec(key.getBytes(),"Blowfish");
            Cipher cipher=Cipher.getInstance("Blowfish");
            cipher.init(Cipher.DECRYPT_MODE, skeyspec);
            byte[] decrypted=cipher.doFinal(
                Base64.getDecoder().decode(strEncrypted.getBytes())
            );
            
            strData=new String(decrypted);
            
        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception(e);
        }
        return strData;
    }

}