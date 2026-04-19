package org.eulerframework.uc.service;

import org.eulerframework.common.util.StringUtils;
import org.eulerframework.security.authentication.device.DeviceAttestationUser;
import org.eulerframework.security.core.userdetails.EulerDeviceAttestationUserDetailsService;
import org.eulerframework.security.core.userdetails.EulerUserDetails;
import org.eulerframework.security.core.userdetails.UserDetailsNotFountException;
import org.eulerframework.security.util.UserDetailsUtils;
import org.eulerframework.uc.entity.DeviceAttestationUserMappingEntity;
import org.eulerframework.uc.model.User;
import org.eulerframework.uc.repository.DeviceAttestationUserMappingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DeviceAttestationUserDetailsService implements EulerDeviceAttestationUserDetailsService {

    private DeviceAttestationUserMappingRepository deviceAttestationUserMappingRepository;

    private UserService userService;

    @Override
    public EulerUserDetails loadUserByAppleAppAttestUser(DeviceAttestationUser attestUser) {
        return this.deviceAttestationUserMappingRepository.findByKeyId(attestUser.getKeyId())
                .map(DeviceAttestationUserMappingEntity::getUserId)
                .map(userId -> this.userService.loadUserById(userId))
                .map(UserDetailsUtils::toEulerUserDetails)
                .orElseThrow(() -> new UserDetailsNotFountException(attestUser.getKeyId()));
    }

    @Override
    public EulerUserDetails createUser(DeviceAttestationUser attestUser) {
        DeviceAttestationUserMappingEntity mappingEntity = new DeviceAttestationUserMappingEntity();
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
        this.deviceAttestationUserMappingRepository.save(mappingEntity);
        return UserDetailsUtils.toEulerUserDetails(createdUser);
    }

    @Autowired
    public void setAppleAppAttestUserMappingRepository(DeviceAttestationUserMappingRepository deviceAttestationUserMappingRepository) {
        this.deviceAttestationUserMappingRepository = deviceAttestationUserMappingRepository;
    }

    @Autowired
    public void setUserService(UserService userService) {
        this.userService = userService;
    }
}
