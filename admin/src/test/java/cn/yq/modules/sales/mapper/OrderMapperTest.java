package cn.yq.modules.sales.mapper;

import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.datasource.unpooled.UnpooledDataSource;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** 在本地内存库验证实际 Mapper SQL，不连接开发环境数据库。 */
class OrderMapperTest {
    SqlSessionFactory factory;

    @BeforeEach void setup() throws Exception {
        var dataSource = new UnpooledDataSource("org.h2.Driver",
                "jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");
        try (var connection = dataSource.getConnection(); var sql = connection.createStatement()) {
            sql.execute("create table sale_product(id bigint primary key, product_name varchar(100), study_mode varchar(20))");
            sql.execute("create table edu_class(id bigint primary key, class_term int)");
            sql.execute("create table edu_student(id bigint primary key, student_code varchar(64) unique, product_id bigint, phone varchar(32), class_id bigint)");
            sql.execute("create table sale_order(id bigint primary key, order_no varchar(64), student_name varchar(64), phone varchar(32), product_id bigint, class_id bigint, student_code varchar(64), product_amount decimal(12,2), discount_amount decimal(12,2), receivable_amount decimal(12,2), paid_amount decimal(12,2), order_status varchar(32), created_at timestamp, updated_at timestamp)");
            sql.execute("insert into sale_product values(2,'Java职业培训','OFFLINE')");
            sql.execute("insert into edu_class values(3,12)");
            sql.execute("insert into edu_student values(10,'S1',2,'13800000000',3),(11,'S5',2,'13800000000',99)");
            String[] statuses = {"PAID", "PAID", "UNPAID", "CLOSED", "PAID"};
            for (int i = 1; i <= statuses.length; i++) {
                sql.execute("insert into sale_order values(" + i + ",'ORDER" + i + "','测试学员','13800000000',2,3,'S" + i + "',1000,0,1000,1000,'" + statuses[i-1] + "',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)");
            }
        }
        var config = new Configuration(new Environment("test", new JdbcTransactionFactory(), dataSource));
        try (var xml = Resources.getResourceAsStream("mapper/OrderMapper.xml")) {
            new XMLMapperBuilder(xml, config, "mapper/OrderMapper.xml", config.getSqlFragments()).parse();
        }
        factory = new SqlSessionFactoryBuilder().build(config);
    }

    @Test void derivesEnrollmentOnlyWhenArchiveMatchesProductPhoneAndClass() {
        try (SqlSession session = factory.openSession()) {
            var mapper = session.getMapper(OrderMapper.class);
            assertEquals("ENROLLED", mapper.selectById(1L).getFulfillmentStatus());
            assertEquals(10L, mapper.selectById(1L).getStudentId());
            assertEquals("PENDING_ENROLLMENT", mapper.selectById(2L).getFulfillmentStatus());
            assertEquals("PENDING_ENROLLMENT", mapper.selectById(5L).getFulfillmentStatus());
            assertEquals("PENDING_PAYMENT", mapper.selectById(3L).getFulfillmentStatus());
            assertEquals("CLOSED", mapper.selectById(4L).getFulfillmentStatus());
        }
    }

    @Test void filtersPendingEnrollmentWithoutIncludingUnpaidOrClosedOrders() {
        try (SqlSession session = factory.openSession()) {
            var result = session.getMapper(OrderMapper.class).selectPage(null, null, "PENDING_ENROLLMENT");
            assertEquals(2, result.size());
            assertTrue(result.stream().allMatch(order -> "PAID".equals(order.getOrderStatus())));
        }
    }

    @Test void composesKeywordPaymentAndFulfillmentFilters() {
        try (SqlSession session = factory.openSession()) {
            var mapper = session.getMapper(OrderMapper.class);
            assertEquals(1, mapper.selectPage("ORDER2", "PAID", "PENDING_ENROLLMENT").size());
            assertTrue(mapper.selectPage(null, "UNPAID", "ENROLLED").isEmpty());
            assertEquals(5, mapper.selectPage(null, null, null).size());
        }
    }

    @Test void locksOnlyExistingOrders() {
        try (SqlSession session = factory.openSession()) {
            var mapper = session.getMapper(OrderMapper.class);
            assertEquals(1L, mapper.lockById(1L));
            assertEquals(2L, mapper.lockByOrderNo("ORDER2"));
            assertNull(mapper.lockById(99L));
        }
    }
}
