package com.example.filing.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

public class JwtTokenUtilTest {

    private JwtTokenUtil jwtTokenUtil;
    private static final String SECRET_KEY = "testsecretkeytestsecretkeytestsecretkeytestsecretkey";
    private static final Long EXPIRATION = 3600000L; // 1 hour

    @BeforeEach
    public void setup() {
        jwtTokenUtil = new JwtTokenUtil();
        ReflectionTestUtils.setField(jwtTokenUtil, "secret", SECRET_KEY);
        ReflectionTestUtils.setField(jwtTokenUtil, "expiration", EXPIRATION);
    }

    @Test
    public void testGenerateToken_FromUserDetails() {
        // Arrange
        UserDetails userDetails = new User("testuser", "password", Collections.emptyList());

        // Act
        String token = jwtTokenUtil.generateToken(userDetails);

        // Assert
        assertNotNull(token);
        assertEquals("testuser", jwtTokenUtil.extractUsername(token));
    }

    @Test
    public void testGenerateToken_WithRole() {
        // Arrange
        String username = "admin";
        int role = 1;

        // Act
        String token = jwtTokenUtil.generateToken(username, role);

        // Assert
        assertNotNull(token);
        assertEquals("admin", jwtTokenUtil.extractUsername(token));
        assertEquals(Integer.valueOf(1), jwtTokenUtil.extractRole(token));
    }

    @Test
    public void testValidateToken_ValidToken() {
        // Arrange
        UserDetails userDetails = new User("testuser", "password", Collections.emptyList());
        String token = jwtTokenUtil.generateToken(userDetails);

        // Act
        boolean isValid = jwtTokenUtil.validateToken(token, userDetails);

        // Assert
        assertTrue(isValid);
    }

    @Test
    public void testValidateToken_ExpiredToken() {
        // Arrange
        UserDetails userDetails = new User("testuser", "password", Collections.emptyList());
        JwtTokenUtil jwtUtil = new JwtTokenUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", SECRET_KEY);
        ReflectionTestUtils.setField(jwtUtil, "expiration", -10000L); // Expired token

        String token = jwtUtil.generateToken(userDetails);

        // Act
        boolean isValid = jwtTokenUtil.validateToken(token, userDetails);

        // Assert
        assertFalse(isValid);
    }

    @Test
    public void testValidateToken_WrongUser() {
        // Arrange
        UserDetails realUser = new User("realuser", "password", Collections.emptyList());
        UserDetails wrongUser = new User("wronguser", "password", Collections.emptyList());

        String token = jwtTokenUtil.generateToken(realUser);

        // Act
        boolean isValid = jwtTokenUtil.validateToken(token, wrongUser);

        // Assert
        assertFalse(isValid);
    }

    @Test
    public void testExtractAllClaims() {
        // Arrange
        String username = "testuser";
        int role = 2;
        String token = jwtTokenUtil.generateToken(username, role);

        // Act & Assert
        assertEquals(username, jwtTokenUtil.extractUsername(token));
        assertEquals(Integer.valueOf(role), jwtTokenUtil.extractRole(token));
        assertNotNull(jwtTokenUtil.extractExpiration(token));
    }
}