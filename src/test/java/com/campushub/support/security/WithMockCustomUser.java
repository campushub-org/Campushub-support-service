package com.campushub.support.security;

import org.springframework.security.test.context.support.WithSecurityContext;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
@WithSecurityContext(factory = WithMockCustomUserFactory.class) // Updated factory reference
public @interface WithMockCustomUser {
    long id() default 1L;
    String username() default "testuser";
    String[] authorities() default {"ROLE_USER"}; // Default authority
}