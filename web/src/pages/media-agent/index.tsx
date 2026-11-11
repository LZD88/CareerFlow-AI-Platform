import { useMemo, useState, type ReactNode } from 'react';
import { BulbOutlined, CheckCircleOutlined, CopyOutlined, ReloadOutlined, SaveOutlined, SearchOutlined } from '@ant-design/icons';
import {
  Alert,
  Button,
  Checkbox,
  Collapse,
  Empty,
  Form,
  Input,
  InputNumber,
  Progress,
  Select,
  Space,
  Tabs,
  Tag,
  Typography,
  message
} from 'antd';
import NoPermission from '../../components/NoPermission';
import PageContainer from '../../components/PageContainer';
import { generateMediaTextDraft, generateMediaTopics, saveMediaDraft } from '../../api/media-agent';
import type { MediaTextDraftRes, MediaTopicItem, MediaTopicReq, MediaTopicRes } from '../../types/media-agent';
import { hasPermission } from '../../utils/permission';

const { TextArea } = Input;

interface TopicFormValues {
  theme: string;
  platforms: string[];
  audience?: string;
  goal?: string;
  style?: string;
  topicCount: number;
}

const platformOptions = [
  { label: '小红书', value: 'xiaohongshu' },
  { label: '抖音', value: 'douyin' }
];

const goalOptions = ['招生转化', '品牌曝光', '科普信任', '活动宣传'].map((value) => ({ label: value, value }));
const styleOptions = ['专业但不生硬', '轻松自然', '故事化', '干货型'].map((value) => ({ label: value, value }));

