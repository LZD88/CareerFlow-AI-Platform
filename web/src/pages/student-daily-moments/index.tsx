import { PlayCircleOutlined } from '@ant-design/icons';
import { Button, Empty, Form, Image, Input, Modal, Space, Table, Tag } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import dayjs from 'dayjs';
import { useEffect, useMemo, useState } from 'react';
import NoPermission from '../../components/NoPermission';
import PageContainer from '../../components/PageContainer';
import PanelTable from '../../components/PanelTable';
import { getStudentDailyMomentPage } from '../../api/student-daily-moments';
import type { StudentDailyMomentAdminItem, StudentDailyMomentAdminQueryParams } from '../../types/system';
import { hasPermission } from '../../utils/permission';

interface SearchValues {
  studentCode?: string;
  studentName?: string;
  title?: string;
}

export default function StudentDailyMomentPage() {
  const canView = hasPermission('menu:student:daily-moment');
  const [searchForm] = Form.useForm<SearchValues>();
  const [loading, setLoading] = useState(false);
  const [dataSource, setDataSource] = useState<StudentDailyMomentAdminItem[]>([]);
  const [currentItem, setCurrentItem] = useState<StudentDailyMomentAdminItem | null>(null);
  const [pageNum, setPageNum] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [total, setTotal] = useState(0);

  const fetchData = async () => {
    setLoading(true);
    try {
      const values = searchForm.getFieldsValue();
      const result = await getStudentDailyMomentPage({ pageNum, pageSize, ...values } as StudentDailyMomentAdminQueryParams);
      setDataSource(result.records);
      setTotal(result.total);
    } finally {
      setLoading(false);
    }
  };

  const resetSearch = () => {
    searchForm.resetFields();
    if (pageNum !== 1) {
      setPageNum(1);
      return;
    }
    fetchData();
  };

  useEffect(() => {
    if (!canView) {
      return;
    }
    fetchData();
  }, [canView, pageNum, pageSize]);

  const columns: ColumnsType<StudentDailyMomentAdminItem> = [
    {
      title: '学员信息',
      dataIndex: 'studentName',
      width: 250,
      render: (_, record) => (
        <div>
          <div style={{ fontWeight: 700, color: '#2f241d', marginBottom: 6 }}>{record.studentName || '-'}</div>
          <div style={{ color: '#7f6a5f', marginBottom: 4 }}>{record.studentCode || '-'}</div>
          <div style={{ color: '#7f6a5f' }}>
            {record.classTerm ? `第${record.classTerm}期` : '未分班'}
            {record.campusName ? ` - ${record.campusName}` : ''}
          </div>
        </div>
      )
    },
    { title: '主题', dataIndex: 'title', width: 180, render: (value) => value || '-' },
    {
      title: '图片/视频',
      dataIndex: 'fileUrls',
      width: 160,
      render: (_, record) => <MomentPreviewCell item={record} onOpen={() => setCurrentItem(record)} />
    },
    {
      title: '随想',
      dataIndex: 'contentText',
      width: 420,
      render: (value) => (value ? <div style={{ whiteSpace: 'pre-wrap' }}>{value}</div> : '-')
    },
    {
      title: '素材时间',
      dataIndex: 'recordTime',
      width: 180,
      render: (value) => (value ? dayjs(value).format('YYYY-MM-DD HH:mm:ss') : '-')
    },
    {
      title: '提交时间',
      dataIndex: 'createTime',
      width: 180,
      render: (value) => (value ? dayjs(value).format('YYYY-MM-DD HH:mm:ss') : '-')
    }
  ];

  if (!canView) {
    return <NoPermission />;
  }

  return (
    <PageContainer title="学员动态" description="查看学员在学员端提交的图片、视频和随想记录。">
      <Form form={searchForm} layout="inline" className="search-form" onFinish={fetchData}>
        <Form.Item name="studentCode" label="学员编号">
          <Input allowClear placeholder="请输入学员编号" style={{ width: 220 }} />
        </Form.Item>
        <Form.Item name="studentName" label="学员姓名">
          <Input allowClear placeholder="请输入学员姓名" style={{ width: 220 }} />
        </Form.Item>
        <Form.Item name="title" label="主题">
          <Input allowClear placeholder="请输入主题" style={{ width: 280 }} />
        </Form.Item>
        <Form.Item>
          <Space>
            <Button type="primary" htmlType="submit">
              查询
            </Button>
            <Button onClick={resetSearch}>重置</Button>
          </Space>
        </Form.Item>
      </Form>

      <PanelTable>
        <Table
          rowKey="id"
          loading={loading}
          columns={columns}
          dataSource={dataSource}
          scroll={{ x: 1420 }}
          pagination={{
            current: pageNum,
            pageSize,
            total,
            showSizeChanger: true,
            onChange: (page, size) => {
              setPageNum(page);
              setPageSize(size);
            }
          }}
        />
      </PanelTable>

      <StudentDailyMomentDetailModal item={currentItem} onClose={() => setCurrentItem(null)} />
    </PageContainer>
  );
}

