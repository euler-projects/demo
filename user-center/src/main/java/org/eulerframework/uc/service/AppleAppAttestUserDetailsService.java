package org.eulerframework.uc.service;

import org.eulerframework.common.util.StringUtils;
import org.eulerframework.security.authentication.apple.AppleAppAttestUser;
import org.eulerframework.security.core.userdetails.EulerAppleAppAttestUserDetailsService;
import org.eulerframework.security.core.userdetails.EulerUserDetails;
import org.eulerframework.security.core.userdetails.UserDetailsNotFountException;
import org.eulerframework.security.util.UserDetailsUtils;
import org.eulerframework.uc.entity.AppleAppAttestUserMappingEntity;
import org.eulerframework.uc.model.User;
import org.eulerframework.uc.repository.AppleAppAttestUserMappingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AppleAppAttestUserDetailsService implements EulerAppleAppAttestUserDetailsService {

    private AppleAppAttestUserMappingRepository appleAppAttestUserMappingRepository;

    private UserService userService;

    @Override
    public EulerUserDetails loadUserByAppleAppAttestUser(AppleAppAttestUser attestUser) {
        return this.appleAppAttestUserMappingRepository.findByKeyId(attestUser.getKeyId())
                .map(AppleAppAttestUserMappingEntity::getUserId)
                .map(userId -> this.userService.loadUserById(userId))
                .map(UserDetailsUtils::toEulerUserDetails)
                .orElseThrow(() -> new UserDetailsNotFountException(attestUser.getKeyId()));
    }

    @Override
    public EulerUserDetails createUser(AppleAppAttestUser attestUser) {
        AppleAppAttestUserMappingEntity mappingEntity = new AppleAppAttestUserMappingEntity();
        mappingEntity.setKeyId(attestUser.getKeyId());
        mappingEntity.setTeamId(attestUser.getTeamId());
        mappingEntity.setBundleId(attestUser.getBundleId());

        EulerUserDetails userDetails = EulerUserDetails.builder()
                .username("attest_" + attestUser.getKeyId().substring(0, Math.min(8, attestUser.getKeyId().length())))
                .password("{noop}" + StringUtils.randomString(32))
                .authorities("user")
                .build();
        User userCreation = new User();
        userCreation.reloadUserDetails(userDetails);
        User createdUser = this.userService.createUser(userCreation);

        mappingEntity.setUserId(createdUser.getUserId());
        this.appleAppAttestUserMappingRepository.save(mappingEntity);
        return UserDetailsUtils.toEulerUserDetails(createdUser);
    }

    @Autowired
    public void setAppleAppAttestUserMappingRepository(AppleAppAttestUserMappingRepository appleAppAttestUserMappingRepository) {
        this.appleAppAttestUserMappingRepository = appleAppAttestUserMappingRepository;
    }

    @Autowired
    public void setUserService(UserService userService) {
        this.userService = userService;
    }
}
