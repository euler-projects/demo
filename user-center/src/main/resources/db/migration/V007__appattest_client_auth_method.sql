-- V007: Move Apple App Attest clients to their own token endpoint authentication method.
--
-- The Apple App Attest variant of attestation-based client authentication used to share the
-- draft's attest_jwt_client_auth value, with the variant selected at runtime by the custom
-- OAuth-Client-Attestation-Type request header. Section 5 of
-- draft-ietf-oauth-attestation-based-client-auth-11 expects each proof of possession mechanism to
-- be identifiable by its own token endpoint authentication method value, so the variant now
-- authenticates as attest_appattest_client_auth and that header is deprecated: the variant is
-- resolved from the credential carriage instead.
--
-- Every existing row declaring attest_jwt_client_auth is an Apple App Attest client -- no released
-- client has ever used the draft's standard PoP JWT variant -- so the value is replaced outright
-- rather than kept alongside, and no client is left declaring a method it does not use.
--
-- oauth2_client.token_endpoint_auth_method is a scalar: RFC 7591 admits exactly one method per
-- client, DefaultEulerOAuth2Client enforces that on the way in and OAuth2ClientUtils lifts the
-- scalar into a singleton RegisteredClient.clientAuthenticationMethods entry on the way out. So
-- this is a plain single-value rewrite, with no delimited list to walk.
--
-- Deploy order: upgrade all application nodes first, then let Flyway run this migration, so that no
-- node still resolves a client by the old value.

update oauth2_client
set token_endpoint_auth_method = 'attest_appattest_client_auth'
where token_endpoint_auth_method = 'attest_jwt_client_auth';
