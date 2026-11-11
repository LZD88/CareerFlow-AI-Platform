package cn.yq.modules.teaching.enums;

import cn.yq.common.exception.BusinessException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum DutyTypeEnum {

    CLASS_NIGHT_DUTY("CLASS_NIGHT_DUTY", "晚自习值班", "19:00", "21:00"),
    GLOBAL_NIGHT_DUTY("GLOBAL_NIGHT_DUTY", "全局值班", "21:00", "22:30");

    private final String code;
    private final String label;
    private final String startTime;
    private final String endTime;

    public static DutyTypeEnum fromCode(String code) {
        for (DutyTypeEnum value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        throw new BusinessException("不支持的值班类型");
    }
}
