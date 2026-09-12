package xin.ctkqiang.burpsuite.remote.userinterface

// 取值必须与 resources/messages*.properties 逐字一致，键名沿用那里的 snake_case。
internal object TextKey {
    // 必须说明「远程端点尚未开放」：否则看到端口号会以为服务已在监听，排查方向会跑到网络上。
    const val PENDING_ENDPOINT_NOTICE = "pending_endpoint_notice"

    // 配对区。
    const val PAIRING_SECTION_HEADING = "pairing_section_heading"

    const val PAIRING_CODE_CAPTION = "pairing_code_caption"

    const val PAIRING_INSTRUCTION = "pairing_instruction"

    // {0} 失效时刻，{1} 剩余时长。
    const val PAIRING_VALIDITY_REMAINING = "pairing_validity_remaining"

    // {0} 分钟数，{1} 秒数；单位属于语言，必须走资源而不是拼接。
    const val REMAINING_DURATION = "remaining_duration"

    const val PAIRING_CODE_EXPIRED = "pairing_code_expired"

    const val REFRESH_PAIRING_CODE_LABEL = "refresh_pairing_code_label"

    const val COPY_PAIRING_CODE_LABEL = "copy_pairing_code_label"

    // 运行参数区。
    const val RUNTIME_SECTION_HEADING = "runtime_section_heading"

    // 已配对设备区。
    const val PAIRED_DEVICE_SECTION_HEADING = "paired_device_section_heading"

    const val PAIRED_DEVICE_EMPTY_NOTICE = "paired_device_empty_notice"

    // {0} 配对时刻。
    const val PAIRED_DEVICE_PAIRED_AT = "paired_device_paired_at"

    const val REMOVE_PAIRED_DEVICE_LABEL = "remove_paired_device_label"

    // {0} 被移除设备的身份标识。
    const val PAIRED_DEVICE_REMOVED_NOTICE = "paired_device_removed_notice"

    const val PROTOCOL_VERSION_LABEL = "protocol_version_label"

    const val ADVERTISED_ADDRESS_LABEL = "advertised_address_label"

    const val REMOTE_PORT_LABEL = "remote_port_label"

    const val SERVER_STATUS_LABEL = "server_status_label"

    const val SERVER_STATUS_RUNNING = "server_status_running"

    const val SERVER_STATUS_STOPPED = "server_status_stopped"

    // 操作反馈。
    const val COPY_SUCCEEDED_NOTICE = "copy_succeeded_notice"

    const val COPY_UNAVAILABLE_NOTICE = "copy_unavailable_notice"
}
