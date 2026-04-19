package org.eulerframework.uc.service;

import org.eulerframework.common.util.StringUtils;
import org.eulerframework.security.authentication.appattest.DeviceAppUser;
import org.eulerframework.security.core.userdetails.EulerDeviceUserDetailsService;
import org.eulerframework.security.core.userdetails.EulerUserDetails;
import org.eulerframework.security.core.userdetails.UserDetailsNotFountException;
import org.eulerframework.security.util.UserDetailsUtils;
import org.eulerframework.uc.entity.DeviceUserMappingEntity;
import org.eulerframework.uc.model.User;
import org.eulerframework.uc.repository.DeviceUserMappingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DeviceUserDetailsService implements EulerDeviceUserDetailsService {

    private DeviceUserMappingRepository deviceUserMappingRepository;

    private UserService userService;

    @Override
    public EulerUserDetails loadUserByDeviceUser(DeviceAppUser deviceAppUser) {
        return this.deviceUserMappingRepository.findByKeyId(deviceAppUser.getKeyId())
                .map(DeviceUserMappingEntity::getUserId)
                .map(userId -> this.userService.loadUserById(userId))
                .map(UserDetailsUtils::toEulerUserDetails)
                .orElseThrow(() -> new UserDetailsNotFountException(deviceAppUser.getKeyId()));
    }

    @Override
    public EulerUserDetails createUser(DeviceAppUser deviceAppUser) {
        DeviceUserMappingEntity mappingEntity = new DeviceUserMappingEntity();
        mappingEntity.setKeyId(deviceAppUser.getKeyId());
        mappingEntity.setTeamId(deviceAppUser.getTeamId());
        mappingEntity.setBundleId(deviceAppUser.getBundleId());

        EulerUserDetails userDetails = EulerUserDetails.builder()
                .username("attest_" + deviceAppUser.getKeyId().substring(0, Math.min(8, deviceAppUser.getKeyId().length())))
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
