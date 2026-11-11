package cn.yq.modules.teaching.enums;

import cn.yq.common.exception.BusinessException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ClassScheduleTypeEnum {

    CLASS("CLASS", "上课"),
    SELF_STUDY("SELF_STUDY", "自习"),
    REST("REST", "休息"),
    EMPTY("EMPTY", "");

    private final String code;
    private final String label;

    public boolean isClassType() {
        return this == CLASS;
    }

    public static ClassScheduleTypeEnum fromCode(String code) {
        for (ClassScheduleTypeEnum value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        throw new BusinessException("不支持的课表类型");
    }
}
