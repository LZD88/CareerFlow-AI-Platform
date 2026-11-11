import { useEffect, useState } from 'react';
import { Button, DatePicker, Empty, Input, Space, Spin, Typography } from 'antd';
import dayjs, { type Dayjs } from 'dayjs';
import NoPermission from '../../components/NoPermission';
import PageContainer from '../../components/PageContainer';
import { getTeacherScheduleCalendar } from '../../api/teacher-schedules';
import type { TeacherScheduleBlock, TeacherScheduleCalendarRes, TeacherScheduleTeacherRow } from '../../types/system';
import { hasPermission } from '../../utils/permission';

const { RangePicker } = DatePicker;

function getDefaultRange(): [Dayjs, Dayjs] {
  const now = dayjs();
  return [now.startOf('month'), now.endOf('month')];
}

export default function TeacherSchedulePage() {
  const canView = hasPermission('menu:teaching:teacher-schedule');
  const [range, setRange] = useState<[Dayjs, Dayjs]>(getDefaultRange);
  const [keyword, setKeyword] = useState('');
  const [keywordInput, setKeywordInput] = useState('');
  const [loading, setLoading] = useState(false);
  const [data, setData] = useState<TeacherScheduleCalendarRes>();

  const fetchData = async (nextRange = range, nextKeyword = keyword) => {
    setLoading(true);
    try {
      const result = await getTeacherScheduleCalendar({
        startDate: nextRange[0].format('YYYY-MM-DD'),
        endDate: nextRange[1].format('YYYY-MM-DD'),
        keyword: nextKeyword || undefined
      });
      setData(result);
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

  const onSearch = () => {
    setKeyword(keywordInput.trim());
    fetchData(range, keywordInput.trim());
  };

  const onReset = () => {
    const nextRange = getDefaultRange();
    setRange(nextRange);
    setKeyword('');
    setKeywordInput('');
    fetchData(nextRange, '');
  };

  return (
    <PageContainer
      title="师资安排"
      description="按老师视角查看排课分布，快速识别有课区间和空闲老师。"
      extra={(
        <Space wrap className="teacher-schedule-toolbar">
          <Input
            allowClear
            value={keywordInput}
            placeholder="搜索老师姓名"
            className="teacher-schedule-search"
            onChange={(event) => setKeywordInput(event.target.value)}
            onPressEnter={onSearch}
          />
          <RangePicker
            allowClear={false}
            value={range}
            onChange={(value) => {
              if (!value?.[0] || !value?.[1]) {
                return;
              }
              setRange([value[0], value[1]]);
            }}
          />
          <Button type="primary" onClick={onSearch}>
            查询
          </Button>
          <Button onClick={onReset}>重置</Button>
        </Space>
      )}
    >
      <div className="teacher-schedule-layout">
        <div className="teacher-schedule-main">
          <div className="teacher-schedule-summary">
            <div>
              <Typography.Text className="teacher-schedule-summary-label">时间范围</Typography.Text>
              <div className="teacher-schedule-summary-range">{data?.startDate} ~ {data?.endDate}</div>
            </div>
            <div className="teacher-schedule-counts">
              <span className="teacher-schedule-count is-active">有课老师 {data?.teachers.length ?? 0}</span>
              <span className="teacher-schedule-count">无课老师 {data?.idleTeachers.length ?? 0}</span>
            </div>
          </div>

          <div className="teacher-schedule-hint">
            连续授课会自动合并显示，方便快速查看老师当天在哪个班上课。
          </div>

          <Spin spinning={loading}>
            {data && (data.teachers.length > 0 || data.idleTeachers.length > 0) ? (
              <>
                <div className="teacher-schedule-table-shell">
                  <table className="teacher-schedule-table">
                    <thead>
                      <tr>
                        <th className="teacher-schedule-teacher-col">老师</th>
                        {data.dateHeaders.map((header) => (
                          <th key={header.date} className="teacher-schedule-date-col">
                            <div>{header.dayLabel}</div>
                            <div className="teacher-schedule-weekday">{header.weekdayLabel}</div>
                          </th>
                        ))}
                      </tr>
                    </thead>
                    <tbody>
                      {data.teachers.map((teacher) => renderTeacherRow(teacher, data.dateHeaders.length))}
                    </tbody>
                  </table>
                </div>

                <div className="teacher-schedule-legend">
                  <span className="teacher-schedule-legend-item">
                    <span className="teacher-schedule-legend-dot" />
                    有课（支持跨天连续显示）
                  </span>
                  <span className="teacher-schedule-legend-text">空白网格：无课</span>
                </div>
              </>
            ) : (
              <Empty description="当前时间范围没有师资安排数据" />
            )}
          </Spin>
        </div>

        <aside className="teacher-schedule-side">
          <div className="teacher-schedule-side-head">
            <Typography.Title level={5} style={{ marginTop: 0, marginBottom: 4 }}>
              无课老师
            </Typography.Title>
            <Typography.Text type="secondary">
              {data?.startDate} ~ {data?.endDate}
            </Typography.Text>
          </div>
          <div className="teacher-schedule-idle-list">
            {data?.idleTeachers.length ? data.idleTeachers.map((teacher) => (
              <div key={teacher.teacherId} className="teacher-schedule-idle-item">
                <span>{teacher.teacherName}</span>
                <span className="teacher-schedule-idle-badge">无课</span>
              </div>
            )) : (
              <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="暂无无课老师" />
            )}
          </div>
        </aside>
      </div>
    </PageContainer>
  );
}

function renderTeacherRow(teacher: TeacherScheduleTeacherRow, totalDays: number) {
  const blockMap = new Map<number, TeacherScheduleBlock>();
  teacher.blocks.forEach((item) => blockMap.set(item.startIndex, item));

  const cells = [];
  for (let dayIndex = 0; dayIndex < totalDays; dayIndex += 1) {
    const block = blockMap.get(dayIndex);
    if (block) {
      cells.push(
        <td key={`${teacher.teacherId}-${dayIndex}`} colSpan={block.spanDays} className="teacher-schedule-block-cell">
          <div className="teacher-schedule-block" title={block.stageName ? `${block.classLabel} / ${block.stageName}` : block.classLabel}>
            <span>{block.classLabel}</span>
            {block.stageName ? <em>{block.stageName}</em> : null}
          </div>
        </td>
      );
      dayIndex += block.spanDays - 1;
      continue;
    }
    cells.push(<td key={`${teacher.teacherId}-${dayIndex}`} className="teacher-schedule-empty-cell" />);
  }

  return (
    <tr key={teacher.teacherId}>
      <th className="teacher-schedule-teacher-name">{teacher.teacherName}</th>
      {cells}
    </tr>
  );
}
