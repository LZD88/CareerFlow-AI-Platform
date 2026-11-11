import { useMemo, useState } from 'react';
import { BarChartOutlined, CopyOutlined, SearchOutlined } from '@ant-design/icons';
import { Alert, Button, Card, Form, Input, InputNumber, Space, Table, Tag, Tooltip, Typography, message } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import NoPermission from '../../components/NoPermission';
import PageContainer from '../../components/PageContainer';
import PanelTable from '../../components/PanelTable';
import { analyzeData } from '../../api/data-analysis';
import type { DataAnalysisAnalyzeRes } from '../../types/data-analysis';
import { hasPermission } from '../../utils/permission';

const { TextArea } = Input;

interface AnalyzeFormValues {
  question: string;
  maxRows: number;
}

export default function DataAnalysisPage() {
  const canView = hasPermission('menu:system:data-analysis');
  const canAnalyze = hasPermission('button:data-analysis:analyze');
  const [form] = Form.useForm<AnalyzeFormValues>();
  const [loading, setLoading] = useState(false);
  const [result, setResult] = useState<DataAnalysisAnalyzeRes>();

  const resultColumns = useMemo(() => {
    const columnNames = resolveColumnNames(result);
    return columnNames.map((columnName) => ({
      title: columnName,
      dataIndex: columnName,
      key: columnName,
      width: 160,
      ellipsis: true,
      render: (value: unknown) => renderCellValue(value)
    })) as ColumnsType<Record<string, unknown>>;
  }, [result]);

  const chartData = useMemo(() => buildChartData(result), [result]);

  const onAnalyze = async () => {
    const values = await form.validateFields();
    setLoading(true);
    try {
      const response = await analyzeData({
        question: values.question.trim(),
        maxRows: values.maxRows
      });
      setResult(response);
    } finally {
      setLoading(false);
    }
  };

  const onReset = () => {
    form.resetFields();
    setResult(undefined);
  };

  const continueClarification = () => {
    const currentQuestion = result?.question || form.getFieldValue('question')?.trim() || '';
    const clarificationText = result?.clarificationQuestions?.length
      ? `\n\n补充条件：\n${result.clarificationQuestions.map((item) => `- ${item}`).join('\n')}\n`
      : '\n\n补充条件：\n';
    form.setFieldValue('question', `${currentQuestion}${clarificationText}`);
  };

  const copySql = async () => {
    if (!result?.sql) {
      return;
    }
    await navigator.clipboard.writeText(result.sql);
    message.success('SQL 已复制');
  };

  if (!canView) {
    return <NoPermission />;
  }

  return (
    <PageContainer
      title="业务数据分析"
      description="用自然语言了解报名收款、培训班级与学员情况，查看查询结果和分析解读。"
    >
      <Form
        form={form}
        layout="vertical"
        initialValues={{ question: '', maxRows: 100 }}
        onFinish={onAnalyze}
      >
        <Form.Item
          name="question"
          label="分析问题"
          rules={[{ required: true, message: '请输入分析问题' }]}
        >
          <TextArea
            rows={4}
            maxLength={500}
            showCount
            placeholder="例如：统计最近一个月各课程商品的报名人数和已付金额，按已付金额从高到低排序"
          />
        </Form.Item>
        <Space align="end" size={12} wrap>
          <Form.Item
            name="maxRows"
            label="最大返回行数"
            rules={[{ required: true, message: '请输入最大返回行数' }]}
          >
            <InputNumber min={1} max={500} precision={0} style={{ width: 160 }} />
          </Form.Item>
          <Form.Item>
            <Space>
              <Button
                type="primary"
                htmlType="submit"
                loading={loading}
                disabled={!canAnalyze}
                icon={<SearchOutlined />}
              >
                开始分析
              </Button>
              <Button onClick={onReset}>清空</Button>
            </Space>
          </Form.Item>
        </Space>
      </Form>

      {!canAnalyze ? (
        <Alert
          style={{ marginBottom: 16 }}
          type="warning"
          showIcon
          message="当前账号没有业务数据分析执行权限"
          description="可以查看页面，但不能发起数据库分析请求。"
        />
      ) : null}

      {result ? (
        <Space direction="vertical" size={16} style={{ width: '100%' }}>
          {result.needsClarification && result.clarificationQuestions?.length ? (
            <Alert
              type="info"
              showIcon
              message="需要补充条件"
              description={
                <Space direction="vertical" size={10} style={{ width: '100%' }}>
                  <ul className="data-analysis-list">
                    {result.clarificationQuestions.map((item, index) => (
                      <li key={`${item}-${index}`}>{item}</li>
                    ))}
                  </ul>
                  <Button size="small" type="primary" onClick={continueClarification}>
                    补充后继续分析
                  </Button>
                </Space>
              }
            />
          ) : null}

          {result.knowledgeMissing && result.knowledgeMissingItems?.length ? (
            <Alert
              type="error"
              showIcon
              message="知识库信息不完整"
              description={
                <Space direction="vertical" size={8} style={{ width: '100%' }}>
                  <Typography.Text>
                    当前问题缺少系统侧字段语义、枚举或口径说明，请先补充业务数据分析知识库后再执行。
                  </Typography.Text>
                  <ul className="data-analysis-list">
                    {result.knowledgeMissingItems.map((item, index) => (
                      <li key={`${item}-${index}`}>{item}</li>
                    ))}
                  </ul>
                </Space>
              }
            />
          ) : null}

          {result.warnings?.length ? (
            <Alert
              type="warning"
              showIcon
              message="分析提示"
              description={result.warnings.map((item, index) => (
                <div key={`${item}-${index}`}>{item}</div>
              ))}
            />
          ) : null}

          {result.missingTables?.length ? (
            <Alert
              type="error"
              showIcon
              message="缺少表结构"
              description={`以下表还没有写入业务数据分析表结构库：${result.missingTables.join('、')}`}
            />
          ) : null}

          <Card
            size="small"
            title="SQL"
            extra={
              result.sql ? (
                <Tooltip title="复制 SQL">
                  <Button type="text" icon={<CopyOutlined />} onClick={copySql} />
                </Tooltip>
              ) : null
            }
          >
            <Typography.Paragraph className="data-analysis-sql" style={{ marginBottom: 0 }}>
              {result.sql || '暂无 SQL'}
            </Typography.Paragraph>
          </Card>

          <Space size={[8, 8]} wrap>
            {(result.candidateTables ?? []).map((table) => (
              <Tag key={table} color="blue">
                {table}
              </Tag>
            ))}
            <Tag color={result.truncated ? 'gold' : 'green'}>
              返回行数：{result.rowCount ?? result.rows?.length ?? 0}
              {result.truncated ? '，可能已截断' : ''}
            </Tag>
          </Space>

          {result.analysis ? (
            <Alert type="success" showIcon message="分析结论" description={result.analysis} />
          ) : null}

          {result.insights?.length ? (
            <Card size="small" title="关键洞察">
              <ul className="data-analysis-list">
                {result.insights.map((item, index) => (
                  <li key={`${item}-${index}`}>{item}</li>
                ))}
              </ul>
            </Card>
          ) : null}

          {chartData ? (
            <Card
              size="small"
              title={
                <Space>
                  <BarChartOutlined />
                  <span>{result.chartSuggestion ? '图表建议' : '自动图表'}</span>
                </Space>
              }
            >
              <Space size={[8, 8]} wrap>
                <Tag color="processing">{result.chartSuggestion?.chartType || chartData.chartType}</Tag>
                <Tag>X：{chartData.xField}</Tag>
                <Tag>Y：{chartData.yField}</Tag>
                {result.chartSuggestion?.seriesField ? <Tag>分组：{result.chartSuggestion.seriesField}</Tag> : null}
              </Space>
              {result.chartSuggestion?.reason ? (
                <Typography.Paragraph style={{ marginTop: 10, marginBottom: 0 }}>
                  {result.chartSuggestion.reason}
                </Typography.Paragraph>
              ) : (
                <Typography.Paragraph type="secondary" style={{ marginTop: 10, marginBottom: 0 }}>
                  根据查询结果自动选择分类字段和数值字段生成柱状图。
                </Typography.Paragraph>
              )}
              <DataAnalysisChart
                chartType={result.chartSuggestion?.chartType || chartData.chartType}
                data={chartData.points}
              />
            </Card>
          ) : result.chartSuggestion ? (
            <Card
              size="small"
              title={
                <Space>
                  <BarChartOutlined />
                  <span>图表建议</span>
                </Space>
              }
            >
              <Space size={[8, 8]} wrap>
                <Tag color="processing">{result.chartSuggestion.chartType || 'chart'}</Tag>
                {result.chartSuggestion.xField ? <Tag>X：{result.chartSuggestion.xField}</Tag> : null}
                {result.chartSuggestion.yField ? <Tag>Y：{result.chartSuggestion.yField}</Tag> : null}
                {result.chartSuggestion.seriesField ? <Tag>分组：{result.chartSuggestion.seriesField}</Tag> : null}
              </Space>
              {result.chartSuggestion.reason ? (
                <Typography.Paragraph style={{ marginTop: 10, marginBottom: 0 }}>
                  {result.chartSuggestion.reason}
                </Typography.Paragraph>
              ) : null}
              <Typography.Text type="secondary" style={{ display: 'block', marginTop: 12 }}>
                当前图表字段与结果列不匹配，暂不渲染图形。
              </Typography.Text>
            </Card>
          ) : null}

          <PanelTable>
            <Table
              rowKey={(_, index) => `row-${index}`}
              loading={loading}
              columns={resultColumns}
              dataSource={result.rows ?? []}
              scroll={{ x: 'max-content' }}
              pagination={{ pageSize: 10, showSizeChanger: true }}
            />
          </PanelTable>

          {result.followUpQuestions?.length ? (
            <Card size="small" title="后续追问">
              <Space direction="vertical" size={8} style={{ width: '100%' }}>
                {result.followUpQuestions.map((item, index) => (
                  <Typography.Text key={`${item}-${index}`}>{`${index + 1}. ${item}`}</Typography.Text>
                ))}
              </Space>
            </Card>
          ) : null}

          {result.assumptions?.length || result.reason ? (
            <Card size="small" title="生成口径">
              {result.reason ? (
                <Typography.Paragraph style={{ marginBottom: result.assumptions?.length ? 8 : 0 }}>
                  {result.reason}
                </Typography.Paragraph>
              ) : null}
              {result.assumptions?.length ? (
                <ul className="data-analysis-list">
                  {result.assumptions.map((item, index) => (
                    <li key={`${item}-${index}`}>{item}</li>
                  ))}
                </ul>
              ) : null}
            </Card>
          ) : null}
        </Space>
      ) : null}
    </PageContainer>
  );
}

