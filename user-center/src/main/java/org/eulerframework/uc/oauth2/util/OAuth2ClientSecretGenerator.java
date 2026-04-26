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

package org.eulerframework.uc.oauth2.util;

import org.springframework.security.oauth2.core.ClientAuthenticationMethod;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * Mints OAuth 2.0 client secrets and decides whether a given client actually
 * needs one based on its {@code token_endpoint_auth_method}.
 *
 * <p>The minted value follows the "api-key" convention popularised by most
 * LLM vendors: a fixed, human-recognisable prefix, followed by a long run of
 * URL-safe base64 characters carrying 256 bits of entropy. The prefix helps
 * operators (and secret scanners) identify the credential at a glance, while
 * the URL-safe base64 encoding keeps the token usable verbatim in HTTP
 * Authorization headers and form bodies.
 *
 * <p>Only the plaintext is returned here; the caller is expected to hash the
 * value with a {@code PasswordEncoder} before persistence, and to hand the
 * plaintext back to the operator exactly once.
 */
public final class OAuth2ClientSecretGenerator {

    /**
     * Prefix for minted client secrets. {@code euler_sk_} stands for
     * "Euler secret key" and mirrors the {@code sk-} / {@code sk_live_}
     * style used by most LLM vendor API keys.
     */
    public static final String SECRET_PREFIX = "euler_sk_";

    /** 256 bits of entropy behind the prefix. */
    private static final int RANDOM_BYTES = 32;

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();

    private OAuth2ClientSecretGenerator() {
        // no instances
    }

    /**
     * Mints a fresh client secret as
     * {@code euler_sk_<url-safe base64 of 32 random bytes>}.
     *
     * @return the freshly generated plaintext secret
     */
    public static String generate() {
        byte[] random = new byte[RANDOM_BYTES];
        RANDOM.nextBytes(random);
        return SECRET_PREFIX + ENCODER.encodeToString(random);
    }

    /**
     * Whether the given {@code token_endpoint_auth_method} keeps a shared
     * secret between the client and the authorization server.
     *
     * <p>The three secret-bearing methods defined by RFC&nbsp;6749 /
     * RFC&nbsp;7523 are {@code client_secret_basic}, {@code client_secret_post}
     * and {@code client_secret_jwt}. Per RFC&nbsp;7591 §2,
     * {@code client_secret_basic} is the default when the value is
     * unspecified, so {@code null} / blank is treated as requiring a secret.
     *
     * @param tokenEndpointAuthMethod the method string as stored on the client
     * @return {@code true} iff a shared secret must be generated
     */
    public static boolean requiresClientSecret(String tokenEndpointAuthMethod) {
        if (tokenEndpointAuthMethod == null || tokenEndpointAuthMethod.isBlank()) {
            return true;
        }
        return ClientAuthenticationMethod.CLIENT_SECRET_BASIC.getValue().equals(tokenEndpointAuthMethod)
                || ClientAuthenticationMethod.CLIENT_SECRET_POST.getValue().equals(tokenEndpointAuthMethod)
                || ClientAuthenticationMethod.CLIENT_SECRET_JWT.getValue().equals(tokenEndpointAuthMethod);
    }
}
