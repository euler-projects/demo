package org.eulerframework.uc.service;

import org.eulerframework.data.util.EntityUtils;
import org.eulerframework.security.core.EulerUser;
import org.eulerframework.security.core.EulerUserService;
import org.eulerframework.security.core.userdetails.EulerUserDetails;
import org.eulerframework.security.exception.EulerAuthorityNotFountException;
import org.eulerframework.security.exception.EulerUserNotFountException;
import org.eulerframework.uc.dao.UserDao;
import org.eulerframework.uc.entity.AuthorityEntity;
import org.eulerframework.uc.entity.UserAuthorityEntity;
import org.eulerframework.uc.entity.UserEntity;
import org.eulerframework.uc.model.Authority;
import org.eulerframework.uc.model.User;
import org.eulerframework.uc.repository.UserAuthorityRepository;
import org.eulerframework.uc.repository.UserRepository;
import org.eulerframework.uc.util.UserCenterModelUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class UserService implements EulerUserService {
    private final Logger logger = LoggerFactory.getLogger(UserService.class);

    private UserDao userDao;
    private UserRepository userRepository;
    private UserAuthorityRepository userAuthorityRepository;

    @Override
    public User loadUserById(String userId) {
        this.logger.debug("loadUserById: {}", userId);
        UserEntity userEntity = this.userDao.queryUserEntityByUserId(userId);
        if (userEntity == null) {
            return null;
        }
        List<AuthorityEntity> authorityEntities = this.userDao.queryUserAuthorityByUserId(userEntity.getUserId());
        return UserCenterModelUtils.toUser(userEntity, authorityEntities);
    }

    @Override
    public User loadUserByUsername(String username) {
        this.logger.debug("loadUserByUsername: {}", username);
        UserEntity userEntity = this.userDao.queryUserEntityByUsername(username);
        if (userEntity == null) {
            return null;
        }
        List<AuthorityEntity> authorityEntities = this.userDao.queryUserAuthorityByUserId(userEntity.getUserId());
        return UserCenterModelUtils.toUser(userEntity, authorityEntities);
    }

    @Override
    public List<EulerUser> listUsers(int offset, int limit) {
        List<UserEntity> userEntities = this.userDao.listUserEntities(offset, limit);
        return userEntities.stream()
                .map(userEntity -> {
                    List<AuthorityEntity> authorityEntities = this.userDao.queryUserAuthorityByUserId(userEntity.getUserId());
                    return UserCenterModelUtils.toUser(userEntity, authorityEntities);
                })
                .collect(Collectors.toList());
    }

    public User loadUserByEmail(String email) {
        this.logger.debug("loadUserByEmail: {}", email);
        UserEntity userEntity = this.userDao.queryUserEntityByEmail(email);
        if (userEntity == null) {
            return null;
        }
        List<AuthorityEntity> authorityEntities = this.userDao.queryUserAuthorityByUserId(userEntity.getUserId());
        return UserCenterModelUtils.toUser(userEntity, authorityEntities);
    }

    public User loadUserByPhone(String phone) {
        UserEntity userEntity = this.userDao.queryUserEntityByPhone(phone);
        if (userEntity == null) {
            return null;
        }
        List<AuthorityEntity> authorityEntities = this.userDao.queryUserAuthorityByUserId(userEntity.getUserId());
        return UserCenterModelUtils.toUser(userEntity, authorityEntities);
    }

    @Override
    @Transactional
    public User createUser(EulerUserDetails userDetails) {
        Assert.isNull(userDetails.getUserId(), "userId must be null");
        User user = new User();
        user.reloadUserDetails(userDetails);
        return this.createUser(user);
    }

    @Override
    @Transactional
    public User createUser(EulerUser eulerUser) {
        User user = this.castEulerUser(eulerUser);
        user.setUserId(UUID.randomUUID().toString());
        UserEntity userEntity = UserCenterModelUtils.toUserEntity(user);
        List<UserAuthorityEntity> userAuthorityEntities = this.getUserAuthorityEntities(user);

        this.userRepository.save(userEntity);

        if (!CollectionUtils.isEmpty(userAuthorityEntities)) {
            this.userAuthorityRepository.saveAll(userAuthorityEntities);
        }

        return this.loadUserById(user.getUserId());
    }

    @Override
    @Transactional
    public void updateUser(EulerUser eulerUser) {
        User user = this.castEulerUser(eulerUser);
        Assert.notNull(user.getUserId(), "userId must not be null");
        UserEntity userEntity = this.userDao.queryUserEntityByUserId(user.getUserId());
        if (userEntity == null) {
            throw new EulerUserNotFountException("User not found, userId: " + user.getUserId());
        }

        // username cannot be updated
        // password cannot be updated
        EntityUtils.updateNullableField(user.getEmail(), userEntity::setEmail);
        EntityUtils.updateNullableField(user.getPhone(), userEntity::setPhone);

        userEntity.setAccountNonExpired(user.isAccountNonExpired());
        userEntity.setAccountNonLocked(user.isAccountNonLocked());
        userEntity.setCredentialsNonExpired(user.isCredentialsNonExpired());
        userEntity.setEnabled(user.isEnabled());

        if (user.getAuthorities() != null) {
            List<UserAuthorityEntity> newUserAuthorityEntities = this.getUserAuthorityEntities(user);
            Set<String> newAuthorities = newUserAuthorityEntities.stream().map(UserAuthorityEntity::getAuthority).collect(Collectors.toSet());

            List<UserAuthorityEntity> existsUserAuthorityEntities = this.userDao.queryUserAuthoritiesByUserId(user.getUserId());
            Set<String> existsAuthorities = existsUserAuthorityEntities.stream().map(UserAuthorityEntity::getAuthority).collect(Collectors.toSet());

            List<UserAuthorityEntity> willDelete = new ArrayList<>();
            List<UserAuthorityEntity> willAdd = new ArrayList<>();

            for (UserAuthorityEntity userAuthorityEntity : existsUserAuthorityEntities) {
                if (!newAuthorities.contains(userAuthorityEntity.getAuthority())) {
                    willDelete.add(userAuthorityEntity);
                }
            }

            for (UserAuthorityEntity userAuthorityEntity : newUserAuthorityEntities) {
                if (!existsAuthorities.contains(userAuthorityEntity.getAuthority())) {
                    willAdd.add(userAuthorityEntity);
                }
            }


            if (!CollectionUtils.isEmpty(willDelete)) {
                this.userAuthorityRepository.deleteAllByIdInBatch(willDelete.stream().map(UserAuthorityEntity::getId).collect(Collectors.toSet()));
            }
            if (!CollectionUtils.isEmpty(willAdd)) {
                this.userAuthorityRepository.saveAll(willAdd);
            }
        }

        this.userRepository.save(userEntity);
    }

    @Override
    @Transactional
    public void deleteUser(String userId) {
        this.userDao.deleteUserAuthorityByUserId(userId);
        this.userRepository.deleteById(userId);
    }

    @Override
    @Transactional
    public void updatePassword(String userId, String newPassword) {
        this.userDao.updatePassword(userId, newPassword);
    }

    @Autowired
    public void setUserDao(UserDao userDao) {
        this.userDao = userDao;
    }

    @Autowired
    public void setUserRepository(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Autowired
    public void setUserAuthorityRepository(UserAuthorityRepository userAuthorityRepository) {
        this.userAuthorityRepository = userAuthorityRepository;
    }

    private User castEulerUser(EulerUser eulerUser) {
        Assert.notNull(eulerUser, "eulerUser must not be null");
        Assert.isInstanceOf(User.class, eulerUser, "userDetails must be an instance of DefaultEulerUserDetails");
        return (User) eulerUser;
    }

    private List<UserAuthorityEntity> getUserAuthorityEntities(User user) {
        return Optional.ofNullable(user.getAuthorities()).orElse(Collections.emptyList())
                .stream()
                .map(Authority::getAuthority)
                .map(authority -> {
                    AuthorityEntity authorityEntity = this.userDao.queryAuthorityEntity(authority);
                    if (authorityEntity == null) {
                        throw new EulerAuthorityNotFountException("Authority '" + authority + "' not found");
                    }
                    UserAuthorityEntity userAuthorityEntity = new UserAuthorityEntity();
                    userAuthorityEntity.setUserId(user.getUserId());
                    userAuthorityEntity.setAuthority(authority);
                    return userAuthorityEntity;
                })
                .toList();
    }
}
