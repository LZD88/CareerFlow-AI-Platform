import type { PropsWithChildren, ReactNode } from 'react';
import { Card, Space, Typography } from 'antd';

interface PageContainerProps extends PropsWithChildren {
  title: string;
  extra?: ReactNode;
  description?: string;
}

export default function PageContainer(props: PageContainerProps) {
  const { title, extra, description, children } = props;

  return (
    <Card className="page-card" bordered={false}>
      <div className="page-card-header">
        <div>
          <Typography.Title level={4} style={{ marginBottom: 4 }}>
            {title}
          </Typography.Title>
          {description ? <Typography.Text type="secondary">{description}</Typography.Text> : null}
        </div>
        <Space>{extra}</Space>
      </div>
      {children}
    </Card>
  );
}
