import React from 'react';
import ReactDOM from 'react-dom/client';
import { App, ConfigProvider } from 'antd';
import zhCN from 'antd/locale/zh_CN';
import AppRoot from './App';
import './styles/global.css';
import './styles/platform.css';

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <ConfigProvider
      locale={zhCN}
      theme={{
        token: {
          colorPrimary: '#3567e8',
          colorText: '#23334b',
          borderRadius: 8,
          fontFamily: '"PingFang SC", "Microsoft YaHei", sans-serif'
        }
      }}
    >
      <App>
        <AppRoot />
      </App>
    </ConfigProvider>
  </React.StrictMode>
);
