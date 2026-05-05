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

import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.eulerframework.security.oauth2.server.authorization.jwk.source.ClusteredReloadableJwkSource;
import org.eulerframework.security.oauth2.server.authorization.jwk.JwkEntry;
import org.eulerframework.security.oauth2.server.authorization.jwk.JwkRepository;
import org.eulerframework.security.oauth2.server.authorization.jwk.source.ReloadableJwkSource;
import org.eulerframework.uc.oauth2.model.JwkKeyView;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Observability half of the JWK admin surface. Read-only endpoints plus the
 * {@code /refresh} reload trigger — all of which work regardless of whether a
 * {@link org.eulerframework.security.oauth2.server.authorization.jwk.JwkManageService}
 * bean is contributed. Mounted at both {@code admin/oauth2/jwks/**} and
 * {@code api/admin/oauth2/jwks/**} to match the dual-path convention used by
 * the other admin controllers.
 */
@RestController
@RequestMapping(path = {"admin/oauth2/jwks", "api/admin/oauth2/jwks"})
@PreAuthorize("hasAnyAuthority('root', 'admin')")
public class AdminOAuth2JwksObservabilityController {

    private final JwkRepository repository;
    private final ReloadableJwkSource reloadableJwkSource;
    private final ClusteredReloadableJwkSource clusteredReloadableJwkSource;

    public AdminOAuth2JwksObservabilityController(
            JwkRepository repository,
            JWKSource<SecurityContext> jwkSource) {
        this.repository = repository;
        if (jwkSource instanceof ClusteredReloadableJwkSource) {
            this.clusteredReloadableJwkSource = (ClusteredReloadableJwkSource) jwkSource;
            this.reloadableJwkSource = this.clusteredReloadableJwkSource;
        } else if (jwkSource instanceof ReloadableJwkSource) {
            this.reloadableJwkSource = (ReloadableJwkSource) jwkSource;
            this.clusteredReloadableJwkSource = null;
        } else {
            this.reloadableJwkSource = null;
            this.clusteredReloadableJwkSource = null;
        }
    }

    /**
     * List every stored JWK as an admin-friendly view. The {@code signing}
     * flag is derived from {@link JwkEntry#isUsableForSigning()} (i.e.
     * {@code status == ACTIVE}); private-key material is never exposed.
     */
    @GetMapping
    public List<JwkKeyView> listKeys() {
        List<JwkEntry> entries = this.repository.load();
        List<JwkKeyView> views = new ArrayList<>(entries.size());
        for (JwkEntry entry : entries) {
            views.add(JwkKeyView.from(entry, entry.isUsableForSigning()));
        }
        return views;
    }

    /**
     * Force a re-read of the authoritative repository and rebuild the live
     * state. Returns {@code 204 No Content}: convergence observation belongs
     * to {@link #cluster()}.
     */
    @PostMapping("refresh")
    public ResponseEntity<Void> refresh() {
        if (this.reloadableJwkSource == null) {
            throw new UnsupportedOperationException("ReloadableJwkSource is not yet implemented");
        }
        this.reloadableJwkSource.reload();
        return ResponseEntity.noContent().build();
    }

    /**
     * Cluster fingerprint and per-node heartbeat snapshot. Returns
     * {@code 501 Not Implemented} in standalone deployments where no
     * {@link ClusteredReloadableJwkSource} bean exists.
     */
    @GetMapping("cluster")
    public ResponseEntity<?> cluster() {
        if (this.clusteredReloadableJwkSource == null) {
            return standaloneResponse();
        }
        ClusteredReloadableJwkSource.ClusterStatus status = this.clusteredReloadableJwkSource.clusterStatus();
        return ResponseEntity.ok(status);
    }

    /**
     * Advisory hint to a specific peer node to re-read the repository.
     * Returns {@code 501 Not Implemented} in standalone deployments.
     */
    @PostMapping("cluster/{nodeId}/refresh")
    public ResponseEntity<Void> refreshNode(@PathVariable String nodeId) {
        if (this.clusteredReloadableJwkSource == null) {
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
        }
        this.clusteredReloadableJwkSource.triggerNodeRefresh(nodeId);
        return ResponseEntity.noContent().build();
    }

    private static ResponseEntity<Map<String, String>> standaloneResponse() {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
                .body(Map.of(
                        "error", "not_implemented",
                        "message", "cluster endpoints are only available when clustered JWK source is active"));
    }
}
