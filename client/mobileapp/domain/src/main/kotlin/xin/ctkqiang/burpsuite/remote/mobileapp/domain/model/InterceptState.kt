package xin.ctkqiang.burpsuite.remote.mobileapp.domain.model

/** 拦截项在队列里的归宿；放行与丢弃是终态，之后不再有别的迁移（plan §24）。 */
enum class InterceptState {
    /** 停在队列里等决定。 */
    Pending,

    /** 被用户改写过，仍在队列里。 */
    Modified,

    /** 已被放行。 */
    Forwarded,

    /** 已被丢弃。 */
    Dropped,
}
