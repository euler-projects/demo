/*
 * Copyright 2013-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.eulerframework.uc.oauth2.service.jwk;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/**
 * Envelope encryption codec for JWK private keys persisted by
 * {@link DefaultJwkManageService}.
 * <p>
 * Algorithm: <strong>AES-256-GCM</strong> (single, fixed; algorithm rotation is
 * delivered via KEK rotation rather than an extra DDL column). Each
 * {@link #encrypt(char[], String)} call generates a fresh 96-bit IV; the
 * 128-bit GCM authentication tag is stored in its own column rather than being
 * appended to the ciphertext. The {@code aad} parameter is bound to the JWK's
 * {@code kid}, defeating row-level ciphertext swap attacks.
 *
 * <h2>KEK sources</h2>
 * <ul>
 *   <li>{@link #fromKeyFile(Path, String)} — production-recommended. The file
 *       MUST contain exactly 32 random bytes and have POSIX permissions
 *       ≤ {@code 0600}.</li>
 *   <li>{@link #fromPassphrase(char[], String)} — development only. Derives a
 *       32-byte KEK via PBKDF2-HMAC-SHA256 (600k iterations, salt derived from
 *       {@code encKid}). A {@code WARN} is emitted on construction.</li>
 * </ul>
 *
 * <h2>Threat model</h2>
 * <ul>
 *   <li>Defends DB-only leakage (backups, SQL injection of {@code SELECT}).</li>
 *   <li>Does NOT defend KEK-file leakage, JVM heap dump, or KEK loss
 *       (operators MUST back up the KEK out-of-band).</li>
 * </ul>
 */
public final class JwkKekCodec {

    private static final Logger log = LoggerFactory.getLogger(JwkKekCodec.class);

    private static final String CIPHER_TRANSFORMATION = "AES/GCM/NoPadding";
    private static final String KEY_ALGORITHM = "AES";
    private static final int IV_LENGTH = 12;
    private static final int TAG_LENGTH_BITS = 128;
    private static final int TAG_LENGTH_BYTES = TAG_LENGTH_BITS / 8;
    private static final int KEK_LENGTH_BYTES = 32;
    private static final int PBKDF2_ITERATIONS = 600_000;
    private static final String PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final String SALT_NAMESPACE = "jwk-kek/";

    private static final Set<PosixFilePermission> ALLOWED_KEY_FILE_PERMS = EnumSet.of(
            PosixFilePermission.OWNER_READ,
            PosixFilePermission.OWNER_WRITE);

    private final SecretKey kek;
    private final String encKid;
    private final SecureRandom secureRandom;

    private JwkKekCodec(SecretKey kek, String encKid, SecureRandom secureRandom) {
        this.kek = Objects.requireNonNull(kek, "kek");
        this.encKid = Objects.requireNonNull(encKid, "encKid");
        this.secureRandom = Objects.requireNonNull(secureRandom, "secureRandom");
    }

    /**
     * Build a codec from a 32-byte KEK file. The file is read once and
     * immediately wiped from the intermediate buffer.
     *
     * @throws IllegalStateException if the file is missing, wrong size, or has
     *                               overly permissive POSIX permissions
     */
    public static JwkKekCodec fromKeyFile(Path keyFile, String encKid) {
        Objects.requireNonNull(keyFile, "keyFile");
        Objects.requireNonNull(encKid, "encKid");
        if (!Files.exists(keyFile)) {
            throw new IllegalStateException("JWK KEK file does not exist: " + keyFile);
        }
        validatePosixPermissions(keyFile);
        byte[] raw;
        try {
            raw = Files.readAllBytes(keyFile);
        }
        catch (IOException ex) {
            throw new IllegalStateException("Unable to read JWK KEK file: " + keyFile, ex);
        }
        if (raw.length != KEK_LENGTH_BYTES) {
            Arrays.fill(raw, (byte) 0);
            throw new IllegalStateException("JWK KEK file must be exactly " + KEK_LENGTH_BYTES
                    + " bytes, got " + raw.length + " (" + keyFile + ")");
        }
        SecretKey key = new SecretKeySpec(raw, KEY_ALGORITHM);
        Arrays.fill(raw, (byte) 0);
        log.info("JWK KEK loaded from key file (encKid={})", encKid);
        return new JwkKekCodec(key, encKid, new SecureRandom());
    }

