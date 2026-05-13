package org.eulerframework.uc.service;

import org.eulerframework.common.util.StringUtils;
import org.eulerframework.security.authentication.appattest.AppAttestUser;
import org.eulerframework.security.core.userdetails.EulerDeviceUserDetailsService;
import org.eulerframework.security.core.userdetails.EulerUserDetails;
import org.eulerframework.security.core.userdetails.UserDetailsNotFoundException;
import org.eulerframework.security.util.UserDetailsUtils;
import org.eulerframework.uc.entity.AppAttestAttestationUserMappingEntity;
import org.eulerframework.uc.model.User;
import org.eulerframework.uc.repository.DeviceUserMappingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DeviceUserDetailsService implements EulerDeviceUserDetailsService {

    private DeviceUserMappingRepository deviceUserMappingRepository;

    private UserService userService;

    @Override
    public EulerUserDetails loadUserByDeviceUser(AppAttestUser appAttestUser) {
        return this.deviceUserMappingRepository.findByKeyId(appAttestUser.getKeyId())
                .map(AppAttestAttestationUserMappingEntity::getUserId)
                .map(userId -> this.userService.loadUserById(userId))
                .map(UserDetailsUtils::toEulerUserDetails)
                .orElseThrow(() -> new UserDetailsNotFoundException(appAttestUser.getKeyId()));
    }

    @Override
    public EulerUserDetails createUser(AppAttestUser appAttestUser) {
        AppAttestAttestationUserMappingEntity mappingEntity = new AppAttestAttestationUserMappingEntity();
        mappingEntity.setKeyId(appAttestUser.getKeyId());
        mappingEntity.setTeamId(appAttestUser.getTeamId());
        mappingEntity.setBundleId(appAttestUser.getBundleId());

        EulerUserDetails userDetails = EulerUserDetails.builder()
                .username("attest_" + appAttestUser.getKeyId().substring(0, Math.min(8, appAttestUser.getKeyId().length())))
                .password("{noop}" + StringUtils.randomString(32))
                .authorities("user")
                .build();
        User userCreation = new User();
        userCreation.reloadUserDetails(userDetails);
        User createdUser = this.userService.createUser(userCreation);

        mappingEntity.setUserId(createdUser.getUserId());
        this.deviceUserMappingRepository.save(mappingEntity);
        return UserDetailsUtils.toEulerUserDetails(createdUser);
    }

    @Autowired
    public void setAppleAppAttestUserMappingRepository(DeviceUserMappingRepository deviceUserMappingRepository) {
        this.deviceUserMappingRepository = deviceUserMappingRepository;
    }

    @Autowired
    public void setUserService(UserService userService) {
        this.userService = userService;
    }
}