export default function MediaAgentPage() {
  const canView = hasPermission('menu:system:media-agent');
  const canGenerate = hasPermission('button:media-agent:generate-topics');
  const canGenerateDraft = hasPermission('button:media-agent:generate-text-draft');
  const canSaveDraft = hasPermission('button:media-agent:save-draft');
  const [form] = Form.useForm<TopicFormValues>();
  const [loading, setLoading] = useState(false);
  const [draftLoading, setDraftLoading] = useState(false);
  const [saveLoading, setSaveLoading] = useState(false);
  const [savedProjectId, setSavedProjectId] = useState<number>();
  const [result, setResult] = useState<MediaTopicRes>();
  const [selectedTopic, setSelectedTopic] = useState<MediaTopicItem>();
  const [draft, setDraft] = useState<MediaTextDraftRes>();

  const sortedTopics = useMemo(() => {
    const topics = result?.topics ?? [];
    return [...topics].sort((a, b) => (b.score ?? 0) - (a.score ?? 0));
  }, [result]);

  const onGenerate = async () => {
    const values = await form.validateFields();
    setLoading(true);
    setSelectedTopic(undefined);
    setDraft(undefined);
    setSavedProjectId(undefined);
    try {
      const request: MediaTopicReq = {
        theme: values.theme.trim(),
        platforms: values.platforms?.length ? values.platforms : ['xiaohongshu', 'douyin'],
        audience: values.audience?.trim(),
        goal: values.goal?.trim(),
        style: values.style?.trim(),
        topicCount: values.topicCount
      };
      const response = await generateMediaTopics(request);
      setResult(response);
    } finally {
      setLoading(false);
    }
  };

  const onReset = () => {
    form.resetFields();
    setResult(undefined);
    setSelectedTopic(undefined);
    setDraft(undefined);
    setSavedProjectId(undefined);
  };

  const onGenerateDraft = async () => {
    if (!result?.theme || !selectedTopic) {
      message.warning('请先选择一个选题');
      return;
    }
    const style = form.getFieldValue('style')?.trim();
    setDraftLoading(true);
    try {
      const response = await generateMediaTextDraft({
        theme: result.theme,
        topic: selectedTopic,
        style
      });
      setDraft(response);
      setSavedProjectId(undefined);
    } finally {
      setDraftLoading(false);
    }
  };

  const onSaveDraft = async () => {
    if (!draft || !selectedTopic || !result?.theme) {
      message.warning('请先生成文字草稿');
      return;
    }
    const style = form.getFieldValue('style')?.trim();
    setSaveLoading(true);
    try {
      const response = await saveMediaDraft({
        theme: result.theme,
        topic: selectedTopic,
        style,
        draft
      });
      setSavedProjectId(response.projectId);
      message.success(`草稿已保存，项目 ID：${response.projectId}`);
    } finally {
      setSaveLoading(false);
    }
  };

  if (!canView) {
    return <NoPermission />;
  }

  return (
    <PageContainer
      title="招生内容创作"
      description="围绕职业技能、课程亮点与学员成长，生成招生宣传的选题与内容草稿。"
    >
      <div className="media-agent-layout">
        <section className="media-agent-composer">
          <div className="media-agent-panel-title">
            <div>
              <Typography.Title level={5}>创作需求</Typography.Title>
              <Typography.Text type="secondary">先把主题说清楚，其他参数可以慢慢调。</Typography.Text>
            </div>
            <BulbOutlined />
          </div>
          <Form
            form={form}
            layout="vertical"
            initialValues={{
              theme: '',
              platforms: ['xiaohongshu', 'douyin'],
              topicCount: 6
            }}
            onFinish={onGenerate}
          >
            <Form.Item
              name="theme"
              label="创作主题"
              className="media-agent-theme-item"
              rules={[{ required: true, message: '请输入创作主题' }]}
            >
              <TextArea
                rows={4}
                maxLength={200}
                showCount
                placeholder="例如：Java 后端开发就业班秋季招生"
              />
            </Form.Item>

            <Collapse
              ghost
              size="small"
              className="media-agent-advanced"
              items={[
                {
                  key: 'advanced',
                  label: '高级设置',
                  children: (
                    <>
                      <Form.Item name="platforms" label="目标平台">
                        <Checkbox.Group className="media-agent-platform-options" options={platformOptions} />
                      </Form.Item>
                      <Form.Item name="audience" label="目标人群">
                        <Input maxLength={200} placeholder="例如：准备从事 Java 开发的应届毕业生" />
                      </Form.Item>
                      <Form.Item name="goal" label="内容目标">
                        <Select options={goalOptions} allowClear placeholder="选择内容目标" />
                      </Form.Item>
                      <Form.Item name="style" label="内容风格">
                        <Select options={styleOptions} allowClear placeholder="选择内容风格" />
                      </Form.Item>
                      <Form.Item
                        name="topicCount"
                        label="选题数量"
                        rules={[{ required: true, message: '请输入选题数量' }]}
                      >
                        <InputNumber min={3} max={8} precision={0} style={{ width: 140 }} />
                      </Form.Item>
                    </>
                  )
                }
              ]}
            />

            {!canGenerate ? (
              <Alert
                style={{ marginBottom: 16 }}
                type="warning"
                showIcon
                message="当前账号没有生成选题权限"
              />
            ) : null}

            <Space>
              <Button
                type="primary"
                htmlType="submit"
                loading={loading}
                disabled={!canGenerate}
                icon={<SearchOutlined />}
              >
                生成选题
              </Button>
              <Button onClick={onReset} icon={<ReloadOutlined />} />
            </Space>
          </Form>
        </section>

        <div className="media-agent-result">
          {sortedTopics.length ? (
            <Space direction="vertical" size={14} style={{ width: '100%' }}>
              <div className="media-agent-result-header">
                <div>
                  <Typography.Title level={5}>选题方向</Typography.Title>
                  <Typography.Text type="secondary">
                    已基于「{result?.theme}」生成 {sortedTopics.length} 个方向
                  </Typography.Text>
                </div>
                {selectedTopic ? (
                  <Tag color="processing" icon={<CheckCircleOutlined />}>
                    已选择：{selectedTopic.topic}
                  </Tag>
                ) : null}
              </div>
              {sortedTopics.map((topic, index) => (
                <TopicCard
                  key={`${topic.topic}-${index}`}
                  topic={topic}
                  active={selectedTopic?.topic === topic.topic}
                  onSelect={() => {
                    setSelectedTopic(topic);
                    setDraft(undefined);
                    setSavedProjectId(undefined);
                  }}
                />
              ))}
              <section className="media-agent-draft-action">
                <div>
                  <Typography.Title level={5}>文字草稿</Typography.Title>
                  <Typography.Text type="secondary">
                    选中一个方向后，生成小红书图文、抖音短视频和抖音图文三版内容。
                  </Typography.Text>
                </div>
                <Button
                  type="primary"
                  loading={draftLoading}
                  disabled={!selectedTopic || !canGenerateDraft}
                  onClick={onGenerateDraft}
                >
                  生成文字草稿
                </Button>
              </section>
              {!canGenerateDraft ? (
                <Alert type="warning" showIcon message="当前账号没有生成文字草稿权限" />
              ) : null}
              {draft ? (
                <DraftPreview
                  draft={draft}
                  onChange={(nextDraft) => {
                    setDraft(nextDraft);
                    setSavedProjectId(undefined);
                  }}
                  canSave={canSaveDraft}
                  saveLoading={saveLoading}
                  savedProjectId={savedProjectId}
                  onSave={onSaveDraft}
                />
              ) : null}
            </Space>
          ) : (
            <section className="media-agent-empty-card">
              <Empty
                image={Empty.PRESENTED_IMAGE_SIMPLE}
                description="输入主题后生成选题方向"
              />
            </section>
          )}
        </div>
      </div>
    </PageContainer>
  );
}

