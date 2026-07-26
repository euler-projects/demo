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
package org.eulerframework.uc.util;

import org.springframework.util.Assert;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;

/**
 * Hashes an email address into the stable {@code subject} value persisted
 * on the parent row of the {@code email} user identity.
 * <p>
 * The input is normalised first (trimmed, lower-cased with
 * {@link Locale#ROOT}) so that case or whitespace variants of the same
 * address always map to the same subject; the output is the lowercase
 * hex-encoded SHA-256 digest of the normalised address: a one-way function
 * used purely for uniqueness checks across accounts, never surfaced to
 * clients.
 */
public final class EmailIdentifierHasher {

    private EmailIdentifierHasher() {
    }

    /**
     * Compute the subject for {@code email}.
     *
     * @param email the raw email address; must be non-empty
     * @return the lowercase hex SHA-256 digest of the normalised address,
     *         always 64 characters
     */
    public static String hash(String email) {
        Assert.hasText(email, "email must not be empty");
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        }
        catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 not available", ex);
        }
        String normalized = email.trim().toLowerCase(Locale.ROOT);
        byte[] hashed = digest.digest(normalized.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(hashed);
    }
}
