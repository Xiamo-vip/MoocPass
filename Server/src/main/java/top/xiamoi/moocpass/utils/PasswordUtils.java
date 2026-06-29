package top.xiamoi.moocpass.utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * 密码加密/校验工具类
 */
public class PasswordUtils {

    private static final String SALT = "MoocPassSalt2026";

    /**
     * 对原始密码进行 SHA-256 加盐加密
     */
    public static String encrypt(String rawPassword) {
        if (rawPassword == null) {
            return null;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String saltedPassword = rawPassword + SALT;
            byte[] hash = digest.digest(saltedPassword.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("密码加密失败", e);
        }
    }

    /**
     * 校验原始密码与加密后密码是否匹配
     */
    public static boolean verify(String rawPassword, String encryptedPassword) {
        if (rawPassword == null || encryptedPassword == null) {
            return false;
        }
        return encrypt(rawPassword).equalsIgnoreCase(encryptedPassword);
    }
}
