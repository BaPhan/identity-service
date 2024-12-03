package com.identityservice.configuration;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CustomAuthoritiesConverter implements Converter<Jwt, Collection<GrantedAuthority>> {
    private final String REALM_ACCESS = "realm_access";
    private final String ROLE_PREFIX = "ROLE_";
    private final String ROLES = "roles";
    @Override
    public Collection<GrantedAuthority> convert(Jwt source) {
        Map<String, Object> claims = source.getClaimAsMap(REALM_ACCESS);
        Object roles = claims.get(ROLES);

        if (roles instanceof List stringRoles){
            return (List<GrantedAuthority>) stringRoles
                    .stream()
                    .map(role -> new SimpleGrantedAuthority(String.format("%s%s", ROLE_PREFIX,role))).collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}
