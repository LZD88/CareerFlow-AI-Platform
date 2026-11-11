import request from '../utils/request';
import type { PageResult } from '../types/common';
import type { OrderCreateReq, OrderEnrollmentReq, OrderEntity, OrderQueryParams, OrderUpdateReq } from '../types/system';

export function enrollOrder(id: number, data: OrderEnrollmentReq) {
  return request.post<never, number>(`/api/sales/orders/${id}/enrollment`, data);
}

export function getOrderPage(params: OrderQueryParams) {
  return request.get<never, PageResult<OrderEntity>>('/api/sales/orders', { params });
}

export function getOrderDetail(id: number) {
  return request.get<never, OrderEntity>(`/api/sales/orders/${id}`);
}

export function createOrder(data: OrderCreateReq) {
  return request.post<never, number>('/api/sales/orders', data);
}

export function updateOrder(id: number, data: OrderUpdateReq) {
  return request.put(`/api/sales/orders/${id}`, data);
}

export function deleteOrder(id: number) {
  return request.delete(`/api/sales/orders/${id}`);
}
