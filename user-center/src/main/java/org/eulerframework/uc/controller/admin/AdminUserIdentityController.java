package org.eulerframework.uc.controller.admin;

import org.eulerframework.security.core.identity.UserIdentity;
import org.eulerframework.security.core.identity.UserIdentityNotFoundException;
import org.eulerframework.security.core.identity.UserIdentityService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Admin-facing CRUDL endpoints for user identities.
 *
 * <p>User Identity is a sub-resource of User. All endpoints require
 * {@code root} or {@code admin} authority. Create and Update bypass
 * per-type verification (no OTP consumption); only {@code phone} and
 * {@code email} types are supported for write operations.
 */
@RestController
@RequestMapping("admin/api/users/{userId}/identities")
@PreAuthorize("hasAnyAuthority('root', 'admin')")
public class AdminUserIdentityController {

    private final UserIdentityService userIdentityService;

    public AdminUserIdentityController(UserIdentityService userIdentityService) {
        this.userIdentityService = userIdentityService;
    }

    @PostMapping
    public UserIdentity createIdentity(@PathVariable String userId,
                                       @RequestBody Map<String, Object> body) {
        UserIdentity prototype = UserIdentity.withExtensions(body)
                .build();

        return this.userIdentityService.createUserIdentity(userId, prototype);
    }

    @GetMapping("/{identityId}")
    public UserIdentity getIdentity(@PathVariable String userId,
                                    @PathVariable String identityId) {
        return this.userIdentityService.getUserIdentity(userId, identityId)
                .orElseThrow(() -> new UserIdentityNotFoundException(identityId));
    }

    @GetMapping
    public List<UserIdentity> listIdentities(
            @PathVariable String userId,
            @RequestParam(required = false) String identityType) {
        return this.userIdentityService.listUserIdentities(userId, identityType);
    }

    @PutMapping("/{identityId}")
    public UserIdentity updateIdentity(@PathVariable String userId,
                                      @PathVariable String identityId,
                                      @RequestBody Map<String, Object> body) {
        // identityType is resolved by the service from the existing record
        UserIdentity prototype = UserIdentity.withExtensions(body)
                .identityType("__update__") // placeholder; service resolves actual type from DB
                .build();

        return this.userIdentityService.updateUserIdentity(userId, identityId, prototype);
    }

    @DeleteMapping("/{identityId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteIdentity(@PathVariable String userId,
                               @PathVariable String identityId) {
        this.userIdentityService.deleteUserIdentity(userId, identityId);
    }

    @GetMapping("/{identityId}/raw-fields/{fieldName}")
    public Map<String, String> getRawFieldValue(@PathVariable String userId,
                                               @PathVariable String identityId,
                                               @PathVariable String fieldName) {
        String rawValue = this.userIdentityService.getRawFieldValue(userId, identityId, fieldName)
                .orElseThrow(() -> new UserIdentityNotFoundException(identityId));
        return Map.of("fieldName", fieldName, "rawValue", rawValue);
    }
}
