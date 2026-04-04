package org.eulerframework.uc.repository;

import org.eulerframework.uc.entity.AppleAppAttestUserMappingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AppleAppAttestUserMappingRepository extends JpaRepository<AppleAppAttestUserMappingEntity, String> {
    Optional<AppleAppAttestUserMappingEntity> findByKeyId(String keyId);
}
