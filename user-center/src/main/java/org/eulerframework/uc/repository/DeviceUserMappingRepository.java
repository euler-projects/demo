package org.eulerframework.uc.repository;

import org.eulerframework.uc.entity.AppAttestAttestationUserMappingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DeviceUserMappingRepository extends JpaRepository<AppAttestAttestationUserMappingEntity, String> {
    Optional<AppAttestAttestationUserMappingEntity> findByKeyId(String keyId);
}
