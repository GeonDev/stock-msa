package com.stock.corp.client;

import lombok.Data;

@Data
public class DartCompanyResponse {
    private String status;
    private String message;
    private String corp_code;
    private String corp_name;
    private String stock_code;
    private String induty_code;
}
