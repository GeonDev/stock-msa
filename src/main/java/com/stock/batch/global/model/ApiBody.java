package com.stock.batch.global.model;


import lombok.Data;

import java.util.List;

@Data
public class ApiBody<T> {
    Integer numOfRows;
    Integer pageNo;
    Integer totalCount;
    List<T> itemList;
}
