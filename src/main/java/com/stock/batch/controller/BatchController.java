package com.stock.batch.controller;


import com.stock.batch.consts.ApplicationConstants;
import com.stock.batch.service.StockApiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import javax.xml.parsers.ParserConfigurationException;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;

import static com.stock.batch.utils.CommonUtils.parseLocdatesFromXml;
import static com.stock.batch.utils.DateUtils.toLocalDateString;
import static com.stock.batch.utils.DateUtils.toStringLocalDate;


@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/batch")
public class BatchController {

    private final JobLauncher jobLauncher;
    private final JobRegistry jobRegistry;

    private final StockApiService stockApiService;

    @PostMapping("/price")
    public ResponseEntity StockPriceApi(@RequestParam(value = "date" , required = false) String date) throws Exception {

        if(StringUtils.hasText(date) ? stockApiService.checkIsDayOff(toStringLocalDate(date)) : stockApiService.checkIsDayOff(LocalDate.now())){

        }

        return ResponseEntity.ok("SET PRICE");
    }


}
