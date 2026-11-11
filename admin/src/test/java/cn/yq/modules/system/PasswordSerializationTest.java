package cn.yq.modules.system;

import cn.yq.modules.system.domain.entity.SysUserEntity;
import cn.yq.modules.student.domain.entity.StudentEntity;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PasswordSerializationTest {
    @Test void userAndStudentResponsesExcludePassword() throws Exception {
        var json = new ObjectMapper();
        var user = new SysUserEntity();
        user.setUsername("operator");
        user.setPassword("private-password");
        var student = new StudentEntity();
        student.setStudentName("测试学员");
        student.setPassword("private-password");
        assertFalse(json.writeValueAsString(user).contains("password"));
        assertFalse(json.writeValueAsString(student).contains("password"));
        assertTrue(json.writeValueAsString(user).contains("operator"));
    }
}
