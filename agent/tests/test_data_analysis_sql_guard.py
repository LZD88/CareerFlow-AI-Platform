import pytest

from app.services.data_analysis.sql_guard import DataAnalysisSqlGuard


def test_sql_guard_appends_limit() -> None:
    guard = DataAnalysisSqlGuard()

    sql = guard.validate(
        "SELECT campus_name FROM edu_campus",
        allowed_tables=["edu_campus"],
        max_rows=50,
    )

    assert sql == "SELECT campus_name FROM edu_campus LIMIT 50"


def test_sql_guard_keeps_existing_limit() -> None:
    guard = DataAnalysisSqlGuard()

    sql = guard.validate(
        "SELECT campus_name FROM edu_campus LIMIT 10",
        allowed_tables=["edu_campus"],
        max_rows=50,
    )

    assert sql == "SELECT campus_name FROM edu_campus LIMIT 10"


def test_sql_guard_allows_select_followed_by_newline() -> None:
    guard = DataAnalysisSqlGuard()

    sql = guard.validate(
        "SELECT\n    campus_name\nFROM edu_campus\nLIMIT 10",
        allowed_tables=["edu_campus"],
        max_rows=50,
    )

    assert sql == "SELECT\n    campus_name\nFROM edu_campus\nLIMIT 10"


def test_sql_guard_rejects_dangerous_sql() -> None:
    guard = DataAnalysisSqlGuard()

    with pytest.raises(ValueError, match="仅允许 SELECT/WITH 查询"):
        guard.validate("DELETE FROM edu_campus", allowed_tables=["edu_campus"])


def test_sql_guard_rejects_unknown_tables() -> None:
    guard = DataAnalysisSqlGuard()

    with pytest.raises(ValueError, match="未授权表"):
        guard.validate(
            "SELECT * FROM sys_user",
            allowed_tables=["edu_campus"],
        )


def test_sql_guard_allows_candidate_table_joins() -> None:
    guard = DataAnalysisSqlGuard()

    sql = guard.validate(
        "SELECT c.campus_name, SUM(o.paid_amount) "
        "FROM sale_order o "
        "JOIN edu_class cls ON o.class_id = cls.id "
        "JOIN edu_campus c ON cls.campus_id = c.id",
        allowed_tables=["sale_order", "edu_class", "edu_campus"],
        max_rows=100,
    )

    assert sql.endswith("LIMIT 100")


def test_sql_guard_allows_cte_names() -> None:
    guard = DataAnalysisSqlGuard()

    sql = guard.validate(
        "WITH order_summary AS (SELECT class_id, SUM(paid_amount) paid_amount FROM sale_order GROUP BY class_id) "
        "SELECT * FROM order_summary",
        allowed_tables=["sale_order"],
        max_rows=100,
    )

    assert sql.endswith("LIMIT 100")
