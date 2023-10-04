package com.algotrader.handler;

import com.algotrader.dto.BaseErrorDetailsDto;
import com.algotrader.dto.BaseErrorResponseDto;
import com.algotrader.exception.InvalidCredentialsException;
import com.algotrader.exception.NoCredentialsException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.List;

@ControllerAdvice
public class TraderExceptionHandler {

    @ExceptionHandler(NoCredentialsException.class)
    protected ResponseEntity<BaseErrorResponseDto> handleNoCredentialsException(NoCredentialsException e) {
        var details = BaseErrorDetailsDto.builder().name("reason").reason("no-credentials").build();
        var responseDto = buildBaseErrorResponseDto("VALIDATION_ERROR", "title", List.of(details));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_JSON)
                .body(responseDto);
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    protected ResponseEntity<BaseErrorResponseDto> handleInvalidCredentials(InvalidCredentialsException e) {
        var details = BaseErrorDetailsDto.builder().name("reason").reason("bad-credentials").build();
        var responseDto = buildBaseErrorResponseDto("VALIDATION_ERROR", "title", List.of(details));
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .contentType(MediaType.APPLICATION_JSON)
                .body(responseDto);
    }

    private BaseErrorResponseDto buildBaseErrorResponseDto(String type,
                                                           String title,
                                                           List<BaseErrorDetailsDto> details) {
        return BaseErrorResponseDto.builder()
                .type(type)
                .title(title)
                .details(details)
                .build();
    }
}
