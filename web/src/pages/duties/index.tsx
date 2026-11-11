import { useEffect, useMemo, useState } from 'react';
import { Button, DatePicker, Empty, Input, Select, Space, Spin, Table, Typography, message } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import dayjs, { type Dayjs } from 'dayjs';
import NoPermission from '../../components/NoPermission';
import PageContainer from '../../components/PageContainer';
import { getDutyManagementDay, saveClassDuty, saveGlobalDuty } from '../../api/duties';
import type { ClassDutyItem, DutyManagementDayRes, GlobalDutyItem } from '../../types/system';
import { hasPermission } from '../../utils/permission';

interface ClassDutyDraft {
  teacherId?: number;
  remark?: string;
}

interface GlobalDutyDraft {
  teacherId?: number;
  remark?: string;
}

export default function DutyManagementPage() {
  const canView = hasPermission('menu:teaching:duty');
  const canSave = hasPermission('button:duty:save');
  const [selectedDate, setSelectedDate] = useState<Dayjs>(dayjs());
  const [loading, setLoading] = useState(false);
  const [savingGlobal, setSavingGlobal] = useState(false);
  const [savingClassIds, setSavingClassIds] = useState<number[]>([]);
  const [data, setData] = useState<DutyManagementDayRes>();
  const [classDrafts, setClassDrafts] = useState<Record<number, ClassDutyDraft>>({});
  const [globalDraft, setGlobalDraft] = useState<GlobalDutyDraft>({});

  const teacherOptions = useMemo(
    () => (data?.teacherOptions ?? []).map((item) => ({ label: item.teacherName, value: item.teacherId })),
    [data]
  );

  const fetchData = async (date = selectedDate) => {
    setLoading(true);
    try {
      const result = await getDutyManagementDay(date.format('YYYY-MM-DD'));
      setData(result);
      setClassDrafts(
        result.classDuties.reduce<Record<number, ClassDutyDraft>>((acc, item) => {
          acc[item.classId] = { teacherId: item.teacherId, remark: item.remark };
          return acc;
        }, {})
      );
      setGlobalDraft({
        teacherId: result.globalDuty.teacherId,
        remark: result.globalDuty.remark
      });
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (!canView) {
      return;
    }
    fetchData();
  }, [canView]);

  if (!canView) {
    return <NoPermission />;
  }

  const updateClassDraft = (classId: number, patch: Partial<ClassDutyDraft>) => {
    setClassDrafts((current) => ({
      ...current,
      [classId]: { ...current[classId], ...patch }
    }));
  };

  const handleSaveClassDuty = async (record: ClassDutyItem) => {
    const draft = classDrafts[record.classId];
    if (!draft?.teacherId) {
      message.warning('请先选择值班老师');
      return;
    }
    setSavingClassIds((current) => [...current, record.classId]);
    try {
      await saveClassDuty(record.classId, {
        dutyDate: data?.dutyDate ?? selectedDate.format('YYYY-MM-DD'),
        teacherId: draft.teacherId,
        remark: draft.remark?.trim()
      });
      message.success('班级值班已保存');
      fetchData(selectedDate);
    } finally {
      setSavingClassIds((current) => current.filter((item) => item !== record.classId));
    }
  };

  const handleSaveGlobalDuty = async () => {
    if (!globalDraft.teacherId) {
      message.warning('请先选择全局值班老师');
      return;
    }
    setSavingGlobal(true);
    try {
      await saveGlobalDuty({
        dutyDate: data?.dutyDate ?? selectedDate.format('YYYY-MM-DD'),
        teacherId: globalDraft.teacherId,
        remark: globalDraft.remark?.trim()
      });
      message.success('全局值班已保存');
      fetchData(selectedDate);
    } finally {
      setSavingGlobal(false);
    }
  };

  const columns: ColumnsType<ClassDutyItem> = [
    {
      title: '班级',
      dataIndex: 'classLabel',
      width: 220,
      render: (_, record) => (
        <div className="duty-class-cell">
          <div className="duty-class-title">{record.classLabel}</div>
          <div className="duty-class-subtitle">{record.campusName || record.courseName || '未设置校区'}</div>
        </div>
      )
    },
    { title: '开班时间', dataIndex: 'startTime', width: 140 },
    { title: '预计毕业', dataIndex: 'graduateDate', width: 140 },
    {
      title: '值班时段',
      dataIndex: 'dutyTimeRange',
      width: 180,
      render: (_, record) => (
        <div className="duty-shift-cell">
          <span className="duty-shift-tag">{record.dutyLabel}</span>
          <span className="duty-shift-time">{record.dutyTimeRange}</span>
        </div>
      )
    },
    {
      title: '值班老师',
      dataIndex: 'teacherId',
      width: 240,
      render: (_, record) => (
        <Select
          allowClear
          showSearch
          placeholder="请选择老师"
          optionFilterProp="label"
          value={classDrafts[record.classId]?.teacherId}
          options={teacherOptions}
          onChange={(value) => updateClassDraft(record.classId, { teacherId: value })}
        />
      )
    },
    {
      title: '备注',
      dataIndex: 'remark',
      width: 220,
      render: (_, record) => (
        <Input
          placeholder="选填备注"
          value={classDrafts[record.classId]?.remark}
          onChange={(event) => updateClassDraft(record.classId, { remark: event.target.value })}
        />
      )
    },
    {
      title: '操作',
      key: 'action',
      fixed: 'right',
      width: 100,
      render: (_, record) => (
        <Button
          type="link"
          disabled={!canSave}
          loading={savingClassIds.includes(record.classId)}
          onClick={() => handleSaveClassDuty(record)}
        >
          保存
        </Button>
      )
    }
  ];

  return (
    <PageContainer
      title="值班安排"
      description="仅展示当天有上课、加课或自习安排的班级。19:00-21:00 为班级值班，21:00-22:30 为全局值班。"
    >
      <div className="duty-toolbar-card">
        <Space wrap>
          <DatePicker
            value={selectedDate}
            onChange={(value) => {
              if (!value) {
                return;
              }
              setSelectedDate(value);
              fetchData(value);
            }}
          />
        </Space>
      </div>

      <Spin spinning={loading}>
        <div className="duty-global-card">
          <div className="duty-section-head">
            <div>
              <Typography.Title level={5} style={{ margin: 0 }}>
                21:00-22:30 全局值班
              </Typography.Title>
              <Typography.Text type="secondary">当天仅需安排一位老师负责全局巡查和值守。</Typography.Text>
            </div>
          </div>
          <div className="duty-global-form">
            <div className="duty-global-field">
              <span className="duty-global-label">{data?.globalDuty.dutyLabel || '全局值班'}</span>
              <span className="duty-global-time">{data?.globalDuty.dutyTimeRange || '21:00-22:30'}</span>
            </div>
            <Select
              allowClear
              showSearch
              placeholder="请选择老师"
              optionFilterProp="label"
              value={globalDraft.teacherId}
              options={teacherOptions}
              onChange={(value) => setGlobalDraft((current) => ({ ...current, teacherId: value }))}
            />
            <Input
              placeholder="选填备注"
              value={globalDraft.remark}
              onChange={(event) => setGlobalDraft((current) => ({ ...current, remark: event.target.value }))}
            />
            <Button type="primary" disabled={!canSave} loading={savingGlobal} onClick={handleSaveGlobalDuty}>
              保存全局值班
            </Button>
          </div>
        </div>

        <div className="duty-class-card">
          <div className="duty-section-head">
            <div>
              <Typography.Title level={5} style={{ margin: 0 }}>
                19:00-21:00 班级/自习值班
              </Typography.Title>
              <Typography.Text type="secondary">一个老师允许同时值多个班，逐行保存即可。</Typography.Text>
            </div>
          </div>

          {data?.classDuties.length ? (
            <Table
              rowKey="classId"
              columns={columns}
              dataSource={data?.classDuties}
              pagination={false}
              scroll={{ x: 1180 }}
            />
          ) : (
            <Empty description="当天没有需要安排值班的班级" />
          )}
        </div>
      </Spin>
    </PageContainer>
  );
}
