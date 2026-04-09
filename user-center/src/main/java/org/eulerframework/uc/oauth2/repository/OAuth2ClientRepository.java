package org.eulerframework.uc.oauth2.repository;

import org.eulerframework.uc.oauth2.entity.OAuth2ClientEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OAuth2ClientRepository extends JpaRepository<OAuth2ClientEntity, String> {

    /**
     * Finds a client entity by its client ID.
     *
     * @param clientId the client identifier
     * @return an optional containing the entity, or empty if not found
     */
    Optional<OAuth2ClientEntity> findByClientId(String clientId);
}
