package org.eulerframework.uc.service;

import org.eulerframework.common.util.StringUtils;
import org.eulerframework.security.authentication.WechatUser;
import org.eulerframework.security.core.userdetails.EulerUserDetails;
import org.eulerframework.security.core.userdetails.EulerWechatUserDetailsService;
import org.eulerframework.security.core.userdetails.UserDetailsNotFountException;
import org.eulerframework.security.util.UserDetailsUtils;
import org.eulerframework.uc.entity.WechatUserMappingEntity;
import org.eulerframework.uc.model.User;
import org.eulerframework.uc.repository.WechatUserMappingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class WechatUserDetailsService implements EulerWechatUserDetailsService {

    private WechatUserMappingRepository wechatUserMappingRepository;

    private UserService userService;

    @Override
    public EulerUserDetails loadUserByWechatUser(WechatUser wechatUser) {
        return this.wechatUserMappingRepository.findByOpenId(wechatUser.getOpenId())
                .map(WechatUserMappingEntity::getUserId)
                .map(userId -> this.userService.loadUserById(userId))
                .map(UserDetailsUtils::toEulerUserDetails)
                .orElseThrow(() -> new UserDetailsNotFountException(wechatUser.getOpenId()));
    }

    @Override
    public EulerUserDetails createUser(WechatUser wechatUser) {
        WechatUserMappingEntity wechatUserMappingEntity = new WechatUserMappingEntity();
        wechatUserMappingEntity.setOpenId(wechatUser.getOpenId());
        wechatUserMappingEntity.setUnionId(wechatUser.getUnionId());
        wechatUserMappingEntity.setNickName(wechatUser.getNickName());
        wechatUserMappingEntity.setGender(wechatUser.getGender());
        wechatUserMappingEntity.setCity(wechatUser.getCity());
        wechatUserMappingEntity.setProvince(wechatUser.getProvince());
        wechatUserMappingEntity.setCountry(wechatUser.getCountry());
        wechatUserMappingEntity.setAvatarUrl(wechatUser.getAvatarUrl());

        EulerUserDetails userDetails = EulerUserDetails.builder()
                .username("wx_" + wechatUser.getOpenId())
                .password("{noop}" + StringUtils.randomString(32))
                .authorities("user")
                .build();
        User userCreation = new User();
        userCreation.reloadUserDetails(userDetails);
        User createdUser = this.userService.createUser(userCreation);

        wechatUserMappingEntity.setUserId(createdUser.getUserId());
        this.wechatUserMappingRepository.save(wechatUserMappingEntity);
        return UserDetailsUtils.toEulerUserDetails(createdUser);
    }

    @Autowired
    public void setWechatUserMappingRepository(WechatUserMappingRepository wechatUserMappingRepository) {
        this.wechatUserMappingRepository = wechatUserMappingRepository;
    }

    @Autowired
    public void setUserService(UserService userService) {
        this.userService = userService;
    }
}
