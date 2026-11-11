import request from '../utils/request';
import type { PageResult } from '../types/common';
import type { ProductCreateReq, ProductEntity, ProductQueryParams, ProductUpdateReq } from '../types/system';

export function getProductPage(params: ProductQueryParams) {
  return request.get<never, PageResult<ProductEntity>>('/api/sales/products', { params });
}

export function getProductDetail(id: number) {
  return request.get<never, ProductEntity>(`/api/sales/products/${id}`);
}

export function createProduct(data: ProductCreateReq) {
  return request.post<never, number>('/api/sales/products', data);
}

export function updateProduct(id: number, data: ProductUpdateReq) {
  return request.put(`/api/sales/products/${id}`, data);
}

export function deleteProduct(id: number) {
  return request.delete(`/api/sales/products/${id}`);
}
