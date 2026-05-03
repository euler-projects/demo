/*
 * Copyright 2013-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.eulerframework.uc;

import jakarta.persistence.EntityManagerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Startup probe that prints time-zone configuration across the JVM,
 * Hibernate, JDBC driver and database session layers, so that the full
 * UTC-normalization chain can be verified in the application log.
 *
 * <p>Supports MySQL / MariaDB / PostgreSQL / Oracle / Microsoft SQL Server / H2.
 * For other databases the database-side check is skipped silently.
 *
 * <h2>Why four layers are required</h2>
 *
 * <p>No single property governs every Java time type. The table below shows
 * which layer actually controls the binding of each type when writing to a
 * {@code DATETIME} / {@code TIMESTAMP} column.
 *
 * <table border="1">
 *   <caption>Coverage matrix</caption>
 *   <tr>
 *     <th>Java type</th>
 *     <th>{@code hibernate.timezone.default_storage}</th>
 *     <th>{@code hibernate.jdbc.time_zone}</th>
 *     <th>JDBC URL {@code connectionTimeZone}</th>
 *   </tr>
 *   <tr>
 *     <td>{@code ZonedDateTime} / {@code OffsetDateTime} / {@code OffsetTime}</td>
 *     <td>Yes (normalizes to UTC)</td><td>Yes</td><td>Yes</td>
 *   </tr>
 *   <tr>
 *     <td>{@code Instant}</td>
 *     <td>n/a (already UTC)</td><td>Yes</td><td>Yes</td>
 *   </tr>
 *   <tr>
 *     <td>{@code java.util.Date} / {@code java.sql.Timestamp}</td>
 *     <td>No</td><td>Yes</td><td>Yes</td>
 *   </tr>
 *   <tr>
 *     <td>{@code LocalDateTime} / {@code LocalTime}</td>
 *     <td>No</td><td>Yes</td><td>Yes</td>
 *   </tr>
 * </table>
 *
 * <p>To guarantee that <em>every</em> time type is persisted as UTC
 * regardless of the JVM default time-zone, all four layers must be
 * configured consistently; otherwise legacy types such as {@code Date}
 * silently drift with the JVM default time-zone.
 *
 * <h2>Recommended configuration</h2>
 *
 * <p><b>Layer 1 &amp; 2 &mdash; Hibernate</b> in {@code application.yml}:
 * <pre>
 * spring:
 *   jpa:
 *     properties:
 *       hibernate:
 *         jdbc:
 *           time_zone: UTC            # covers Date / Timestamp / LocalDateTime / Instant
 *         timezone:
 *           default_storage: NORMALIZE_UTC   # covers Zoned/Offset types
 * </pre>
 *
 * <p><b>Layer 3 &amp; 4 &mdash; JDBC driver and database session</b> in the
 * JDBC URL. Examples per driver:
 * <ul>
 *   <li>MySQL Connector/J 8.0.23+:<br>
 *     {@code jdbc:mysql://host:3306/db?connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true}<br>
 *     {@code connectionTimeZone=UTC} pins the driver-side time-zone, and
 *     {@code forceConnectionTimeZoneToSession=true} issues
 *     {@code SET time_zone='+00:00'} at connection open so that {@code NOW()}
 *     and {@code DEFAULT CURRENT_TIMESTAMP} also run in UTC.</li>
 *   <li>MariaDB Connector/J: {@code ?connectionTimeZone=UTC}</li>
 *   <li>PostgreSQL: set the server/cluster default to {@code UTC}, or use
 *     HikariCP {@code connectionInitSql: SET TIME ZONE 'UTC'}.</li>
 *   <li>Oracle / SQL Server: prefer {@code TIMESTAMP WITH TIME ZONE} or
 *     {@code DATETIMEOFFSET} columns and rely on
 *     {@code hibernate.jdbc.time_zone=UTC} for binding.</li>
 * </ul>
 *
 * <h2>Column type selection</h2>
 *
 * <p>Use {@code DATETIME(3)} on MySQL/MariaDB (wide range, millisecond
 * precision) combined with the UTC-normalization chain above. Avoid MySQL
 * {@code TIMESTAMP} because its upper bound is {@code 2038-01-19 03:14:07 UTC}
 * and it applies implicit session-time-zone conversions that may clash with
 * the normalization chain.
 *
 * <h2>How to verify &mdash; read the probe output</h2>
 *
 * <p>On {@link ApplicationReadyEvent} the probe prints lines similar to:
 * <pre>
 * [TimeZoneProbe] JVM default timezone               = Asia/Shanghai
 * [TimeZoneProbe] hibernate.timezone.default_storage = NORMALIZE_UTC
 * [TimeZoneProbe] hibernate.jdbc.time_zone           = UTC
 * [TimeZoneProbe] DB product                         = MySQL
 * [TimeZoneProbe] DB session timezone                = +00:00
 * [TimeZoneProbe] DB global/db timezone              = SYSTEM
 * [TimeZoneProbe] DB current timestamp               = 2026-04-29 06:30:15
 * </pre>
 *
 * <p>Expected values for a fully UTC-normalized deployment:
 * <ul>
 *   <li>{@code hibernate.timezone.default_storage} = {@code NORMALIZE_UTC}</li>
 *   <li>{@code hibernate.jdbc.time_zone} = {@code UTC}</li>
 *   <li>{@code DB session timezone} = {@code +00:00} (or {@code UTC})</li>
 *   <li>{@code DB current timestamp} matches the current UTC wall-clock</li>
 * </ul>
 *
 * <p>The JVM default time-zone is intentionally reported but not required to
 * be UTC: with all four layers configured correctly the JVM time-zone no
 * longer affects persistence and may be any value.
 *
 * <h2>Round-trip sanity check</h2>
 *
 * <p>For the strongest evidence, boot the application with a non-UTC JVM
 * time-zone (e.g. {@code -Duser.timezone=Asia/Shanghai}), persist a known
 * UTC instant such as {@code Instant.parse("2026-01-01T00:00:00Z")}, then
 * read the raw column with a plain SQL client &mdash; it must read back
 * exactly {@code 2026-01-01 00:00:00.000}.
 *
 * <h2>Disabling</h2>
 *
 * <p>The probe is component-scanned and runs once. To disable it in
 * production, annotate with {@code @Profile("!prod")} or remove the bean.
 */