function DraftPreview(props: {
  draft: MediaTextDraftRes;
  onChange: (draft: MediaTextDraftRes) => void;
  canSave: boolean;
  saveLoading: boolean;
  savedProjectId?: number;
  onSave: () => void;
}) {
  const { draft, onChange } = props;
  const patchDraft = <K extends 'xiaohongshuNote' | 'douyinVideo' | 'douyinImageText'>(
    key: K,
    patch: Partial<MediaTextDraftRes[K]>
  ) => {
    onChange({
      ...draft,
      [key]: {
        ...draft[key],
        ...patch
      }
    });
  };

  return (
    <section className="media-agent-draft-preview">
      <div className="media-agent-draft-savebar">
        <div>
          <Typography.Title level={5}>编辑草稿</Typography.Title>
          <Typography.Text type="secondary">
            {props.savedProjectId ? `已保存为项目 #${props.savedProjectId}` : '编辑完成后先保存文字草稿，再进入图片方案。'}
          </Typography.Text>
        </div>
        <Button
          type="primary"
          icon={<SaveOutlined />}
          loading={props.saveLoading}
          disabled={!props.canSave}
          onClick={props.onSave}
        >
          保存草稿
        </Button>
      </div>
      {!props.canSave ? (
        <Alert
          type="warning"
          showIcon
          message="当前账号没有保存草稿权限"
          style={{ margin: '0 18px 14px' }}
        />
      ) : null}
      <Tabs
        items={[
          {
            key: 'xiaohongshu',
            label: '小红书图文',
            children: (
              <DraftBlock
                titleLabel="标题候选"
                titleItems={draft.xiaohongshuNote.titles}
                coverText={draft.xiaohongshuNote.coverText}
                bodyLabel="图文正文"
                body={draft.xiaohongshuNote.body}
                planLabel="图片页建议"
                planItems={draft.xiaohongshuNote.imagePagePlan}
                hashtags={draft.xiaohongshuNote.hashtags}
                onTitleItemsChange={(titles) => patchDraft('xiaohongshuNote', { titles })}
                onCoverTextChange={(coverText) => patchDraft('xiaohongshuNote', { coverText })}
                onBodyChange={(body) => patchDraft('xiaohongshuNote', { body })}
                onPlanItemsChange={(imagePagePlan) => patchDraft('xiaohongshuNote', { imagePagePlan })}
                onHashtagsChange={(hashtags) => patchDraft('xiaohongshuNote', { hashtags })}
              />
            )
          },
          {
            key: 'douyin-video',
            label: '抖音短视频',
            children: (
              <DraftBlock
                titleLabel="开场钩子"
                titleItems={draft.douyinVideo.hooks}
                bodyLabel="口播脚本"
                body={draft.douyinVideo.script}
                planLabel="分镜建议"
                planItems={draft.douyinVideo.shotList}
                subtitleItems={draft.douyinVideo.subtitles}
                hashtags={draft.douyinVideo.hashtags}
                onTitleItemsChange={(hooks) => patchDraft('douyinVideo', { hooks })}
                onBodyChange={(script) => patchDraft('douyinVideo', { script })}
                onPlanItemsChange={(shotList) => patchDraft('douyinVideo', { shotList })}
                onSubtitleItemsChange={(subtitles) => patchDraft('douyinVideo', { subtitles })}
                onHashtagsChange={(hashtags) => patchDraft('douyinVideo', { hashtags })}
              />
            )
          },
          {
            key: 'douyin-image-text',
            label: '抖音图文',
            children: (
              <DraftBlock
                titleLabel="标题候选"
                titleItems={draft.douyinImageText.titles}
                coverText={draft.douyinImageText.coverText}
                bodyLabel="图文正文"
                body={draft.douyinImageText.body}
                planLabel="图片页结构"
                planItems={draft.douyinImageText.imagePagePlan}
                hashtags={draft.douyinImageText.hashtags}
                onTitleItemsChange={(titles) => patchDraft('douyinImageText', { titles })}
                onCoverTextChange={(coverText) => patchDraft('douyinImageText', { coverText })}
                onBodyChange={(body) => patchDraft('douyinImageText', { body })}
                onPlanItemsChange={(imagePagePlan) => patchDraft('douyinImageText', { imagePagePlan })}
                onHashtagsChange={(hashtags) => patchDraft('douyinImageText', { hashtags })}
              />
            )
          }
        ]}
      />
    </section>
  );
}

