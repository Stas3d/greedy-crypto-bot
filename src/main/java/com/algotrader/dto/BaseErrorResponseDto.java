package com.algotrader.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class BaseErrorResponseDto {

    private String type;
    private String title;
    private List<BaseErrorDetailsDto> details;
}
