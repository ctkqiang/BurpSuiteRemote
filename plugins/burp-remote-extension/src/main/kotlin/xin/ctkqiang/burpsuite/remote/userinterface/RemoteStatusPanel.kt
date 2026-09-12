/**
 * Burp Remote —— 界面层 / 状态面板
 *
 * 提供扩展在 Burp 主窗口中所占标签页的实际内容。标签页不会因为 JAR 里存在
 * `BurpExtension` 实现就自动出现：扩展必须把一个 `java.awt.Component` 交给
 * `burp.api.montoya.ui.UserInterface.registerSuiteTab`，标签页才会被创建，
 * 而本文件提供的正是那个组件。
 *
 * 面板只呈现注入给它的配对票据，自身不探测网络、不生成凭证、不持有连接，也不安排任何
 * 后台工作，因此不会在 Burp 的界面线程上引入阻塞。
 *
 * @author 钟智强
 */

package xin.ctkqiang.burpsuite.remote.userinterface

import xin.ctkqiang.burpsuite.remote.protocol.DeviceIdentifier
import xin.ctkqiang.burpsuite.remote.protocol.PairedDevice
import xin.ctkqiang.burpsuite.remote.protocol.PairingTicket
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Font
import java.awt.GridBagLayout
import java.awt.GridLayout
import java.awt.Insets
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
import javax.swing.JTextField
import javax.swing.Timer

/**
 * Burp Remote 标签页所展示的状态面板。
 *
 * 界面按「先讲状态、再给入口、最后列参数」排布。这个顺序不是审美取舍：配对是使用远程
 * 控制的前置条件，把它放在最上方，操作者就不会先看到端口号、误以为服务已经在监听。
 *
 * 选择继承 `JPanel` 而不是直接实现 `java.awt.Component`，是因为 Burp 对组件唯一的要求就是
 * `Component`，而 `JPanel` 自带布局管理。改用绝对坐标后，用户一旦调整 Burp 的界面字体或
 * 切换主题，控件会立刻错位。
 *
 * 界面上的每一段文字都来自资源束，本文件里没有任何面向操作者的字符串字面量：文案随操作
 * 系统的语言设置变化，而写死在控件里的句子只能有一种语言（见 rules.md §9）。
 *
 * @param extensionName 扩展的展示名称。它必须与「已安装扩展」列表中的名字一致，否则用户
 *   无法把标签页归属到正确的扩展上。
 * @param clock 时间源。面板用它计算剩余有效期，注入而非直接读取系统时间，是为了让「已过期」
 *   这一状态的展示可被测试，而不是只能在真实等待五分钟之后才看得到。
 * @param locale 界面语言，由调用方按操作系统的语言设置注入。面板绝不自行读取默认语言环境：
 *   在控件内部读环境，等于把「界面语言从哪来」藏进一个无法替换的调用，而它恰恰是测试与
 *   多语言评审最需要替换的东西。
 * @param pairingTicketSupplier 取票据的动作。面板不生成凭证，只负责展示：票据的签发、
 *   有效期与作废策略都属于安全层的职责。
 * @param pairedDeviceSupplier 取已配对设备列表的动作。面板不做过滤也不做排序，读到什么就
 *   显示什么——「谁被信任」这件事的答案只能有一个来源。
 * @param pairedDeviceRevoker 移除一台已配对设备的动作。移除是安全动作，面板只负责发出请求，
 *   是否真的从登记处消失由安全层决定；因此移除之后面板会重新读一遍列表，而不是自行删掉那一行。
 */
