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

package org.eulerframework.uc.oauth2.dao;

import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.eulerframework.uc.oauth2.entity.JwkEntity;
import org.eulerframework.uc.oauth2.entity.QJwkEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * QueryDSL-based DAO dedicated to read-only projections of {@link JwkEntity}.
 * It exists so fingerprint computation can bypass the encrypted {@code data}
 * column, which keeps the JWK ciphertext out of the query path.
 */
@Repository
public class JwkEntryDao {
    @Autowired
    private JPAQueryFactory jpaQueryFactory;

    /**
     * Load every JWK row projected to the subset of columns needed to derive the
     * aggregated JWK-set fingerprint ({@code kid}, {@code iat}, {@code status},
     * {@code fingerprint}, and the audit timestamps).
     *
     * @return a snapshot list; never {@code null}
     */
    public List<JwkEntity> listForCalcFingerprint() {
        QJwkEntity jwkEntity = QJwkEntity.jwkEntity;
        return this.jpaQueryFactory.select(Projections.bean(JwkEntity.class,
                        jwkEntity.kid,
                        jwkEntity.iat,
                        jwkEntity.status,
                        jwkEntity.fingerprint,
                        jwkEntity.createdDate,
                        jwkEntity.modifiedDate
                )).from(jwkEntity)
                .fetch();
    }
}
