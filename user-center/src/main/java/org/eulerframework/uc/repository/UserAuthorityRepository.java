package org.eulerframework.uc.repository;

import org.eulerframework.uc.entity.UserAuthorityEntity;
import org.eulerframework.uc.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserAuthorityRepository extends JpaRepository<UserAuthorityEntity, Long> {
}
