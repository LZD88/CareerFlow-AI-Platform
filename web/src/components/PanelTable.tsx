import type { PropsWithChildren } from 'react';

export default function PanelTable({ children }: PropsWithChildren) {
  return <div className="panel-table">{children}</div>;
}
