/**
 * Burp Remote —— 界面层 / 文本键
 *
 * 声明插件界面用到的全部文本键。把键集中在一处而不是散落在控件代码里的字符串字面量，
 * 是为了让「界面上究竟有哪些文案」这个问题有一个可枚举的答案——评审多语言、检查漏翻，
 * 都从这份清单出发。
 *
 * @author 钟智强
 */

package xin.ctkqiang.burpsuite.remote.userinterface

/**
 * 插件界面文本的键。
 *
 * 取值必须与 `src/main/resources/messages*.properties` 中的键逐字一致。这里不复用枚举：
 * 枚举条目会强迫每个键都配一段说明，而那种说明只能重复键名本身（见 rules.md §4.6），
 * 真正需要解释的是「文案属于哪个界面区域」，因此改用分区注释来表达。
 *
 * 键名使用 snake_case，与资源文件中的写法保持同一形状，任何人都能一眼对上。
 */
internal object TextKey {
    /**
     * 顶部状态区。
     *
     * 这一句刻意说明「远程端点尚未开放」：不写清楚，操作者会以为看到端口号就代表服务已经
     * 在监听，进而把排查方向放在网络而不是配对流程上。
     */
    const val PENDING_ENDPOINT_NOTICE = "pending_endpoint_notice"

    /** 配对区。 */
    const val PAIRING_SECTION_HEADING = "pairing_section_heading"

    const val PAIRING_CODE_CAPTION = "pairing_code_caption"

    const val PAIRING_INSTRUCTION = "pairing_instruction"

    /** 剩余有效期的整句，{0} 为失效时刻，{1} 为剩余时长。 */
    const val PAIRING_VALIDITY_REMAINING = "pairing_validity_remaining"

    /** 剩余时长本身，{0} 为分钟数，{1} 为秒数。单位属于语言，因此必须走资源而不是拼接。 */
    const val REMAINING_DURATION = "remaining_duration"

    const val PAIRING_CODE_EXPIRED = "pairing_code_expired"

    const val REFRESH_PAIRING_CODE_LABEL = "refresh_pairing_code_label"

    const val COPY_PAIRING_CODE_LABEL = "copy_pairing_code_label"

    /** 运行参数区。 */
    const val RUNTIME_SECTION_HEADING = "runtime_section_heading"

    /** 已配对设备区。 */
    const val PAIRED_DEVICE_SECTION_HEADING = "paired_device_section_heading"

    const val PAIRED_DEVICE_EMPTY_NOTICE = "paired_device_empty_notice"

    /** 单台设备的配对时刻，{0} 为时刻。 */
    const val PAIRED_DEVICE_PAIRED_AT = "paired_device_paired_at"

    const val REMOVE_PAIRED_DEVICE_LABEL = "remove_paired_device_label"

    /** 移除成功后的反馈，{0} 为被移除设备的身份标识。 */
    const val PAIRED_DEVICE_REMOVED_NOTICE = "paired_device_removed_notice"

    const val PROTOCOL_VERSION_LABEL = "protocol_version_label"

    const val ADVERTISED_ADDRESS_LABEL = "advertised_address_label"

    const val REMOTE_PORT_LABEL = "remote_port_label"

    /** 操作反馈。 */
    const val COPY_SUCCEEDED_NOTICE = "copy_succeeded_notice"

    const val COPY_UNAVAILABLE_NOTICE = "copy_unavailable_notice"
}