class RemoteStatusPanel(
    extensionName: String,
    private val clock: Clock,
    private val locale: Locale,
    private val pairingTicketSupplier: () -> PairingTicket,
    private val pairedDeviceSupplier: () -> List<PairedDevice>,
    private val pairedDeviceRevoker: (DeviceIdentifier) -> Unit,
) : JPanel(BorderLayout()) {
    /**
     * 当前票据的失效时刻。
     *
     * 只由界面线程读写：Swing 的事件分发模型保证组件状态不会被并发访问，因此这里既不需要
     * 同步，也不需要把它设计成不可变结构。
     */
    private var currentTicketExpiry: Instant? = null

    /**
     * 界面文案的查询入口，按注入的语言在构造期解析一次。
     *
     * 刻意不做运行期切换：Swing 组件换语言需要逐个控件重建或调用 `updateUI`，而操作系统的
     * 语言设置在进程运行期间几乎不会改变。为它引入一套热切换机制，代价远大于收益，还会
     * 引出「一部分控件已换语言、另一部分还没换」这种中间态。
     */
    private val localisedText = LocalisedText.forLocale(locale)

    /**
     * 失效时刻的展示格式。
     *
     * 使用本地的中等时间格式，而不是写死 `HH:mm:ss`：同一个时刻在中文与德语下都是 24 小时制，
     * 在英语下则带 AM/PM。操作者看到的应当是符合自己语言习惯的形式。
     *
     * 时区在构造期确定，而不是每次格式化时重新读取系统默认时区：中途修改系统时区是极少数
     * 情况，每次读取只会让「同一时刻前后显示不同」变成难以复现的隐患。
     */
    private val localTimeFormatter =
        DateTimeFormatter.ofLocalizedTime(FormatStyle.MEDIUM)
            .withLocale(locale)
            .withZone(ZoneId.systemDefault())

    private val pairingCodeValue =
        createSelectableValue().apply {
            // 配对码是机器码，必须逐字符核对，因此改用等宽字体并放大字号。这里刻意不沿用
            // 界面字体：成比例字体下 0 与 O、1 与 l 的形状差异太小，抄写时极易出错。
            font = Font(Font.MONOSPACED, Font.BOLD, PAIRING_CODE_FONT_SIZE)
        }

    private val validityLabel = JLabel()

    /**
     * 常驻操作指引。
     *
     * 与 [feedbackLabel] 分开，是因为我曾经把它们合成一个标签：点一次「复制配对码」之后，
     * 指引被提示语顶掉，而且再也不会回来，操作者以为配对流程变了。指引是常驻信息，提示语是
     * 瞬时反馈，两者的生命周期不同，就不该共用一个控件。
     */
    private val instructionLabel = createSecondaryLabel()

    /**
     * 瞬时反馈（复制成功、剪贴板不可用等）。
     *
     * 初始为空，由 [showFeedback] 填入并安排若干秒后自行清空，因此它不会长期占据界面。
     */
    private val feedbackLabel = createSecondaryLabel()

    /**
     * 二维码的容器。
     *
     * 使用 `GridBagLayout` 而不是 `BorderLayout`：后者会把子组件拉伸到填满容器，二维码
     * 一旦被拉成非正方形就无法被扫描；`GridBagLayout` 让子组件保持自身首选尺寸并居中。
     */
    private val qrCodeHolder = JPanel(GridBagLayout())

    /**
     * 已配对设备列表的容器。
     *
     * 每次刷新都整体重建子组件，而不是增量地增删某一行：列表的来源是登记处的快照，
     * 「按快照重建」与「按差异修补」相比，少了一整类「界面和真实状态不一致」的缺陷。
     */
    private val pairedDeviceRows =
        JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
        }

    private val protocolVersionValueLabel = JLabel()

    /**
     * 配对地址同样做成可选中的取值。
     *
     * 扫码之外还有一条手工通道：操作者在手机上手动输入地址。IP 地址恰好是最容易被抄错的
     * 一类值——一个数字看错，界面上没有任何提示，只有「连不上」这一个结果。
     */
    private val advertisedAddressValue = createSelectableValue(ADVERTISED_ADDRESS_COLUMNS)

    private val remotePortValueLabel = JLabel()

    /**
     * 每秒刷新剩余有效期的定时器。
     *
     * 只在组件真正挂上显示树时运行（见 [addNotify]）。回调运行在界面线程，且只更新一个
     * 标签，不发起任何 I/O。
     */
    private val remainingValidityTimer =
        Timer(REMAINING_VALIDITY_TICK_MILLISECONDS) {
            updateRemainingValidity()
        }

    /**
     * 清空瞬时反馈的一次性定时器。
     *
     * 不复用 [remainingValidityTimer]：那个每秒重复触发，负责倒计时；这个只响一次。合成
     * 一个计时器，就得在回调里先判断「这次响是因为什么」，那是把两件事塞进一个回调解。
     */
    private val feedbackClearTimer =
        Timer(FEEDBACK_VISIBLE_MILLISECONDS) {
            clearFeedback()
        }.apply {
            isRepeats = false
        }

    init {
        border = BorderFactory.createEmptyBorder(PANEL_PADDING, PANEL_PADDING, PANEL_PADDING, PANEL_PADDING)

        // 标题与两张卡片纵向排列，并整体钉在 NORTH 上。之所以不把任何区块放进 CENTER：
        // BorderLayout 的 CENTER 会把区块拉满剩余空间，结果是卡片之间出现大片空白、
        // 二维码周围多出一圈无意义的背景；钉在 NORTH 时每个区块都保持自己的首选高度。
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
    }

    /**
     * 组件进入显示树时启动倒计时。
     *
     * `addNotify` 是 Swing 保证的「已经挂到显示树上」时机，在这里启动可以避免为一个从未
     * 显示过的面板空转一个定时器。窗口被最小化后重新展开时本方法会再次被调用，因此定时器
     * 也不需要额外的恢复逻辑。
     */
    override fun addNotify() {
        super.addNotify()
        remainingValidityTimer.start()
    }

    /**
     * 组件离开显示树时停止倒计时。
     *
     * 与 [addNotify] 成对存在，且必须在这里停止：定时器会持有本面板的引用并每秒唤醒事件
     * 线程，扩展被卸载而定时器仍在运行时，那块界面既不会被回收，也再无机会被刷新。
     */
    override fun removeNotify() {
        remainingValidityTimer.stop()
        // 一次性定时器同样要停：它虽然只会响一次，但如果响的时候面板已经离开显示树，
        // 那次回调就是在给一块不再可见的界面写文字。
        feedbackClearTimer.stop()
        super.removeNotify()
    }

    /**
     * 构建顶部标题区：扩展名，以及当前远程端点的真实状态。
     */
    private fun buildHeader(extensionName: String): JPanel {
        val header = JPanel()
        header.layout = BoxLayout(header, BoxLayout.Y_AXIS)

        val titleLabel = JLabel(extensionName)
        titleLabel.font = deriveFont(titleLabel.font, TITLE_FONT_SIZE_DELTA, Font.BOLD)

        header.add(titleLabel)
        header.add(Box.createVerticalStrut(LINE_SPACING))
        header.add(createSecondaryLabel(localisedText.text(TextKey.PENDING_ENDPOINT_NOTICE)))
        return header
    }

    /**
     * 构建配对区：左侧二维码，右侧配对码、有效期与操作。
     *
     * 整块区域用带标题的边框围起来，而不是只靠一行加粗标题。并排的两块内容若没有边界，
     * 操作者得先逐字读完才知道「哪几行属于同一件事」；边框把这层信息交给视觉去传达。
     *
     * 边框的线条与标题颜色一律留给当前的 Look and Feel 决定，此处不写死：Burp 允许在浅色
     * 与深色主题之间切换，写死的颜色必然在其中一种主题下失效。
     */
    private fun buildPairingSection(): JPanel {
        val section = JPanel(BorderLayout(SECTION_SPACING, 0))
        section.border = BorderFactory.createTitledBorder(localisedText.text(TextKey.PAIRING_SECTION_HEADING))
        section.add(qrCodeHolder, BorderLayout.WEST)

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

        // 右侧内容整体钉在 NORTH 上。写成 CENTER 也能显示，但 CENTER 的高度取自整块配对区，
        // 而配对区的高度由二维码决定——多出来的空间会全部落到按钮行上，把按钮推到远离
        // 配对码的位置。钉在 NORTH 才是「文字从上往下依次排列」。
        val detailsContainer = JPanel(BorderLayout())
        detailsContainer.add(pairingDetails, BorderLayout.NORTH)

        section.add(detailsContainer, BorderLayout.CENTER)
        return section
    }

    /**
     * 构建配对区的操作按钮。
     */
    private fun buildPairingActions(): JPanel {
        val actions = JPanel()
        actions.layout = BoxLayout(actions, BoxLayout.X_AXIS)
        // 先放弹簧、后放按钮，按钮组因此靠右：这一区的视觉焦点是配对码与二维码，
        // 按钮挤在它们下方会影响阅读顺序。
        actions.add(Box.createHorizontalGlue())

        // 两个按钮都挂助记键。键位固定为 R 与 C，不跟随各语言的按钮文案：助记键靠的是肌肉
        // 记忆，同一个位置在换语言之后突然变成另一个字母，比没有助记键更糟。
        val refreshButton = JButton(localisedText.text(TextKey.REFRESH_PAIRING_CODE_LABEL))
        refreshButton.mnemonic = KeyEvent.VK_R
        refreshButton.addActionListener {
            // 顺带刷新设备列表：这个按钮是当前唯一的人工刷新入口，而设备列表在没有传输层之前
            // 也只能由本面板改变。等到设备可以经网络配对时，列表必须改为由登记处主动通知刷新，
            // 而不是指望操作者记得回来点一下。
            refreshPairingTicket()
            refreshPairedDevices()
        }
        actions.add(refreshButton)
        actions.add(Box.createHorizontalStrut(BUTTON_SPACING))

        val copyButton = JButton(localisedText.text(TextKey.COPY_PAIRING_CODE_LABEL))
        copyButton.mnemonic = KeyEvent.VK_C
        copyButton.addActionListener { copyPairingCodeToClipboard() }
        actions.add(copyButton)
        return actions
    }

    /**
     * 构建运行参数区。
     */
    private fun buildRuntimeSection(): JPanel {
        val section = JPanel(BorderLayout())
        section.border = BorderFactory.createTitledBorder(localisedText.text(TextKey.RUNTIME_SECTION_HEADING))

        // 参数排成「标签—取值」两列。用网格而不是逐项指定坐标：Burp 允许用户放大界面字体，
        // 写死的坐标在字体变化之后必然错位，而网格由 Swing 在每次布局时重新计算。
        val details = JPanel(GridLayout(0, DETAIL_COLUMNS, DETAIL_HORIZONTAL_GAP, DETAIL_VERTICAL_GAP))
        details.add(createSecondaryLabel(localisedText.text(TextKey.PROTOCOL_VERSION_LABEL)))
        details.add(protocolVersionValueLabel)
        details.add(createSecondaryLabel(localisedText.text(TextKey.ADVERTISED_ADDRESS_LABEL)))
        details.add(advertisedAddressValue)
        details.add(createSecondaryLabel(localisedText.text(TextKey.REMOTE_PORT_LABEL)))
        details.add(remotePortValueLabel)

        // 网格钉在 WEST 上，宽度因此收敛为「标签列 + 取值列」的首选宽度。
        // 直接把它加进纵向布局是不行的：BoxLayout 会把它横向拉满整个标签页，而 GridLayout
        // 又把宽度平均分给两列，结果是「预留监听端口」贴在左边缘、它的取值落在屏幕中央，
        // 两者之间隔着一整屏空白，眼睛无法把它们连起来。
        val detailsRow = JPanel(BorderLayout())
        detailsRow.add(details, BorderLayout.WEST)
        section.add(detailsRow, BorderLayout.NORTH)
        return section
    }

    /**
     * 构建已配对设备区。
     *
     * 这一区回答的是「此刻谁被允许控制这台 Burp」。它紧跟在配对区之后、排在运行参数之前，
     * 因为排错时「移除一台不该被信任的设备」比「端口是多少」紧迫得多。
     */
    private fun buildPairedDeviceSection(): JPanel {
        val section = JPanel(BorderLayout())
        section.border =
            BorderFactory.createTitledBorder(localisedText.text(TextKey.PAIRED_DEVICE_SECTION_HEADING))
        section.add(pairedDeviceRows, BorderLayout.NORTH)
        return section
    }

    /**
     * 按登记处的当前状态重建设备列表。
     *
     * 读一次快照、整体重建，而不是增量地增删某一行，也不在界面上留一份「本地认为的设备列表」：
     * 界面持有的副本一旦与真实状态分离，就会出现「已经踢掉的设备仍显示在列表里」这种最不该
     * 出现在安全界面上的现象。
     */
    private fun refreshPairedDevices() {
        val pairedDevices = pairedDeviceSupplier()
        pairedDeviceRows.removeAll()

        if (pairedDevices.isEmpty()) {
            pairedDeviceRows.add(createSecondaryLabel(localisedText.text(TextKey.PAIRED_DEVICE_EMPTY_NOTICE)))
        } else {
            for (pairedDevice in pairedDevices) {
                // 间隔加在行与行之间，而不是每行后面都补一个：否则最后一行下方会多出一段
                // 悬空的空白，卡片看起来像被裁短了。
                if (pairedDeviceRows.componentCount > 0) {
                    pairedDeviceRows.add(Box.createVerticalStrut(LINE_SPACING))
                }
                pairedDeviceRows.add(buildPairedDeviceRow(pairedDevice))
            }
        }

        // 换过子组件之后必须显式重新布局：Swing 不会自动发现组件集合已经变化。
        pairedDeviceRows.revalidate()
        pairedDeviceRows.repaint()
    }

    /**
     * 构建一台设备所占的行：左侧是身份与配对时刻，右侧是移除按钮。
     *
     * 身份做成可选中的取值而不是普通文字，理由与配对码相同——它是操作者事后要用来说
     * 「请把这一台加回来」的凭据，必须能被完整复制出去。
     */
    private fun buildPairedDeviceRow(pairedDevice: PairedDevice): JPanel {
        val deviceIdentifierValue = createSelectableValue()
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

    /**
     * 移除一台已配对设备。
     *
     * 顺序是「先请求、再重读」，而不是「先把界面那一行删掉、再去请求」：界面必须反映登记处的
     * 真实结果，而不是我们的预期。反馈里带上被移除的身份标识——误点一行时，那是操作者唯一能
     * 知道「刚才踢掉的是哪一台」的线索；想让它回来，只能重新扫一张新票据。
     *
     * @param deviceIdentifier 待移除的设备身份。
     */
    private fun removePairedDevice(deviceIdentifier: DeviceIdentifier) {
        pairedDeviceRevoker(deviceIdentifier)
        refreshPairedDevices()
        showFeedback(localisedText.format(TextKey.PAIRED_DEVICE_REMOVED_NOTICE, deviceIdentifier.value))
    }

    /**
     * 取一张新票据并刷新全部展示。
     *
     * 刷新即意味着旧票据作废，因此这里不做「沿用未过期票据」的优化：界面上显示的必须始终
     * 是安全层此刻唯一有效的那张票据，否则操作者会对着一个已经失效的二维码反复尝试。
     */
    private fun refreshPairingTicket() {
        val pairingTicket = pairingTicketSupplier()

        pairingCodeValue.text = pairingTicket.pairingCode.value
        protocolVersionValueLabel.text = pairingTicket.protocolVersion.toString()
        advertisedAddressValue.text = pairingTicket.host
        remotePortValueLabel.text = pairingTicket.port.toString()
        instructionLabel.text = localisedText.text(TextKey.PAIRING_INSTRUCTION)
        // 换票据时顺手清掉上一次的操作反馈：那句话讲的事情已经随着旧票据一起作废了。
        clearFeedback()
        currentTicketExpiry = pairingTicket.expiresAt

        qrCodeHolder.removeAll()
        qrCodeHolder.add(PairingQrCodeView(PairingQrCodeRenderer.render(pairingTicket)))
        // 换过子组件之后必须显式重新布局：Swing 不会自动发现组件集合已经变化，
        // 漏掉这两行，界面上留下的仍然是上一张二维码。
        qrCodeHolder.revalidate()
        qrCodeHolder.repaint()

        updateRemainingValidity()
    }

    /**
     * 刷新剩余有效期。
     *
     * 剩余时长每次都按「失效时刻」与「当前时刻」现算，而不是递减一个计数器：递减会累积
     * 误差，也会在系统休眠之后与真实时间脱节，而票据是否有效最终是由安全层按绝对时间判定
     * 的，界面上的剩余时间必须与那个判定一致。
     */
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

    /**
     * 把时间点格式化为操作者所在时区的本地时间。
     *
     * 票据里的时刻是绝对时间点，只有在展示时才转换为本地时区；写入二维码的始终是绝对时间，
     * 因此手机与电脑的时区设置不同也不会影响判定结果。
     */
    private fun formatLocalTime(instant: Instant): String = localTimeFormatter.format(instant)

    /**
     * 把剩余时长描述成本地化的「几分几秒」。
     *
     * 单位词随语言变化——中文是「分／秒」，英语是「min／sec」——因此整句连同单位一起交给
     * 资源文件，而不是在这里拼接。拼接的写法在中文下看不出问题，换一种语言就会露出来。
     */
    private fun describeRemaining(remaining: Duration): String {
        val totalSeconds = remaining.seconds.coerceAtLeast(0)
        return localisedText.format(
            TextKey.REMAINING_DURATION,
            totalSeconds / SECONDS_PER_MINUTE,
            totalSeconds % SECONDS_PER_MINUTE,
        )
    }

    /**
     * 把配对码复制到系统剪贴板。
     */
    private fun copyPairingCodeToClipboard() {
        val pairingCode = pairingCodeValue.text
        try {
            Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(pairingCode), null)
            showFeedback(localisedText.text(TextKey.COPY_SUCCEEDED_NOTICE))
        } catch (expectedClipboardUnavailability: IllegalStateException) {
            // 剪贴板是操作系统资源，在无图形会话或剪贴板被其它进程独占时会不可用。这里把
            // 失败直接告诉操作者，而不是让它变成界面线程上的未捕获异常——后者只会留下
            // 一行栈信息，用户看不到任何提示。
            showFeedback(localisedText.text(TextKey.COPY_UNAVAILABLE_NOTICE))
        }
    }

    /**
     * 显示一段瞬时反馈，并安排它在数秒后自行清空。
     *
     * 每次调用都重新启动定时器：连续复制两次时，第二条提示必须从头开始计时，否则第一条
     * 提示的剩余时间会把第二条一起提前抹掉。
     *
     * @param feedbackText 待显示的反馈文案。
     */
    private fun showFeedback(feedbackText: String) {
        feedbackLabel.text = feedbackText
        feedbackClearTimer.restart()
    }

    /**
     * 清空瞬时反馈并撤销待执行的清空动作。
     */
    private fun clearFeedback() {
        feedbackClearTimer.stop()
        feedbackLabel.text = ""
    }

    /**
     * 造出一个「可选中但不可编辑」的取值控件。
     *
     * 用它代替 `JLabel` 的理由很具体：文本字段可以用鼠标选中并按 Ctrl+C，而标签不行。
     * 配对码与配对地址都是要原样抄到另一台设备上的机器值，只让操作者依赖「复制」按钮或
     * 肉眼抄写，等于把他的出错概率当成可接受成本。
     *
     * 关掉不透明与边框，是为了让它在视觉上仍然是一个静止的取值，而不是一个「等着你输入」的
     * 输入框——它一旦长得像输入框，操作者就会去点它，然后奇怪为什么打不了字。
     *
     * @param columns 首选宽度，以字符列数计；0 表示按当前内容自适应。
     */
    private fun createSelectableValue(columns: Int = CONTENT_WIDTH_COLUMNS): JTextField {
        val valueField = JTextField(columns)
        valueField.isEditable = false
        valueField.isOpaque = false
        valueField.border = null
        valueField.margin = Insets(0, 0, 0, 0)
        // 显式左对齐。Swing 组件的默认横向对齐是「居中」，而一个宽度不能随容器伸展的控件
        // 在纵向布局里就会被居中摆放——取值会比它上方的说明文字向右偏出一段，看上去像排版错误。
        valueField.alignmentX = Component.LEFT_ALIGNMENT
        return valueField
    }

    /**
     * 造出一个次级文字控件（字段说明、操作指引、瞬时反馈）。
     *
     * 把这件事收在一个工厂方法里，而不是在每个调用点各自调一次派生字体：漏掉任何一处，
     * 界面上就会出现一行与周围不协调的文字，而「哪一行漏了」在评审时极难被眼睛发现。
     *
     * @param secondaryText 初始文案，默认为空。
     */
    private fun createSecondaryLabel(secondaryText: String = ""): JLabel =
        JLabel(secondaryText).apply {
            font = deriveFont(font, SECONDARY_FONT_SIZE_DELTA)
            alignmentX = Component.LEFT_ALIGNMENT
        }

    /**
     * 按相对增量派生字体。
     *
     * 为什么不写死字号：Burp 允许用户调整界面字体大小，写死 18pt 的标题在小字号主题下显得
     * 突兀，在大字号主题下又会被正文反超，层级反而消失；相对增量在两种情况下都保持同样的
     * 比例关系。基准字体取自控件自身，因此主题换了字体也自动跟上。
     *
     * @param baseFont 作为基准的字体。
     * @param sizeDelta 字号增量，可为负。
     * @param style 字形，默认沿用基准字体的字形。
     * @return 派生出的字体。
     */
    private fun deriveFont(
        baseFont: Font,
        sizeDelta: Float,
        style: Int = baseFont.style,
    ): Font = baseFont.deriveFont(style, baseFont.size2D + sizeDelta)

    /**
     * 与语言无关的界面度量值。
     *
     * 面向操作者的文案不在这里：它们随操作系统的语言变化，一律经 [localisedText] 从资源束
     * 取得。只保留留白、字体增量与刷新间隔这类与语言无关的数值，是为了让「漏翻一段文案」
     * 只可能发生在资源文件里，而不会藏在这个对象的某个常量中。
     */
    private companion object {
        private const val PANEL_PADDING = 16

        private const val SECTION_SPACING = 16

        private const val LINE_SPACING = 6

        private const val BUTTON_SPACING = 8

        private const val DETAIL_COLUMNS = 2

        private const val DETAIL_HORIZONTAL_GAP = 12

        private const val DETAIL_VERTICAL_GAP = 6

        /**
         * 字号层级的相对增量。
         *
         * 层级不写成具体字号，而是相对当前主题字体做增减：Burp 允许用户调整界面字体大小，
         * 写死字号会在小字号主题下显得突兀、在大字号主题下被正文反超，层级反而消失。
         */
        private const val TITLE_FONT_SIZE_DELTA = 6f

        /** 次级文字（字段说明、操作指引、瞬时反馈）比正文小一档。 */
        private const val SECONDARY_FONT_SIZE_DELTA = -2f

        private const val PAIRING_CODE_FONT_SIZE = 22

        /**
         * 取值的首选宽度改为「按内容自适应」。
         *
         * `JTextField` 的列数为 0 时，宽度由当前内容算得。配对码长度由安全层决定，
         * 在这里写死一个列数等于把那个长度复制了一遍——安全层一旦调整长度，界面就会把
         * 末尾几位裁掉，而裁掉的部分在屏幕上「看起来没问题」。
         */
        private const val CONTENT_WIDTH_COLUMNS = 0

        /**
         * 配对地址的固定宽度，以字符列数计。
         *
         * IPv4 的点分十进制写法最长 15 个字符（255.255.255.255），因此固定成这个宽度既不会
         * 截断，也不会在刷新时因为地址长短变化而让整块布局左右跳动。
         */
        private const val ADVERTISED_ADDRESS_COLUMNS = 15

        /**
         * 瞬时反馈的停留时长。
         *
         * 四秒：短到不会长期占位，长到操作者移开视线再回来看也还能看到刚才那句提示。
         */
        private const val FEEDBACK_VISIBLE_MILLISECONDS = 4000

        private const val REMAINING_VALIDITY_TICK_MILLISECONDS = 1000

        private const val SECONDS_PER_MINUTE = 60L
    }
}