@Component
public class TimeZoneStartupProbe {

    private static final Logger log = LoggerFactory.getLogger(TimeZoneStartupProbe.class);

    private final EntityManagerFactory entityManagerFactory;
    private final JdbcTemplate jdbcTemplate;

    public TimeZoneStartupProbe(EntityManagerFactory entityManagerFactory, JdbcTemplate jdbcTemplate) {
        this.entityManagerFactory = entityManagerFactory;
        this.jdbcTemplate = jdbcTemplate;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void logTimeZoneChain() {
        String jvmTz = TimeZone.getDefault().getID();
        Object hibernateDefaultStorage = entityManagerFactory.getProperties()
                .get("hibernate.timezone.default_storage");
        Object hibernateJdbcTimeZone = entityManagerFactory.getProperties()
                .get("hibernate.jdbc.time_zone");

        log.info("[TimeZoneProbe] JVM default timezone               = {}", jvmTz);
        log.info("[TimeZoneProbe] hibernate.timezone.default_storage = {}", hibernateDefaultStorage);
        log.info("[TimeZoneProbe] hibernate.jdbc.time_zone           = {}", hibernateJdbcTimeZone);

        String productName = detectDatabaseProductName();
        if (productName == null) {
            return;
        }
        log.info("[TimeZoneProbe] DB product                         = {}", productName);

        try {
            TimeZoneInfo info = probeDatabase(productName);
            if (info == null) {
                log.info("[TimeZoneProbe] No time-zone probe for '{}', database-side check skipped.", productName);
                return;
            }
            log.info("[TimeZoneProbe] DB session timezone                = {}", info.sessionTimeZone);
            log.info("[TimeZoneProbe] DB global/db timezone              = {}", info.globalTimeZone);
            log.info("[TimeZoneProbe] DB current timestamp               = {}", info.currentTimestamp.toInstant());
        } catch (Exception ex) {
            log.warn("[TimeZoneProbe] Failed to query time-zone state for {}", productName, ex);
        }
    }

    private String detectDatabaseProductName() {
        try {
            return jdbcTemplate.execute((ConnectionCallback<String>) conn -> conn.getMetaData().getDatabaseProductName());
        } catch (Exception ex) {
            log.warn("[TimeZoneProbe] Failed to detect database product", ex);
            return null;
        }
    }

    private TimeZoneInfo probeDatabase(String productName) {
        String p = productName.toLowerCase(Locale.ROOT);
        if (p.contains("mysql") || p.contains("mariadb")) {
            return jdbcTemplate.queryForObject(
                    "SELECT @@session.time_zone, @@global.time_zone, NOW()",
                    (rs, rn) -> new TimeZoneInfo(rs.getString(1), rs.getString(2), rs.getTimestamp(3)));
        }
        if (p.contains("postgres")) {
            return jdbcTemplate.queryForObject(
                    "SELECT current_setting('timezone'), current_setting('timezone'), now()",
                    (rs, rn) -> new TimeZoneInfo(rs.getString(1), rs.getString(2), rs.getTimestamp(3)));
        }
        if (p.contains("oracle")) {
            return jdbcTemplate.queryForObject(
                    "SELECT SESSIONTIMEZONE, DBTIMEZONE, CURRENT_TIMESTAMP FROM DUAL",
                    (rs, rn) -> new TimeZoneInfo(rs.getString(1), rs.getString(2), rs.getTimestamp(3)));
        }
        if (p.contains("microsoft sql server") || p.contains("sql server")) {
            return jdbcTemplate.queryForObject(
                    "SELECT CURRENT_TIMEZONE(), CURRENT_TIMEZONE(), SYSDATETIMEOFFSET()",
                    (rs, rn) -> new TimeZoneInfo(rs.getString(1), rs.getString(2), rs.getTimestamp(3)));
        }
        if (p.contains("h2")) {
            return jdbcTemplate.queryForObject(
                    "SELECT 'n/a', 'n/a', CURRENT_TIMESTAMP",
                    (rs, rn) -> new TimeZoneInfo(rs.getString(1), rs.getString(2), rs.getTimestamp(3)));
        }
        return null;
    }

    private record TimeZoneInfo(String sessionTimeZone, String globalTimeZone, Timestamp currentTimestamp) {
    }
}
