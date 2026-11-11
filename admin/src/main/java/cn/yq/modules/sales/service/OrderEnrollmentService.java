package cn.yq.modules.sales.service;

import cn.yq.common.exception.BusinessException;
import cn.yq.modules.sales.domain.entity.OrderEntity;
import cn.yq.modules.sales.mapper.OrderMapper;
import cn.yq.modules.student.domain.entity.StudentEntity;
import cn.yq.modules.student.domain.vo.req.StudentCreateReq;
import cn.yq.modules.student.service.StudentService;
import cn.yq.modules.studentportal.domain.vo.req.StudentEnrollmentSubmitReq;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Objects;

/**
 * 已支付报名订单的入班履约，供管理端与学员端共用。
 * 用订单行锁串行化同一订单的重复提交；档案创建、商品与班级绑定处于同一事务。
 */
@Service
@RequiredArgsConstructor
public class OrderEnrollmentService {

    private final OrderMapper orderMapper;
    private final StudentService studentService;

    @Transactional(rollbackFor = Exception.class)
    public Long enroll(Long orderId, StudentEnrollmentSubmitReq request) {
        if (orderMapper.lockById(orderId) == null) {
            throw new BusinessException("报名订单不存在");
        }
        OrderEntity order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException("报名订单不存在");
        }
        if (!"PAID".equals(order.getOrderStatus()) || order.getPaidAmount() == null
                || order.getPaidAmount().compareTo(order.getReceivableAmount()) < 0) {
            throw new BusinessException("报名订单尚未支付完成，不能办理入班");
        }
        if (!Objects.equals(order.getPhone(), request.getPhone().trim())) {
            throw new BusinessException("入班手机号必须与报名订单一致");
        }
        boolean offline = "OFFLINE".equals(order.getProductStudyMode());
        if (offline && order.getClassId() == null) {
            throw new BusinessException("线下报名订单缺少班级，请先核实订单");
        }
        if (request.getClassId() != null && !Objects.equals(request.getClassId(), order.getClassId())) {
            throw new BusinessException("入班班级必须与报名订单一致");
        }
        StudentEntity existing = studentService.findByStudentCode(order.getStudentCode());
        if (existing != null) {
            if (!Objects.equals(existing.getProductId(), order.getProductId())
                    || !Objects.equals(existing.getPhone(), order.getPhone())
                    || (offline && !Objects.equals(existing.getClassId(), order.getClassId()))) {
                throw new BusinessException("已有学员档案与报名订单不一致，请核实后处理");
            }
            return existing.getId();
        }
        String password = request.getPassword();
        if (!StringUtils.hasText(password) || password.length() < 8 || password.length() > 64
                || !password.equals(password.trim())) {
            throw new BusinessException("请设置8至64位登录密码，首尾不能包含空格");
        }
        StudentCreateReq create = new StudentCreateReq();
        create.setStudentCode(order.getStudentCode());
        create.setStudentName(order.getStudentName());
        create.setPhone(order.getPhone());
        create.setPassword(password);
        create.setGraduationSession(request.getGraduationSession());
        create.setSchool(request.getSchool());
        create.setEducation(request.getEducation());
        create.setJoinTime(request.getJoinTime());
        create.setStudyMode(order.getProductStudyMode());
        create.setProductId(order.getProductId());
        create.setClassId(offline ? order.getClassId() : null);
        return studentService.create(create);
    }
}
