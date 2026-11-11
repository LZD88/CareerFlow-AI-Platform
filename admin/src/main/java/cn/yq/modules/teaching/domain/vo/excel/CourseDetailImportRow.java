package cn.yq.modules.teaching.domain.vo.excel;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

/**
 * 课程详情导入行模型。
 * Excel 表头固定为：阶段 / 第几天 / 课程内容。
 */
@Data
public class CourseDetailImportRow {

    @ExcelProperty("阶段")
    private String stageName;

    @ExcelProperty("第几天")
    private Integer dayNum;

    @ExcelProperty("课程内容")
    private String courseContent;
}
