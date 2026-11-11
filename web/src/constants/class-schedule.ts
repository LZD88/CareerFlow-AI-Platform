export const CLASS_SCHEDULE_TYPES = {
  CLASS: 'CLASS',
  SELF_STUDY: 'SELF_STUDY',
  REST: 'REST',
  EMPTY: 'EMPTY'
} as const;

export type ClassScheduleType = (typeof CLASS_SCHEDULE_TYPES)[keyof typeof CLASS_SCHEDULE_TYPES];

export const CLASS_SCHEDULE_LABELS: Record<ClassScheduleType, string> = {
  [CLASS_SCHEDULE_TYPES.CLASS]: '上课',
  [CLASS_SCHEDULE_TYPES.SELF_STUDY]: '自习',
  [CLASS_SCHEDULE_TYPES.REST]: '休息',
  [CLASS_SCHEDULE_TYPES.EMPTY]: ''
};
