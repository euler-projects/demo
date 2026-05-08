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

import org.eulerframework.security.crypto.AesGcmDataCipher;
import org.eulerframework.security.crypto.DataCipher;
import org.eulerframework.security.crypto.DelegatingDataCipher;
import org.eulerframework.security.crypto.InMemoryKeyRepository;
import org.eulerframework.security.crypto.KeyMaterialLoader;
import org.eulerframework.security.crypto.KeyRepository;
import org.eulerframework.security.crypto.NoopDataCipher;
import org.eulerframework.uc.oauth2.service.jwk.keygen.JwkGenerator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Wires the data-encryption stack for the JPA-backed JWK store.
 *
 * <p>Bean layout:
 * <ul>
 *   <li>{@link KeyRepository} — {@link InMemoryKeyRepository} built from
 *       {@link UserCenterJwkProperties.DataEncryption#getKeys()}.</li>
 *   <li>{@link NoopDataCipher} — always registered; can be selected as
 *       {@code primary-alg} to disable encryption for new writes while still
 *       decrypting historical ciphertexts written under other algorithms.</li>
 *   <li>{@link AesGcmDataCipher} — registered whenever the {@code AES-256-GCM}
 *       block is present in configuration.</li>
 *   <li>{@link DelegatingDataCipher} (exposed as {@link DataCipher}) — the
 *       bean consumed by every {@code AbstractEncryptedAttributeConverter}.
 *       Its primary cipher follows
 *       {@link UserCenterJwkProperties.DataEncryption#getPrimaryAlg()}.</li>
 * </ul>
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(UserCenterJwkProperties.class)
@ConditionalOnProperty(prefix = "euler.uc.oauth2.jwk.data-encryption", name = "primary-alg")
public class JwkManageServiceConfiguration {

    @Bean
    public KeyRepository jwkDataEncryptionKeyRepository(UserCenterJwkProperties properties) {
        InMemoryKeyRepository.Builder builder = InMemoryKeyRepository.builder();
        Map<String, UserCenterJwkProperties.AlgorithmKeys> keys = properties.getDataEncryption().getKeys();
        for (Map.Entry<String, UserCenterJwkProperties.AlgorithmKeys> algEntry : keys.entrySet()) {
            String alg = algEntry.getKey();
            UserCenterJwkProperties.AlgorithmKeys algKeys = algEntry.getValue();
            if (algKeys == null) {
                throw new IllegalStateException(
                        "euler.uc.oauth2.jwk.data-encryption.keys." + alg + " is present but empty");
            }
            if (!StringUtils.hasText(algKeys.getPrimaryKid())) {
                throw new IllegalStateException(
                        "euler.uc.oauth2.jwk.data-encryption.keys." + alg + ".primary-kid is required");
            }
            if (algKeys.getItems().isEmpty()) {
                throw new IllegalStateException(
                        "euler.uc.oauth2.jwk.data-encryption.keys." + alg + ".items must contain at least one entry");
            }
            builder.primaryKid(alg, algKeys.getPrimaryKid());
            for (Map.Entry<String, UserCenterJwkProperties.KeyItem> kidEntry : algKeys.getItems().entrySet()) {
                String kid = kidEntry.getKey();
                UserCenterJwkProperties.KeyItem item = kidEntry.getValue();
                if (item == null) {
                    throw new IllegalStateException(
                            "euler.uc.oauth2.jwk.data-encryption.keys." + alg + ".items." + kid + " is empty");
                }
                byte[] material = KeyMaterialLoader.load(alg, kid,
                        item.getKeyFile(), item.getPassphrase(), item.getSaltNamespace());
                builder.addKey(alg, kid, material);
            }
        }
        return builder.build();
    }

    @Bean
    public DataCipher jwkDataCipher(UserCenterJwkProperties properties,
                                    KeyRepository keyRepository) {
        String primaryAlg = properties.getDataEncryption().getPrimaryAlg();
        if (!StringUtils.hasText(primaryAlg)) {
            throw new IllegalStateException(
                    "euler.uc.oauth2.jwk.data-encryption.primary-alg is required");
        }
        Map<String, DataCipher> ciphers = new LinkedHashMap<>();
        ciphers.put(NoopDataCipher.ALGORITHM, new NoopDataCipher());
        if (keyRepository.supports(AesGcmDataCipher.ALGORITHM)) {
            ciphers.put(AesGcmDataCipher.ALGORITHM, new AesGcmDataCipher(keyRepository));
        }
        return new DelegatingDataCipher(primaryAlg, ciphers);
    }

    @Bean
    public JwkGenerator jwkGenerator() {
        return new JwkGenerator();
    }
}
