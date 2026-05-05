/*
 * Copyright 2013-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 */
package org.eulerframework.uc.oauth2.service.jwk;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.SecureRandom;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwkKekCodecTests {

    @TempDir
    Path tempDir;

    // --- happy path ---

    @Test
    void encryptDecryptRoundTripsWithKidAad() throws Exception {
        Path keyFile = writeRandomKekFile(tempDir.resolve("kek.bin"));
        JwkKekCodec codec = JwkKekCodec.fromKeyFile(keyFile, "default");

        char[] plaintext = "secret-pem-content".toCharArray();
        JwkKekCodec.Ciphertext ct = codec.encrypt(plaintext, "kid-A");

        assertThat(ct.encKid()).isEqualTo("default");
        assertThat(ct.iv()).hasSize(12);
        assertThat(ct.tag()).hasSize(16);
        assertThat(ct.cipher()).isNotEmpty();

        char[] decrypted = codec.decrypt(ct.encKid(), ct.iv(), ct.tag(), ct.cipher(), "kid-A");
        assertThat(new String(decrypted)).isEqualTo("secret-pem-content");
    }

    // --- AAD binding: swapping kid must fail ---

    @Test
    void decryptRejectsRowLevelKidSwap() throws Exception {
        Path keyFile = writeRandomKekFile(tempDir.resolve("kek.bin"));
        JwkKekCodec codec = JwkKekCodec.fromKeyFile(keyFile, "default");

        JwkKekCodec.Ciphertext ct = codec.encrypt("payload".toCharArray(), "kid-A");

        // Caller asks to decrypt with a different kid -> AEAD tag verification must fail.
        assertThatThrownBy(() -> codec.decrypt(ct.encKid(), ct.iv(), ct.tag(), ct.cipher(), "kid-B"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("decryption failed");
    }

    @Test
    void decryptRejectsTamperedCiphertext() throws Exception {
        Path keyFile = writeRandomKekFile(tempDir.resolve("kek.bin"));
        JwkKekCodec codec = JwkKekCodec.fromKeyFile(keyFile, "default");

        JwkKekCodec.Ciphertext ct = codec.encrypt("payload".toCharArray(), "kid-A");
        byte[] tampered = ct.cipher().clone();
        tampered[0] ^= 0x01;

        assertThatThrownBy(() -> codec.decrypt(ct.encKid(), ct.iv(), ct.tag(), tampered, "kid-A"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("decryption failed");
    }

    @Test
    void decryptRejectsUnknownEncKid() throws Exception {
        Path keyFile = writeRandomKekFile(tempDir.resolve("kek.bin"));
        JwkKekCodec codec = JwkKekCodec.fromKeyFile(keyFile, "default");
        JwkKekCodec.Ciphertext ct = codec.encrypt("payload".toCharArray(), "kid-A");

        assertThatThrownBy(() -> codec.decrypt("foreign-encKid", ct.iv(), ct.tag(), ct.cipher(), "kid-A"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Row encrypted with unknown KEK");
    }

    @Test
    void decryptRejectsBadIvLength() throws Exception {
        Path keyFile = writeRandomKekFile(tempDir.resolve("kek.bin"));
        JwkKekCodec codec = JwkKekCodec.fromKeyFile(keyFile, "default");
        JwkKekCodec.Ciphertext ct = codec.encrypt("payload".toCharArray(), "kid-A");

        assertThatThrownBy(() -> codec.decrypt(ct.encKid(), new byte[8], ct.tag(), ct.cipher(), "kid-A"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Invalid IV length");
    }

    // --- KEK file validation ---

    @Test
    void rejectsKeyFileWithWrongLength() throws Exception {
        Path keyFile = tempDir.resolve("kek-short.bin");
        Files.write(keyFile, new byte[16]);
        Files.setPosixFilePermissions(keyFile, Set.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE));

        assertThatThrownBy(() -> JwkKekCodec.fromKeyFile(keyFile, "default"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("must be exactly 32 bytes");
    }

    @Test
    void rejectsKeyFileWithLoosePermissions() throws Exception {
        Path keyFile = tempDir.resolve("kek-loose.bin");
        byte[] raw = new byte[32];
        new SecureRandom().nextBytes(raw);
        Files.write(keyFile, raw);
        Files.setPosixFilePermissions(keyFile, PosixFilePermissions.fromString("rw-r--r--"));

        assertThatThrownBy(() -> JwkKekCodec.fromKeyFile(keyFile, "default"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("overly permissive");
    }

    // --- passphrase mode ---

    @Test
    void passphraseRoundTrips() {
        JwkKekCodec codec = JwkKekCodec.fromPassphrase("hunter2-passphrase".toCharArray(), "dev-encKid");
        JwkKekCodec.Ciphertext ct = codec.encrypt("ed-pem".toCharArray(), "kid-x");
        char[] back = codec.decrypt(ct.encKid(), ct.iv(), ct.tag(), ct.cipher(), "kid-x");
        assertThat(new String(back)).isEqualTo("ed-pem");
    }

    @Test
    void passphraseRejectsEmpty() {
        assertThatThrownBy(() -> JwkKekCodec.fromPassphrase(new char[0], "x"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("passphrase must not be empty");
    }

    private static Path writeRandomKekFile(Path target) throws Exception {
        byte[] raw = new byte[32];
        new SecureRandom().nextBytes(raw);
        Files.write(target, raw);
        Files.setPosixFilePermissions(target, Set.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE));
        return target;
    }
}
