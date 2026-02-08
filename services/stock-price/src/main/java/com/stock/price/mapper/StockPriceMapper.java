package com.stock.price.mapper;

import com.stock.common.dto.StockPriceDto;
import com.stock.price.entity.StockPrice;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface StockPriceMapper {
    
    StockPriceDto toDto(StockPrice entity);
    
    List<StockPriceDto> toDtoList(List<StockPrice> entities);
}
