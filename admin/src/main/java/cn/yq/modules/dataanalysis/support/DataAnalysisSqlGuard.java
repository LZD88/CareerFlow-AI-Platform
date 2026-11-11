package cn.yq.modules.dataanalysis.support;

import cn.yq.common.exception.BusinessException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class DataAnalysisSqlGuard {

    /**
     * SQL Guard 是 Java 执行数据库查询前的最后一道门。
     *
     * <p>Python Agent 生成 SQL 后，Java 不能直接信任模型输出：
     * - 模型可能生成 DDL/DML；
     * - 模型可能访问不在候选表里的表；
     * - 模型可能漏掉 LIMIT；
     * - 模型可能输出 Markdown 代码块或尾部分号。</p>
     *
     * <p>第一版使用保守正则校验；后续如果查询复杂度上来，建议升级为 JSQLParser AST 校验。</p>
     */
    private static final Pattern DANGEROUS_PATTERN = Pattern.compile(
            "\\b(insert|update|delete|drop|alter|truncate|create|replace|grant|revoke|call|load|merge|execute)\\b",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern SYSTEM_SCHEMA_PATTERN = Pattern.compile(
            "\\b(information_schema|mysql|performance_schema|sys)\\b",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern LIMIT_PATTERN = Pattern.compile("\\blimit\\s+(\\d+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern READONLY_HEAD_PATTERN = Pattern.compile("^(select|with)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern TABLE_PATTERN = Pattern.compile(
            "\\b(?:from|join)\\s+`?([a-zA-Z_][\\w]*)`?(?:\\s*\\.\\s*`?([a-zA-Z_][\\w]*)`?)?",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern CTE_PATTERN = Pattern.compile(
            "(?:with|,)\\s+`?([a-zA-Z_][\\w]*)`?\\s+as\\s*\\(",
            Pattern.CASE_INSENSITIVE);

    /**
     * 校验并规范化模型生成的 SQL。
     *
     * @param sql 模型生成的 SQL
     * @param allowedTables Python 选表阶段返回的候选表
     * @param maxRows 最大返回行数
     * @return 可执行的安全 SQL
     */
    public String validate(String sql, List<String> allowedTables, Integer maxRows) {
        String value = normalizeSql(sql);
        if (!StringUtils.hasText(value)) {
            throw new BusinessException("SQL 不能为空");
        }
        if (!READONLY_HEAD_PATTERN.matcher(value).find()) {
            throw new BusinessException("仅允许 SELECT/WITH 查询");
        }
        if (value.contains(";")) {
            throw new BusinessException("禁止多语句 SQL");
        }
        if (DANGEROUS_PATTERN.matcher(value).find()) {
            throw new BusinessException("SQL 包含危险关键字");
        }
        if (SYSTEM_SCHEMA_PATTERN.matcher(value).find()) {
            throw new BusinessException("禁止访问系统库");
        }
        validateTables(value, allowedTables);
        return ensureLimit(value, maxRows);
    }

    private String normalizeSql(String sql) {
        String value = sql == null ? "" : sql.trim();
        // 兼容模型偶尔返回 ```sql ... ``` 代码块的情况。
        if (value.startsWith("```")) {
            value = value.replaceFirst("(?is)^```(?:sql)?\\s*", "");
            value = value.replaceFirst("(?is)\\s*```$", "");
        }
        value = value.trim();
        // 允许 SQL 末尾带一个分号，但会先去掉；中间出现分号仍会被上层多语句校验拦截。
        if (value.endsWith(";")) {
            value = value.substring(0, value.length() - 1).trim();
        }
        return value;
    }

    private void validateTables(String sql, List<String> allowedTables) {
        Set<String> allowed = new HashSet<>();
        if (allowedTables != null) {
            for (String table : allowedTables) {
                if (StringUtils.hasText(table)) {
                    allowed.add(table.toLowerCase(Locale.ROOT));
                }
            }
        }
        if (allowed.isEmpty()) {
            throw new BusinessException("候选表为空，禁止执行 SQL");
        }

        // WITH 子句里的 CTE 名不是物理表名，例如：
        // WITH order_summary AS (...) SELECT * FROM order_summary
        // 最外层 FROM order_summary 不应该被误判成越权表。
        Set<String> cteNames = new HashSet<>();
        Matcher cteMatcher = CTE_PATTERN.matcher(sql);
        while (cteMatcher.find()) {
            cteNames.add(cteMatcher.group(1).toLowerCase(Locale.ROOT));
        }

        List<String> foundTables = new ArrayList<>();
        Matcher tableMatcher = TABLE_PATTERN.matcher(sql);
        while (tableMatcher.find()) {
            // 支持 db.table 和普通 table 两种写法；如果出现 db.table，实际表名取第二段。
            String table = StringUtils.hasText(tableMatcher.group(2)) ? tableMatcher.group(2) : tableMatcher.group(1);
            if (cteNames.contains(table.toLowerCase(Locale.ROOT))) {
                continue;
            }
            foundTables.add(table);
        }

        if (foundTables.isEmpty()) {
            throw new BusinessException("SQL 未识别到可查询表");
        }

        List<String> illegalTables = foundTables.stream()
                .filter(table -> !allowed.contains(table.toLowerCase(Locale.ROOT)))
                .toList();
        if (!illegalTables.isEmpty()) {
            throw new BusinessException("SQL 包含未授权表: " + String.join(", ", illegalTables));
        }
    }

    private String ensureLimit(String sql, Integer maxRows) {
        int safeMaxRows = Math.min(Math.max(maxRows == null ? 100 : maxRows, 1), 500);
        Matcher matcher = LIMIT_PATTERN.matcher(sql);
        if (!matcher.find()) {
            // 模型没补 LIMIT 时，Java 主动补上，避免大结果集拖垮页面或再次发送给 Python 解释。
            return sql + " LIMIT " + safeMaxRows;
        }
        int currentLimit = Integer.parseInt(matcher.group(1));
        if (currentLimit <= safeMaxRows) {
            return sql;
        }
        // 模型给了更大的 LIMIT 时，Java 收紧到 maxRows。
        return matcher.replaceFirst("LIMIT " + safeMaxRows);
    }
}
