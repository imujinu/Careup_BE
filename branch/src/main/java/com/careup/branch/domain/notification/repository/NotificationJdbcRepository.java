package com.careup.branch.domain.notification.repository;

import com.careup.branch.domain.notification.domain.Notification;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class NotificationJdbcRepository {

    private final JdbcTemplate jdbcTemplate;

    public void batchInsert(List<Notification> notifications) {
        // 필수 컬럼 위주로 구성 (기본값이 있는 컬럼은 제외 가능)
        String sql = "INSERT INTO notification (title, body, event_name, receiver_email, " +
                "branch_id, is_read, is_deleted, created_at, updated_at, type) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                Notification n = notifications.get(i);

                ps.setString(1, n.getTitle());
                ps.setString(2, n.getBody());
                ps.setString(3, n.getEventName());
                ps.setString(4, n.getReceiverEmail());
                ps.setLong(5, n.getBranchId()); // 지점 ID

                // 초기값 설정
                ps.setBoolean(6, false); // is_read
                ps.setBoolean(7, false); // is_deleted

                // 시간 설정 (LocalDateTime -> Timestamp)
                Timestamp now = Timestamp.valueOf(LocalDateTime.now());
                ps.setTimestamp(8, now); // created_at
                ps.setTimestamp(9, now); // updated_at

                ps.setString(10, n.getType()); // 알림 타입
            }

            @Override
            public int getBatchSize() {
                return notifications.size();
            }
        });
    }
}
