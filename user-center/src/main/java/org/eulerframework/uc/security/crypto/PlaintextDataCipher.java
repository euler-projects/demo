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

import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;

/**
 * Null {@link DataCipher} that wraps the plaintext unchanged into a single body
 * segment. Intended for development, testing and historical-data compatibility;
 * has no KEK and therefore its envelope header omits the {@code kid} field.
 * <p>
 * Using this cipher for production data is unsafe — a {@code WARN} is emitted
 * on construction so that the choice is never accidental.
 */
public final class PlaintextDataCipher implements DataCipher {

    /** Stable algorithm identifier used in envelope headers and registry keys. */
    public static final String ALGORITHM_ID = "plain";

    private static final Logger log = LoggerFactory.getLogger(PlaintextDataCipher.class);

    public PlaintextDataCipher() {
        log.warn("PlaintextDataCipher is active — data-encryption algorithm '{}' stores values unencrypted. " +
                "Use only for development, testing or historical-data compatibility.", ALGORITHM_ID);
    }

    @Override
    public String algorithmId() {
        return ALGORITHM_ID;
    }

    @Override
    @Nullable
    public String kekKid() {
        return null;
    }

    @Override
    public List<byte[]> encrypt(byte[] plaintext) {
        Objects.requireNonNull(plaintext, "plaintext");
        return List.of(plaintext);
    }

    @Override
    public byte[] decrypt(List<byte[]> bodyParts) {
        Objects.requireNonNull(bodyParts, "bodyParts");
        if (bodyParts.size() != 1) {
            throw new IllegalStateException("PlaintextDataCipher expects 1 body part, got " + bodyParts.size());
        }
        return bodyParts.get(0);
    }
}
