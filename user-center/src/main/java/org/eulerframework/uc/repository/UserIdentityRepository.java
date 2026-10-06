/*
 * Copyright 2013-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.eulerframework.uc.repository;

import org.eulerframework.uc.entity.UserIdentityEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserIdentityRepository extends JpaRepository<UserIdentityEntity, String> {

    Optional<UserIdentityEntity> findByIdAndUserId(String id, String userId);

    Optional<UserIdentityEntity> findByIdAndUserIdAndIdentityType(
            String id, String userId, String identityType);

    List<UserIdentityEntity> findAllByUserIdAndIdentityType(String userId, String identityType);

    /**
     * Whether the account already carries an identity of a type other than the given one.
     *
     * <p>Spans the parent table rather than one backend's rows because a backend has to
     * refuse an account that is already identified some other way, and cannot see other
     * backends' child tables.
     */
    boolean existsByUserIdAndIdentityTypeNot(String userId, String identityType);

    /**
     * Whether the account already carries an identity of the given type.
     *
     * <p>The complement of the above, for a backend that admits only one identity per
     * account. {@code app_attest_instance_key} is such a backend: its single key is the whole
     * credential, and a second one could only be asked for by a caller able to name the
     * account rather than prove control of it.
     */
    boolean existsByUserIdAndIdentityType(String userId, String identityType);

    boolean existsByIdentityTypeAndSubject(String identityType, String subject);

    Optional<UserIdentityEntity> findByIdentityTypeAndSubject(String identityType, String subject);
}
