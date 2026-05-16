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
package org.eulerframework.uc.entity.converter;

import jakarta.persistence.Converter;
import org.eulerframework.data.convert.encrypt.AbstractEncryptedAttributeConverter;
import org.eulerframework.security.crypto.DataCipher;

/**
 * JPA {@link jakarta.persistence.AttributeConverter} that persists a raw
 * E.164 phone number as an encrypted envelope string. Used for the
 * {@code phone} column of {@code t_user_authentication_factor_phone} so the
 * raw PII never lands on disk in clear text.
 */
@Converter
public class PhoneNumberAttributeConverter extends AbstractEncryptedAttributeConverter<String> {

    public PhoneNumberAttributeConverter(DataCipher dataCipher) {
        super(dataCipher);
    }

    @Override
    protected String serialize(String value) {
        return value;
    }

    @Override
    protected String deserialize(String plaintext) {
        return plaintext;
    }
}
