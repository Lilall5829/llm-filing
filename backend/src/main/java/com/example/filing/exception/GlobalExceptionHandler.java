package com.example.filing.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import com.example.filing.util.Result;

@ControllerAdvice
public class GlobalExceptionHandler {

        private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

        @ExceptionHandler(UsernameNotFoundException.class)
        public ResponseEntity<Result<String>> handleUsernameNotFoundException(
                        UsernameNotFoundException ex, WebRequest request) {
                logger.error("用户不存在异常: {}", ex.getMessage(), ex);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                .body(Result.failed(HttpStatus.NOT_FOUND.value(), ex.getMessage()));
        }

        @ExceptionHandler(BadCredentialsException.class)
        public ResponseEntity<Result<String>> handleBadCredentialsException(
                        BadCredentialsException ex, WebRequest request) {
                logger.error("凭证异常: {}", ex.getMessage(), ex);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                .body(Result.failed(HttpStatus.UNAUTHORIZED.value(), ex.getMessage()));
        }

        @ExceptionHandler(BusinessException.class)
        public ResponseEntity<Result<String>> handleBusinessException(
                        BusinessException ex, WebRequest request) {
                logger.error("业务异常: {}", ex.getMessage(), ex);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body(Result.failed(HttpStatus.INTERNAL_SERVER_ERROR.value(), ex.getMessage()));
        }

        @ExceptionHandler(AccessDeniedException.class)
        public ResponseEntity<Result<String>> handleAccessDeniedException(
                        AccessDeniedException ex, WebRequest request) {
                logger.error("访问拒绝异常: {}", ex.getMessage(), ex);
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                .body(Result.failed(HttpStatus.FORBIDDEN.value(), "Access Denied"));
        }

        @ExceptionHandler(Exception.class)
        public ResponseEntity<Result<String>> handleGlobalException(
                        Exception ex, WebRequest request) {
                logger.error("全局未处理异常: {}", ex.getMessage(), ex);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body(Result.failed(HttpStatus.INTERNAL_SERVER_ERROR.value(), ex.getMessage()));
        }
}