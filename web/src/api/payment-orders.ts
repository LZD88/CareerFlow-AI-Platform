import request from '../utils/request';
import type { PageResult } from '../types/common';
import type { PaymentChannelOrderEntity, PaymentOrderEntity, PaymentOrderQueryParams } from '../types/system';

export function getPaymentOrderPage(params: PaymentOrderQueryParams) {
  return request.get<never, PageResult<PaymentOrderEntity>>('/api/sales/payment-orders', { params });
}

export function getPaymentChannelOrder(paymentOrderNo: string) {
  return request.get<never, PaymentChannelOrderEntity | null>(
    `/api/sales/payment-orders/${encodeURIComponent(paymentOrderNo)}/channel-order`
  );
}
