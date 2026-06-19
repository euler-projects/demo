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
import org.eulerframework.security.jwk.JwkEntry;
import org.eulerframework.security.jwk.JwkManageService;
import org.eulerframework.security.jwk.JwkStatus;
import org.eulerframework.security.jwk.ManagedJwk;
import org.eulerframework.uc.oauth2.model.JwkKeyCreateRequest;
import org.eulerframework.uc.oauth2.model.JwkKeyPatchRequest;
import org.eulerframework.uc.oauth2.model.JwkKeyView;
import org.eulerframework.uc.oauth2.model.JwkModel;
import org.eulerframework.uc.oauth2.service.jwk.keygen.JwkGenerator;
import org.eulerframework.web.core.exception.web.api.ResourceNotFoundException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;


/**
 * Administrative REST API for managing the OAuth2 authorization server JWK set.
 * It is only wired when a {@link JwkManageService} bean is present, which is
 * what ties the lifecycle of these endpoints to the {@code persistent-jwk-source}
 * profile.
 * <p>
 * All endpoints require either the {@code root} or {@code admin} authority.
 */
@RestController
@RequestMapping("admin/api/oauth2/jwks")
@PreAuthorize("hasAnyAuthority('root', 'admin')")
@ConditionalOnBean(JwkManageService.class)
public class AdminOAuth2JwksManagementController {

    private final JwkManageService jwkManageService;
    private final JwkGenerator jwkGenerator;

    public AdminOAuth2JwksManagementController(
            JwkManageService jwkManageService, JwkGenerator jwkGenerator) {
        this.jwkManageService = jwkManageService;
        this.jwkGenerator = jwkGenerator;
    }

    /**
     * Generate a new JWK according to {@code request} and persist it with
     * {@link JwkStatus#PENDING} status.
     *
     * @param request generation parameters (algorithm, optional key size/curve)
     * @return {@code 201 Created} with the newly generated {@link JwkKeyView}
     */
    @PostMapping
    public ResponseEntity<JwkKeyView> createKey(@RequestBody JwkKeyCreateRequest request) {
        JWK jwk = this.jwkGenerator.generate(request.toSpec());
        JwkEntry entry = new JwkEntry(jwk, JwkStatus.PENDING);
        ManagedJwk created = this.jwkManageService.createJwk(entry);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(JwkKeyView.from(created));
    }

    /**
     * @return every managed JWK as an administrative view
     */
    @GetMapping
    public List<JwkKeyView> listKeys() {
        List<ManagedJwk> managedJwks = this.jwkManageService.listJwks();
        List<JwkKeyView> views = new ArrayList<>(managedJwks.size());
        for (ManagedJwk managedJwk : managedJwks) {
            views.add(JwkKeyView.from(managedJwk));
        }
        return views;
    }

    /**
     * Fetch a single JWK by its {@code kid}.
     *
     * @param kid JWK key identifier
     * @return the matching {@link JwkKeyView}
     * @throws ResourceNotFoundException if no key matches {@code kid}
     */
    @GetMapping("/{kid}")
    public JwkKeyView getKey(@PathVariable String kid) {
        ManagedJwk managedJwk = this.jwkManageService.getJwk(kid);
        if (managedJwk == null) {
            throw new ResourceNotFoundException("Key not found");
        }
        return JwkKeyView.from(managedJwk);
    }

    /**
     * Apply a partial update (currently only {@link JwkStatus}) to the JWK
     * identified by {@code kid}.
     *
     * @param kid     JWK key identifier
     * @param request patch payload; fields left {@code null} are preserved
     */
    @PatchMapping("/{kid}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void patchKey(@PathVariable String kid,
                         @RequestBody JwkKeyPatchRequest request) {
        JwkModel patching = new JwkModel();
        patching.setKid(kid);
        patching.setStatus(request.status());
        this.jwkManageService.patchJwk(patching);
    }

    /**
     * Delete the JWK identified by {@code kid}.
     *
     * @param kid JWK key identifier
     */
    @DeleteMapping("/{kid}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteKey(@PathVariable String kid) {
        this.jwkManageService.deleteJwk(kid);
    }
}