function DraftBlock(props: {
  titleLabel: string;
  titleItems?: string[];
  coverText?: string;
  bodyLabel: string;
  body?: string;
  planLabel: string;
  planItems?: string[];
  subtitleItems?: string[];
  hashtags?: string[];
  onTitleItemsChange: (items: string[]) => void;
  onCoverTextChange?: (value: string) => void;
  onBodyChange: (value: string) => void;
  onPlanItemsChange: (items: string[]) => void;
  onSubtitleItemsChange?: (items: string[]) => void;
  onHashtagsChange: (items: string[]) => void;
}) {
  const copyDraft = async () => {
    await navigator.clipboard.writeText(buildDraftText(props));
    message.success('草稿已复制');
  };

  return (
    <div className="media-agent-draft-block">
      <div className="media-agent-draft-toolbar">
        <Typography.Text type="secondary">文字草稿可直接复制，也可以先微调后再进入图片方案。</Typography.Text>
        <Button size="small" icon={<CopyOutlined />} onClick={copyDraft}>
          复制整版
        </Button>
      </div>

      <DraftSection title={props.titleLabel}>
        <Select
          mode="tags"
          value={props.titleItems ?? []}
          onChange={props.onTitleItemsChange}
          tokenSeparators={['\n']}
          placeholder="输入候选项，回车添加"
          className="media-agent-edit-select"
        />
      </DraftSection>

      {props.onCoverTextChange ? (
        <DraftSection title="封面文案">
          <Input
            value={props.coverText}
            maxLength={80}
            showCount
            onChange={(event) => props.onCoverTextChange?.(event.target.value)}
            placeholder="输入封面文案"
          />
        </DraftSection>
      ) : null}

      <DraftSection title={props.bodyLabel}>
        <TextArea
          value={props.body}
          autoSize={{ minRows: 6, maxRows: 18 }}
          onChange={(event) => props.onBodyChange(event.target.value)}
          placeholder="输入正文内容"
          className="media-agent-edit-textarea"
        />
      </DraftSection>

      <DraftSection title={props.planLabel}>
        <TextArea
          value={joinLines(props.planItems)}
          autoSize={{ minRows: 4, maxRows: 10 }}
          onChange={(event) => props.onPlanItemsChange(splitLines(event.target.value))}
          placeholder="一行一条，例如：第 1 页：封面"
          className="media-agent-edit-textarea"
        />
      </DraftSection>

      {props.onSubtitleItemsChange ? (
        <DraftSection title="屏幕字幕">
          <TextArea
            value={joinLines(props.subtitleItems)}
            autoSize={{ minRows: 3, maxRows: 8 }}
            onChange={(event) => props.onSubtitleItemsChange?.(splitLines(event.target.value))}
            placeholder="一行一条字幕"
            className="media-agent-edit-textarea"
          />
        </DraftSection>
      ) : null}

      <DraftSection title="话题标签">
        <Select
          mode="tags"
          value={props.hashtags ?? []}
          onChange={props.onHashtagsChange}
          tokenSeparators={[' ', '，', ',', '\n']}
          placeholder="输入标签，回车添加"
          className="media-agent-edit-select"
        />
      </DraftSection>
    </div>
  );
}

