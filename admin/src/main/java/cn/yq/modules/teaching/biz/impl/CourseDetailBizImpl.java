package cn.yq.modules.teaching.biz.impl;

import com.alibaba.excel.EasyExcel;
import cn.yq.common.exception.BusinessException;
import cn.yq.modules.teaching.biz.CourseDetailBiz;
import cn.yq.modules.teaching.domain.entity.CourseDetailEntity;
import cn.yq.modules.teaching.domain.vo.excel.CourseDetailImportRow;
import cn.yq.modules.teaching.domain.vo.req.CourseDetailCreateReq;
import cn.yq.modules.teaching.service.CourseDetailService;
import cn.yq.modules.teaching.support.CourseSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 课程详情业务层。
 * 负责“插入后顺延”“Excel 导入”等复杂业务编排。
 */
@Service
@RequiredArgsConstructor
public class CourseDetailBizImpl implements CourseDetailBiz {

    private final CourseDetailService courseDetailService;
    private final CourseSupport courseSupport;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(Long courseId, CourseDetailCreateReq request) {
        courseSupport.requireCourse(courseId);
        validateCreateRequest(request);

        // 在指定天数插入一条新记录，原有 >= 当前天数的数据整体顺延一天。
        courseDetailService.increaseDayNumFrom(courseId, request.getDayNum());

        CourseDetailEntity detail = new CourseDetailEntity();
        detail.setCourseId(courseId);
        detail.setStageName(request.getStageName().trim());
        detail.setDayNum(request.getDayNum());
        detail.setCourseContent(request.getCourseContent().trim());
        detail.setCreatedAt(new Date());
        detail.setUpdatedAt(new Date());
        return courseDetailService.create(detail);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long courseId, Long id) {
        courseSupport.requireCourse(courseId);
        CourseDetailEntity detail = courseDetailService.detail(courseId, id);
        courseDetailService.delete(courseId, id);
        // 删除某一天后，后续所有天数整体前移一天，保证课表连续。
        courseDetailService.decreaseDayNumAfter(courseId, detail.getDayNum());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer importExcel(Long courseId, MultipartFile file) {
        courseSupport.requireCourse(courseId);
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请上传 Excel 文件");
        }

        List<CourseDetailImportRow> rows;
        try {
            rows = EasyExcel.read(file.getInputStream(), CourseDetailImportRow.class, null)
                    .sheet()
                    .doReadSync();
        } catch (IOException ex) {
            throw new BusinessException("Excel 读取失败，请检查文件格式");
        }

        if (rows == null || rows.isEmpty()) {
            throw new BusinessException("Excel 中没有可导入的数据");
        }

        List<CourseDetailEntity> existedDetails = courseDetailService.listByCourseId(courseId);
        Map<Integer, CourseDetailEntity> existedMap = new LinkedHashMap<>();
        existedDetails.forEach(item -> existedMap.put(item.getDayNum(), item));

        Date now = new Date();
        Set<Integer> importedDayNums = new HashSet<>();
        List<CourseDetailEntity> toInsert = new ArrayList<>();
        List<CourseDetailEntity> toUpdate = new ArrayList<>();
        for (int i = 0; i < rows.size(); i += 1) {
            CourseDetailImportRow row = rows.get(i);
            if (isEmptyRow(row)) {
                continue;
            }

            int excelRowNum = i + 2;
            validateImportRow(row, excelRowNum);
            if (!importedDayNums.add(row.getDayNum())) {
                throw new BusinessException("Excel 第 " + excelRowNum + " 行的第几天重复");
            }

            CourseDetailEntity existed = existedMap.get(row.getDayNum());
            if (existed == null) {
                CourseDetailEntity detail = new CourseDetailEntity();
                detail.setCourseId(courseId);
                detail.setStageName(row.getStageName().trim());
                detail.setDayNum(row.getDayNum());
                detail.setCourseContent(row.getCourseContent().trim());
                detail.setCreatedAt(now);
                detail.setUpdatedAt(now);
                toInsert.add(detail);
            } else {
                existed.setStageName(row.getStageName().trim());
                existed.setCourseContent(row.getCourseContent().trim());
                existed.setUpdatedAt(now);
                toUpdate.add(existed);
            }
        }

        if (!toInsert.isEmpty()) {
            courseDetailService.createBatch(toInsert);
        }
        if (!toUpdate.isEmpty()) {
            courseDetailService.updateBatch(toUpdate);
        }

        int importedCount = toInsert.size() + toUpdate.size();
        if (importedCount == 0) {
            throw new BusinessException("Excel 中没有可导入的数据");
        }
        return importedCount;
    }

    private void validateCreateRequest(CourseDetailCreateReq request) {
        if (request.getStageName() == null || request.getStageName().trim().isEmpty()) {
            throw new BusinessException("所属阶段不能为空");
        }
        if (request.getDayNum() == null || request.getDayNum() < 1) {
            throw new BusinessException("第几天课必须大于 0");
        }
        if (request.getCourseContent() == null || request.getCourseContent().trim().isEmpty()) {
            throw new BusinessException("课程内容不能为空");
        }
    }

    private void validateImportRow(CourseDetailImportRow row, int excelRowNum) {
        if (row.getStageName() == null || row.getStageName().trim().isEmpty()) {
            throw new BusinessException("Excel 第 " + excelRowNum + " 行的阶段不能为空");
        }
        if (row.getDayNum() == null || row.getDayNum() < 1) {
            throw new BusinessException("Excel 第 " + excelRowNum + " 行的第几天必须大于 0");
        }
        if (row.getCourseContent() == null || row.getCourseContent().trim().isEmpty()) {
            throw new BusinessException("Excel 第 " + excelRowNum + " 行的课程内容不能为空");
        }
    }

    private boolean isEmptyRow(CourseDetailImportRow row) {
        return row == null
                || ((row.getStageName() == null || row.getStageName().trim().isEmpty())
                && row.getDayNum() == null
                && (row.getCourseContent() == null || row.getCourseContent().trim().isEmpty()));
    }
}
