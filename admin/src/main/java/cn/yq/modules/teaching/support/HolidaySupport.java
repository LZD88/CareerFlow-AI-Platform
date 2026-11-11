package cn.yq.modules.teaching.support;

import cn.hutool.http.HttpUtil;
import cn.yq.common.exception.BusinessException;
import cn.yq.modules.teaching.domain.vo.res.HolidayInfoRes;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.StringJoiner;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 节假日支撑组件。
 * 数据来源：timor.tech 免费节假日 API。
 */
@Component
@Slf4j
public class HolidaySupport {

    private static final String TIMOR_MONTH_API = "https://timor.tech/api/holiday/year/";
    private static final String TIMOR_BATCH_API = "https://timor.tech/api/holiday/batch";
    /** 按年份缓存节假日数据，避免同一年反复请求 timor 触发限流。 */
    private final Map<Integer, Map<String, HolidayInfoRes>> yearHolidayCache = new ConcurrentHashMap<>();

    public Map<String, HolidayInfoRes> getMonthHolidayInfo(int year, int month) {
        // 先确保整年的数据已经缓存在内存里，再按月份切出当前月给业务层使用。
        Map<String, HolidayInfoRes> yearData = yearHolidayCache.computeIfAbsent(year, this::loadYearHolidayInfo);
        Map<String, HolidayInfoRes> monthData = new LinkedHashMap<>();
        YearMonth yearMonth = YearMonth.of(year, month);
        for (int day = 1; day <= yearMonth.lengthOfMonth(); day += 1) {
            String date = yearMonth.atDay(day).toString();
            monthData.put(date, yearData.getOrDefault(date, defaultDayInfo(date)));
        }
        return monthData;
    }

    private Map<String, HolidayInfoRes> loadYearHolidayInfo(int year) {
        try {
            // timor 官方建议获取整年数据时使用 year/{year}/ 这种写法。
            return fetchByYearEndpoint(year);
        } catch (BusinessException ex) {
            // 如果整年接口波动，再回退到按月 batch 拉取，尽量保证课表生成可用。
            log.warn("按年获取节假日失败，回退到按月批量拉取: year={}", year, ex);
            return fetchYearByBatch(year);
        }
    }

    private Map<String, HolidayInfoRes> fetchYearByBatch(int year) {
        Map<String, HolidayInfoRes> result = new LinkedHashMap<>();
        for (int month = 1; month <= 12; month += 1) {
            YearMonth yearMonth = YearMonth.of(year, month);
            StringJoiner joiner = new StringJoiner("&", TIMOR_BATCH_API + "?", "&type=Y");
            for (int day = 1; day <= yearMonth.lengthOfMonth(); day += 1) {
                String date = yearMonth.atDay(day).toString();
                joiner.add("d=" + URLEncoder.encode(date, StandardCharsets.UTF_8));
            }

            JSONObject root = requestJson(joiner.toString(), "节假日服务访问失败，请稍后重试");
            JSONObject holidayMap = root.getJSONObject("holiday");
            JSONObject typeMap = root.getJSONObject("type");
            for (int day = 1; day <= yearMonth.lengthOfMonth(); day += 1) {
                String date = yearMonth.atDay(day).toString();
                JSONObject typeObj = typeMap == null ? null : typeMap.getJSONObject(date);
                JSONObject holidayObj = holidayMap == null ? null : holidayMap.getJSONObject(date);
                result.put(date, buildHolidayInfo(date, typeObj, holidayObj));
            }
        }
        return result;
    }

    private Map<String, HolidayInfoRes> fetchByYearEndpoint(int year) {
        String url = TIMOR_MONTH_API + URLEncoder.encode(String.valueOf(year), StandardCharsets.UTF_8) + "/?type=Y&week=Y";
        JSONObject root = requestJson(url, "节假日服务访问失败，请稍后重试");

        JSONObject holidayMap = root.getJSONObject("holiday");
        JSONObject typeMap = root.getJSONObject("type");
        Map<String, HolidayInfoRes> result = new LinkedHashMap<>();
        Map<String, JSONObject> holidayByDate = new HashMap<>();
        if (holidayMap != null) {
            for (String key : holidayMap.keySet()) {
                JSONObject holidayObj = holidayMap.getJSONObject(key);
                if (holidayObj == null) {
                    continue;
                }
                // year 接口的 holiday key 可能是 mm-dd，所以优先取响应体里的完整 date 字段做索引。
                String fullDate = holidayObj.getString("date");
                if (fullDate != null && !fullDate.isBlank()) {
                    holidayByDate.put(fullDate, holidayObj);
                }
            }
        }

        LocalDate cursor = LocalDate.of(year, 1, 1);
        LocalDate end = LocalDate.of(year, 12, 31);
        while (!cursor.isAfter(end)) {
            String date = cursor.toString();
            JSONObject typeObj = typeMap == null ? null : typeMap.getJSONObject(date);
            JSONObject holidayObj = holidayByDate.get(date);
            result.put(date, buildHolidayInfo(date, typeObj, holidayObj));
            cursor = cursor.plusDays(1);
        }
        return result;
    }

    private JSONObject requestJson(String url, String accessFailedMessage) {
        String body;
        try {
            body = HttpUtil.get(url, 8000);
        } catch (Exception ex) {
            log.error("节假日服务访问失败: url={}", url, ex);
            throw new BusinessException(accessFailedMessage);
        }

        JSONObject root;
        try {
            root = JSON.parseObject(body);
        } catch (Exception ex) {
            log.error("节假日服务响应解析失败: url={}, body={}", url, body, ex);
            throw new BusinessException("节假日服务返回异常");
        }

        if (root == null || root.getIntValue("code") != 0) {
            log.error("节假日服务返回异常: url={}, body={}", url, body);
            throw new BusinessException("节假日服务返回异常");
        }
        return root;
    }

    private HolidayInfoRes buildHolidayInfo(String date, JSONObject typeObj, JSONObject holidayObj) {
        HolidayInfoRes item = new HolidayInfoRes();
        item.setDate(date);
        item.setType(typeObj == null ? null : typeObj.getInteger("type"));
        item.setWeek(typeObj == null ? null : typeObj.getInteger("week"));
        item.setName(holidayObj != null ? holidayObj.getString("name") : (typeObj == null ? null : typeObj.getString("name")));
        item.setHoliday(holidayObj != null ? holidayObj.getBoolean("holiday") : Boolean.FALSE);
        return item;
    }

    private HolidayInfoRes defaultDayInfo(String date) {
        HolidayInfoRes item = new HolidayInfoRes();
        item.setDate(date);
        item.setHoliday(Boolean.FALSE);
        return item;
    }

    public boolean isWorkingDay(HolidayInfoRes info) {
        if (info == null || info.getType() == null) {
            return true;
        }
        // 0=工作日，3=调休补班，这两种都允许排正式课程。
        return info.getType() == 0 || info.getType() == 3;
    }
}
