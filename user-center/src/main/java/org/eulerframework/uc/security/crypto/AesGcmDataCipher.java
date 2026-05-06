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
package org.eulerframework.uc.security.crypto;

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
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * AES-256-GCM implementation of {@link DataCipher}. Each call to
 * {@link #encrypt(byte[])} draws a fresh 96-bit IV; the 128-bit GCM tag is
 * appended to the ciphertext (standard JCA output layout). No AAD is used.
 *
 * <h2>KEK sources</h2>
 * <ul>
 *   <li>{@link #fromKeyFile(Path, String)} — production-recommended. The file
 *       MUST contain exactly 32 random bytes and have POSIX permissions
 *       {@code 0600}.</li>
 *   <li>{@link #fromPassphrase(char[], String)} — development only. Derives a
 *       32-byte KEK via PBKDF2-HMAC-SHA256 (600k iterations, salt derived from
 *       {@code kekKid}). A {@code WARN} is emitted on construction.</li>
 * </ul>
 */
public final class AesGcmDataCipher implements DataCipher {

    public static final String ALGORITHM_ID = "AES-256-GCM";

    private static final Logger log = LoggerFactory.getLogger(AesGcmDataCipher.class);

    private static final String CIPHER_TRANSFORMATION = "AES/GCM/NoPadding";
    private static final String KEY_ALGORITHM = "AES";
    private static final int IV_LENGTH = 12;
    private static final int TAG_LENGTH_BITS = 128;
    private static final int KEK_LENGTH_BYTES = 32;
    private static final int PBKDF2_ITERATIONS = 600_000;
    private static final String PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final String SALT_NAMESPACE = "euler-uc-data-kek/";

    private static final Set<PosixFilePermission> ALLOWED_KEY_FILE_PERMS = EnumSet.of(
            PosixFilePermission.OWNER_READ,
            PosixFilePermission.OWNER_WRITE);

    private final SecretKey kek;
    private final String kekKid;
    private final SecureRandom secureRandom;

    private AesGcmDataCipher(SecretKey kek, String kekKid, SecureRandom secureRandom) {
        this.kek = Objects.requireNonNull(kek, "kek");
        this.kekKid = Objects.requireNonNull(kekKid, "kekKid");
        this.secureRandom = Objects.requireNonNull(secureRandom, "secureRandom");
    }

    /**
     * Build a cipher from a 32-byte KEK file. The file is read once and
     * immediately wiped from the intermediate buffer.
     *
     * @throws IllegalStateException if the file is missing, wrong size, or has
     *                               overly permissive POSIX permissions
     */
    public static AesGcmDataCipher fromKeyFile(Path keyFile, String kekKid) {
        Objects.requireNonNull(keyFile, "keyFile");
        Objects.requireNonNull(kekKid, "kekKid");
        if (!Files.exists(keyFile)) {
            throw new IllegalStateException("KEK file does not exist: " + keyFile);
        }
        validatePosixPermissions(keyFile);
        byte[] raw;
        try {
            raw = Files.readAllBytes(keyFile);
        }
        catch (IOException ex) {
            throw new IllegalStateException("Unable to read KEK file: " + keyFile, ex);
        }
        if (raw.length != KEK_LENGTH_BYTES) {
            Arrays.fill(raw, (byte) 0);
            throw new IllegalStateException("KEK file must be exactly " + KEK_LENGTH_BYTES
                    + " bytes, got " + raw.length + " (" + keyFile + ")");
        }
        SecretKey key = new SecretKeySpec(raw, KEY_ALGORITHM);
        Arrays.fill(raw, (byte) 0);
        log.info("KEK loaded from key file (kekKid={})", kekKid);
        return new AesGcmDataCipher(key, kekKid, new SecureRandom());
    }

    /**
     * Build a cipher by deriving the KEK from a passphrase via PBKDF2-HMAC-SHA256.
     * <strong>For development only</strong>; emits a {@code WARN} on every
     * construction.
     */
    public static AesGcmDataCipher fromPassphrase(char[] passphrase, String kekKid) {
        Objects.requireNonNull(passphrase, "passphrase");
        Objects.requireNonNull(kekKid, "kekKid");
        if (passphrase.length == 0) {
            throw new IllegalStateException("KEK passphrase must not be empty");
        }
        log.warn("KEK is derived from a passphrase (kekKid={}). " +
                "Passphrase mode is for development only and MUST NOT be used in production.", kekKid);
        byte[] salt = deriveSalt(kekKid);
        try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM);
            PBEKeySpec spec = new PBEKeySpec(passphrase, salt, PBKDF2_ITERATIONS, KEK_LENGTH_BYTES * 8);
            try {
                byte[] derived = factory.generateSecret(spec).getEncoded();
                SecretKey key = new SecretKeySpec(derived, KEY_ALGORITHM);
                Arrays.fill(derived, (byte) 0);
                return new AesGcmDataCipher(key, kekKid, new SecureRandom());
            }
            finally {
                spec.clearPassword();
            }
        }
        catch (GeneralSecurityException ex) {
            throw new IllegalStateException("Failed to derive KEK from passphrase", ex);
        }
    }

    @Override
    public String algorithmId() {
        return ALGORITHM_ID;
    }

    @Override
    public String kekKid() {
        return this.kekKid;
    }

    @Override
    public List<byte[]> encrypt(byte[] plaintext) {
        Objects.requireNonNull(plaintext, "plaintext");
        byte[] iv = new byte[IV_LENGTH];
        this.secureRandom.nextBytes(iv);
        try {
            Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, this.kek, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            byte[] ctWithTag = cipher.doFinal(plaintext);
            return List.of(iv, ctWithTag);
        }
        catch (GeneralSecurityException ex) {
            throw new IllegalStateException("AES-256-GCM encryption failed", ex);
        }
    }

    @Override
    public byte[] decrypt(List<byte[]> bodyParts) {
        Objects.requireNonNull(bodyParts, "bodyParts");
        if (bodyParts.size() != 2) {
            throw new IllegalStateException("AES-256-GCM expects 2 body parts [iv, ctWithTag], got " + bodyParts.size());
        }
        byte[] iv = bodyParts.get(0);
        byte[] ciphertextWithTag = bodyParts.get(1);
        if (iv.length != IV_LENGTH) {
            throw new IllegalStateException("Invalid IV length: expected " + IV_LENGTH + ", got " + iv.length);
        }
        try {
            Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, this.kek, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            return cipher.doFinal(ciphertextWithTag);
        }
        catch (GeneralSecurityException ex) {
            throw new IllegalStateException("AES-256-GCM decryption failed (kekKid=" + this.kekKid + ")", ex);
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
                        + "; required \u2264 0600 (rw- --- ---)");
            }
        }
    }

    private static byte[] deriveSalt(String kekKid) {
        try {
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            byte[] full = sha256.digest((SALT_NAMESPACE + kekKid).getBytes(StandardCharsets.UTF_8));
            return Arrays.copyOf(full, 16);
        }
        catch (GeneralSecurityException ex) {
            throw new IllegalStateException("SHA-256 unavailable for PBKDF2 salt derivation", ex);
        }
    }
}
