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

import com.nimbusds.jose.Algorithm;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.KeyUse;
import org.eulerframework.security.oauth2.server.authorization.jwk.AbstractJwkManageService;
import org.eulerframework.security.oauth2.server.authorization.jwk.JwkEntry;
import org.eulerframework.security.oauth2.server.authorization.jwk.JwkRepositoryChangedEvent;
import org.eulerframework.security.oauth2.server.authorization.jwk.JwkStatus;
import org.eulerframework.uc.oauth2.entity.JwkEntity;
import org.eulerframework.uc.oauth2.repository.JwkEntityRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * JPA-backed {@link org.eulerframework.security.oauth2.server.authorization.jwk.JwkManageService}
 * implementation. Persists each JWK as its standard JSON serialisation (RFC 7517)
 * wrapped in an AES-256-GCM envelope; the JWK's {@code kid} is bound to the GCM
 * authentication tag as AAD, defeating row-level ciphertext swap attacks. Algorithm,
 * use and {@code iat} are part of the JWK JSON itself, so the table only needs the
 * lifecycle status alongside the cipher columns.
 *
 * <h2>Responsibilities</h2>
 * This class only owns row-level persistence and per-JWK <em>self-description</em>
 * validation (supported {@code alg}, {@code use=sig}, non-null {@code iat}). The
 * CRUDL flow, audit logging, {@link JwkRepositoryChangedEvent reload-notification}
 * broadcast and the cross-entry aggregate contract (at most one ACTIVE per
 * algorithm, kid uniqueness, private-key requirement on ACTIVE/PENDING) live in
 * {@link AbstractJwkManageService} and MUST NOT be re-implemented here.
 */
@Service
@Transactional
public class DefaultJwkManageService extends AbstractJwkManageService {

    private static final Logger log = LoggerFactory.getLogger(DefaultJwkManageService.class);

    private static final Set<String> SUPPORTED_ALGS = Set.of(
            "RS256", "RS384", "RS512",
            "ES256", "ES384", "ES512",
            "EdDSA");

    private final JwkEntityRepository repository;
    private final JwkKekCodec kekCodec;

    public DefaultJwkManageService(JwkEntityRepository repository,
                                   JwkKekCodec kekCodec,
                                   ApplicationEventPublisher publisher) {
        super(publisher);
        this.repository = Objects.requireNonNull(repository, "repository");
        this.kekCodec = Objects.requireNonNull(kekCodec, "kekCodec");
    }

    // ---- backend hooks ----

    @Override
    protected JwkEntry doCreate(JwkEntry entry) {
        JWK jwk = entry.jwk();
        String kid = entry.kid();
        validateJwkSelfDescription(jwk, kid);

        JwkEntity row = new JwkEntity();
        row.setKid(kid);
        applyCipher(row, entry);

        JwkEntity persisted = this.repository.save(row);
        log.info("Created JWK kid={} alg={} status={}", kid, algName(jwk), entry.status());
        return new JwkEntry(jwk, JwkStatus.valueOf(persisted.getStatus().toUpperCase(Locale.ROOT)));
    }

    @Override
    @Transactional(readOnly = true)
    protected JwkEntry doFindByKid(String kid) {
        return this.repository.findById(kid).map(this::decode).orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    protected List<JwkEntry> doList() {
        List<JwkEntity> rows = this.repository.findAll();
        List<JwkEntry> entries = new ArrayList<>(rows.size());
        for (JwkEntity row : rows) {
            entries.add(decode(row));
        }
        return List.copyOf(entries);
    }

    @Override
    protected void doUpdate(JwkEntry entry) {
        applyReplace(entry, "Updated");
    }

    @Override
    protected void doPatch(JwkEntry entry) {
        // The base template has already merged the incoming partial state with
        // the existing row (null fields carried over); at this point a full
        // replacement is semantically equivalent to a patch.
        applyReplace(entry, "Patched");
    }

    @Override
    protected void doDelete(String kid) {
        Optional<JwkEntity> existing = this.repository.findById(kid);
        if (existing.isEmpty()) {
            throw new IllegalStateException("Delete race: kid=" + kid + " not found");
        }
        this.repository.deleteById(kid);
        log.info("Deleted JWK kid={}", kid);
    }

    // ---- internals ----

    private void applyReplace(JwkEntry entry, String opLabel) {
        JWK jwk = entry.jwk();
        String kid = entry.kid();
        validateJwkSelfDescription(jwk, kid);

        JwkEntity row = this.repository.findById(kid).orElseThrow(() -> new IllegalStateException(
                "Update race: JWK kid=" + kid + " not found"));
        applyCipher(row, entry);
        this.repository.save(row);
        log.info("{} JWK kid={} alg={} status={}", opLabel, kid, algName(jwk), entry.status());
    }

    private void applyCipher(JwkEntity row, JwkEntry entry) {
        JWK jwk = entry.jwk();
        String kid = entry.kid();
        char[] plaintext = jwk.toJSONString().toCharArray();
        JwkKekCodec.Ciphertext ct;
        try {
            ct = this.kekCodec.encrypt(plaintext, kid);
        }
        finally {
            Arrays.fill(plaintext, '\0');
        }
        row.setStatus(entry.status().name());
        row.setEncKid(ct.encKid());
        row.setEncIv(ct.iv());
        row.setEncTag(ct.tag());
        row.setJwkCipher(ct.cipher());
    }

    private JwkEntry decode(JwkEntity row) {
        String kid = row.getKid();
        String statusName = row.getStatus();
        if (kid == null || statusName == null) {
            throw new IllegalStateException("Row in oauth2_jwk has null mandatory column (kid/status)");
        }
        JwkStatus status = JwkStatus.valueOf(statusName.toUpperCase(Locale.ROOT));

        char[] plaintext = this.kekCodec.decrypt(
                row.getEncKid(), row.getEncIv(), row.getEncTag(), row.getJwkCipher(), kid);
        try {
            JWK jwk;
            try {
                jwk = JWK.parse(new String(plaintext));
            }
            catch (ParseException ex) {
                throw new IllegalStateException("Entry " + kid + ": failed to parse JWK JSON payload", ex);
            }
            if (!kid.equals(jwk.getKeyID())) {
                throw new IllegalStateException("Entry " + kid
                        + ": JWK JSON kid='" + jwk.getKeyID() + "' does not match column kid");
            }
            validateJwkSelfDescription(jwk, kid);
            return new JwkEntry(jwk, status);
        }
        finally {
            Arrays.fill(plaintext, '\0');
        }
    }

    private static void validateJwkSelfDescription(JWK jwk, String kid) {
        Algorithm alg = jwk.getAlgorithm();
        if (alg == null || !SUPPORTED_ALGS.contains(alg.getName())) {
            throw new IllegalStateException("JWK kid=" + kid + " has unsupported or missing 'alg'="
                    + alg + " (supported: " + SUPPORTED_ALGS + ")");
        }
        KeyUse use = jwk.getKeyUse();
        if (use == null || !KeyUse.SIGNATURE.equals(use)) {
            throw new IllegalStateException("JWK kid=" + kid + " must declare use=sig, got " + use);
        }
        if (jwk.getIssueTime() == null) {
            throw new IllegalStateException("JWK kid=" + kid + " is missing 'iat'");
        }
    }

    private static String algName(JWK jwk) {
        return jwk.getAlgorithm() == null ? "null" : jwk.getAlgorithm().getName();
    }
}
