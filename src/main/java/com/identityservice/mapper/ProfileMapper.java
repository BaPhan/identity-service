package com.identityservice.mapper;

import com.identityservice.dto.request.RegistrationRequest;
import com.identityservice.dto.response.ProfileResponse;
import com.identityservice.entity.Profile;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;


@Component
public class ProfileMapper {
    public Profile toProfile(RegistrationRequest request){
        Profile profile = new Profile();
        BeanUtils.copyProperties(request,profile);
        return profile;
    }
    public ProfileResponse toProfileResponse(Profile profile){
        ProfileResponse profileResponse = new ProfileResponse();
        BeanUtils.copyProperties(profile,profileResponse);
        return profileResponse;
    }
}