    /**
     * Build a codec by deriving the KEK from a passphrase via PBKDF2-HMAC-SHA256.
     * <strong>For development only</strong>; emits a {@code WARN} on every
     * construction.
     */
    public static JwkKekCodec fromPassphrase(char[] passphrase, String encKid) {
        Objects.requireNonNull(passphrase, "passphrase");
        Objects.requireNonNull(encKid, "encKid");
        if (passphrase.length == 0) {
            throw new IllegalStateException("JWK KEK passphrase must not be empty");
        }
        log.warn("JWK KEK is derived from a passphrase (encKid={}). " +
                "Passphrase mode is for development only and MUST NOT be used in production.", encKid);
        byte[] salt = deriveSalt(encKid);
        try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM);
            PBEKeySpec spec = new PBEKeySpec(passphrase, salt, PBKDF2_ITERATIONS, KEK_LENGTH_BYTES * 8);
            try {
                byte[] derived = factory.generateSecret(spec).getEncoded();
                SecretKey key = new SecretKeySpec(derived, KEY_ALGORITHM);
                Arrays.fill(derived, (byte) 0);
                return new JwkKekCodec(key, encKid, new SecureRandom());
            }
            finally {
                spec.clearPassword();
            }
        }
        catch (GeneralSecurityException ex) {
            throw new IllegalStateException("Failed to derive JWK KEK from passphrase", ex);
        }
    }

    /** {@code enc_kid} value to be persisted with every encrypted row. */
    public String encKid() {
        return this.encKid;
    }

    /**
     * Encrypt a private-key PEM payload bound to the JWK's {@code kid} as AAD.
     * The supplied {@code plaintextChars} buffer is left untouched (the caller
     * is responsible for wiping it).
     */
    public Ciphertext encrypt(char[] plaintextChars, String aad) {
        Objects.requireNonNull(plaintextChars, "plaintextChars");
        Objects.requireNonNull(aad, "aad");
        byte[] iv = new byte[IV_LENGTH];
        this.secureRandom.nextBytes(iv);
        byte[] plaintext = toUtf8(plaintextChars);
        try {
            Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, this.kek, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            cipher.updateAAD(aad.getBytes(StandardCharsets.UTF_8));
            byte[] full = cipher.doFinal(plaintext);
            // JCA returns ciphertext || tag; split into separate columns.
            int cipherLen = full.length - TAG_LENGTH_BYTES;
            byte[] cipherOnly = Arrays.copyOfRange(full, 0, cipherLen);
            byte[] tag = Arrays.copyOfRange(full, cipherLen, full.length);
            Arrays.fill(full, (byte) 0);
            return new Ciphertext(this.encKid, iv, tag, cipherOnly);
        }
        catch (GeneralSecurityException ex) {
            throw new IllegalStateException("AES-256-GCM encryption failed (kid AAD=" + aad + ")", ex);
        }
        finally {
            Arrays.fill(plaintext, (byte) 0);
        }
    }

    /**
     * Decrypt a ciphertext bound to the JWK's {@code kid} as AAD. The
     * {@code encKid} argument MUST match {@link #encKid()} (KEK rotation is not
     * yet supported in this codec; mismatched values are rejected).
     */
    public char[] decrypt(String encKid, byte[] iv, byte[] tag, byte[] cipherOnly, String aad) {
        Objects.requireNonNull(encKid, "encKid");
        Objects.requireNonNull(iv, "iv");
        Objects.requireNonNull(tag, "tag");
        Objects.requireNonNull(cipherOnly, "cipherOnly");
        Objects.requireNonNull(aad, "aad");
        if (!this.encKid.equals(encKid)) {
            throw new IllegalStateException("Row encrypted with unknown KEK encKid='" + encKid
                    + "', current codec encKid='" + this.encKid + "' (KEK rotation not yet supported)");
        }
        if (iv.length != IV_LENGTH) {
            throw new IllegalStateException("Invalid IV length: expected " + IV_LENGTH + ", got " + iv.length);
        }
        if (tag.length != TAG_LENGTH_BYTES) {
            throw new IllegalStateException("Invalid tag length: expected " + TAG_LENGTH_BYTES + ", got " + tag.length);
        }
        byte[] full = new byte[cipherOnly.length + tag.length];
        System.arraycopy(cipherOnly, 0, full, 0, cipherOnly.length);
        System.arraycopy(tag, 0, full, cipherOnly.length, tag.length);
        try {
            Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, this.kek, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            cipher.updateAAD(aad.getBytes(StandardCharsets.UTF_8));
            byte[] plaintext = cipher.doFinal(full);
            try {
                return fromUtf8(plaintext);
            }
            finally {
                Arrays.fill(plaintext, (byte) 0);
            }
        }
        catch (GeneralSecurityException ex) {
            throw new IllegalStateException("AES-256-GCM decryption failed (kid AAD=" + aad + ")", ex);
        }
        finally {
            Arrays.fill(full, (byte) 0);
        }
    }

    private static void validatePosixPermissions(Path keyFile) {
        PosixFileAttributeView view = Files.getFileAttributeView(keyFile, PosixFileAttributeView.class);
        if (view == null) {
            // Non-POSIX file system (e.g. Windows). Skip permission check; warn instead.
            log.warn("Skipping POSIX permission check on non-POSIX filesystem for KEK file: {}", keyFile);
            return;
        }
        Set<PosixFilePermission> actual;
        try {
            actual = view.readAttributes().permissions();
        }
        catch (IOException ex) {
            throw new IllegalStateException("Unable to read POSIX permissions for KEK file: " + keyFile, ex);
        }
        for (PosixFilePermission perm : actual) {
            if (!ALLOWED_KEY_FILE_PERMS.contains(perm)) {
                throw new IllegalStateException("KEK file " + keyFile
                        + " has overly permissive permissions " + actual
                        + "; required ≤ 0600 (rw- --- ---)");
            }
        }
    }

    private static byte[] deriveSalt(String encKid) {
        try {
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            byte[] full = sha256.digest((SALT_NAMESPACE + encKid).getBytes(StandardCharsets.UTF_8));
            return Arrays.copyOf(full, 16);
        }
        catch (GeneralSecurityException ex) {
            throw new IllegalStateException("SHA-256 unavailable for PBKDF2 salt derivation", ex);
        }
    }

    private static byte[] toUtf8(char[] chars) {
        java.nio.CharBuffer cb = java.nio.CharBuffer.wrap(chars);
        java.nio.ByteBuffer bb = StandardCharsets.UTF_8.encode(cb);
        byte[] out = new byte[bb.remaining()];
        bb.get(out);
        // Best-effort wipe of the intermediate ByteBuffer's backing array if accessible.
        if (bb.hasArray()) {
            Arrays.fill(bb.array(), bb.arrayOffset(), bb.arrayOffset() + bb.capacity(), (byte) 0);
        }
        return out;
    }

    private static char[] fromUtf8(byte[] bytes) {
        java.nio.ByteBuffer bb = java.nio.ByteBuffer.wrap(bytes);
        java.nio.CharBuffer cb = StandardCharsets.UTF_8.decode(bb);
        char[] out = new char[cb.remaining()];
        cb.get(out);
        return out;
    }

    /** Output bundle of {@link #encrypt(char[], String)}; columns map 1:1 to {@code oauth2_jwk}. */
    public record Ciphertext(String encKid, byte[] iv, byte[] tag, byte[] cipher) {
    }
}
