package org.eulerframework.uc.repository;

import org.eulerframework.uc.entity.WechatUserMappingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WechatUserMappingRepository extends JpaRepository<WechatUserMappingEntity, String> {
    Optional<WechatUserMappingEntity> findByOpenId(String openId);
}
