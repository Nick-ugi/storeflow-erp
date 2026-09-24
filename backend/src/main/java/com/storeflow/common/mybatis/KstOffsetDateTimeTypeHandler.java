package com.storeflow.common.mybatis;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedTypes;

/**
 * TIMESTAMPTZ ↔ OffsetDateTime.
 * PostgreSQL JDBC는 TIMESTAMPTZ를 UTC 오프셋으로 돌려주므로, 응답이 항상 한국 시간(+09:00)으로 나가도록 변환한다.
 */
@MappedTypes(OffsetDateTime.class)
public class KstOffsetDateTimeTypeHandler extends BaseTypeHandler<OffsetDateTime> {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, OffsetDateTime parameter, JdbcType jdbcType) throws SQLException {
        ps.setObject(i, parameter);
    }

    @Override
    public OffsetDateTime getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return toKst(rs.getObject(columnName, OffsetDateTime.class));
    }

    @Override
    public OffsetDateTime getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return toKst(rs.getObject(columnIndex, OffsetDateTime.class));
    }

    @Override
    public OffsetDateTime getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return toKst(cs.getObject(columnIndex, OffsetDateTime.class));
    }

    private static OffsetDateTime toKst(OffsetDateTime value) {
        return value == null ? null : value.atZoneSameInstant(KST).toOffsetDateTime().truncatedTo(ChronoUnit.SECONDS);
    }

}
