from app.models.data_analysis import DataAnalysisChartSuggestion
from app.services.data_analysis.result_interpreter import DataAnalysisResultInterpreter


def test_compact_rows_limits_rows_and_long_text() -> None:
    interpreter = DataAnalysisResultInterpreter()
    rows = [{"id": index, "name": "x" * 600} for index in range(60)]

    compact_rows = interpreter._compact_rows(rows)

    assert len(compact_rows) == 50
    assert compact_rows[0]["name"].endswith("...(truncated)")
    assert len(compact_rows[0]["name"]) < 530


def test_parse_chart_suggestion_accepts_camel_case() -> None:
    interpreter = DataAnalysisResultInterpreter()

    suggestion = interpreter._parse_chart_suggestion(
        {
            "chartType": "bar",
            "xField": "campus_name",
            "yField": "paid_amount",
            "seriesField": "",
            "reason": "适合比较校区金额",
        }
    )

    assert isinstance(suggestion, DataAnalysisChartSuggestion)
    assert suggestion.chartType == "bar"
    assert suggestion.xField == "campus_name"
    assert suggestion.yField == "paid_amount"


def test_parse_chart_suggestion_returns_none_without_type() -> None:
    interpreter = DataAnalysisResultInterpreter()

    assert interpreter._parse_chart_suggestion({"xField": "campus_name"}) is None
