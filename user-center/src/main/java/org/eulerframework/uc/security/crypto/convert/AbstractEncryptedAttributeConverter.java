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
package org.eulerframework.uc.security.crypto.convert;

import jakarta.persistence.AttributeConverter;
import org.eulerframework.uc.security.crypto.StringEncryptor;
import org.springframework.util.Assert;

/**
 * Layer 4 of the attribute-encryption stack: template base for any
 * {@link AttributeConverter} that wants to transparently encrypt a domain
 * value as a single database string column.
 * <p>
 * Subclasses only need to implement the domain-specific
 * {@code T <-> plaintext String} mapping via {@link #serialize(Object)} and
 * {@link #deserialize(String)}. Encryption, envelope framing and cipher
 * dispatch are fully handled by {@link StringEncryptor}.
 *
 * @param <T> the entity attribute type
 */
public abstract class AbstractEncryptedAttributeConverter<T> implements AttributeConverter<T, String> {

    private final StringEncryptor encryptor;

    protected AbstractEncryptedAttributeConverter(StringEncryptor encryptor) {
        Assert.notNull(encryptor, "encryptor is required");
        this.encryptor = encryptor;
    }

    /** Serialize the domain value into its plaintext {@link String} form. */
    protected abstract String serialize(T value);

    /** Inverse of {@link #serialize(Object)}. */
    protected abstract T deserialize(String plaintext);

    @Override
    public final String convertToDatabaseColumn(T attribute) {
        if (attribute == null) {
            return null;
        }
        return this.encryptor.encrypt(serialize(attribute));
    }

    @Override
    public final T convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        return deserialize(this.encryptor.decrypt(dbData));
    }
}
