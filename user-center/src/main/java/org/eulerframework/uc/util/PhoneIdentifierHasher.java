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

/**
 * Hashes an E.164 phone number into the stable, factor-scoped
 * {@code identifier} value used by the {@code phone} authentication factor.
 * <p>
 * The output is the lowercase hex-encoded SHA-256 digest of the raw phone
 * number, matching the contract documented in
 * {@code Model-#-User-Identity.md}: a one-way function used purely for
 * uniqueness checks across accounts; never surfaced to clients.
 */
public final class PhoneIdentifierHasher {

    private PhoneIdentifierHasher() {
    }

    /**
     * Compute the identifier for {@code phone}.
     *
     * @param phone the raw E.164 phone number; must be non-empty
     * @return the lowercase hex SHA-256 digest, always 64 characters
     */
    public static String hash(String phone) {
        Assert.hasText(phone, "phone must not be empty");
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        }
        catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 not available", ex);
        }
        byte[] hashed = digest.digest(phone.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(hashed);
    }
}
