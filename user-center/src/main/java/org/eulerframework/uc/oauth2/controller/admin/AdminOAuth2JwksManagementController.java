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
package org.eulerframework.uc.oauth2.controller.admin;

import com.nimbusds.jose.jwk.JWK;
import org.eulerframework.security.oauth2.server.authorization.jwk.JwkEntry;
import org.eulerframework.security.oauth2.server.authorization.jwk.JwkManageService;
import org.eulerframework.security.oauth2.server.authorization.jwk.JwkStatus;
import org.eulerframework.uc.oauth2.model.JwkKeyCreateRequest;
import org.eulerframework.uc.oauth2.model.JwkKeyUpdateRequest;
import org.eulerframework.uc.oauth2.model.JwkKeyView;
import org.eulerframework.uc.oauth2.service.jwk.keygen.JwkGenerator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

/**
 * Management half of the JWK admin surface. Exposes strict CRUDL endpoints that
 * mutate the persistent store and trigger reload + cluster-convergence polling
 * through {@link JwkManageService}. Only activated when a
 * {@link JwkManageService} bean is contributed by the application &mdash; the
 * in-memory profile deliberately lacks this controller so callers get a clean
 * {@code 404} rather than a stub {@code 501}.
 *
 * <h2>Endpoint semantics</h2>
 * <ul>
 *   <li>{@code POST /} &mdash; generate a fresh key pair server-side, persist
 *       it as a {@link JwkStatus#PENDING PENDING} entry, respond
 *       {@code 201 Created} with the public projection.</li>
 *   <li>{@code GET /} &mdash; list every stored key as {@link JwkKeyView}.</li>
 *   <li>{@code GET /{kid}} &mdash; fetch a single entry as {@link JwkKeyView};
 *       {@code 404 Not Found} when the kid does not exist.</li>
 *   <li>{@code PUT /{kid}} &mdash; full-overwrite update of the lifecycle
 *       state. Body {@code status} is mandatory. Responds {@code 204 No
 *       Content}. Cluster convergence is not carried in the response; the
 *       observability surface exposes it separately.</li>
 *   <li>{@code PATCH /{kid}} &mdash; partial update of the lifecycle state.
 *       A {@code null} body status means no-op. Responds {@code 204 No
 *       Content}.</li>
 *   <li>{@code DELETE /{kid}} &mdash; physically remove a
 *       {@link JwkStatus#RETIRED RETIRED} entry. Responds {@code 204 No
 *       Content}; attempts to delete a non-RETIRED entry surface as
 *       {@code 400 Bad Request} via the global exception handler.</li>
 * </ul>
 *
 * <p>Mounted at both {@code admin/oauth2/jwks/**} and
 * {@code api/admin/oauth2/jwks/**}.
 */
@RestController
@RequestMapping(path = {"admin/oauth2/jwks", "api/admin/oauth2/jwks"})
@PreAuthorize("hasAnyAuthority('root', 'admin')")
@ConditionalOnBean(JwkManageService.class)
public class AdminOAuth2JwksManagementController {

    private final JwkManageService jwkManageService;
    private final JwkGenerator jwkGenerator;

    public AdminOAuth2JwksManagementController(JwkManageService jwkManageService,
                                               JwkGenerator jwkGenerator) {
        this.jwkManageService = jwkManageService;
        this.jwkGenerator = jwkGenerator;
    }

    /**
     * Generate a fresh key pair server-side and persist it as a {@code PENDING}
     * entry. Private-key material never escapes this method: it is fed
     * directly into {@link JwkManageService#createKey(JwkEntry)} and only the
     * public projection ({@link JwkKeyView}) is returned to the caller.
     */
    @PostMapping
    public ResponseEntity<JwkKeyView> createKey(@RequestBody JwkKeyCreateRequest request) {
        JWK jwk = this.jwkGenerator.generate(request.toSpec());
        JwkEntry entry = new JwkEntry(jwk, JwkStatus.PENDING);
        JwkEntry created = this.jwkManageService.createKey(entry);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(JwkKeyView.from(created, created.isUsableForSigning()));
    }

    /** Lists every stored JWK entry as an admin-friendly view (no private material). */
    @GetMapping
    public List<JwkKeyView> listKeys() {
        List<JwkEntry> entries = this.jwkManageService.listKeys();
        List<JwkKeyView> views = new ArrayList<>(entries.size());
        for (JwkEntry entry : entries) {
            views.add(JwkKeyView.from(entry, entry.isUsableForSigning()));
        }
        return views;
    }

    /** Fetches a single entry by {@code kid}; {@code 404} when not found. */
    @GetMapping("/{kid}")
    public JwkKeyView getKey(@PathVariable String kid) {
        JwkEntry entry = this.jwkManageService.findByKid(kid);
        if (entry == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "JWK kid=" + kid + " not found");
        }
        return JwkKeyView.from(entry, entry.isUsableForSigning());
    }

    /**
     * Full-overwrite update of the lifecycle state (HTTP {@code PUT}). The
     * path {@code kid} is authoritative: the underlying JWK material is
     * fetched from the persistent store and combined with the requested
     * {@code status} before being handed to
     * {@link JwkManageService#updateKey(JwkEntry)}.
     * <p>
     * {@code status} is mandatory; {@code null} surfaces as {@code 400 Bad
     * Request}. Non-existent {@code kid} surfaces as the service layer's
     * {@code IllegalStateException}, which the global handler maps to
     * {@code 404 Not Found}.
     */
    @PutMapping("/{kid}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateKey(@PathVariable String kid,
                          @RequestBody JwkKeyUpdateRequest request) {
        if (request == null || request.status() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "status is required for PUT");
        }
        JwkEntry existing = requireExisting(kid);
        this.jwkManageService.updateKey(new JwkEntry(existing.jwk(), request.status()));
    }

    /**
     * Partial update of the lifecycle state (HTTP {@code PATCH}). A {@code
     * null} {@code status} is a no-op; any non-{@code null} value is applied
     * through {@link JwkManageService#patchKey(JwkEntry)}.
     */
    @PatchMapping("/{kid}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void patchKey(@PathVariable String kid,
                         @RequestBody JwkKeyUpdateRequest request) {
        if (request == null || request.status() == null) {
            return;
        }
        JwkEntry existing = requireExisting(kid);
        this.jwkManageService.patchKey(new JwkEntry(existing.jwk(), request.status()));
    }

    /**
     * Physically delete a {@link JwkStatus#RETIRED RETIRED} entry. Pre-validation
     * of the status happens inside
     * {@link JwkManageService#deleteByKid(String)}; attempts to delete a
     * non-RETIRED entry surface as {@code 400 Bad Request} via the global
     * exception handler chain.
     */
    @DeleteMapping("/{kid}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteKey(@PathVariable String kid) {
        this.jwkManageService.deleteByKid(kid);
    }

    private JwkEntry requireExisting(String kid) {
        JwkEntry existing = this.jwkManageService.findByKid(kid);
        if (existing == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "JWK kid=" + kid + " not found");
        }
        return existing;
    }
}
