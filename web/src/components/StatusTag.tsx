import { Tag } from 'antd';

interface StatusTagProps {
  value: number;
}

export default function StatusTag({ value }: StatusTagProps) {
  return value === 1 ? <Tag color="success">启用</Tag> : <Tag color="default">禁用</Tag>;
}
