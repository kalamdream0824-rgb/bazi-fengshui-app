package com.bazi.app.service;

import com.nlf.calendar.Lunar;
import com.nlf.calendar.Solar;
import com.nlf.calendar.EightChar;
import com.nlf.calendar.eightchar.DaYun;
import com.nlf.calendar.eightchar.Yun;
import com.bazi.app.dto.DaYunDto;
import com.bazi.app.dto.HideGanDto;
import com.bazi.app.dto.LiuNianDto;
import com.bazi.app.dto.PaipanRequest;
import com.bazi.app.dto.PaipanResultDto;
import com.bazi.app.dto.PillarDto;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class BaziService {

  private static final Map<String, String> GAN_WUXING = Map.of(
      "甲", "mu", "乙", "mu", "丙", "huo", "丁", "huo", "戊", "tu",
      "己", "tu", "庚", "jin", "辛", "jin", "壬", "shui", "癸", "shui");
  private static final Map<String, String> ZHI_WUXING = Map.ofEntries(
      Map.entry("子", "shui"),
      Map.entry("丑", "tu"),
      Map.entry("寅", "mu"),
      Map.entry("卯", "mu"),
      Map.entry("辰", "tu"),
      Map.entry("巳", "huo"),
      Map.entry("午", "huo"),
      Map.entry("未", "tu"),
      Map.entry("申", "jin"),
      Map.entry("酉", "jin"),
      Map.entry("戌", "tu"),
      Map.entry("亥", "shui"));

  public PaipanResultDto paipan(PaipanRequest req) {
    String[] parts = req.solarDateTime().split("T");
    String[] date = parts[0].split("-");
    String[] time = (parts.length > 1 ? parts[1] : "00:00").split(":");
    int y = Integer.parseInt(date[0]);
    int m = Integer.parseInt(date[1]);
    int d = Integer.parseInt(date[2]);
    int h = Integer.parseInt(time[0]);
    int min = Integer.parseInt(time[1]);

    Solar solar = Solar.fromYmdHms(y, m, d, h, min, 0);
    Lunar lunar = solar.getLunar();
    EightChar ec = lunar.getEightChar();

    Map<String, PillarDto> pillars = new LinkedHashMap<>();
    pillars.put("year", pillar("year", ec.getYearGan(), ec.getYearZhi(), ec.getYearShiShenGan(), ec.getYearHideGan(), ec.getYearNaYin(), ec.getYearDiShi(), ec.getYearXunKong()));
    pillars.put("month", pillar("month", ec.getMonthGan(), ec.getMonthZhi(), ec.getMonthShiShenGan(), ec.getMonthHideGan(), ec.getMonthNaYin(), ec.getMonthDiShi(), ec.getMonthXunKong()));
    pillars.put("day", pillar("day", ec.getDayGan(), ec.getDayZhi(), "日主", ec.getDayHideGan(), ec.getDayNaYin(), ec.getDayDiShi(), ec.getDayXunKong()));
    pillars.put("time", pillar("time", ec.getTimeGan(), ec.getTimeZhi(), ec.getTimeShiShenGan(), ec.getTimeHideGan(), ec.getTimeNaYin(), ec.getTimeDiShi(), ec.getTimeXunKong()));

    Map<String, Integer> wuXing = new LinkedHashMap<>();
    wuXing.put("jin", 0);
    wuXing.put("mu", 0);
    wuXing.put("shui", 0);
    wuXing.put("huo", 0);
    wuXing.put("tu", 0);
    for (PillarDto p : pillars.values()) {
      wuXing.merge(GAN_WUXING.getOrDefault(p.gan(), "tu"), 1, Integer::sum);
      wuXing.merge(ZHI_WUXING.getOrDefault(p.zhi(), "tu"), 1, Integer::sum);
    }

    List<DaYunDto> daYun = new ArrayList<>();
    int nowYear = LocalDate.now().getYear();
    Yun yun = ec.getYun(req.isMale() ? 1 : 0);
    for (DaYun dy : yun.getDaYun()) {
      String gz = dy.getGanZhi();
      if (gz == null || gz.isEmpty()) {
        continue;
      }
      if (daYun.size() >= 8) {
        break;
      }
      int startYear = dy.getStartYear();
      daYun.add(new DaYunDto(
          dy.getStartAge() + " - " + (dy.getStartAge() + 10) + " 岁",
          gz,
          startYear + " - " + (startYear + 10),
          startYear <= nowYear && nowYear < startYear + 10,
          List.of()));
    }

    Lunar today = Lunar.fromDate(new Date());
    String yearGanZhi = today.getYearInGanZhiExact();
    String currentYearGanZhi = yearGanZhi + "年 " + today.getMonthInGanZhiExact() + "月 " + today.getDayInGanZhiExact() + "日";

    return new PaipanResultDto(
        String.format("%04d-%02d-%02d %02d:%02d", y, m, d, h, min),
        lunar.toString(),
        lunar.getYearShengXiao(),
        lunar.getTimeZhi(),
        pillars,
        wuXing,
        daYun,
        currentYearGanZhi,
        new LiuNianDto(yearGanZhi, List.of()),
        null);
  }

  private PillarDto pillar(String label, String gan, String zhi, String shiShen, List<String> hideGan, String naYin, String diShi, String xunKong) {
    List<HideGanDto> hideGanDtos = new ArrayList<>();
    if (hideGan != null) {
      for (String g : hideGan) {
        hideGanDtos.add(new HideGanDto(g, GAN_WUXING.getOrDefault(g, "tu")));
      }
    }
    return new PillarDto(label, gan, zhi, shiShen, hideGanDtos, naYin, diShi, xunKong, List.of());
  }
}
