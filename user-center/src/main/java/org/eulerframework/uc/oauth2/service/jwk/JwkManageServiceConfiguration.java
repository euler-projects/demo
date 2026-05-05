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
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.nio.file.Paths;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(UserCenterJwkProperties.class)
@ConditionalOnProperty(prefix = "euler.uc.oauth2.jwk.encryption", name = "enc-kid")
public class JwkManageServiceConfiguration {

    @Bean
    public JwkKekCodec jwkKekCodec(UserCenterJwkProperties properties) {
        String encKid = properties.getEncKid();
        if (!StringUtils.hasText(encKid)) {
            throw new IllegalStateException(
                    "euler.uc.oauth2.jwk.encryption.enc-kid is required when DefaultJwkManageService is active");
        }
        return switch (properties.getSource()) {
            case KEY_FILE -> {
                if (!StringUtils.hasText(properties.getKeyFile())) {
                    throw new IllegalStateException(
                            "euler.uc.oauth2.jwk.encryption.key-file is required when source=KEY_FILE");
                }
                yield JwkKekCodec.fromKeyFile(Paths.get(properties.getKeyFile()), encKid);
            }
            case PASSPHRASE -> {
                String passphrase = properties.getPassphrase();
                if (!StringUtils.hasText(passphrase)) {
                    throw new IllegalStateException(
                            "euler.uc.oauth2.jwk.encryption.passphrase is required when source=PASSPHRASE");
                }
                yield JwkKekCodec.fromPassphrase(passphrase.toCharArray(), encKid);
            }
        };
    }

    @Bean
    public JwkGenerator jwkGenerator() {
        return new JwkGenerator();
    }
}
