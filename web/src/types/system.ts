import type { ClassScheduleType } from '../constants/class-schedule';

export interface UserEntity {
  id: number;
  username: string;
  password?: string;
  nickname: string;
  realName?: string;
  phone?: string;
  email?: string;
  status: number;
  createdAt?: string;
  updatedAt?: string;
}

export interface CampusEntity {
  id: number;
  campusName: string;
  principalUserId: number;
  principalName?: string;
  address: string;
  contactPhone?: string;
  remark?: string;
  status: number;
  createdAt?: string;
  updatedAt?: string;
}

export interface CourseEntity {
  id: number;
  courseName: string;
  courseDays: number;
  feishuMaterialPath: string;
  status: number;
  createdAt?: string;
  updatedAt?: string;
}

export interface CourseDetailEntity {
  id: number;
  courseId: number;
  stageName: string;
  dayNum: number;
  courseContent: string;
  createdAt?: string;
  updatedAt?: string;
  stageRowSpan?: number;
}

export type ClassStatusCode = 'WAITING' | 'TEACHING' | 'FINISHED';

export interface ClassEntity {
  id: number;
  classTerm: number;
  campusId: number;
  campusName?: string;
  headTeacherId: number;
  headTeacherName?: string;
  classStatus: ClassStatusCode;
  startTime: string;
  courseId: number;
  courseName?: string;
  studentCount: number;
  hasSchedule?: boolean;
  createdAt?: string;
  updatedAt?: string;
}

export interface ClassScheduleCalendarDay {
  date: string;
  dayOfMonth: number;
  currentMonth: boolean;
  label?: string;
  scheduleType?: ClassScheduleType;
  holidayName?: string;
  holiday?: boolean;
  courseDayNum?: number;
  courseContent?: string;
  stageName?: string;
  teacherId?: number;
  teacherName?: string;
}

export interface ClassScheduleCalendarRes {
  classInfo: ClassEntity;
  year: number;
  month: number;
  days: ClassScheduleCalendarDay[];
}

export interface RoleEntity {
  id: number;
  roleCode: string;
  roleName: string;
  description?: string;
  status: number;
  createdAt?: string;
  updatedAt?: string;
}

export interface PermissionEntity {
  id: number;
  parentId: number;
  permissionCode: string;
  permissionName: string;
  permissionType: string;
  apiPath?: string;
  routePath?: string;
  icon?: string;
  sortNum: number;
  description?: string;
  status: number;
  createdAt?: string;
  updatedAt?: string;
}

export interface SysConfigEntity {
  id: number;
  k: string;
  v: string;
}

export interface CampusQueryParams {
  pageNum?: number;
  pageSize?: number;
  keyword?: string;
  status?: number;
  principalUserId?: number;
}

export interface ClassQueryParams {
  pageNum?: number;
  pageSize?: number;
  keyword?: string;
  classStatus?: ClassStatusCode;
  campusId?: number;
  headTeacherId?: number;
  courseId?: number;
}

export interface PermissionTreeRes extends PermissionEntity {
  children?: PermissionTreeRes[];
}

export interface StudentPermissionTreeRes extends PermissionEntity {
  children?: StudentPermissionTreeRes[];
}

