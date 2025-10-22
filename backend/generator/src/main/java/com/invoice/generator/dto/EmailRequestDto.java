package com.invoice.generator.dto;

import lombok.Data;

@Data
public class EmailRequestDto {
    private String to;
    private String customMessage;
}