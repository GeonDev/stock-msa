package com.stock.finance.dto;

import lombok.Data;
import java.util.List;

@Data
public class DartListResponse {
    private String status;
    private String message;
    private Integer page_no;
    private Integer page_count;
    private Integer total_count;
    private Integer total_page;
    private List<DartDisclosure> list;

    public record DartDisclosure(
        String corp_code,
        String corp_name,
        String stock_code,
        String corp_cls,
        String report_nm,
        String rcept_no,
        String flr_nm,
        String rcept_dt,
        String rmk
    ) {}
}