function resolveColumnNames(result?: DataAnalysisAnalyzeRes) {
  if (result?.columns?.length) {
    return result.columns;
  }
  const firstRow = result?.rows?.[0];
  return firstRow ? Object.keys(firstRow) : [];
}

function renderCellValue(value: unknown) {
  if (value === null || value === undefined || value === '') {
    return '-';
  }
  if (typeof value === 'object') {
    return (
      <Typography.Text code>
        {JSON.stringify(value)}
      </Typography.Text>
    );
  }
  return String(value);
}

interface ChartPoint {
  name: string;
  value: number;
}

interface BuiltChartData {
  chartType: string;
  xField: string;
  yField: string;
  points: ChartPoint[];
}

function buildChartData(result?: DataAnalysisAnalyzeRes): BuiltChartData | null {
  const rows = result?.rows ?? [];
  if (!rows.length) {
    return null;
  }

  const firstRow = rows[0];
  const xField = resolveFieldName(result?.chartSuggestion?.xField, firstRow) ?? resolveFallbackXField(firstRow);
  const yField = resolveFieldName(result?.chartSuggestion?.yField, firstRow) ?? resolveFallbackYField(firstRow, xField);
  if (!xField || !yField) {
    return null;
  }

  const points = rows
    .map((row) => ({
      name: String(row[xField] ?? '-'),
      value: toNumber(row[yField])
    }))
    .filter((point) => Number.isFinite(point.value));

  return points.length
    ? {
        chartType: result?.chartSuggestion?.chartType || 'bar',
        xField,
        yField,
        points
      }
    : null;
}

