import { getLoginUser } from './auth';
import type { AppMenuItem } from '../constants/menu';

const SUPER_ADMIN_ROLE = 'SUPER_ADMIN';

export function isSuperAdmin() {
  const user = getLoginUser();
  return Boolean(user?.roleCodes?.includes(SUPER_ADMIN_ROLE));
}

export function hasPermission(permissionCode: string) {
  if (isSuperAdmin()) {
    return true;
  }
  const user = getLoginUser();
  return getPermissionCodeSet(user?.permissionCodes).has(permissionCode);
}

export function filterMenuItems(items: AppMenuItem[]): AppMenuItem[] {
  if (isSuperAdmin()) {
    return items;
  }

  return items.reduce<AppMenuItem[]>((result, item) => {
    const children = item.children ? filterMenuItems(item.children) : undefined;

    if (children?.length) {
      result.push({ ...item, children });
      return result;
    }

    // 分组没有可访问的子菜单时隐藏，避免把分组地址当成页面跳转。
    if (item.children) {
      return result;
    }

    if (!item.permissionCode || hasPermission(item.permissionCode)) {
      result.push(item);
    }

    return result;
  }, []);
}

export function getFirstAccessibleMenuPath(items: AppMenuItem[]): string {
  const filteredItems = filterMenuItems(items);
  return findFirstLeafPath(filteredItems) ?? '/';
}

function getPermissionCodeSet(permissionCodes?: string[]) {
  return new Set(
    (permissionCodes ?? [])
      .map((item) => item?.trim())
      .filter((item): item is string => Boolean(item))
  );
}

function findFirstLeafPath(items: AppMenuItem[]): string | null {
  for (const item of items) {
    if (item.children?.length) {
      const childPath = findFirstLeafPath(item.children);
      if (childPath) {
        return childPath;
      }
      continue;
    }
    if (item.key.startsWith('/')) {
      return item.key;
    }
  }
  return null;
}
