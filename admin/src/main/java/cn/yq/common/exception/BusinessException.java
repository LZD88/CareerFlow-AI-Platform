package cn.yq.common.exception;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {

    private final Integer code;

    public static final BusinessException REDIS_ERROR = new BusinessException(10001, "redis异常");

    public BusinessException(String message) {
        super(message);
        this.code = 400;
    }

    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }


    public BusinessException newInstance(String message) {
        return new BusinessException(this.getCode(), message);
    }
}
