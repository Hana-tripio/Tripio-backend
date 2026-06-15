package com.tripio.global.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.tripio.support.IntegrationTestSupport;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class DatabaseSchemaMigrationTest extends IntegrationTestSupport {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void flywayCreatesCoreTripioTables() {
        List<String> tableNames = jdbcTemplate.queryForList("""
                SELECT table_name
                FROM information_schema.tables
                WHERE table_schema = 'public'
                ORDER BY table_name
                """, String.class);

        assertThat(tableNames).contains(
                "users",
                "regions",
                "places",
                "travel_etfs",
                "design_sessions",
                "trip_executions",
                "card_payment_events",
                "trip_verifications",
                "reward_histories",
                "local_contributions"
        );
    }
}
