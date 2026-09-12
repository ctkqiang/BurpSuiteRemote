package xin.ctkqiang.burpsuite.remote.userinterface

import xin.ctkqiang.burpsuite.remote.protocol.DeviceIdentifier
import xin.ctkqiang.burpsuite.remote.protocol.PairedDevice
import xin.ctkqiang.burpsuite.remote.protocol.PairingTicket
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Font
import java.awt.GridBagLayout
import java.awt.GridLayout
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.awt.event.KeyEvent
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import javax.swing.BorderFactory
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.Timer

/** Burp Remote 标签页的面板。文案全部取自资源束，自身不发网络请求，因此不会阻塞 Burp 的界面线程。 */
class RemoteStatusPanel(
    extensionName: String,
    private val clock: Clock,
    private val locale: Locale,
    private val isRemoteServerRunning: () -> Boolean,
    private val pairingTicketSupplier: () -> PairingTicket,
    private val pairedDeviceSupplier: () -> List<PairedDevice>,
    private val pairedDeviceRevoker: (DeviceIdentifier) -> Unit,
) : JPanel(BorderLayout()) {
    // 只由界面线程读写：Swing 的事件分发模型保证组件状态不会被并发访问，无需同步。
    private var currentTicketExpiry: Instant? = null

    // 构造期解析一次，不做运行期切换：换语言要逐个控件重建，而系统语言在进程内几乎不变。
    private val localisedText = LocalisedText.forLocale(locale)

    // 用本地化的中等时间格式而非写死 HH:mm:ss；时区也在构造期定下，免得同一时刻前后显示不一致。
    private val localTimeFormatter =
        DateTimeFormatter.ofLocalizedTime(FormatStyle.MEDIUM)
            .withLocale(locale)
            .withZone(ZoneId.systemDefault())

    private val pairingCodeValue =
        SelectableValueField().apply {
            // 用等宽字体并放大：成比例字体下 0 与 O、1 与 l 太像，逐字符核对时极易抄错。
            font = Font(Font.MONOSPACED, Font.BOLD, PAIRING_CODE_FONT_SIZE)
        }

    private val validityLabel = JLabel()

    // 与 feedbackLabel 分开：指引是常驻信息，提示语是瞬时反馈，共用一个标签会让指引被顶掉后再不回来。
    private val instructionLabel = createSecondaryLabel()

    // 初始为空，由 showFeedback 填入并在数秒后清空，不会长期占位。
    private val feedbackLabel = createSecondaryLabel()

    // 服务已监听时这句「尚未开放」就不成立，因此它必须是一个可切换显示的字段，而不是构造期写死的一段文字。
    private val pendingEndpointNoticeLabel = createSecondaryLabel(localisedText.text(TextKey.PENDING_ENDPOINT_NOTICE))

    private val serverStatusValueLabel = JLabel()

    // 用 GridBagLayout：BorderLayout 会把子组件拉伸填满，二维码一旦不是正方形就扫不出来。
    private val qrCodeHolder = JPanel(GridBagLayout())

    // 每次刷新整体重建：来源是登记处的快照，按差异修补会多出一类「界面与真实状态不一致」的缺陷。
    private val pairedDeviceRows =
        JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
        }

    private val protocolVersionValueLabel = JLabel()

    // 也做成可选中的取值：扫码之外还有手工输入地址这条通道，而 IP 最容易抄错且界面上没有提示。
    private val advertisedAddressValue = SelectableValueField(ADVERTISED_ADDRESS_COLUMNS)

    private val remotePortValueLabel = JLabel()

    // 只在组件挂上显示树时运行；回调在界面线程，且只改一个标签、不发起任何 I/O。
    private val remainingValidityTimer =
        Timer(REMAINING_VALIDITY_TICK_MILLISECONDS) {
            updateRemainingValidity()
        }

    // 不复用倒计时定时器：那个每秒重复，这个只响一次，合在一起就得在回调里判断这次响是为了什么。
    private val feedbackClearTimer =
        Timer(FEEDBACK_VISIBLE_MILLISECONDS) {
            clearFeedback()
        }.apply {
            isRepeats = false
        }

    init {
        border = BorderFactory.createEmptyBorder(PANEL_PADDING, PANEL_PADDING, PANEL_PADDING, PANEL_PADDING)

        // 整体钉在 NORTH：BorderLayout 的 CENTER 会把区块拉满剩余空间，卡片之间会多出大片空白。
        val content = JPanel()
        content.layout = BoxLayout(content, BoxLayout.Y_AXIS)
        content.add(buildHeader(extensionName))
        content.add(Box.createVerticalStrut(SECTION_SPACING))
        content.add(buildPairingSection())
        content.add(Box.createVerticalStrut(SECTION_SPACING))
        content.add(buildPairedDeviceSection())
        content.add(Box.createVerticalStrut(SECTION_SPACING))
        content.add(buildRuntimeSection())

        add(content, BorderLayout.NORTH)

        refreshPairingTicket()
        refreshPairedDevices()
        refreshServerStatus()
    }

    // addNotify 是「已挂上显示树」的时机，在这里启动可以避免为从未显示过的面板空转定时器。
    override fun addNotify() {
        super.addNotify()
        remainingValidityTimer.start()
    }

    // 与 addNotify 成对：定时器持有本面板引用并每秒唤醒事件线程，卸载后会让这块界面既不被回收也无法刷新。
    override fun removeNotify() {
        remainingValidityTimer.stop()
        // 一次性定时器也要停：若它响时面板已离开显示树，那次回调就是在给一块不可见的界面写文字。
        feedbackClearTimer.stop()
        super.removeNotify()
    }

    private fun buildHeader(extensionName: String): JPanel {
        val header = JPanel()
        header.layout = BoxLayout(header, BoxLayout.Y_AXIS)

        val titleLabel = JLabel(extensionName)
        titleLabel.font = deriveFont(titleLabel.font, TITLE_FONT_SIZE_DELTA, Font.BOLD)

        header.add(titleLabel)
        header.add(Box.createVerticalStrut(LINE_SPACING))
        header.add(pendingEndpointNoticeLabel)
        return header
    }

    private fun buildPairingSection(): JPanel {
        val pairingContent = JPanel(BorderLayout(SECTION_SPACING, 0))
        pairingContent.add(qrCodeHolder, BorderLayout.WEST)

        val pairingDetails = JPanel()
        pairingDetails.layout = BoxLayout(pairingDetails, BoxLayout.Y_AXIS)
        pairingDetails.add(createSecondaryLabel(localisedText.text(TextKey.PAIRING_CODE_CAPTION)))
        pairingDetails.add(pairingCodeValue)
        pairingDetails.add(Box.createVerticalStrut(LINE_SPACING))
        pairingDetails.add(validityLabel)
        pairingDetails.add(Box.createVerticalStrut(LINE_SPACING))
        pairingDetails.add(instructionLabel)
        pairingDetails.add(Box.createVerticalStrut(LINE_SPACING))
        pairingDetails.add(feedbackLabel)
        pairingDetails.add(Box.createVerticalStrut(LINE_SPACING))
        pairingDetails.add(buildPairingActions())

        // 钉在 NORTH：CENTER 的高度取自整块配对区（由二维码决定），多余空间会落到按钮行上把它推远。
        val detailsContainer = JPanel(BorderLayout())
        detailsContainer.add(pairingDetails, BorderLayout.NORTH)

        pairingContent.add(detailsContainer, BorderLayout.CENTER)
        return SectionCard(localisedText.text(TextKey.PAIRING_SECTION_HEADING), pairingContent)
    }

    private fun buildPairingActions(): JPanel {
        val actions = JPanel()
        actions.layout = BoxLayout(actions, BoxLayout.X_AXIS)
        // 先放弹簧再放按钮，按钮组靠右，不会挤在配对码下方打乱阅读顺序。
        actions.add(Box.createHorizontalGlue())

        // 助记键固定为 R 与 C，不随语言变：靠肌肉记忆的键位在换语言后改名，比没有助记键更糟。
        val refreshButton = JButton(localisedText.text(TextKey.REFRESH_PAIRING_CODE_LABEL))
        refreshButton.mnemonic = KeyEvent.VK_R
        // 只用加粗区分主次，不给按钮填色——填色得写死色值，必然在其中一种主题下刺眼。
        refreshButton.font = refreshButton.font.deriveFont(Font.BOLD)
        refreshButton.addActionListener {
            // 顺带刷新设备列表：这是当前唯一的人工刷新入口，设备能经网络配对后应改为由登记处主动通知。
            refreshPairingTicket()
            refreshPairedDevices()
            refreshServerStatus()
        }
        actions.add(refreshButton)
        actions.add(Box.createHorizontalStrut(BUTTON_SPACING))

        val copyButton = JButton(localisedText.text(TextKey.COPY_PAIRING_CODE_LABEL))
        copyButton.mnemonic = KeyEvent.VK_C
        copyButton.addActionListener { copyPairingCodeToClipboard() }
        actions.add(copyButton)
        return actions
    }

    private fun buildRuntimeSection(): JPanel {
        // 用网格而非写死坐标：Burp 允许用户放大界面字体，坐标在字体变化后必然错位。
        val details = JPanel(GridLayout(0, DETAIL_COLUMNS, DETAIL_HORIZONTAL_GAP, DETAIL_VERTICAL_GAP))
        details.add(createSecondaryLabel(localisedText.text(TextKey.PROTOCOL_VERSION_LABEL)))
        details.add(protocolVersionValueLabel)
        details.add(createSecondaryLabel(localisedText.text(TextKey.ADVERTISED_ADDRESS_LABEL)))
        details.add(advertisedAddressValue)
        details.add(createSecondaryLabel(localisedText.text(TextKey.REMOTE_PORT_LABEL)))
        details.add(remotePortValueLabel)
        details.add(createSecondaryLabel(localisedText.text(TextKey.SERVER_STATUS_LABEL)))
        details.add(serverStatusValueLabel)

        // 钉在 WEST，宽度才收敛为两列的首选宽度；否则 BoxLayout 拉满后标签贴左、取值落到屏幕中央。
        val detailsRow = JPanel(BorderLayout())
        detailsRow.add(details, BorderLayout.WEST)
        return SectionCard(localisedText.text(TextKey.RUNTIME_SECTION_HEADING), detailsRow)
    }

    // 排在运行参数之前：排错时「移除一台不该被信任的设备」比「端口是多少」紧迫。
    private fun buildPairedDeviceSection(): JPanel {
        return SectionCard(localisedText.text(TextKey.PAIRED_DEVICE_SECTION_HEADING), pairedDeviceRows)
    }

    // 读实时状态而不是构造期的快照：服务器可能在面板显示之后才启动或停止。
    private fun refreshServerStatus() {
        val isRunning = isRemoteServerRunning()
        serverStatusValueLabel.text =
            localisedText.text(if (isRunning) TextKey.SERVER_STATUS_RUNNING else TextKey.SERVER_STATUS_STOPPED)
        // 服务已在监听时「尚未开放」已经不成立，留着只会把排查方向带到网络上。
        pendingEndpointNoticeLabel.isVisible = !isRunning
    }

    // 读快照整体重建，界面不留本地副本：副本一旦与真实状态脱节，就会出现「已经踢掉的设备还列在表里」。
    private fun refreshPairedDevices() {
        val pairedDevices = pairedDeviceSupplier()
        pairedDeviceRows.removeAll()

        if (pairedDevices.isEmpty()) {
            pairedDeviceRows.add(createSecondaryLabel(localisedText.text(TextKey.PAIRED_DEVICE_EMPTY_NOTICE)))
        } else {
            for (pairedDevice in pairedDevices) {
                // 间隔加在行与行之间，而不是每行后面都补一个，否则最后一行下方会多出一段悬空空白。
                if (pairedDeviceRows.componentCount > 0) {
                    pairedDeviceRows.add(Box.createVerticalStrut(LINE_SPACING))
                }
                pairedDeviceRows.add(buildPairedDeviceRow(pairedDevice))
            }
        }

        // Swing 不会自动发现子组件集合已经变化，换过之后必须显式重新布局。
        pairedDeviceRows.revalidate()
        pairedDeviceRows.repaint()
    }

    // 身份做成可选中的取值：它是操作者事后「请把这一台加回来」的凭据，必须能被完整复制出去。
    private fun buildPairedDeviceRow(pairedDevice: PairedDevice): JPanel {
        val deviceIdentifierValue = SelectableValueField()
        deviceIdentifierValue.text = pairedDevice.deviceIdentifier.value

        val pairedAtLabel =
            createSecondaryLabel(
                localisedText.format(TextKey.PAIRED_DEVICE_PAIRED_AT, formatLocalTime(pairedDevice.pairedAt)),
            )

        val deviceDetails = JPanel()
        deviceDetails.layout = BoxLayout(deviceDetails, BoxLayout.Y_AXIS)
        deviceDetails.add(deviceIdentifierValue)
        deviceDetails.add(pairedAtLabel)

        val row = JPanel(BorderLayout(SECTION_SPACING, 0))
        row.add(deviceDetails, BorderLayout.CENTER)

        val removeButton = JButton(localisedText.text(TextKey.REMOVE_PAIRED_DEVICE_LABEL))
        removeButton.addActionListener { removePairedDevice(pairedDevice.deviceIdentifier) }
        row.add(removeButton, BorderLayout.EAST)
        return row
    }

    // 先请求再重读：界面只反映登记处的真实结果。反馈带上身份标识，误点时才说得清踢掉的是哪一台。
    private fun removePairedDevice(deviceIdentifier: DeviceIdentifier) {
        pairedDeviceRevoker(deviceIdentifier)
        refreshPairedDevices()
        showFeedback(localisedText.format(TextKey.PAIRED_DEVICE_REMOVED_NOTICE, deviceIdentifier.value))
    }

    // 刷新即作废旧票据，因此不做「沿用未过期票据」的优化，否则操作者会对着失效的二维码反复扫。
    private fun refreshPairingTicket() {
        val pairingTicket = pairingTicketSupplier()

        pairingCodeValue.text = pairingTicket.pairingCode.value
        protocolVersionValueLabel.text = pairingTicket.protocolVersion.toString()
        advertisedAddressValue.text = pairingTicket.host
        remotePortValueLabel.text = pairingTicket.port.toString()
        instructionLabel.text = localisedText.text(TextKey.PAIRING_INSTRUCTION)
        // 换票据时清掉上一次的反馈：那句话讲的事情已经随着旧票据一起作废。
        clearFeedback()
        currentTicketExpiry = pairingTicket.expiresAt

        qrCodeHolder.removeAll()
        qrCodeHolder.add(PairingQrCodeView(PairingQrCodeRenderer.render(pairingTicket)))
        // Swing 不会自动发现子组件集合已经变化，漏掉这两行界面上留下的仍是上一张二维码。
        qrCodeHolder.revalidate()
        qrCodeHolder.repaint()

        updateRemainingValidity()
    }

    // 剩余时长按绝对时刻现算：递减计数器会累积误差，系统休眠后还会与安全层的判定脱节。
    private fun updateRemainingValidity() {
        val ticketExpiry = currentTicketExpiry ?: return
        val remaining = Duration.between(clock.instant(), ticketExpiry)
        if (remaining.isNegative) {
            validityLabel.text = localisedText.text(TextKey.PAIRING_CODE_EXPIRED)
            return
        }
        validityLabel.text =
            localisedText.format(
                TextKey.PAIRING_VALIDITY_REMAINING,
                formatLocalTime(ticketExpiry),
                describeRemaining(remaining),
            )
    }

    // 票据里存的是绝对时刻，只在展示时转本地时区，手机与电脑时区不同也不影响判定。
    private fun formatLocalTime(instant: Instant): String = localTimeFormatter.format(instant)

    // 单位词随语言变（分/秒 与 min/sec），整句连同单位交给资源文件，不在代码里拼。
    private fun describeRemaining(remaining: Duration): String {
        val totalSeconds = remaining.seconds.coerceAtLeast(0)
        return localisedText.format(
            TextKey.REMAINING_DURATION,
            totalSeconds / SECONDS_PER_MINUTE,
            totalSeconds % SECONDS_PER_MINUTE,
        )
    }

    private fun copyPairingCodeToClipboard() {
        val pairingCode = pairingCodeValue.text
        try {
            Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(pairingCode), null)
            showFeedback(localisedText.text(TextKey.COPY_SUCCEEDED_NOTICE))
        } catch (expectedClipboardUnavailability: IllegalStateException) {
            // 剪贴板在无图形会话或被其它进程独占时会不可用；直接告诉操作者，别让它变成界面线程上的未捕获异常。
            showFeedback(localisedText.text(TextKey.COPY_UNAVAILABLE_NOTICE))
        }
    }

    // 每次都重启定时器：连续复制两次时，第一条提示的剩余时间不能把第二条一起提前抹掉。
    private fun showFeedback(feedbackText: String) {
        feedbackLabel.text = feedbackText
        feedbackClearTimer.restart()
    }

    private fun clearFeedback() {
        feedbackClearTimer.stop()
        feedbackLabel.text = ""
    }

    // 收在一个工厂里：漏掉任何一处派生字体，界面上就会出现一行与周围不协调的文字，评审时极难发现。
    private fun createSecondaryLabel(secondaryText: String = ""): JLabel =
        JLabel(secondaryText).apply {
            font = deriveFont(font, SECONDARY_FONT_SIZE_DELTA)
            alignmentX = Component.LEFT_ALIGNMENT
        }

    // 用相对增量而非写死字号：Burp 允许调大界面字体，写死字号会让层级在大字号主题下反转。
    private fun deriveFont(
        baseFont: Font,
        sizeDelta: Float,
        style: Int = baseFont.style,
    ): Font = baseFont.deriveFont(style, baseFont.size2D + sizeDelta)

    // 只放留白、字号增量与刷新间隔这类与语言无关的数值，面向操作者的文案一律走 localisedText。
    private companion object {
        private const val PANEL_PADDING = 16

        private const val SECTION_SPACING = 16

        private const val LINE_SPACING = 6

        private const val BUTTON_SPACING = 8

        private const val DETAIL_COLUMNS = 2

        private const val DETAIL_HORIZONTAL_GAP = 12

        private const val DETAIL_VERTICAL_GAP = 6

        private const val TITLE_FONT_SIZE_DELTA = 6f

        private const val SECONDARY_FONT_SIZE_DELTA = -2f

        private const val PAIRING_CODE_FONT_SIZE = 22

        // IPv4 点分十进制最长 15 个字符，固定这个宽度既不截断，也不会因地址长短变化让布局左右跳动。
        private const val ADVERTISED_ADDRESS_COLUMNS = 15

        // 四秒：短到不会长期占位，长到移开视线再回来看还能看到刚才那句提示。
        private const val FEEDBACK_VISIBLE_MILLISECONDS = 4000

        private const val REMAINING_VALIDITY_TICK_MILLISECONDS = 1000

        private const val SECONDS_PER_MINUTE = 60L
    }
}
