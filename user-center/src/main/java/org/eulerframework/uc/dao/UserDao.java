package org.eulerframework.uc.dao;

import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.annotation.Resource;
import org.eulerframework.uc.entity.*;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public class UserDao {
    @Resource
    private JPAQueryFactory jpaQueryFactory;

    public List<UserEntity> listUserEntities(int offset, int limit) {
        QUserEntity userEntity = QUserEntity.userEntity;
        return this.jpaQueryFactory.selectFrom(userEntity)
                .orderBy(userEntity.createdDate.desc())
                .offset(offset)
                .limit(limit)
                .fetch();
    }

    public UserEntity queryUserEntityByUserId(String userId) {
        QUserEntity userEntity = QUserEntity.userEntity;
        return this.jpaQueryFactory.selectFrom(userEntity)
                .where(userEntity.userId.eq(userId))
                .fetchOne();
    }

    public UserEntity queryUserEntityByUsername(String username) {
        QUserEntity userEntity = QUserEntity.userEntity;
        return this.jpaQueryFactory.selectFrom(userEntity)
                .where(userEntity.username.eq(username))
                .fetchOne();
    }

    public UserEntity queryUserEntityByEmail(String email) {
        QUserEntity userEntity = QUserEntity.userEntity;
        return this.jpaQueryFactory.selectFrom(userEntity)
                .where(userEntity.email.eq(email))
                .fetchOne();
    }

    public UserEntity queryUserEntityByPhone(String phone) {
        QUserEntity userEntity = QUserEntity.userEntity;
        return this.jpaQueryFactory.selectFrom(userEntity)
                .where(userEntity.phone.eq(phone))
                .fetchOne();
    }

    public List<AuthorityEntity> queryUserAuthorityByUserId(String userId) {
        QAuthorityEntity authorityEntity = QAuthorityEntity.authorityEntity;
        QUserAuthorityEntity userAuthorityEntity = QUserAuthorityEntity.userAuthorityEntity;
        return this.jpaQueryFactory.selectFrom(authorityEntity)
                .where(authorityEntity.authority.in(
                        JPAExpressions.select(userAuthorityEntity.authority)
                                .from(userAuthorityEntity)
                                .where(userAuthorityEntity.userId.eq(userId))
                ))
                .fetch();
    }

    public AuthorityEntity queryAuthorityEntity(String authority) {
        QAuthorityEntity authorityEntity = QAuthorityEntity.authorityEntity;
        return this.jpaQueryFactory.selectFrom(authorityEntity)
                .where(authorityEntity.authority.eq(authority))
                .fetchOne();
    }

    public List<UserAuthorityEntity> queryUserAuthoritiesByUserId(String userId) {
        QUserAuthorityEntity userAuthorityEntity = QUserAuthorityEntity.userAuthorityEntity;
        return this.jpaQueryFactory.selectFrom(userAuthorityEntity)
                .where(userAuthorityEntity.userId.eq(userId))
                .fetch();
    }

    public UserAuthorityEntity queryUserAuthorityEntityByUserIdAndAuthority(String userId, String authority) {
        QUserAuthorityEntity userAuthorityEntity = QUserAuthorityEntity.userAuthorityEntity;
        return this.jpaQueryFactory.selectFrom(userAuthorityEntity)
                .where(userAuthorityEntity.userId.eq(userId).and(userAuthorityEntity.authority.eq(authority)))
                .fetchOne();
    }

    public void updatePassword(String userId, String newPassword) {
        QUserEntity userEntity = QUserEntity.userEntity;
        this.jpaQueryFactory.update(userEntity)
                .set(userEntity.password, newPassword)
                .set(userEntity.modifiedDate, new Date())
                .where(userEntity.userId.eq(userId))
                .execute();
    }

    public void deleteUserAuthorityByUserId(String userId) {
        QUserAuthorityEntity userAuthorityEntity = QUserAuthorityEntity.userAuthorityEntity;
        this.jpaQueryFactory.delete(userAuthorityEntity)
                .where(userAuthorityEntity.userId.eq(userId))
                .execute();
    }

    public void disableUserById(String userId) {
        QUserEntity userEntity = QUserEntity.userEntity;
        this.jpaQueryFactory.update(userEntity)
                .set(userEntity.enabled, false)
                .set(userEntity.modifiedDate, new Date())
                .execute();
    }
}
