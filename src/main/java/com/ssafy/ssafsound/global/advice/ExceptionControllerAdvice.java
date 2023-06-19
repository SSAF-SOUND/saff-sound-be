package com.ssafy.ssafsound.global.advice;


import com.ssafy.ssafsound.domain.auth.exception.AuthException;
import com.ssafy.ssafsound.domain.lunch.exception.LunchException;
import com.ssafy.ssafsound.domain.member.exception.MemberException;
import com.ssafy.ssafsound.global.common.exception.GlobalErrorInfo;
import com.ssafy.ssafsound.global.common.exception.ResourceNotFoundException;
import com.ssafy.ssafsound.global.common.response.EnvelopeResponse;
import com.ssafy.ssafsound.infra.exception.InfraException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class ExceptionControllerAdvice {

    @ExceptionHandler(MemberException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public EnvelopeResponse MemberExceptionHandler(MemberException e) {
        log.error(e.getMessage());
        return EnvelopeResponse.builder()
                .code(e.getInfo().getCode())
                .message(e.getInfo().getMessage())
                .build();
    }

    @ExceptionHandler(AuthException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public EnvelopeResponse AuthExceptionHandler(AuthException e) {
        log.error(e.getMessage());

        return EnvelopeResponse.builder()
                .code(e.getInfo().getCode())
                .message(e.getInfo().getMessage())
                .build();
    }
  
    @ExceptionHandler(LunchException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public EnvelopeResponse LunchExceptionHandler(LunchException e) {
        log.error(e.getMessage());

        return EnvelopeResponse.builder()
                .code(e.getInfo().getCode())
                .message((e.getInfo().getMessage()))
                .build();
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public EnvelopeResponse BadRequestExceptionHandler(MethodArgumentNotValidException e) {
        log.error(e.getMessage());

        StringBuilder errorMessage = new StringBuilder();
        e.getBindingResult().getAllErrors().forEach((error) -> {
            errorMessage.append(error.getDefaultMessage());
        });

        return EnvelopeResponse.builder()
                .code(String.valueOf(HttpStatus.BAD_REQUEST.value()))
                .message(errorMessage.toString())
                .build();
    }

    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public EnvelopeResponse RuntimeExceptionHandler(RuntimeException e) {
        log.error(e.getMessage());

        return EnvelopeResponse.builder()
                .code(GlobalErrorInfo.INTERNAL_SERVER_ERROR.getCode())
                .message(GlobalErrorInfo.INTERNAL_SERVER_ERROR.getMessage())
                .build();
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public EnvelopeResponse ResourceNotFoundExceptionHandler(ResourceNotFoundException e) {
        log.error(e.getMessage());

        return EnvelopeResponse.builder()
                .code(e.getInfo().getCode())
                .message(e.getInfo().getMessage())
                .build();
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public EnvelopeResponse DataIntegrityViolationExceptionHandler(DataIntegrityViolationException e) {
        log.error(e.getMessage());

        return EnvelopeResponse.builder()
                .code(GlobalErrorInfo.NOT_FOUND.getCode())
                .message(GlobalErrorInfo.NOT_FOUND.getMessage())
                .build();
    }

    @ExceptionHandler(InfraException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public EnvelopeResponse InfraExceptionHandler(InfraException e){
        log.error(e.getMessage());

        return EnvelopeResponse.builder()
                .code(e.getInfo().getCode())
                .message(e.getInfo().getMessage())
                .build();
    }
}