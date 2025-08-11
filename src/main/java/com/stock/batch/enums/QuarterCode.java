package com.stock.batch.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum QuarterCode {

    Q1("11013"),
    Q2("11012"),
    Q3("11014"),
    Q4("11011");

    private final String code;


    public String getBefore(){
        return switch (this.code) {
            case "11013" -> "11011";
            case "11012" -> "11013";
            case "11014" -> "11012";
            case "11011" -> "11014";
            default -> "";
        };
    }

}
