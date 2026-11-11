package cn.yq.modules.studentportal.context;

import cn.yq.modules.studentportal.domain.StudentPortalAccessContext;

public final class StudentPortalAccessContextHolder {

    private static final ThreadLocal<StudentPortalAccessContext> CONTEXT_HOLDER = new ThreadLocal<>();

    private StudentPortalAccessContextHolder() {
    }

    public static void set(StudentPortalAccessContext context) {
        CONTEXT_HOLDER.set(context);
    }

    public static StudentPortalAccessContext get() {
        return CONTEXT_HOLDER.get();
    }

    public static void clear() {
        CONTEXT_HOLDER.remove();
    }
}
