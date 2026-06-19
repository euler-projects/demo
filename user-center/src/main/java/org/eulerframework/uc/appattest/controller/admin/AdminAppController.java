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
package org.eulerframework.uc.appattest.controller.admin;

import org.eulerframework.security.authentication.appattest.AppAttestApp;
import org.eulerframework.security.authentication.appattest.AppAttestAppService;
import org.eulerframework.security.authentication.appattest.DefaultAppAttestApp;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Admin-facing CRUD endpoints for App Attest registered apps.
 *
 * <p>Request and response bodies are the service-layer model
 * {@link AppAttestApp} itself ({@link DefaultAppAttestApp} on the request side).
 * The fields exposed ({@code registrationId}, {@code teamId}, {@code bundleId},
 * {@code oauth2Enabled}, {@code oauth2ClientType}) are simple enough that no
 * dedicated request/response DTO layer is needed at this stage. If field-level
 * rules grow in complexity, DTOs can be introduced following the same pattern
 * as the OAuth2 client admin API.
 */
@RestController
@RequestMapping("admin/api/appattest/apps")
@PreAuthorize("hasAnyAuthority('root', 'admin')")
public class AdminAppController {

    private final AppAttestAppService appAttestAppService;

    public AdminAppController(AppAttestAppService appAttestAppService) {
        this.appAttestAppService = appAttestAppService;
    }

    /**
     * Creates a new App Attest app registration.
     *
     * <p>The {@code registrationId} is a server-owned field: any value carried
     * by the request body is discarded, and the service allocates a new
     * UUID-formatted identifier. Human-readable ids (e.g. {@code apple-default})
     * are reserved for pre-configured entries in {@code application.yml} and
     * cannot be injected through this endpoint.
     *
     * @param request the app to create
     * @return the created app with its {@code registrationId} populated
     */
    @PostMapping
    public AppAttestApp createApp(@RequestBody DefaultAppAttestApp request) {
        request.setRegistrationId(null);
        return this.appAttestAppService.createApp(request);
    }

    /**
     * Retrieves an app by its {@code registrationId}.
     *
     * @param registrationId the registration identifier
     * @return the app, or {@code null} if not found
     */
    @GetMapping("/{registrationId}")
    public AppAttestApp getApp(@PathVariable String registrationId) {
        return this.appAttestAppService.findByRegistrationId(registrationId);
    }

    /**
     * Lists apps with pagination.
     *
     * @param offset the offset of the first result
     * @param limit  the maximum number of results
     * @return a list of apps
     */
    @GetMapping
    public List<AppAttestApp> listApps(@RequestParam int offset, @RequestParam int limit) {
        return this.appAttestAppService.listApps(offset, limit);
    }

    /**
     * Replaces an existing app end-to-end (HTTP {@code PUT} semantics). Fields
     * omitted from {@code request} are reset, not preserved. The path variable
     * is authoritative: any {@code registrationId} carried by the request body
     * is overwritten.
     *
     * @param registrationId the registration identifier
     * @param request        the app with updated state
     * @return the persisted app
     */
    @PutMapping("/{registrationId}")
    public AppAttestApp updateApp(@PathVariable String registrationId,
                                  @RequestBody DefaultAppAttestApp request) {
        request.setRegistrationId(registrationId);
        this.appAttestAppService.updateApp(request);
        return this.appAttestAppService.findByRegistrationId(registrationId);
    }

    /**
     * Patches an existing app (HTTP {@code PATCH} semantics). Only fields
     * carrying a non-{@code null} value on {@code request} are applied;
     * omitted fields keep the persisted value. The path variable is
     * authoritative.
     *
     * <p>{@code teamId} and {@code bundleId} must be patched together &mdash;
     * either both present or both absent, see
     * {@link AppAttestApp#getAppId()} for rationale.
     *
     * @param registrationId the registration identifier
     * @param request        the app carrying the fields to patch
     * @return the persisted app
     */
    @PatchMapping("/{registrationId}")
    public AppAttestApp patchApp(@PathVariable String registrationId,
                                 @RequestBody DefaultAppAttestApp request) {
        request.setRegistrationId(registrationId);
        this.appAttestAppService.patchApp(request);
        return this.appAttestAppService.findByRegistrationId(registrationId);
    }

    /**
     * Deletes an app by its {@code registrationId}.
     *
     * @param registrationId the registration identifier
     */
    @DeleteMapping("/{registrationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteApp(@PathVariable String registrationId) {
        this.appAttestAppService.deleteByRegistrationId(registrationId);
    }
}
