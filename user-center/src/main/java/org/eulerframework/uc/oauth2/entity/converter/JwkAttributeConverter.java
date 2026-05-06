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
package org.eulerframework.uc.oauth2.entity.converter;

import com.nimbusds.jose.jwk.JWK;
import jakarta.persistence.Converter;
import org.eulerframework.uc.security.crypto.StringEncryptor;
import org.eulerframework.uc.security.crypto.convert.AbstractEncryptedAttributeConverter;

import java.text.ParseException;

/**
 * JPA {@link jakarta.persistence.AttributeConverter} that persists a Nimbus
 * {@link JWK} as an encrypted envelope string. Delegates all crypto concerns
 * to {@link AbstractEncryptedAttributeConverter} / {@link StringEncryptor};
 * only the {@code JWK <-> JSON String} mapping lives here.
 */
@Converter
public class JwkAttributeConverter extends AbstractEncryptedAttributeConverter<JWK> {

    public JwkAttributeConverter(StringEncryptor encryptor) {
        super(encryptor);
    }

    @Override
    protected String serialize(JWK value) {
        return value.toJSONString();
    }

    @Override
    protected JWK deserialize(String plaintext) {
        try {
            return JWK.parse(plaintext);
        }
        catch (ParseException ex) {
            throw new IllegalStateException("Failed to parse decrypted JWK payload", ex);
        }
    }
}
