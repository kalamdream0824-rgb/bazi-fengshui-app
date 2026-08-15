package com.bazi.app.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.bazi.app.domain.BaziRecord;
import com.bazi.app.dto.PaipanRequest;
import com.bazi.app.dto.PaipanResultDto;
import com.bazi.app.dto.RecordDto;
import com.bazi.app.mapper.BaziRecordMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class RecordService {

  private final BaziService baziService;
  private final BaziRecordMapper mapper;
  private final ObjectMapper objectMapper;

  public RecordService(BaziService baziService, BaziRecordMapper mapper, ObjectMapper objectMapper) {
    this.baziService = baziService;
    this.mapper = mapper;
    this.objectMapper = objectMapper;
  }

  public PaipanResultDto create(PaipanRequest request, Long userId) throws Exception {
    PaipanResultDto result = baziService.paipan(request);
    String requestJson = objectMapper.writeValueAsString(request);
    Long exists = mapper.selectCount(new QueryWrapper<BaziRecord>().eq("user_id", userId).eq("request_json", requestJson));
    if (exists == 0) {
      BaziRecord record = new BaziRecord();
      record.setUserId(userId);
      record.setRequestJson(requestJson);
      record.setResultJson(objectMapper.writeValueAsString(result));
      record.setCreatedAt(LocalDateTime.now());
      mapper.insert(record);
    }
    return result;
  }

  public List<RecordDto> list(Long userId) throws Exception {
    List<BaziRecord> records = mapper.selectList(
        new QueryWrapper<BaziRecord>().eq("user_id", userId).orderByDesc("created_at"));
    List<RecordDto> dtos = new ArrayList<>();
    for (BaziRecord record : records) {
      dtos.add(toDto(record));
    }
    return dtos;
  }

  public Optional<RecordDto> get(Long userId, Long id) throws Exception {
    BaziRecord record = mapper.selectOne(new QueryWrapper<BaziRecord>().eq("id", id).eq("user_id", userId));
    return record == null ? Optional.empty() : Optional.of(toDto(record));
  }

  public boolean delete(Long userId, Long id) {
    return mapper.delete(new QueryWrapper<BaziRecord>().eq("id", id).eq("user_id", userId)) > 0;
  }

  /** 清空当前用户全部记录 */
  public void clear(Long userId) {
    mapper.delete(new QueryWrapper<BaziRecord>().eq("user_id", userId));
  }

  private RecordDto toDto(BaziRecord record) throws Exception {
    return new RecordDto(
        record.getId(),
        objectMapper.readValue(record.getRequestJson(), PaipanRequest.class),
        objectMapper.readValue(record.getResultJson(), PaipanResultDto.class),
        record.getCreatedAt());
  }
}
