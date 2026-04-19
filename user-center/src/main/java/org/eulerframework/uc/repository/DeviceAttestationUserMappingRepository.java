package org.eulerframework.uc.repository;

import org.eulerframework.uc.entity.DeviceAttestationUserMappingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DeviceAttestationUserMappingRepository extends JpaRepository<DeviceAttestationUserMappingEntity, String> {
    Optional<DeviceAttestationUserMappingEntity> findByKeyId(String keyId);
}
