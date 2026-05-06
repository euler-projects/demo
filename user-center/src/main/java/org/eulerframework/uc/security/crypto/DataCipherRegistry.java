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

import org.springframework.util.Assert;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Registry of {@link DataCipher} instances keyed by {@link DataCipher#algorithmId()}.
 * Holds one designated {@link #primaryCipher() primary} cipher used for all
 * write operations, while read operations may resolve any registered cipher
 * via {@link #resolve(String)} to support algorithm lifecycle (new algorithm
 * introduction, historical-data decryption).
 * <p>
 * The current version enforces at most one cipher per algorithm. Multi-KEK
 * rotation within the same algorithm will be introduced by upgrading the key
 * to {@code algorithmId + "/" + kekKid} — the envelope format already carries
 * {@code kid}, so that upgrade will not require a format change.
 */
public final class DataCipherRegistry {

    private final Map<String, DataCipher> ciphers;
    private final DataCipher primary;

    public DataCipherRegistry(Collection<DataCipher> ciphers, String primaryAlg) {
        Assert.notEmpty(ciphers, "ciphers must not be empty");
        Assert.hasText(primaryAlg, "primaryAlg is required");
        Map<String, DataCipher> map = new LinkedHashMap<>();
        for (DataCipher cipher : ciphers) {
            Objects.requireNonNull(cipher, "cipher");
            String alg = cipher.algorithmId();
            Assert.hasText(alg, "DataCipher#algorithmId must not be blank");
            if (map.putIfAbsent(alg, cipher) != null) {
                throw new IllegalStateException("Duplicate DataCipher registration for algorithm '" + alg + "'");
            }
        }
        DataCipher chosen = map.get(primaryAlg);
        if (chosen == null) {
            throw new IllegalStateException("Primary algorithm '" + primaryAlg
                    + "' is not present in registry; registered algorithms: " + map.keySet());
        }
        this.ciphers = Map.copyOf(map);
        this.primary = chosen;
    }

    /** Cipher used to encrypt new values. */
    public DataCipher primaryCipher() {
        return this.primary;
    }

    /**
     * Resolve a registered cipher by the algorithm identifier carried in the
     * envelope header. Throws {@link IllegalStateException} when no match is
     * registered — the caller should treat this as a data-integrity error.
     */
    public DataCipher resolve(String algorithmId) {
        Assert.hasText(algorithmId, "algorithmId is required");
        DataCipher cipher = this.ciphers.get(algorithmId);
        if (cipher == null) {
            throw new IllegalStateException("No DataCipher registered for algorithm '" + algorithmId + "'");
        }
        return cipher;
    }
}
