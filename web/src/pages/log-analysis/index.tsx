import { useState } from 'react';
import { Alert, Button, Card, DatePicker, Form, Input, Space, Table, Tag, Timeline, Typography, message } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import dayjs, { type Dayjs } from 'dayjs';
import NoPermission from '../../components/NoPermission';
import PageContainer from '../../components/PageContainer';
import PanelTable from '../../components/PanelTable';
import { diagnoseLogsStream, searchLogs } from '../../api/log-analysis';
import type { LogDiagnosisRes, LogDiagnosisStreamEvent, LogRecordRes, LogSearchRes } from '../../types/log-analysis';
import { hasPermission } from '../../utils/permission';

const { RangePicker } = DatePicker;
const DEFAULT_RANGE: [Dayjs, Dayjs] = [dayjs().subtract(1, 'hour'), dayjs()];

interface SearchFormValues {
  query?: string;
  timeRange: [Dayjs, Dayjs];
}

interface DiagnoseFormValues {
  guid: string;
}

export default function LogAnalysisPage() {
  const canView = hasPermission('menu:system:log-analysis');
  const [form] = Form.useForm<SearchFormValues>();
  const [diagnoseForm] = Form.useForm<DiagnoseFormValues>();
  const [loading, setLoading] = useState(false);
  const [diagnoseLoading, setDiagnoseLoading] = useState(false);
  const [result, setResult] = useState<LogSearchRes>();
  const [diagnosis, setDiagnosis] = useState<LogDiagnosisRes>();
  const [diagnoseEvents, setDiagnoseEvents] = useState<LogDiagnosisStreamEvent[]>([]);

  const onSearch = async () => {
    const values = await form.validateFields();
    setLoading(true);
    try {
      const response = await searchLogs({
        query: values.query?.trim(),
        startTime: values.timeRange[0].format('YYYY-MM-DD HH:mm:ss'),
        endTime: values.timeRange[1].format('YYYY-MM-DD HH:mm:ss')
      });
      setResult(response);
    } finally {
      setLoading(false);
    }
  };

  const onReset = () => {
    form.setFieldsValue({
      query: '',
      timeRange: DEFAULT_RANGE
    });
    setResult(undefined);
  };

  const onDiagnose = async () => {
    const values = await diagnoseForm.validateFields();
    setDiagnoseLoading(true);
    setDiagnosis(undefined);
    setDiagnoseEvents([]);
    try {
      await diagnoseLogsStream(
        {
          guid: values.guid.trim()
        },
        {
          onEvent: (event) => {
            setDiagnoseEvents((previous) => [...previous, event]);
            if (event.stage === 'DONE' && event.data) {
              setDiagnosis(event.data as LogDiagnosisRes);
            }
            if (event.status === 'ERROR') {
              message.error(event.message || '流式诊断失败');
            }
          },
          onDone: () => {
            setDiagnoseLoading(false);
          }
        }
      );
    } catch (error: any) {
      message.error(error?.message || '流式诊断失败');
    } finally {
      setDiagnoseLoading(false);
    }
  };

  const onDiagnoseReset = () => {
    diagnoseForm.resetFields();
    setDiagnosis(undefined);
    setDiagnoseEvents([]);
  };

  const timelineItems = diagnoseEvents.map((event, index) => ({
    key: `${event.stage}-${index}`,
    color: event.status === 'ERROR' ? 'red' : event.status === 'DONE' ? 'green' : 'blue',
    children: (
      <Space direction="vertical" size={2}>
        <Typography.Text strong>{event.message}</Typography.Text>
        <Typography.Text type="secondary">
          {event.stage} / {event.status}
          {event.timestamp ? ` / ${dayjs(event.timestamp).format('HH:mm:ss')}` : ''}
        </Typography.Text>
      </Space>
    )
  }));

  const columns: ColumnsType<LogRecordRes> = [
    {
      title: '时间',
      dataIndex: 'time',
      width: 180,
      render: (value?: string) => formatLogTime(value)
    },
    {
      title: '日志内容',
      dataIndex: 'content',
      render: (value?: string) => (
        <Typography.Paragraph style={{ marginBottom: 0, whiteSpace: 'pre-wrap' }}>
          {value || '-'}
        </Typography.Paragraph>
      )
    }
  ];

  if (!canView) {
    return <NoPermission />;
  }

  return (
    <PageContainer
      title="运行日志诊断"
      description="检索平台运行日志，按请求 GUID 定位报名、教学与就业服务中的异常。"
    >
      <Form
        form={form}
        layout="inline"
        className="search-form"
        initialValues={{ query: '', timeRange: DEFAULT_RANGE }}
        onFinish={onSearch}
      >
        <Form.Item name="query" style={{ minWidth: 320 }}>
          <Input placeholder="请输入检索语句，例如 guid:xxx 或 /api/auth/change-password" allowClear />
        </Form.Item>
        <Form.Item
          name="timeRange"
          rules={[{ required: true, message: '请选择时间范围' }]}
        >
          <RangePicker showTime format="YYYY-MM-DD HH:mm:ss" />
        </Form.Item>
        <Form.Item>
          <Space>
            <Button type="primary" htmlType="submit" loading={loading}>
              查询日志
            </Button>
            <Button onClick={onReset}>
              重置
            </Button>
          </Space>
        </Form.Item>
      </Form>

      <Card title="按 GUID 诊断" style={{ marginTop: 16 }}>
        <Form form={diagnoseForm} layout="inline" onFinish={onDiagnose}>
          <Form.Item
            name="guid"
            rules={[{ required: true, message: '请输入 guid' }]}
            style={{ minWidth: 360 }}
          >
            <Input placeholder="请输入请求 guid，例如 f6c67cb7a026449587e84b51f94544a8" allowClear />
          </Form.Item>
          <Form.Item>
            <Space>
              <Button type="primary" htmlType="submit" loading={diagnoseLoading}>
                诊断
              </Button>
              <Button onClick={onDiagnoseReset}>
                重置
              </Button>
            </Space>
          </Form.Item>
        </Form>

        {diagnoseEvents.length ? (
          <Card size="small" title="诊断进度" style={{ marginTop: 16 }}>
            <Timeline items={timelineItems} />
          </Card>
        ) : null}

        {diagnosis ? (
          <Space direction="vertical" size={12} style={{ width: '100%', marginTop: 16 }}>
            <Space size={[12, 12]} wrap>
              <Tag color={diagnosis.analysisSource === 'PYTHON_AGENT' ? 'green' : 'gold'}>
                分析来源：{diagnosis.analysisSource || 'UNKNOWN'}
              </Tag>
              <Tag color="processing">命中日志数：{diagnosis.matchedLogCount ?? 0}</Tag>
            </Space>
            <Alert
              type={diagnosis.analysisSource === 'PYTHON_AGENT' ? 'success' : 'info'}
              showIcon
              message="根因判断"
              description={diagnosis.rootCause || diagnosis.possibleReason || diagnosis.simulatedReason || '暂无诊断结果'}
            />
            <Card size="small" title="置信度与证据">
              <Space direction="vertical" size={8} style={{ width: '100%' }}>
                <Typography.Paragraph style={{ marginBottom: 0 }}>
                  <strong>置信度：</strong>
                  {typeof diagnosis.confidence === 'number' ? `${Math.round(diagnosis.confidence * 100)}%` : '-'}
                </Typography.Paragraph>
                <Typography.Paragraph style={{ marginBottom: 0 }}>
                  <strong>需要补充信息：</strong>{diagnosis.needMoreInfo ? '是' : '否'}
                </Typography.Paragraph>
                {diagnosis.needMoreInfo || diagnosis.missingInfo?.length ? (
                  <Typography.Paragraph style={{ marginBottom: 0, whiteSpace: 'pre-wrap' }}>
                    <strong>可补查信息：</strong>
                    {diagnosis.missingInfo?.length
                      ? `\n${diagnosis.missingInfo.map((item, index) => `${index + 1}. ${item}`).join('\n')}`
                      : ' -'}
                  </Typography.Paragraph>
                ) : null}
                {diagnosis.manualCheckItems?.length ? (
                  <Typography.Paragraph style={{ marginBottom: 0, whiteSpace: 'pre-wrap' }}>
                    <strong>人工核对项：</strong>
                    {`\n${diagnosis.manualCheckItems.map((item, index) => `${index + 1}. ${item}`).join('\n')}`}
                  </Typography.Paragraph>
                ) : null}
                <Typography.Paragraph style={{ marginBottom: 0, whiteSpace: 'pre-wrap' }}>
                  <strong>证据：</strong>
                  {diagnosis.evidenceLogs?.length
                    ? `\n${diagnosis.evidenceLogs.map((item, index) => `${index + 1}. ${item}`).join('\n')}`
                    : ' -'}
                </Typography.Paragraph>
              </Space>
            </Card>
            {diagnosis.analysisSummary ? (
              <Card size="small" title="分析摘要">
                <Typography.Paragraph style={{ marginBottom: 0, whiteSpace: 'pre-wrap' }}>
                  {diagnosis.analysisSummary}
                </Typography.Paragraph>
              </Card>
            ) : null}
            <Card size="small" title="结构化结果">
              <Typography.Paragraph style={{ marginBottom: 8 }}>
                <strong>GUID：</strong>{diagnosis.guid}
              </Typography.Paragraph>
              <Typography.Paragraph style={{ marginBottom: 8 }}>
                <strong>请求路径：</strong>{diagnosis.requestUri || '-'}
              </Typography.Paragraph>
              <Typography.Paragraph style={{ marginBottom: 8 }}>
                <strong>请求方法：</strong>{diagnosis.httpMethod || '-'}
              </Typography.Paragraph>
              <Typography.Paragraph style={{ marginBottom: 8 }}>
                <strong>控制器：</strong>{diagnosis.controller || '-'}
              </Typography.Paragraph>
              <Typography.Paragraph style={{ marginBottom: 8, whiteSpace: 'pre-wrap' }}>
                <strong>请求参数：</strong>{diagnosis.requestArgs || '-'}
              </Typography.Paragraph>
              <Typography.Paragraph style={{ marginBottom: 8 }}>
                <strong>异常类型：</strong>{diagnosis.exceptionClass || '-'}
              </Typography.Paragraph>
              <Typography.Paragraph style={{ marginBottom: 8 }}>
                <strong>异常信息：</strong>{diagnosis.exceptionMessage || '-'}
              </Typography.Paragraph>
              <Typography.Paragraph style={{ marginBottom: 8 }}>
                <strong>首个堆栈：</strong>{diagnosis.topStackFrame || '-'}
              </Typography.Paragraph>
              <Typography.Paragraph style={{ marginBottom: 0 }}>
                <strong>查询语句：</strong>{diagnosis.query || '-'}
              </Typography.Paragraph>
            </Card>
            {(diagnosis.relatedFiles?.length || diagnosis.nextActions?.length || diagnosis.relatedCode?.length || diagnosis.suggestions?.length) ? (
              <Card size="small" title="诊断补充">
                <Space direction="vertical" size={8} style={{ width: '100%' }}>
                  <Typography.Paragraph style={{ marginBottom: 0 }}>
                    <strong>相关文件/位置：</strong>
                    {(diagnosis.relatedFiles?.length ? diagnosis.relatedFiles : diagnosis.relatedCode)?.length
                      ? (diagnosis.relatedFiles?.length ? diagnosis.relatedFiles : diagnosis.relatedCode)?.join('，')
                      : '-'}
                  </Typography.Paragraph>
                  <Typography.Paragraph style={{ marginBottom: 0, whiteSpace: 'pre-wrap' }}>
                    <strong>下一步动作：</strong>
                    {(diagnosis.nextActions?.length ? diagnosis.nextActions : diagnosis.suggestions)?.length
                      ? `\n${(diagnosis.nextActions?.length ? diagnosis.nextActions : diagnosis.suggestions)?.map((item, index) => `${index + 1}. ${item}`).join('\n')}`
                      : ' -'}
                  </Typography.Paragraph>
                </Space>
              </Card>
            ) : null}
            <Card size="small" title="原始日志">
              <Typography.Paragraph style={{ marginBottom: 0, whiteSpace: 'pre-wrap' }}>
                {(diagnosis.rawLogs || []).join('\n\n') || '-'}
              </Typography.Paragraph>
            </Card>
          </Space>
        ) : null}
      </Card>

      {result ? (
        <Space direction="vertical" size={16} style={{ width: '100%' }}>
          <Card>
            <Space size={[12, 12]} wrap>
              <Tag color="blue">状态：{result.resultStatus || '-'}</Tag>
              <Tag color="processing">命中数：{result.hitCount ?? 0}</Tag>
              <Tag color="default">返回数：{result.logs.length}</Tag>
              <Tag color={result.isAnalysis ? 'gold' : 'green'}>
                {result.isAnalysis ? '分析查询' : '普通查询'}
              </Tag>
            </Space>
            <Alert
              style={{ marginTop: 16 }}
              type="info"
              showIcon
              message="当前页面默认不暴露 topicId 和 limit，topic 由后端系统配置提供，limit 使用后端默认值。"
            />
          </Card>

          <PanelTable>
            <Table
              rowKey={(_, index) => index ?? 0}
              loading={loading}
              columns={columns}
              dataSource={result.logs}
              pagination={false}
              scroll={{ x: 960 }}
            />
          </PanelTable>

          {result.rawResponse ? (
            <Card title="原始响应">
              <Typography.Paragraph style={{ marginBottom: 0, whiteSpace: 'pre-wrap' }}>
                {result.rawResponse}
              </Typography.Paragraph>
            </Card>
          ) : null}
        </Space>
      ) : null}
    </PageContainer>
  );
}

function formatLogTime(value?: string) {
  if (!value) {
    return '-';
  }
  if (/^\d{13}$/.test(value)) {
    return dayjs(Number(value)).format('YYYY-MM-DD HH:mm:ss.SSS');
  }
  if (/^\d{10}$/.test(value)) {
    return dayjs(Number(value) * 1000).format('YYYY-MM-DD HH:mm:ss');
  }
  return value;
}
