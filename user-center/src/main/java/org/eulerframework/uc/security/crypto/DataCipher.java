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

import java.util.List;

/**
 * Layer 1 of the attribute-encryption stack: a raw {@code byte[] -> List<byte[]>}
 * cipher bound to a single {@link #algorithmId() algorithm} and at most one
 * {@link #kekKid() key-encryption-key}. Stateless apart from the key material
 * it encapsulates.
 * <p>
 * The body of an envelope is represented as an ordered list of byte segments
 * whose count and semantics are <em>algorithm-specific</em>:
 * <ul>
 *   <li>AEAD (e.g. AES-256-GCM): {@code [iv, ciphertextWithTag]} — 2 segments</li>
 *   <li>Plaintext: {@code [plaintext]} — 1 segment</li>
 *   <li>Future AES-CBC-HS256: {@code [iv, ciphertext, mac]} — 3 segments</li>
 * </ul>
 * Layer 2 does not interpret the body segments; it only packages/unpackages
 * them as Base64URL segments alongside the header.
 */
public interface DataCipher {

    /** Stable identifier of the algorithm, e.g. {@code "AES-256-GCM"}. */
    String algorithmId();

    /**
     * Identifier of the key-encryption key this cipher is bound to, or
     * {@code null} for algorithms that have no KEK concept (e.g. plaintext).
     */
    @Nullable
    String kekKid();

    /**
     * Encrypt a plaintext byte array and return the ordered body segments.
     * The shape (count and semantics) of the returned list is fixed per
     * algorithm; callers do not inspect individual segments.
     */
    List<byte[]> encrypt(byte[] plaintext);

    /**
     * Inverse of {@link #encrypt(byte[])}. Implementations MUST fail fast when
     * {@code bodyParts} does not match the shape produced by
     * {@link #encrypt(byte[])}.
     */
    byte[] decrypt(List<byte[]> bodyParts);
}
