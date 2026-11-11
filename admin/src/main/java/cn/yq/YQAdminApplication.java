package cn.yq;

import org.mybatis.spring.annotation.MapperScans;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.mybatis.spring.annotation.MapperScan;

@SpringBootApplication
@MapperScans(value = {
        @MapperScan("cn.yq.modules.system.mapper"),
        @MapperScan("cn.yq.modules.teaching.mapper"),
        @MapperScan("cn.yq.modules.sales.mapper"),
        @MapperScan("cn.yq.modules.payment.mapper"),
        @MapperScan("cn.yq.modules.student.mapper"),
        @MapperScan("cn.yq.modules.studentportal.mapper"),
        @MapperScan("cn.yq.modules.knowledge.mapper"),
        @MapperScan("cn.yq.modules.mediaagent.mapper"),
        @MapperScan("cn.yq.modules.interviewreview.mapper"),
        @MapperScan("cn.yq.modules.studentresume.mapper"),
        @MapperScan("cn.yq.common.dataConfig.mapper")})
public class YQAdminApplication {

    public static void main(String[] args) {
        SpringApplication.run(YQAdminApplication.class, args);
    }
}
