package com.stock.finance.converter;

import com.stock.common.enums.ReportCode;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * ReportCode Enum을 DB VARCHAR로 저장하기 위한 Converter
 * DB에는 "11011", "11012" 등의 코드 값으로 저장됨
 */
@Converter(autoApply = true)
public class ReportCodeConverter implements AttributeConverter<ReportCode, String> {

    @Override
    public String convertToDatabaseColumn(ReportCode attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.getCode();
    }

    @Override
    public ReportCode convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty()) {
            return null;
        }
        return ReportCode.fromCode(dbData);
    }
}
