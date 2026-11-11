package cn.yq.common.dataConfig.service;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SystemConfigItem<T> {

    private String key;

    private T defaultValue;

    private Class<T> clazz;

}