export interface StudentEntity {
  id: number;
  studentCode: string;
  studentName: string;
  phone: string;
  graduationSession?: number;
  school?: string;
  education?: string;
  studyMode: 'ONLINE' | 'OFFLINE';
  classId?: number;
  className?: string;
  productId: number;
  productName?: string;
  currentPermissionTemplateId?: number;
  currentPermissionTemplateName?: string;
  currentPermissionTemplateSource?: 'MANUAL' | 'AUTO' | 'NONE';
  joinTime?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface StudentQueryParams {
  pageNum?: number;
  pageSize?: number;
  keyword?: string;
  studyMode?: StudentEntity['studyMode'];
  classId?: number;
  productId?: number;
}

export interface StudentPermissionTemplateEntity {
  id: number;
  templateCode: string;
  templateName: string;
  studyMode?: 'ONLINE' | 'OFFLINE';
  productId?: number;
  classId?: number;
  description?: string;
  status: number;
  createdAt?: string;
  updatedAt?: string;
}

export interface StudentPermissionTemplateDetailRes {
  template: StudentPermissionTemplateEntity;
  permissions: StudentPermissionTreeRes[];
}

export interface StudentPermissionTemplateBindingRes {
  id: number;
  studentId: number;
  templateId: number;
  templateCode?: string;
  templateName?: string;
  effectiveStartTime?: string;
  effectiveEndTime?: string;
  status: number;
  createdAt?: string;
  updatedAt?: string;
}

export interface StudentPermissionBindingDetailRes {
  studentId: number;
  studentCode: string;
  studentName: string;
  manualBinding?: StudentPermissionTemplateBindingRes;
  autoMatchedTemplate?: StudentPermissionTemplateEntity;
  effectiveTemplate?: StudentPermissionTemplateEntity;
  source: 'MANUAL' | 'AUTO' | 'NONE';
}

export interface StudentBindPermissionTemplateReq {
  templateId: number;
  effectiveStartTime?: string;
  effectiveEndTime?: string;
  status?: number;
}

export interface StudentPermissionTemplateQueryParams {
  pageNum?: number;
  pageSize?: number;
  keyword?: string;
  status?: number;
  studyMode?: StudentPermissionTemplateEntity['studyMode'];
  productId?: number;
  classId?: number;
}

export interface StudentPermissionTemplateCreateReq {
  templateCode: string;
  templateName: string;
  studyMode?: StudentPermissionTemplateEntity['studyMode'];
  productId?: number;
  classId?: number;
  description?: string;
  status: number;
}

export interface StudentPermissionTemplateUpdateReq {
  templateName: string;
  studyMode?: StudentPermissionTemplateEntity['studyMode'];
  productId?: number;
  classId?: number;
  description?: string;
  status: number;
}

export interface StudentDailyMomentAdminItem {
  id: number;
  studentCode?: string;
  studentName?: string;
  classId?: number;
  classTerm?: number;
  campusName?: string;
  title?: string;
  contentText?: string;
  fileUrls: string[];
  recordTime?: string;
  createTime?: string;
}

export interface StudentDailyMomentAdminQueryParams {
  pageNum?: number;
  pageSize?: number;
  studentCode?: string;
  studentName?: string;
  title?: string;
}

export type HomeworkStatusCode = 'PUBLISHED' | 'CLOSED';

export type HomeworkAnswerPublishStatus = 'UNPUBLISHED' | 'PUBLISHED';

export type HomeworkSubmissionStatusCode = 'UNSUBMITTED' | 'SUBMITTED' | 'REVIEWED' | 'RESUBMIT_REQUIRED';

export interface HomeworkAssignmentAdminItem {
  id: number;
  title: string;
  description?: string;
  classId: number;
  classTerm?: number;
  campusName?: string;
  courseId: number;
  courseName?: string;
  courseDetailId?: number;
  courseDayNum?: number;
  courseContentSnapshot?: string;
  homeworkDate?: string;
  scheduleType?: string;
  attachmentName?: string;
  attachmentUrl?: string;
  attachmentDownloadUrl?: string;
  attachmentType?: string;
  answerAttachmentName?: string;
  answerAttachmentUrl?: string;
  answerAttachmentDownloadUrl?: string;
  answerAttachmentType?: string;
  answerPublishStatus: HomeworkAnswerPublishStatus;
  answerPublishTime?: string;
  answerPublishTeacherId?: number;
  answerPublishTeacherName?: string;
  publishTeacherId: number;
  publishTeacherName?: string;
  publishDate?: string;
  deadline?: string;
  status: HomeworkStatusCode;
  allowResubmit: boolean;
  studentCountSnapshot: number;
  submittedCount: number;
}

export interface HomeworkAssignmentQueryParams {
  pageNum?: number;
  pageSize?: number;
  title?: string;
  classId?: number;
  status?: HomeworkStatusCode;
}

export interface HomeworkPublishContextRes {
  classId: number;
  classTerm?: number;
  campusName?: string;
  courseId: number;
  courseName?: string;
  courseDetailId?: number;
  courseDayNum?: number;
  courseContentSnapshot?: string;
  scheduleType?: string;
  homeworkDate?: string;
}

export interface HomeworkAssignmentSaveReq {
  classId: number;
  homeworkDate: string;
  courseId: number;
  courseDetailId?: number;
  courseContentSnapshot: string;
  title: string;
  description?: string;
  attachmentName?: string;
  attachmentUrl?: string;
  attachmentType?: string;
  deadline?: string;
  allowResubmit: boolean;
}

export interface HomeworkAssignmentStatusUpdateReq {
  status: HomeworkStatusCode;
}

export interface HomeworkAnswerManageReq {
  answerAttachmentName?: string;
  answerAttachmentUrl?: string;
  answerAttachmentType?: string;
  publishAnswer: boolean;
}

export interface HomeworkSubmissionAdminItem {
  submissionId?: number;
  studentId?: number;
  studentCode: string;
  studentName: string;
  phone?: string;
  graduationSession?: number;
  school?: string;
  status?: HomeworkSubmissionStatusCode;
  submitContent?: string;
  attachmentName?: string;
  attachmentUrl?: string;
  attachmentDownloadUrl?: string;
  attachmentType?: string;
  submitTime?: string;
  score?: number;
  teacherComment?: string;
  reviewTeacherId?: number;
  reviewTeacherName?: string;
  reviewTime?: string;
  version?: number;
}

export interface HomeworkSubmissionAdminQueryParams {
  pageNum?: number;
  pageSize?: number;
  studentCode?: string;
  studentName?: string;
  status?: HomeworkSubmissionStatusCode;
}

export interface HomeworkSubmissionReviewReq {
  status: Exclude<HomeworkSubmissionStatusCode, 'UNSUBMITTED' | 'SUBMITTED'>;
  score?: number;
  teacherComment?: string;
}

export interface UserDetailRes {
  user: UserEntity;
  roles: RoleEntity[];
  rolePermissions: PermissionEntity[];
}

export interface RoleDetailRes {
  role: RoleEntity;
  permissions: PermissionEntity[];
}

export interface UserCreateReq {
  username: string;
  password: string;
  nickname: string;
  realName?: string;
  phone?: string;
  email?: string;
  status: number;
}

export interface UserUpdateReq {
  nickname: string;
  realName?: string;
  phone?: string;
  email?: string;
  status: number;
}

export interface RoleCreateReq {
  roleCode: string;
  roleName: string;
  description?: string;
  status: number;
}

export interface RoleUpdateReq {
  roleName: string;
  description?: string;
  status: number;
}

export interface PermissionCreateReq {
  parentId: number;
  permissionCode: string;
  permissionName: string;
  permissionType: string;
  sortNum: number;
  description?: string;
  status: number;
}

export interface PermissionUpdateReq {
  parentId: number;
  permissionName: string;
  permissionType: string;
  sortNum: number;
  description?: string;
  status: number;
}

export interface SysConfigCreateReq {
  k: string;
  v: string;
}

export interface SysConfigUpdateReq {
  k: string;
  v: string;
}

export interface CampusCreateReq {
  campusName: string;
  principalUserId: number;
  address: string;
  contactPhone?: string;
  remark?: string;
  status: number;
}

export interface CampusUpdateReq {
  campusName: string;
  principalUserId: number;
  address: string;
  contactPhone?: string;
  remark?: string;
  status: number;
}

export interface CourseCreateReq {
  courseName: string;
  courseDays: number;
  feishuMaterialPath: string;
  status: number;
}

export interface CourseUpdateReq {
  courseName: string;
  courseDays: number;
  feishuMaterialPath: string;
  status: number;
}

export interface CourseDetailCreateReq {
  stageName: string;
  dayNum: number;
  courseContent: string;
}

export interface CourseDetailUpdateReq {
  stageName: string;
  dayNum: number;
  courseContent: string;
}

export interface ClassCreateReq {
  classTerm: number;
  campusId: number;
  headTeacherId: number;
  classStatus: ClassStatusCode;
  startTime: string;
  courseId: number;
  studentCount: number;
}

export interface ClassUpdateReq {
  classTerm: number;
  campusId: number;
  headTeacherId: number;
  classStatus: ClassStatusCode;
  startTime: string;
  courseId: number;
  studentCount: number;
}

export interface ClassScheduleGenerateReq {
  firstCourseDate: string;
}

export interface ClassScheduleArrangeReq {
  scheduleDate: string;
  scheduleType: ClassScheduleType;
  stageName?: string;
  courseContent?: string;
  teacherId?: number;
}

export interface ClassScheduleDeleteReq {
  scheduleDate: string;
}

export interface ClassScheduleChangeTeacherReq {
  scheduleDate: string;
  teacherId: number;
}

export interface ClassScheduleChangeContentReq {
  scheduleDate: string;
  courseContent: string;
}

export interface StageTeacherOption {
  teacherId: number;
  teacherName: string;
}

export interface ClassStageTeacher {
  stageName: string;
  dayCount: number;
  startDate: string;
  endDate: string;
  teacherId?: number;
  teacherName?: string;
  splitAssigned?: boolean;
  teacherSummary?: string;
  availableTeachers: StageTeacherOption[];
}

export interface AssignStageTeacherReq {
  items: Array<{
    stageName: string;
    teacherId?: number;
  }>;
}

export interface TeacherScheduleDate {
  date: string;
  dayLabel: string;
  weekdayLabel: string;
}

export interface TeacherScheduleBlock {
  startDate: string;
  endDate: string;
  startIndex: number;
  spanDays: number;
  classId: number;
  classTerm?: number;
  classLabel: string;
  stageName?: string;
}

export interface TeacherScheduleTeacherRow {
  teacherId: number;
  teacherName: string;
  blocks: TeacherScheduleBlock[];
}

export interface TeacherScheduleIdleTeacher {
  teacherId: number;
  teacherName: string;
}

export interface TeacherScheduleCalendarRes {
  startDate: string;
  endDate: string;
  dateHeaders: TeacherScheduleDate[];
  teachers: TeacherScheduleTeacherRow[];
  idleTeachers: TeacherScheduleIdleTeacher[];
}

export interface DutyTeacherOption {
  teacherId: number;
  teacherName: string;
}

export interface ClassDutyItem {
  classId: number;
  classLabel: string;
  campusName?: string;
  courseName?: string;
  startTime: string;
  graduateDate: string;
  dutyLabel: string;
  dutyTimeRange: string;
  teacherId?: number;
  teacherName?: string;
  remark?: string;
}

export interface GlobalDutyItem {
  dutyLabel: string;
  dutyTimeRange: string;
  teacherId?: number;
  teacherName?: string;
  remark?: string;
}

export interface DutyManagementDayRes {
  dutyDate: string;
  classDuties: ClassDutyItem[];
  globalDuty: GlobalDutyItem;
  teacherOptions: DutyTeacherOption[];
}

export interface ProductEntity {
  id: number;
  productCode: string;
  productName: string;
  studyMode: 'ONLINE' | 'OFFLINE';
  productPrice: number;
  createdAt?: string;
  updatedAt?: string;
}

export interface ProductQueryParams {
  pageNum?: number;
  pageSize?: number;
  keyword?: string;
}

export interface ProductCreateReq {
  productCode: string;
  productName: string;
  studyMode: 'ONLINE' | 'OFFLINE';
  productPrice: number;
}

export interface ProductUpdateReq {
  productCode: string;
  productName: string;
  studyMode: 'ONLINE' | 'OFFLINE';
  productPrice: number;
}

export type OrderStatusCode = 'UNPAID' | 'PARTIAL_PAID' | 'PAID' | 'CLOSED';
export type FulfillmentStatusCode = 'PENDING_PAYMENT' | 'PENDING_ENROLLMENT' | 'ENROLLED' | 'CLOSED';

export interface OrderEntity {
  id: number;
  orderNo: string;
  studentName: string;
  phone: string;
  productId: number;
  productName?: string;
  productStudyMode?: 'ONLINE' | 'OFFLINE';
  classId?: number;
  classTerm?: number;
  studentCode: string;
  productAmount: number;
  discountAmount: number;
  receivableAmount: number;
  paidAmount: number;
  orderStatus: OrderStatusCode;
  studentId?: number;
  fulfillmentStatus?: FulfillmentStatusCode;
  createdAt?: string;
  updatedAt?: string;
}

export interface OrderQueryParams {
  pageNum?: number;
  pageSize?: number;
  keyword?: string;
  orderStatus?: OrderStatusCode;
  fulfillmentStatus?: FulfillmentStatusCode;
}

export interface OrderEnrollmentReq {
  phone: string;
  password: string;
  graduationSession: number;
  school: string;
  education: string;
  classId?: number;
  joinTime: string;
}

export interface OrderCreateReq {
  studentName: string;
  phone: string;
  productId: number;
  discountAmount: number;
  classId?: number;
}

export interface OrderUpdateReq {
  studentName: string;
  phone: string;
  productId: number;
  discountAmount: number;
  classId?: number;
}

export type PaymentOrderStatusCode =
  | 'INIT'
  | 'PROCESSING'
  | 'UNIFIED_ORDER_CREATED'
  | 'PAY_SUCCESS'
  | 'CLOSED'
  | 'FAILED';

export interface PaymentOrderEntity {
  id: number;
  paymentOrderNo: string;
  orderNo: string;
  studentCode: string;
  studentName: string;
  phone: string;
  productName: string;
  paymentAmount: number;
  paymentChannel: string;
  paymentStatus: PaymentOrderStatusCode;
  uniqueOrderNo?: string;
  lastErrorMsg?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface PaymentOrderQueryParams {
  pageNum?: number;
  pageSize?: number;
  keyword?: string;
  paymentStatus?: PaymentOrderStatusCode;
  paymentChannel?: string;
}

export interface PaymentChannelOrderEntity {
  id: number;
  paymentOrderNo?: string;
  uniqueOrderNo: string;
  merchantNo?: string;
  orderId?: string;
  channelOrderId?: string;
  orderAmount?: number;
  payAmount?: number;
  status?: string;
  paySuccessDate?: string;
  channel?: string;
  payerInfo?: string;
  failCode?: string;
  failReason?: string;
  channelTrxId?: string;
  bankOrderId?: string;
  callbackType?: string;
  callbackBody?: string;
  callbackTime?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface ClassDutySaveReq {
  dutyDate: string;
  teacherId: number;
  remark?: string;
}

export interface GlobalDutySaveReq {
  dutyDate: string;
  teacherId: number;
  remark?: string;
}
