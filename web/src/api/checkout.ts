import axios from 'axios';
import request, { apiBaseURL } from '../utils/request';

export interface CheckoutView {
  orderId: number;
  orderNo: string;
  orderStatus: string;
  receivableAmount: number;
  paidAmount: number;
  remainingAmount: number;
  paymentOrderNo?: string;
  paymentStatus?: string;
  paymentAmount?: number;
  cashierUrl?: string;
  expiresAt?: string;
  expired: boolean;
  ready: boolean;
  sandbox: boolean;
  missingConfig?: string[];
  message: string;
}
export const getCheckout = (id: number, silent = false) => request.get<never, CheckoutView>(`/api/payments/orders/${id}/checkout`, { silent });
export const createCheckout = (id: number) => request.post<never, CheckoutView>(`/api/payments/orders/${id}/checkout`, {}, { timeout: 60000 });

// 不携带管理员身份，不触发管理员登录跳转。
export async function getPublicCheckoutStatus(id: number, paymentOrderNo: string, signal?: AbortSignal): Promise<CheckoutView> {
  const response = await axios.get(`${apiBaseURL}/student-api/public/orders/${id}/checkout-status`, {
    params: { paymentOrderNo }, timeout: 10000, signal
  });
  if (response.data.code !== 200) throw new Error(response.data.message || '查询付款结果失败');
  return response.data.data;
}
