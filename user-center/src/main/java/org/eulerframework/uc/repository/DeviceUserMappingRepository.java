package org.eulerframework.uc.repository;

import org.eulerframework.uc.entity.DeviceUserMappingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DeviceUserMappingRepository extends JpaRepository<DeviceUserMappingEntity, String> {
    Optional<DeviceUserMappingEntity> findByKeyId(String keyId);
}
