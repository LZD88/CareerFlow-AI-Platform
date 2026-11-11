/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_API_BASE_URL?: string;
  readonly VITE_API_SIGN_PATH_PREFIX?: string;
  readonly VITE_FEISHU_APP_ID?: string;
  readonly VITE_FEISHU_REDIRECT_URI?: string;
  readonly VITE_FEISHU_AUTH_URL?: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}
