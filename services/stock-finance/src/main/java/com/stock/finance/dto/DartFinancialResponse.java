package com.stock.finance.dto;

import lombok.Data;
import java.util.List;

@Data
public class DartFinancialResponse {
    private String status;
    private String message;
    private List<DartAccount> list;
}
