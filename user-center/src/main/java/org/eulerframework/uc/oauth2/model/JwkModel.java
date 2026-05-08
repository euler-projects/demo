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

package org.eulerframework.uc.oauth2.model;

import com.nimbusds.jose.jwk.JWK;
import org.eulerframework.security.jwk.JwkEntry;
import org.eulerframework.security.jwk.JwkStatus;
import org.eulerframework.security.jwk.ManagedJwk;

/**
 * Mutable, javabean-style implementation of {@link ManagedJwk} used as the
 * canonical in-memory / DTO representation of a managed JWK inside the
 * user-center. It is deliberately detached from any persistence framework so
 * it can also flow through service and controller layers.
 */
public class JwkModel implements ManagedJwk {
    private String kid;
    private JwkStatus status;
    private JWK jwk;

    @Override
    public String getKid() {
        return kid;
    }

    public void setKid(String kid) {
        this.kid = kid;
    }

    @Override
    public JwkStatus getStatus() {
        return status;
    }

    public void setStatus(JwkStatus status) {
        this.status = status;
    }

    @Override
    public JWK getJwk() {
        return jwk;
    }

    public void setJwk(JWK jwk) {
        this.jwk = jwk;
    }

    @Override
    public void reloadJwkEntry(JwkEntry jwkEntry) {
        this.kid = jwkEntry.kid();
        this.jwk = jwkEntry.jwk();
        this.status = jwkEntry.status();
    }
}