function DraftSection(props: { title: string; children: ReactNode }) {
  return (
    <div className="media-agent-draft-section">
      <Typography.Text strong>{props.title}</Typography.Text>
      {props.children}
    </div>
  );
}

function buildDraftText(props: {
  titleLabel: string;
  titleItems?: string[];
  coverText?: string;
  bodyLabel: string;
  body?: string;
  planLabel: string;
  planItems?: string[];
  subtitleItems?: string[];
  hashtags?: string[];
}) {
  return [
    `${props.titleLabel}：`,
    ...(props.titleItems ?? []).map((item) => `- ${item}`),
    props.coverText ? `\n封面文案：\n${props.coverText}` : '',
    `\n${props.bodyLabel}：\n${props.body || ''}`,
    props.planItems?.length ? `\n${props.planLabel}：\n${props.planItems.map((item, index) => `${index + 1}. ${item}`).join('\n')}` : '',
    props.subtitleItems?.length ? `\n屏幕字幕：\n${props.subtitleItems.map((item) => `- ${item}`).join('\n')}` : '',
    props.hashtags?.length ? `\n话题标签：\n${props.hashtags.map((item) => `#${item}`).join(' ')}` : ''
  ]
    .filter(Boolean)
    .join('\n');
}

function splitLines(value: string) {
  return value
    .split('\n')
    .map((item) => item.trim())
    .filter(Boolean);
}

function joinLines(value?: string[]) {
  return (value ?? []).join('\n');
}

function TopicCard(props: { topic: MediaTopicItem; active: boolean; onSelect: () => void }) {
  const { topic, active, onSelect } = props;

  const copyTopic = async () => {
    await navigator.clipboard.writeText(buildTopicText(topic));
    message.success('选题已复制');
  };

  return (
    <section
      className={active ? 'media-agent-topic-card active' : 'media-agent-topic-card'}
    >
      <div className="media-agent-topic-head">
        <div className="media-agent-topic-title">
          <BulbOutlined />
          <Typography.Title level={5}>{topic.topic}</Typography.Title>
        </div>
        <Space>
          <Progress
            type="circle"
            percent={topic.score ?? 0}
            size={38}
            strokeColor={resolveScoreColor(topic.score)}
          />
          <Button size="small" type={active ? 'primary' : 'default'} onClick={onSelect}>
            选择
          </Button>
          <Button size="small" icon={<CopyOutlined />} onClick={copyTopic} />
        </Space>
      </div>
      <Space size={[8, 8]} wrap>
        {topic.angle ? <Tag color="processing">{topic.angle}</Tag> : null}
        {topic.audience ? <Tag>{topic.audience}</Tag> : null}
        {(topic.platformFit ?? []).map((platform) => (
          <Tag key={platform} color={platform === 'xiaohongshu' ? 'magenta' : 'volcano'}>
            {formatPlatform(platform)}
          </Tag>
        ))}
      </Space>

      {topic.reason ? (
        <Typography.Paragraph style={{ marginTop: 12, marginBottom: 8 }}>
          {topic.reason}
        </Typography.Paragraph>
      ) : null}

      {topic.contentPoints?.length ? (
        <ul className="media-agent-point-list">
          {topic.contentPoints.map((item, index) => (
            <li key={`${item}-${index}`}>{item}</li>
          ))}
        </ul>
      ) : null}
    </section>
  );
}

function formatPlatform(platform: string) {
  if (platform === 'xiaohongshu') {
    return '小红书';
  }
  if (platform === 'douyin') {
    return '抖音';
  }
  return platform;
}

function resolveScoreColor(score = 0) {
  if (score >= 85) {
    return '#52c41a';
  }
  if (score >= 70) {
    return '#1677ff';
  }
  return '#faad14';
}

function buildTopicText(topic: MediaTopicItem) {
  return [
    `选题：${topic.topic}`,
    topic.angle ? `角度：${topic.angle}` : '',
    topic.audience ? `人群：${topic.audience}` : '',
    topic.reason ? `理由：${topic.reason}` : '',
    topic.contentPoints?.length ? `要点：\n${topic.contentPoints.map((item) => `- ${item}`).join('\n')}` : ''
  ]
    .filter(Boolean)
    .join('\n');
}
