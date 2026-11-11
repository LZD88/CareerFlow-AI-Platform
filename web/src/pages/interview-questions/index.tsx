import { useEffect, useState } from 'react';
import { BookOutlined, ReloadOutlined } from '@ant-design/icons';
import { Button, Drawer, Form, Input, Select, Space, Table, Tag, Typography } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import dayjs from 'dayjs';
import { getInterviewQuestionDetail, getInterviewQuestionPage, getInterviewQuestionSources } from '../../api/interview-questions';
import NoPermission from '../../components/NoPermission';
import PageContainer from '../../components/PageContainer';
import PanelTable from '../../components/PanelTable';
import type { InterviewQuestionBank, InterviewQuestionQueryParams, InterviewQuestionSource } from '../../types/interview-question';
import { hasPermission } from '../../utils/permission';

const CATEGORY_OPTIONS = ['Java基础', '项目经验', 'AI', 'RAG', 'Agent', '数据库', '系统设计', '问题排查', '表达沟通', '其他']
  .map((value) => ({ label: value, value }));

const QUALITY_META: Record<string, { label: string; color: string }> = {
  GOOD: { label: '答得较好', color: 'green' },
  PARTIAL: { label: '部分答到', color: 'gold' },
  WRONG: { label: '存在错误', color: 'red' },
  INSUFFICIENT: { label: '回答不足', color: 'orange' },
  UNKNOWN: { label: '无法判断', color: 'default' }
};

