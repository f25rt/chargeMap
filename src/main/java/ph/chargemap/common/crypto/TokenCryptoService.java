package ph.chargemap.common.crypto;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES-256-GCM encryption for sensitive at-rest values (OAuth tokens, VIN).
 *
 * <p>Ciphertext format is {@code base64(iv):base64(ciphertext+tag)}. A fresh 96-bit IV is
 * generated per value. The 256-bit key comes from {@code CHARGEMAP_CRYPTO_KEY} (Base64,
 * 32 bytes) in real environments; when unset, a deterministic dev-only key is derived and
 * a warning is logged. Never log plaintext or ciphertext values.
 */
@Service
public class TokenCryptoService {

    private static final Logger log = LoggerFactory.getLogger(TokenCryptoService.class);
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;

    private final SecretKeySpec key;
    private final SecureRandom random = new SecureRandom();

    public TokenCryptoService() {
        this.key = resolveKey();
    }

    private SecretKeySpec resolveKey() {
        String configured = System.getenv("CHARGEMAP_CRYPTO_KEY");
        if (configured != null && !configured.isBlank()) {
            byte[] raw = Base64.getDecoder().decode(configured.trim());
            if (raw.length != 32) {
                throw new IllegalStateException(
                        "CHARGEMAP_CRYPTO_KEY must be Base64-encoded 32 bytes (256-bit)");
            }
            return new SecretKeySpec(raw, "AES");
        }
        // Dev-only fallback: derive a stable 256-bit key so encrypted values survive a
        // restart in local dev. NOT for production.
        log.warn("CHARGEMAP_CRYPTO_KEY not set — using a DEV-ONLY derived key. Set a real "
                + "key in production.");
        byte[] derived = sha256("chargemap-dev-only-crypto-key-do-not-use-in-prod".getBytes(
                StandardCharsets.UTF_8));
        return new SecretKeySpec(derived, "AES");
    }

    /** Encrypts plaintext; returns {@code base64(iv):base64(ciphertext)} or null for null input. */
    public String encrypt(String plaintext) {
        if (plaintext == null) {
            return null;
        }
        try {
            byte[] iv = new byte[IV_BYTES];
            random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            byte[] ct = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(iv) + ":"
                    + Base64.getEncoder().encodeToString(ct);
        } catch (Exception e) {
            throw new IllegalStateException("Encryption failed", e);
        }
    }

    /** Reverses {@link #encrypt}; returns null for null input. */
    public String decrypt(String stored) {
        if (stored == null) {
            return null;
        }
        try {
            int sep = stored.indexOf(':');
            if (sep < 0) {
                throw new IllegalArgumentException("Malformed ciphertext");
            }
            byte[] iv = Base64.getDecoder().decode(stored.substring(0, sep));
            byte[] ct = Base64.getDecoder().decode(stored.substring(sep + 1));
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            return new String(cipher.doFinal(ct), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("Decryption failed", e);
        }
    }

    private static byte[] sha256(byte[] input) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(input);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
