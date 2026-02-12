package com.stock.finance.mapper;

import com.stock.common.dto.CorpFinanceIndicatorDto;
import com.stock.finance.entity.CorpFinanceIndicator;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CorpFinanceIndicatorMapper {
    CorpFinanceIndicatorDto toDto(CorpFinanceIndicator entity);
    List<CorpFinanceIndicatorDto> toDtoList(List<CorpFinanceIndicator> entities);
}
