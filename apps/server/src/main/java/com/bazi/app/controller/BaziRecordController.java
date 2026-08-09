package com.bazi.app.controller;

import com.bazi.app.dto.PaipanRequest;
import com.bazi.app.dto.PaipanResultDto;
import com.bazi.app.dto.RecordDto;
import com.bazi.app.service.RecordService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/records")
public class BaziRecordController {

  private final RecordService recordService;

  public BaziRecordController(RecordService recordService) {
    this.recordService = recordService;
  }

  @PostMapping
  public PaipanResultDto create(@Valid @RequestBody PaipanRequest request, HttpServletRequest httpRequest) throws Exception {
    return recordService.create(request, userId(httpRequest));
  }

  @GetMapping
  public List<RecordDto> list(HttpServletRequest httpRequest) throws Exception {
    return recordService.list(userId(httpRequest));
  }

  @GetMapping("/{id}")
  public ResponseEntity<RecordDto> get(@PathVariable Long id, HttpServletRequest httpRequest) throws Exception {
    return recordService.get(userId(httpRequest), id)
        .map(ResponseEntity::ok)
        .orElseGet(() -> ResponseEntity.notFound().build());
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable Long id, HttpServletRequest httpRequest) {
    return recordService.delete(userId(httpRequest), id)
        ? ResponseEntity.noContent().build()
        : ResponseEntity.notFound().build();
  }

  private static Long userId(HttpServletRequest httpRequest) {
    return (Long) httpRequest.getAttribute("userId");
  }
}
