import { Button, Result } from 'antd';
import { useNavigate } from 'react-router-dom';
import { menuItems } from '../constants/menu';
import { getFirstAccessibleMenuPath } from '../utils/permission';

export default function NoPermission() {
  const navigate = useNavigate();
  const targetPath = getFirstAccessibleMenuPath(menuItems);
  const canGoHome = targetPath !== '/';

  return (
    <Result
      status="403"
      title="403"
      subTitle="当前账号暂无此页面访问权限。"
      extra={
        <Button
          type="primary"
          onClick={() => navigate(canGoHome ? targetPath : '/login', { replace: true })}
        >
          {canGoHome ? '返回首页' : '返回登录'}
        </Button>
      }
    />
  );
}
