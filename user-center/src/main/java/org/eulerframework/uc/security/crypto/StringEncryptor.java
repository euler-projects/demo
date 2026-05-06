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

import java.util.List;

/**
 * Layer 3 of the attribute-encryption stack: turns a plaintext {@link String}
 * into an envelope {@link String} and back. Internally composes
 * {@link DataCipherRegistry} (dispatch) and {@link EncryptedEnvelopeCodec}
 * (on-disk format); it is the only thing layer 4 converters need to depend on.
 * <p>
 * {@code null} inputs are passed through unchanged in both directions.
 */
public final class StringEncryptor {

    private final DataCipherRegistry registry;

    public StringEncryptor(DataCipherRegistry registry) {
        Assert.notNull(registry, "registry is required");
        this.registry = registry;
    }

    /** Encrypt plaintext under the registry's primary cipher. */
    public String encrypt(String plaintext) {
        if (plaintext == null) {
            return null;
        }
        DataCipher cipher = this.registry.primaryCipher();
        List<byte[]> bodyParts = cipher.encrypt(EncryptedEnvelopeCodec.toUtf8(plaintext));
        return EncryptedEnvelopeCodec.encode(cipher.algorithmId(), cipher.kekKid(),
                bodyParts.toArray(new byte[0][]));
    }

    /** Decrypt an envelope produced by {@link #encrypt(String)}. */
    public String decrypt(String envelope) {
        if (envelope == null) {
            return null;
        }
        EncryptedEnvelopeCodec.Envelope parsed = EncryptedEnvelopeCodec.decode(envelope);
        DataCipher cipher = this.registry.resolve(parsed.algorithmId());
        byte[] plaintext = cipher.decrypt(parsed.bodyParts());
        return EncryptedEnvelopeCodec.fromUtf8(plaintext);
    }
}
