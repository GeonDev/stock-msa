package com.stock.price.mapper;

import com.stock.common.dto.StockIndicatorDto;
import com.stock.price.entity.StockIndicator;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface StockIndicatorMapper {
    @org.mapstruct.Mapping(source = "stockPrice.stockCode", target = "stockCode")
    StockIndicatorDto toDto(StockIndicator entity);
    List<StockIndicatorDto> toDtoList(List<StockIndicator> entities);
}
