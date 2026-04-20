package org.eulerframework.uc.repository;

import org.eulerframework.uc.entity.DeviceAppUserMappingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DeviceUserMappingRepository extends JpaRepository<DeviceAppUserMappingEntity, String> {
    Optional<DeviceAppUserMappingEntity> findByKeyId(String keyId);
}
