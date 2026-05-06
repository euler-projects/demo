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
import org.eulerframework.uc.security.crypto.AesGcmDataCipher;
import org.eulerframework.uc.security.crypto.DataCipher;
import org.eulerframework.uc.security.crypto.DataCipherRegistry;
import org.eulerframework.uc.security.crypto.PlaintextDataCipher;
import org.eulerframework.uc.security.crypto.StringEncryptor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.nio.file.Paths;
import java.util.List;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(UserCenterJwkProperties.class)
@ConditionalOnProperty(prefix = "euler.uc.oauth2.jwk.data-encryption", name = "primary-alg")
public class JwkManageServiceConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "euler.uc.oauth2.jwk.data-encryption.aes-gcm", name = "enabled", havingValue = "true")
    public AesGcmDataCipher aesGcmDataCipher(UserCenterJwkProperties properties) {
        UserCenterJwkProperties.AesGcm aesGcm = properties.getDataEncryption().getAesGcm();
        String kid = aesGcm.getKid();
        if (!StringUtils.hasText(kid)) {
            throw new IllegalStateException(
                    "euler.uc.oauth2.jwk.data-encryption.aes-gcm.kid is required when aes-gcm is enabled");
        }
        if (StringUtils.hasText(aesGcm.getKeyFile())) {
            return AesGcmDataCipher.fromKeyFile(Paths.get(aesGcm.getKeyFile()), kid);
        }
        if (StringUtils.hasText(aesGcm.getPassphrase())) {
            return AesGcmDataCipher.fromPassphrase(aesGcm.getPassphrase().toCharArray(), kid);
        }
        throw new IllegalStateException(
                "euler.uc.oauth2.jwk.data-encryption.aes-gcm requires either 'key-file' or 'passphrase'");
    }

    @Bean
    @ConditionalOnProperty(prefix = "euler.uc.oauth2.jwk.data-encryption.plain", name = "enabled", havingValue = "true")
    public PlaintextDataCipher plaintextDataCipher() {
        return new PlaintextDataCipher();
    }

    @Bean
    public DataCipherRegistry dataCipherRegistry(List<DataCipher> ciphers, UserCenterJwkProperties properties) {
        String primaryAlg = properties.getDataEncryption().getPrimaryAlg();
        if (!StringUtils.hasText(primaryAlg)) {
            throw new IllegalStateException(
                    "euler.uc.oauth2.jwk.data-encryption.primary-alg is required");
        }
        return new DataCipherRegistry(ciphers, primaryAlg);
    }

    @Bean
    public StringEncryptor stringEncryptor(DataCipherRegistry registry) {
        return new StringEncryptor(registry);
    }

    @Bean
    public JwkGenerator jwkGenerator() {
        return new JwkGenerator();
    }
}
