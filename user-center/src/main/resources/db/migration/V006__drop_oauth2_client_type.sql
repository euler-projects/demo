-- V006: Drop the oauth2_client_type column from app_attest_app_registration.
--
-- RegisteredApp no longer carries an OAuth2 client provisioning type: every
-- OAuth2-enabled app now registers a per-key client through RFC 7591 dynamic
-- client registration. Historical STATIC OAuth2 clients are unaffected because
-- they live in oauth2_registered_client (with their client-type marker preserved
-- in client_settings) and are resolved by the compatibility fallback in
-- EulerOAuth2ClientAttestationVerifier.
--
-- Deploy order: upgrade all application nodes first, then let Flyway run this
-- migration, so that no node still maps the dropped column.

alter table app_attest_app_registration
    drop column oauth2_client_type;
