package cn.yq.modules.teaching.enums;

import cn.yq.common.exception.BusinessException;
import lombok.Getter;

import java.util.Arrays;

@Getter
public enum ClassStatusEnum {

    WAITING("WAITING", "待开班"),
    TEACHING("TEACHING", "上课中"),
    FINISHED("FINISHED", "已结课");

    private final String code;
    private final String desc;

    ClassStatusEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static void validate(String code) {
        boolean matched = Arrays.stream(values()).anyMatch(item -> item.code.equals(code));
        if (!matched) {
            throw new BusinessException("班级状态不正确");
        }
    }
}
