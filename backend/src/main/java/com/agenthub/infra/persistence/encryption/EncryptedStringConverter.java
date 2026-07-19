package com.agenthub.infra.persistence.encryption;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import java.lang.reflect.Field;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES-256-GCM encryption converter for JPA entity fields.
 *
 * <p>Uses the configured {@code agenthub.db.encryption.key} to encrypt/decrypt
 * sensitive string fields transparently. The encrypted value is stored as
 * Base64-encoded {@code IV || ciphertext} in the database.
 *
 * <p>Fields annotated with {@code @Convert(converter = EncryptedStringConverter.class)}
 * are automatically encrypted on persist and decrypted on load.
 *
 * <p>Example:
 * <pre>
 *   {@code @Convert(converter = EncryptedStringConverter.class)}
 *   private String secret;
 * </pre>
 */
@Converter(autoApply = false)
public class EncryptedStringConverter implements AttributeConverter<String, String> {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128;
    private static final int GCM_IV_LENGTH = 12;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /** Lazily initialized key spec */
    private volatile javax.crypto.spec.SecretKeySpec keySpec;

    public EncryptedStringConverter() {
        // No-arg constructor for JPA. Key is resolved lazily on first use.
    }

    private javax.crypto.spec.SecretKeySpec getKeySpec() {
        if (keySpec == null) {
            synchronized (this) {
                if (keySpec == null) {
                    keySpec = buildKeySpec(resolveKey());
                }
            }
        }
        return keySpec;
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return attribute;
        }
        try {
            javax.crypto.spec.SecretKeySpec spec = getKeySpec();
            javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance(ALGORITHM);
            byte[] iv = new byte[GCM_IV_LENGTH];
            SECURE_RANDOM.nextBytes(iv);
            javax.crypto.spec.GCMParameterSpec gcmSpec = new javax.crypto.spec.GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, spec, gcmSpec);

            byte[] encrypted = cipher.doFinal(attribute.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            byte[] combined = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);
            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to encrypt attribute", ex);
        }
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty()) {
            return dbData;
        }
        try {
            javax.crypto.spec.SecretKeySpec spec = getKeySpec();
            byte[] combined = Base64.getDecoder().decode(dbData);
            if (combined.length < GCM_IV_LENGTH) {
                throw new IllegalArgumentException("Invalid encrypted data length");
            }
            byte[] iv = new byte[GCM_IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH);
            byte[] encrypted = new byte[combined.length - GCM_IV_LENGTH];
            System.arraycopy(combined, GCM_IV_LENGTH, encrypted, 0, encrypted.length);

            javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance(ALGORITHM);
            javax.crypto.spec.GCMParameterSpec gcmSpec = new javax.crypto.spec.GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(javax.crypto.Cipher.DECRYPT_MODE, spec, gcmSpec);

            byte[] decrypted = cipher.doFinal(encrypted);
            return new String(decrypted, java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to decrypt attribute", ex);
        }
    }

    // ------------------------------------------------------------------
    // Key configuration
    // ------------------------------------------------------------------

    private static String resolveKey() {
        // Priority: system property > env var > default
        String key = System.getProperty("agenthub.db.encryption.key");
        if (key == null || key.isEmpty()) {
            key = System.getenv("AGENTHUB_DB_ENCRYPTION_KEY");
        }
        if (key == null || key.isEmpty()) {
            // Return a default-derived key so tests without explicit config don't fail.
            // In production, the key MUST be set via env var or system property.
            key = "LSY@!0924";
        }
        return key;
    }

    private static javax.crypto.spec.SecretKeySpec buildKeySpec(String key) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] keyBytes = digest.digest(key.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return new javax.crypto.spec.SecretKeySpec(keyBytes, "AES");
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 not available", ex);
        }
    }
}
