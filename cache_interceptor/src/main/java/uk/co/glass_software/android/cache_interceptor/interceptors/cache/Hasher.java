package uk.co.glass_software.android.cache_interceptor.interceptors.cache;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;

import uk.co.glass_software.android.cache_interceptor.utils.Function;

class Hasher implements Function<byte[], String> {
    
    private final MessageDigest messageDigest;
    private final static char[] hexArray = "0123456789ABCDEF".toCharArray();
    
    Hasher(MessageDigest messageDigest) {
        this.messageDigest = messageDigest;
    }
    
    String hash(String text) throws UnsupportedEncodingException {
        byte[] textBytes = text.getBytes("UTF-8");
        messageDigest.update(textBytes, 0, textBytes.length);
        return get(messageDigest.digest());
    }
    
    @Override
    public String get(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }
}
