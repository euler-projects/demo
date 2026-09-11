package org.eulerframework.uc.repository;

import org.eulerframework.uc.entity.AppAttestAttestationUserMappingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * @deprecated dropped together with {@code AppAttestAttestationUserMappingEntity} and
 * {@code EulerDeviceUserDetailsService}; see the SPI for the authoritative notice.
 */
@Deprecated
@Repository
public interface DeviceUserMappingRepository extends JpaRepository<AppAttestAttestationUserMappingEntity, String> {
    Optional<AppAttestAttestationUserMappingEntity> findByKeyId(String keyId);
}
