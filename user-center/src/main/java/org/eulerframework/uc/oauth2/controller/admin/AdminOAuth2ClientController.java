package org.eulerframework.uc.oauth2.controller.admin;

import org.eulerframework.security.oauth2.server.authorization.client.EulerOAuth2Client;
import org.eulerframework.security.oauth2.server.authorization.client.EulerOAuth2ClientService;
import org.eulerframework.uc.oauth2.model.OAuth2Client;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.CredentialsContainer;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(path = {"admin/oauth2/clients", "api/admin/oauth2/clients"})
@PreAuthorize("hasAnyAuthority('root', 'admin')")
public class AdminOAuth2ClientController {

    private final EulerOAuth2ClientService oauth2ClientService;

    public AdminOAuth2ClientController(EulerOAuth2ClientService oauth2ClientService) {
        this.oauth2ClientService = oauth2ClientService;
    }

    /**
     * Creates a new OAuth 2.0 client.
     *
     * @param client the client to create
     * @return the created client (credentials erased)
     */
    @PostMapping
    public OAuth2Client createClient(@RequestBody OAuth2Client client) {
        EulerOAuth2Client created = this.oauth2ClientService.createClient(client);
        created.eraseCredentials();
        return (OAuth2Client) created;
    }

    /**
     * Retrieves a client by its registration ID.
     *
     * @param registrationId the registration identifier
     * @return the client (credentials erased)
     */
    @GetMapping("/{registrationId}")
    public OAuth2Client getClient(@PathVariable String registrationId) {
        EulerOAuth2Client client = this.oauth2ClientService.loadClientByRegistrationId(registrationId);
        if (client != null) {
            client.eraseCredentials();
        }
        return (OAuth2Client) client;
    }

    /**
     * Lists clients with pagination.
     *
     * @param offset the offset of the first result
     * @param limit  the maximum number of results
     * @return a list of clients (credentials erased)
     */
    @GetMapping
    public List<EulerOAuth2Client> listClients(
            @RequestParam int offset,
            @RequestParam int limit
    ) {
        return this.oauth2ClientService.listClients(offset, limit)
                .stream()
                .peek(CredentialsContainer::eraseCredentials)
                .toList();
    }

    /**
     * Updates an existing client using patch semantics.
     *
     * @param registrationId the registration identifier
     * @param client         the client with updated fields
     */
    @PutMapping("/{registrationId}")
    public void updateClient(@PathVariable String registrationId, @RequestBody OAuth2Client client) {
        client.setRegistrationId(registrationId);
        this.oauth2ClientService.updateClient(client);
    }

    /**
     * Deletes a client by its registration ID.
     *
     * @param registrationId the registration identifier
     */
    @DeleteMapping("/{registrationId}")
    public void deleteClient(@PathVariable String registrationId) {
        this.oauth2ClientService.deleteClient(registrationId);
    }

    /**
     * Updates only the client secret for the specified client.
     *
     * @param registrationId the registration identifier
     * @param body           a map containing the {@code "clientSecret"} key
     */
    @PutMapping("/{registrationId}/secret")
    public void updateClientSecret(@PathVariable String registrationId, @RequestBody Map<String, String> body) {
        String clientSecret = body.get("clientSecret");
        this.oauth2ClientService.updateClientSecret(registrationId, clientSecret);
    }
}
