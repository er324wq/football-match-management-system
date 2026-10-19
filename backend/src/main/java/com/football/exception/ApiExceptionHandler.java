package com.football.exception;

import com.football.vo.ResultVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.http.converter.HttpMessageNotReadableException;

import javax.servlet.http.HttpServletRequest;
import javax.validation.ConstraintViolationException;
import java.util.Objects;

@RestControllerAdvice
public class ApiExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler({BindException.class, ConstraintViolationException.class,
            MissingServletRequestParameterException.class, MethodArgumentTypeMismatchException.class,
            HttpMessageNotReadableException.class, IllegalArgumentException.class})
    public ResponseEntity<ResultVO<Void>> badRequest(Exception exception) {
        return response(HttpStatus.BAD_REQUEST, "请求参数或业务状态无效");
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ResultVO<Void>> notFound(ResourceNotFoundException exception) {
        return response(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ResultVO<Void>> conflict(DataIntegrityViolationException exception) {
        return response(HttpStatus.CONFLICT, "数据冲突或违反完整性约束");
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ResultVO<Void>> forbidden(AccessDeniedException exception) {
        return response(HttpStatus.FORBIDDEN, "无权访问该接口");
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ResultVO<Void>> unauthorized(AuthenticationException exception) {
        return response(HttpStatus.UNAUTHORIZED, "请先登录");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ResultVO<Void>> methodNotAllowed(HttpRequestMethodNotSupportedException exception) {
        return response(HttpStatus.METHOD_NOT_ALLOWED, "不支持该请求方法");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ResultVO<Void>> unexpected(Exception exception, HttpServletRequest request) {
        // Log server-side details without recording request bodies, credentials, or authorization headers.
        log.error("Unhandled API exception method={} path={} type={}",
                request.getMethod(), request.getRequestURI(), exception.getClass().getName(), exception);
        return response(HttpStatus.INTERNAL_SERVER_ERROR, "服务器内部错误");
    }

    private ResponseEntity<ResultVO<Void>> response(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(ResultVO.error(status.value(), Objects.requireNonNull(message)));
    }
}
