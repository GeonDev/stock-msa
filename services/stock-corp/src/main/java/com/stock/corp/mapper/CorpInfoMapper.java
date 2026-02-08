package com.stock.corp.mapper;

import com.stock.common.dto.CorpInfoDto;
import com.stock.corp.entity.CorpInfo;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface CorpInfoMapper {
    
    CorpInfoDto toDto(CorpInfo entity);
    
    List<CorpInfoDto> toDtoList(List<CorpInfo> entities);
}
