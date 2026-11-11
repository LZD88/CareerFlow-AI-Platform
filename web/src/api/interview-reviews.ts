import axios from 'axios';
import request from '../utils/request';
import type { PageResult } from '../types/common';
import type { InterviewReviewQueryParams, InterviewReviewTask, InterviewReviewUploadValues, UploadPresignRes } from '../types/interview-review';

export function getInterviewReviewPage(params: InterviewReviewQueryParams) {
  return request.get<never, PageResult<InterviewReviewTask>>('/api/interview-reviews', { params });
}

export function getInterviewReviewDetail(id: number) {
  return request.get<never, InterviewReviewTask>(`/api/interview-reviews/${id}`);
}

export function transcribeInterviewReview(id: number, speakerDiarization = false) {
  return request.post<never, InterviewReviewTask>(`/api/interview-reviews/${id}/transcribe`, null, {
    params: { speakerDiarization }
  });
}

export function generateInterviewReview(id: number) {
  return request.post<never, InterviewReviewTask>(`/api/interview-reviews/${id}/review`);
}

export async function uploadInterviewResume(
  taskId: number,
  file: File,
  onProgress?: (percent: number) => void
) {
  const presign = await createInterviewFilePresign(file, 'RESUME');
  await uploadInterviewAudioToTos(presign.uploadUrl, file, presign.headers, onProgress);
  return request.post<never, InterviewReviewTask>(`/api/interview-reviews/${taskId}/resume`, {
    resumeFilename: file.name,
    resumeObjectKey: presign.objectKey,
    resumeUrl: presign.fileUrl,
    resumeContentType: file.type || 'application/octet-stream',
    resumeSize: file.size
  });
}

export async function uploadInterviewAudio(
  file: File,
  values: InterviewReviewUploadValues,
  onProgress?: (percent: number) => void,
  resumeFile?: File
) {
  const audioPresign = await createInterviewFilePresign(file, 'AUDIO');
  await uploadInterviewAudioToTos(audioPresign.uploadUrl, file, audioPresign.headers, onProgress);

  let resumePresign: UploadPresignRes | undefined;
  if (resumeFile) {
    resumePresign = await createInterviewFilePresign(resumeFile, 'RESUME');
    await uploadInterviewAudioToTos(resumePresign.uploadUrl, resumeFile, resumePresign.headers);
  }

  return confirmInterviewAudioUpload(file, values, audioPresign, resumeFile, resumePresign);
}

export function createInterviewFilePresign(file: File, fileCategory: 'AUDIO' | 'RESUME') {
  return request.post<never, UploadPresignRes>('/api/interview-reviews/audio/presign', {
    fileCategory,
    fileName: file.name,
    contentType: file.type || 'application/octet-stream',
    fileSize: file.size
  });
}

export async function uploadInterviewAudioToTos(
  uploadUrl: string,
  file: File,
  headers?: Record<string, string>,
  onProgress?: (percent: number) => void
) {
  await axios.put(uploadUrl, file, {
    headers: {
      ...(headers ?? {}),
      'Content-Type': headers?.['Content-Type'] ?? file.type ?? 'application/octet-stream'
    },
    onUploadProgress(progressEvent) {
      if (!onProgress || !progressEvent.total) {
        return;
      }
      const percent = Math.min(99, Number(((progressEvent.loaded / progressEvent.total) * 100).toFixed(2)));
      onProgress(percent);
    }
  });
}

export function confirmInterviewAudioUpload(
  file: File,
  values: InterviewReviewUploadValues,
  presign: UploadPresignRes,
  resumeFile?: File,
  resumePresign?: UploadPresignRes
) {
  return request.post<never, InterviewReviewTask>('/api/interview-reviews/audio/confirm', {
    title: values.title?.trim(),
    candidateName: values.candidateName?.trim(),
    interviewRole: values.interviewRole?.trim(),
    originalFilename: file.name,
    audioObjectKey: presign.objectKey,
    audioUrl: presign.fileUrl,
    audioContentType: file.type || 'application/octet-stream',
    audioSize: file.size,
    resumeFilename: resumeFile?.name,
    resumeObjectKey: resumePresign?.objectKey,
    resumeUrl: resumePresign?.fileUrl,
    resumeContentType: resumeFile?.type || undefined,
    resumeSize: resumeFile?.size
  });
}