function resolveFieldName(fieldName: string | undefined, row: Record<string, unknown>) {
  if (!fieldName) {
    return undefined;
  }
  if (Object.prototype.hasOwnProperty.call(row, fieldName)) {
    return fieldName;
  }
  const normalizedFieldName = normalizeFieldName(fieldName);
  return Object.keys(row).find((key) => normalizeFieldName(key) === normalizedFieldName);
}

function normalizeFieldName(value: string) {
  return value.replace(/[`"'\s]/g, '').toLowerCase();
}

function resolveFallbackXField(row: Record<string, unknown>) {
  const keys = Object.keys(row);
  return keys.find((key) => !Number.isFinite(toNumber(row[key]))) ?? keys[0];
}

function resolveFallbackYField(row: Record<string, unknown>, xField?: string) {
  return Object.keys(row).find((key) => key !== xField && Number.isFinite(toNumber(row[key])));
}

function toNumber(value: unknown) {
  if (typeof value === 'number') {
    return value;
  }
  if (typeof value === 'bigint') {
    return Number(value);
  }
  if (typeof value === 'string') {
    return Number(value.replace(/,/g, ''));
  }
  return Number.NaN;
}

function DataAnalysisChart(props: { chartType?: string; data: ChartPoint[] }) {
  const chartType = (props.chartType || 'bar').toLowerCase();
  if (chartType.includes('pie')) {
    return <PieChart data={props.data} />;
  }
  if (chartType.includes('line')) {
    return <LineChart data={props.data} />;
  }
  return <BarChart data={props.data} />;
}

function BarChart({ data }: { data: ChartPoint[] }) {
  const maxValue = Math.max(...data.map((item) => item.value), 0);

  return (
    <div className="data-analysis-chart data-analysis-bar-chart">
      {data.map((item, index) => {
        const percent = maxValue > 0 ? Math.max((item.value / maxValue) * 100, 2) : 0;
        return (
          <div className="data-analysis-bar-row" key={`${item.name}-${index}`}>
            <div className="data-analysis-bar-name" title={item.name}>
              {item.name}
            </div>
            <div className="data-analysis-bar-track">
              <div className="data-analysis-bar-fill" style={{ width: `${percent}%` }} />
            </div>
            <div className="data-analysis-bar-value">{formatNumber(item.value)}</div>
          </div>
        );
      })}
    </div>
  );
}

function LineChart({ data }: { data: ChartPoint[] }) {
  const width = 720;
  const height = 260;
  const padding = 36;
  const maxValue = Math.max(...data.map((item) => item.value), 0);
  const minValue = Math.min(...data.map((item) => item.value), 0);
  const range = Math.max(maxValue - minValue, 1);
  const points = data.map((item, index) => {
    const x = data.length === 1 ? width / 2 : padding + (index / (data.length - 1)) * (width - padding * 2);
    const y = height - padding - ((item.value - minValue) / range) * (height - padding * 2);
    return { ...item, x, y };
  });
  const polyline = points.map((point) => `${point.x},${point.y}`).join(' ');

  return (
    <div className="data-analysis-chart">
      <svg className="data-analysis-line-svg" viewBox={`0 0 ${width} ${height}`} role="img">
        <line x1={padding} y1={height - padding} x2={width - padding} y2={height - padding} />
        <line x1={padding} y1={padding} x2={padding} y2={height - padding} />
        <polyline points={polyline} />
        {points.map((point, index) => (
          <g key={`${point.name}-${index}`}>
            <circle cx={point.x} cy={point.y} r="4" />
            <text x={point.x} y={point.y - 10} textAnchor="middle">
              {formatNumber(point.value)}
            </text>
          </g>
        ))}
      </svg>
      <div className="data-analysis-chart-axis">
        {data.map((item, index) => (
          <span key={`${item.name}-${index}`}>{item.name}</span>
        ))}
      </div>
    </div>
  );
}

function PieChart({ data }: { data: ChartPoint[] }) {
  const total = data.reduce((sum, item) => sum + Math.max(item.value, 0), 0);
  let accumulated = 0;
  const palette = ['#1677ff', '#13c2c2', '#52c41a', '#faad14', '#f759ab', '#722ed1', '#fa541c'];
  const gradient = total > 0
    ? data
        .map((item, index) => {
          const start = (accumulated / total) * 100;
          accumulated += Math.max(item.value, 0);
          const end = (accumulated / total) * 100;
          return `${palette[index % palette.length]} ${start}% ${end}%`;
        })
        .join(', ')
    : '#d9d9d9 0% 100%';

  return (
    <div className="data-analysis-chart data-analysis-pie-chart">
      <div className="data-analysis-pie" style={{ background: `conic-gradient(${gradient})` }} />
      <div className="data-analysis-pie-legend">
        {data.map((item, index) => (
          <div key={`${item.name}-${index}`} className="data-analysis-pie-legend-item">
            <span style={{ backgroundColor: palette[index % palette.length] }} />
            <Typography.Text>
              {item.name}：{formatNumber(item.value)}
            </Typography.Text>
          </div>
        ))}
      </div>
    </div>
  );
}

function formatNumber(value: number) {
  return new Intl.NumberFormat('zh-CN', {
    maximumFractionDigits: 2
  }).format(value);
}
