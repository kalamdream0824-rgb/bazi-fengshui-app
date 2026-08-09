package com.bazi.app.controller;

import com.bazi.app.domain.BaziRecord;
import com.bazi.app.dto.PaipanRequest;
import com.bazi.app.dto.PaipanResultDto;
import com.bazi.app.dto.RecordDto;
import com.bazi.app.mapper.BaziRecordMapper;
import com.bazi.app.service.BaziService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/records")
public class BaziRecordController {

  private final BaziService service;
  private final BaziRecordMapper mapper;
  private final ObjectMapper objectMapper;

  public BaziRecordController(BaziService service, BaziRecordMapper mapper, ObjectMapper objectMapper) {
    this.service = service;
    this.mapper = mapper;
    this.objectMapper = objectMapper;
  }

  @PostMapping
  public PaipanResultDto create(@Valid @RequestBody PaipanRequest request) throws Exception {
    PaipanResultDto result = service.paipan(request);
    BaziRecord record = new BaziRecord();
    record.setRequestJson(objectMapper.writeValueAsString(request));
    record.setResultJson(objectMapper.writeValueAsString(result));
    record.setCreatedAt(LocalDateTime.now());
    mapper.insert(record);
    return result;
  }

  @GetMapping
  public List<RecordDto> list() throws Exception {
    List<BaziRecord> records = mapper.selectList(null);
    List<RecordDto> dtos = new ArrayList<>();
    for (BaziRecord record : records) {
      dtos.add(toDto(record));
    }
    return dtos;
  }

  @GetMapping("/{id}")
  public ResponseEntity<RecordDto> get(@PathVariable Long id) throws Exception {
    BaziRecord record = mapper.selectById(id);
    if (record == null) {
      return ResponseEntity.notFound().build();
    }
    return ResponseEntity.ok(toDto(record));
  }

  private RecordDto toDto(BaziRecord record) throws Exception {
    return new RecordDto(
        record.getId(),
        objectMapper.readValue(record.getRequestJson(), PaipanRequest.class),
        objectMapper.readValue(record.getResultJson(), PaipanResultDto.class),
        record.getCreatedAt());
  }
}
