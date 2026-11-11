package cn.yq.modules.sales.service;

import cn.yq.common.exception.BusinessException;
import cn.yq.modules.sales.domain.entity.OrderEntity;
import cn.yq.modules.sales.mapper.OrderMapper;
import cn.yq.modules.student.domain.entity.StudentEntity;
import cn.yq.modules.student.domain.vo.req.StudentCreateReq;
import cn.yq.modules.student.service.StudentService;
import cn.yq.modules.studentportal.domain.vo.req.StudentEnrollmentSubmitReq;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderEnrollmentServiceTest {
    @Mock OrderMapper orders;
    @Mock StudentService students;
    @InjectMocks OrderEnrollmentService service;
    OrderEntity order;
    StudentEnrollmentSubmitReq request;

    @BeforeEach
    void setup() {
        order = new OrderEntity();
        order.setId(1L);
        order.setStudentCode("TRAINEE001");
        order.setStudentName("测试学员");
        order.setPhone("13800000000");
        order.setOrderStatus("PAID");
        order.setReceivableAmount(new BigDecimal("1000"));
        order.setPaidAmount(new BigDecimal("1000"));
        order.setProductId(2L);
        order.setProductStudyMode("OFFLINE");
        order.setClassId(3L);
        request = new StudentEnrollmentSubmitReq();
        request.setPhone(order.getPhone());
        request.setPassword("test-password");
        request.setGraduationSession(2026);
        request.setSchool("测试院校");
        request.setEducation("本科");
        request.setJoinTime(new Date());
    }

    void stubOrder() {
        when(orders.lockById(1L)).thenReturn(1L);
        when(orders.selectById(1L)).thenReturn(order);
    }

    @Test void createsArchiveUsingOrderProductAndClass() {
        stubOrder();
        when(students.create(any())).thenReturn(10L);
        assertEquals(10L, service.enroll(1L, request));
        var captured = ArgumentCaptor.forClass(StudentCreateReq.class);
        verify(students).create(captured.capture());
        assertEquals(order.getClassId(), captured.getValue().getClassId());
        assertEquals(order.getProductId(), captured.getValue().getProductId());
        assertEquals(order.getStudentCode(), captured.getValue().getStudentCode());
        var sequence = inOrder(orders, students);
        sequence.verify(orders).lockById(1L);
        sequence.verify(orders).selectById(1L);
        sequence.verify(students).findByStudentCode("TRAINEE001");
        sequence.verify(students).create(any());
    }

    @Test void repeatedEnrollmentReturnsExistingArchive() {
        stubOrder();
        var existing = new StudentEntity();
        existing.setId(10L);
        existing.setProductId(2L);
        existing.setClassId(3L);
        existing.setPhone(order.getPhone());
        when(students.findByStudentCode("TRAINEE001")).thenReturn(existing);
        assertEquals(10L, service.enroll(1L, request));
        verify(students, never()).create(any());
    }

    @Test void rejectsUnpaidOrder() {
        stubOrder();
        order.setOrderStatus("UNPAID");
        assertThrows(BusinessException.class, () -> service.enroll(1L, request));
        verifyNoInteractions(students);
    }

    @Test void rejectsInsufficientPaymentEvenIfStatusSaysPaid() {
        stubOrder();
        order.setPaidAmount(new BigDecimal("999"));
        assertThrows(BusinessException.class, () -> service.enroll(1L, request));
        verifyNoInteractions(students);
    }

    @Test void rejectsChangingTheOrderPhone() {
        stubOrder();
        request.setPhone("13900000000");
        assertThrows(BusinessException.class, () -> service.enroll(1L, request));
        verifyNoInteractions(students);
    }

    @Test void rejectsChangingTheOrderClass() {
        stubOrder();
        request.setClassId(99L);
        assertThrows(BusinessException.class, () -> service.enroll(1L, request));
        verifyNoInteractions(students);
    }

    @Test void rejectsMissingOfflineClass() {
        stubOrder();
        order.setClassId(null);
        assertThrows(BusinessException.class, () -> service.enroll(1L, request));
        verifyNoInteractions(students);
    }

    @Test void doesNotOverwriteAnUnrelatedArchive() {
        stubOrder();
        var existing = new StudentEntity();
        existing.setProductId(99L);
        when(students.findByStudentCode("TRAINEE001")).thenReturn(existing);
        assertThrows(BusinessException.class, () -> service.enroll(1L, request));
        verify(students, never()).create(any());
    }

    @Test void requiresExplicitInitialPassword() {
        stubOrder();
        request.setPassword(null);
        assertThrows(BusinessException.class, () -> service.enroll(1L, request));
        verify(students, never()).create(any());
    }

    @Test void onlineEnrollmentHasNoOfflineClass() {
        stubOrder();
        order.setProductStudyMode("ONLINE");
        order.setClassId(null);
        when(students.create(any())).thenReturn(10L);
        service.enroll(1L, request);
        var captured = ArgumentCaptor.forClass(StudentCreateReq.class);
        verify(students).create(captured.capture());
        assertNull(captured.getValue().getClassId());
        assertEquals("ONLINE", captured.getValue().getStudyMode());
    }

    @Test void rejectsMissingOrderBeforeReadingOtherData() {
        when(orders.lockById(1L)).thenReturn(null);
        assertThrows(BusinessException.class, () -> service.enroll(1L, request));
        verify(orders, never()).selectById(any());
        verifyNoInteractions(students);
    }
}