function MomentPreviewCell({ item, onOpen }: { item: StudentDailyMomentAdminItem; onOpen: () => void }) {
  const previewUrl = item.fileUrls?.[0];
  const isImage = previewUrl ? matchesFileExt(previewUrl, ['png', 'jpg', 'jpeg', 'gif', 'webp']) : false;
  const isVideo = previewUrl ? matchesFileExt(previewUrl, ['mp4', 'mov', 'm4v', 'webm']) : false;

  if (!previewUrl) {
    return <span>-</span>;
  }

  return (
    <div
      onClick={onOpen}
      style={{
        width: 84,
        height: 84,
        borderRadius: 16,
        overflow: 'hidden',
        background: '#111a33',
        cursor: 'pointer',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center'
      }}
    >
      {isImage ? (
        <img src={previewUrl} alt={item.title || '素材预览'} style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
      ) : isVideo ? (
        <div style={{ color: '#fff', fontSize: 32, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
          <PlayCircleOutlined />
        </div>
      ) : (
        <Tag color="default">文件</Tag>
      )}
    </div>
  );
}

function StudentDailyMomentDetailModal({
  item,
  onClose
}: {
  item: StudentDailyMomentAdminItem | null;
  onClose: () => void;
}) {
  const imageUrls = useMemo(
    () => (item?.fileUrls ?? []).filter((url) => matchesFileExt(url, ['png', 'jpg', 'jpeg', 'gif', 'webp'])),
    [item]
  );
  const videoUrls = useMemo(
    () => (item?.fileUrls ?? []).filter((url) => matchesFileExt(url, ['mp4', 'mov', 'm4v', 'webm'])),
    [item]
  );
  const otherUrls = useMemo(
    () => (item?.fileUrls ?? []).filter((url) => !matchesFileExt(url, ['png', 'jpg', 'jpeg', 'gif', 'webp', 'mp4', 'mov', 'm4v', 'webm'])),
    [item]
  );

  return (
    <Modal open={Boolean(item)} onCancel={onClose} footer={null} destroyOnClose width={920} title={item?.title || '剪影详情'}>
      {item ? (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 20 }}>
          <div style={{ display: 'flex', flexWrap: 'wrap', gap: 10, color: '#7f6a5f' }}>
            <Tag color="orange" bordered={false}>
              {item.fileUrls?.length ? `${item.fileUrls.length} 个素材` : '纯文字'}
            </Tag>
            <span>{item.studentName || '-'}</span>
            <span>{item.studentCode || '-'}</span>
            <span>{item.recordTime ? dayjs(item.recordTime).format('YYYY-MM-DD HH:mm:ss') : '-'}</span>
            <span>{item.createTime ? `提交于 ${dayjs(item.createTime).format('YYYY-MM-DD HH:mm:ss')}` : ''}</span>
          </div>

          <div style={{ padding: '16px 18px', borderRadius: 16, background: '#f8f3ef', whiteSpace: 'pre-wrap' }}>
            {item.contentText || '这条剪影没有填写随想。'}
          </div>

          {imageUrls.length ? (
            <div>
              <div style={{ marginBottom: 12, fontWeight: 700 }}>图片素材</div>
              <Image.PreviewGroup>
                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))', gap: 12 }}>
                  {imageUrls.map((url) => (
                    <Image key={url} src={url} alt={item.title || '图片素材'} />
                  ))}
                </div>
              </Image.PreviewGroup>
            </div>
          ) : null}

          {videoUrls.length ? (
            <div>
              <div style={{ marginBottom: 12, fontWeight: 700 }}>视频素材</div>
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(260px, 1fr))', gap: 16 }}>
                {videoUrls.map((url) => (
                  <video
                    key={url}
                    src={url}
                    controls
                    preload="metadata"
                    style={{ width: '100%', maxHeight: 420, borderRadius: 18, background: '#000' }}
                  />
                ))}
              </div>
            </div>
          ) : null}

          {otherUrls.length ? (
            <div>
              <div style={{ marginBottom: 12, fontWeight: 700 }}>其他文件</div>
              <Space direction="vertical">
                {otherUrls.map((url) => (
                  <a key={url} href={url} target="_blank" rel="noreferrer">
                    {url}
                  </a>
                ))}
              </Space>
            </div>
          ) : null}
        </div>
      ) : null}
    </Modal>
  );
}

function matchesFileExt(url: string, exts: string[]) {
  const pathname = getUrlPathname(url).toLowerCase();
  return exts.some((ext) => pathname.endsWith(`.${ext}`));
}

function getUrlPathname(url: string) {
  try {
    return new URL(url).pathname;
  } catch {
    return url;
  }
}
