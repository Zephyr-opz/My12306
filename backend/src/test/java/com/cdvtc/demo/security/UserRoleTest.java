package com.cdvtc.demo.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UserRoleTest {

    @Test
    void mapsStoredRoleToSpringAuthority() {
        assertEquals(UserRole.ADMIN, UserRole.from("ADMIN"));
        assertEquals("ROLE_ADMIN", UserRole.ADMIN.authority());
        assertEquals(UserRole.USER, UserRole.from(null));
        assertEquals(UserRole.USER, UserRole.from("unknown"));
    }
}
