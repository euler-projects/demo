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

package org.eulerframework.uc.oauth2.service.jwk;

import org.eulerframework.uc.oauth2.service.jwk.keygen.JwkGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * JWK-specific bean wiring for the JPA-backed JWK store.
 *
 * <p>The data-encryption stack that used to live here (the {@code KeyRepository}
 * and {@code DataCipher} beans) now ships with the framework under the
 * {@code euler.data.jpa.encryption} auto-configuration, because a single
 * {@code DataCipher} instance governs every
 * {@code AbstractEncryptedAttributeConverter} in the application — not just
 * {@code JwkAttributeConverter}. This class is therefore reduced to JWK
 * concerns proper.
 */
@Configuration(proxyBeanMethods = false)
public class JwkManageServiceConfiguration {

    @Bean
    public JwkGenerator jwkGenerator() {
        return new JwkGenerator();
    }
}
