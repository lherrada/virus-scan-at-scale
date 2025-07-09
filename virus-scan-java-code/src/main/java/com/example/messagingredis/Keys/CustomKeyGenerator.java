package com.example.messagingredis.Keys;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.interceptor.KeyGenerator;

import java.lang.reflect.Method;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class CustomKeyGenerator implements KeyGenerator {
    private final String HASH_ALGO = "SHA3-256";

    MessageDigest messageDigest = MessageDigest.getInstance(HASH_ALGO);
    private static final Logger LOGGER = LoggerFactory.getLogger(CustomKeyGenerator.class);

    public CustomKeyGenerator() throws NoSuchAlgorithmException {
    }

    @Override
    public Object generate(Object target, Method method, Object... params) {
        byte[] data = (byte[])params[1];
        return bytesToHex(data);
    }

    public  String bytesToHex(byte[] data) {
        byte[] hash = messageDigest.digest(data);

        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            hexString.append(String.format("%02x", b));
        }
        return hexString.toString();
    }
}