export default function InterviewQuestionPage() {
  const canView = hasPermission('menu:system:interview-question') || hasPermission('menu:system:interview-review');
  const [form] = Form.useForm<InterviewQuestionQueryParams>();
  const [loading, setLoading] = useState(false);
  const [records, setRecords] = useState<InterviewQuestionBank[]>([]);
  const [pagination, setPagination] = useState({ current: 1, pageSize: 10, total: 0 });
  const [detailOpen, setDetailOpen] = useState(false);
  const [detail, setDetail] = useState<InterviewQuestionBank>();
  const [sources, setSources] = useState<InterviewQuestionSource[]>([]);

  const fetchPage = async (pageNum = pagination.current, pageSize = pagination.pageSize) => {
    const values = form.getFieldsValue();
    setLoading(true);
    try {
      const response = await getInterviewQuestionPage({
        pageNum,
        pageSize,
        keyword: values.keyword?.trim(),
        category: values.category
      });
      setRecords(response.records);
      setPagination({ current: response.pageNum, pageSize: response.pageSize, total: response.total });
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchPage(1, pagination.pageSize);
  }, []);

  const onOpenDetail = async (id: number) => {
    setDetailOpen(true);
    setDetail(undefined);
    setSources([]);
    const [detailResponse, sourceResponse] = await Promise.all([
      getInterviewQuestionDetail(id),
      getInterviewQuestionSources(id)
    ]);
    setDetail(detailResponse);
    setSources(sourceResponse);
  };

  const columns: ColumnsType<InterviewQuestionBank> = [
    {
      title: '标准问题',
      dataIndex: 'normalizedQuestion',
      render: (value: string, record) => (
        <Button type="link" style={{ padding: 0, height: 'auto', whiteSpace: 'normal', textAlign: 'left' }} onClick={() => onOpenDetail(record.id)}>
          {value}
        </Button>
      )
    },
    {
      title: '分类',
      dataIndex: 'category',
      width: 120,
      render: (value?: string) => value ? <Tag color="blue">{value}</Tag> : '-'
    },
    {
      title: '标签',
      dataIndex: 'tags',
      width: 260,
      render: (tags: string[]) => tags?.length ? tags.slice(0, 4).map((tag) => <Tag key={tag}>{tag}</Tag>) : '-'
    },
    {
      title: '出现次数',
      dataIndex: 'sourceCount',
      width: 100
    },
    {
      title: '更新时间',
      dataIndex: 'updatedAt',
      width: 170,
      render: (value?: string) => value ? dayjs(value).format('YYYY-MM-DD HH:mm') : '-'
    }
  ];

  if (!canView) {
    return <NoPermission />;
  }

  return (
    <PageContainer
      title="面试训练题库"
      description="从真实面试转写中沉淀高价值问题和标准答案。"
      extra={<BookOutlined style={{ fontSize: 24, color: '#1677ff' }} />}
    >
      <Form form={form} layout="inline" className="search-form" onFinish={() => fetchPage(1, pagination.pageSize)}>
        <Form.Item name="keyword" style={{ minWidth: 300 }}>
          <Input placeholder="问题 / 答案 / 标签" allowClear />
        </Form.Item>
        <Form.Item name="category" style={{ minWidth: 160 }}>
          <Select placeholder="分类" allowClear options={CATEGORY_OPTIONS} />
        </Form.Item>
        <Form.Item>
          <Space>
            <Button type="primary" htmlType="submit">查询</Button>
            <Button onClick={() => { form.resetFields(); fetchPage(1, pagination.pageSize); }}>重置</Button>
            <Button icon={<ReloadOutlined />} onClick={() => fetchPage()}>刷新</Button>
          </Space>
        </Form.Item>
      </Form>

      <PanelTable>
        <Table
          rowKey="id"
          loading={loading}
          columns={columns}
          dataSource={records}
          pagination={{
            current: pagination.current,
            pageSize: pagination.pageSize,
            total: pagination.total,
            showSizeChanger: true,
            showTotal: (total) => `共 ${total} 条`,
            onChange: (page, pageSize) => fetchPage(page, pageSize)
          }}
        />
      </PanelTable>

      <Drawer title="题目详情" open={detailOpen} width={860} onClose={() => setDetailOpen(false)}>
        {detail ? (
          <Space direction="vertical" size="large" style={{ width: '100%' }}>
            <Typography.Title level={4} style={{ margin: 0 }}>{detail.normalizedQuestion}</Typography.Title>
            <Space wrap>
              {detail.category ? <Tag color="blue">{detail.category}</Tag> : null}
              {detail.tags.map((tag) => <Tag key={tag}>{tag}</Tag>)}
              <Typography.Text type="secondary">出现 {detail.sourceCount} 次</Typography.Text>
            </Space>
            <section>
              <Typography.Title level={5}>标准答案</Typography.Title>
              <Typography.Paragraph style={{ whiteSpace: 'pre-wrap' }}>{detail.standardAnswer}</Typography.Paragraph>
            </section>
            {detail.shortAnswer ? (
              <section>
                <Typography.Title level={5}>简短回答</Typography.Title>
                <Typography.Paragraph>{detail.shortAnswer}</Typography.Paragraph>
              </section>
            ) : null}
            <section>
              <Typography.Title level={5}>可能追问</Typography.Title>
              {detail.followUpQuestions.length ? detail.followUpQuestions.map((item) => <Typography.Paragraph key={item}>- {item}</Typography.Paragraph>) : '-'}
            </section>
            <section>
              <Typography.Title level={5}>常见扣分点</Typography.Title>
              {detail.pitfalls.length ? detail.pitfalls.map((item) => <Typography.Paragraph key={item}>- {item}</Typography.Paragraph>) : '-'}
            </section>
            <section>
              <Typography.Title level={5}>来源面试</Typography.Title>
              {sources.length ? (
                <Space direction="vertical" size="middle" style={{ width: '100%' }}>
                  {sources.map((source) => (
                    <div key={source.id} style={{ border: '1px solid #f0f0f0', borderRadius: 8, padding: 16 }}>
                      <Space direction="vertical" size={8} style={{ width: '100%' }}>
                        <Space wrap>
                          <Typography.Text strong>{source.taskTitle || `任务 #${source.taskId}`}</Typography.Text>
                          {source.interviewRole ? <Tag color="cyan">{source.interviewRole}</Tag> : null}
                          <Typography.Text type="secondary">{dayjs(source.createdAt).format('YYYY-MM-DD HH:mm')}</Typography.Text>
                        </Space>
                        {source.originalFilename ? (
                          <Typography.Text type="secondary">{source.originalFilename}</Typography.Text>
                        ) : null}
                        <Typography.Paragraph style={{ marginBottom: 0 }}>
                          <Typography.Text strong>原始问题：</Typography.Text>
                          {source.originalQuestion}
                        </Typography.Paragraph>
                        {source.answerContext ? (
                          <Typography.Paragraph style={{ marginBottom: 0, whiteSpace: 'pre-wrap' }}>
                            <Typography.Text strong>回答摘要：</Typography.Text>
                            {source.answerContext}
                          </Typography.Paragraph>
                        ) : null}
                        <Space wrap>
                          {source.studentAnswerQuality ? (
                            <Tag color={QUALITY_META[source.studentAnswerQuality]?.color || 'default'}>
                              {QUALITY_META[source.studentAnswerQuality]?.label || source.studentAnswerQuality}
                            </Tag>
                          ) : null}
                          {typeof source.studentAnswerScore === 'number' ? (
                            <Typography.Text type="secondary">评分 {source.studentAnswerScore}/100</Typography.Text>
                          ) : null}
                        </Space>
                        {source.studentAnswerAnalysis ? (
                          <Typography.Paragraph style={{ marginBottom: 0, whiteSpace: 'pre-wrap' }}>
                            <Typography.Text strong>答题分析：</Typography.Text>
                            {source.studentAnswerAnalysis}
                          </Typography.Paragraph>
                        ) : null}
                        {source.improvementSuggestion ? (
                          <Typography.Paragraph style={{ marginBottom: 0, whiteSpace: 'pre-wrap' }}>
                            <Typography.Text strong>改进建议：</Typography.Text>
                            {source.improvementSuggestion}
                          </Typography.Paragraph>
                        ) : null}
                      </Space>
                    </div>
                  ))}
                </Space>
              ) : (
                <Typography.Text type="secondary">暂无来源记录</Typography.Text>
              )}
            </section>
          </Space>
        ) : (
          <Typography.Text type="secondary">加载中...</Typography.Text>
        )}
      </Drawer>
    </PageContainer>
  );
}
