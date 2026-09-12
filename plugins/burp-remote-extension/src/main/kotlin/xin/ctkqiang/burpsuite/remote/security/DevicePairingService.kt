/**
 * Burp Remote —— 安全层 / 设备配对
 *
 * 实现 plan §55 配对流程中「插件侧」的全部职责：生成一次性配对会话、把会话渲染成票据、
 * 在移动端回传配对码时判定成败。配对是本系统唯一的安全入口——未经配对的身份不允许执行
 * 任何控制命令（plan §54）——因此本文件里不存在为了「方便联调」而放宽的路径。
 *
 * @author 钟智强
 */

package xin.ctkqiang.burpsuite.remote.security

import xin.ctkqiang.burpsuite.remote.protocol.DEFAULT_REMOTE_PORT
import xin.ctkqiang.burpsuite.remote.protocol.DeviceIdentifier
import xin.ctkqiang.burpsuite.remote.protocol.PairingChallengeIdentifier
import xin.ctkqiang.burpsuite.remote.protocol.PairingCode
import xin.ctkqiang.burpsuite.remote.protocol.PairingTicket
import xin.ctkqiang.burpsuite.remote.protocol.RejectionReason
import xin.ctkqiang.burpsuite.remote.protocol.RemoteProtocolVersion
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.HexFormat
import java.util.concurrent.atomic.AtomicReference

/**
 * 配对服务：签发配对会话，并校验移动端回传的配对码。
 *
 * 服务不持有连接、不启动线程、也不产生事件——它只做判定与登记，因此能在没有 Burp、没有网络
 * 的环境里被完整测试。
 *
 * 并发语义：界面线程在刷新二维码时调用 [openPairingSession]，将来的传输层会在网络线程上
 * 调用 [redeemPairingCode]。两者通过一个原子引用交换「当前待消费的会话」，因此无论调用
 * 顺序如何，同一张票据的配对码都只会成功一次。这里刻意不加锁：临界区只有一次比较并交换，
 * 用锁反而会把网络线程拖进界面线程的竞争里。
 *
 * @param localNetworkAddressResolver 用于取得写进票据的本机地址。
 * @param pairedDeviceRegistry 配对成功后登记设备的唯一权威。它必须是必填依赖而不是可选项：
 *   一个忘记接线的登记处会让配对「成功」得毫无痕迹，而授权的依据恰恰就是这份登记。
 * @param clock 时间源。必须显式注入：配对有效期与「已过期」的测试都依赖它，直接读取系统
 *   时间会让过期行为无法被确定性地验证。
 * @param remotePort 票据中公布的端口。默认值取自协议层，可配置，但不允许在此处硬编码。
 */
class DevicePairingService(
    private val localNetworkAddressResolver: LocalNetworkAddressResolver,
    private val pairedDeviceRegistry: PairedDeviceRegistry,
    private val clock: Clock,
    private val remotePort: Int = DEFAULT_REMOTE_PORT,
) {
    private val secureRandom = SecureRandom()

    private val pendingPairingSession = AtomicReference<PendingPairingSession?>(null)

    /**
     * 签发一张新的配对票据，并让它立刻成为唯一有效的票据。
     *
     * 新票据会顶替先前那张，无论后者是否仍在有效期内。这一点是安全要求而非实现细节：
     * 屏幕上只能显示一张二维码，若旧票据在后台继续有效，就等于存在一张操作者看不见、
     * 却仍可被使用的活凭证。
     *
     * 本函数无 I/O、无阻塞，可以在界面线程上直接调用。
     *
     * @return 可交给二维码编码器与移动端的配对票据。
     */
    fun openPairingSession(): PairingTicket {
        val pairingCode = generatePairingCode()
        val challengeIdentifier = PairingChallengeIdentifier("challenge_${randomHexText(IDENTIFIER_BYTE_LENGTH)}")
        val expiresAt = clock.instant().plus(pairingValidity)

        // 直接覆盖而不排队：同一时刻只允许存在一张有效票据。
        pendingPairingSession.set(PendingPairingSession(challengeIdentifier, pairingCode, expiresAt))

        return PairingTicket(
            protocolVersion = RemoteProtocolVersion.CURRENT_PROTOCOL_VERSION,
            // 探测不到局域网地址时写入回环地址，而不是留空：票据字段保持非空，
            // 操作者也能一眼看出地址不对——界面上的 127.0.0.1 比一个空白字段更容易被察觉。
            host = localNetworkAddressResolver.resolveAdvertisedHostAddress() ?: LOOPBACK_HOST_ADDRESS,
            port = remotePort,
            challengeIdentifier = challengeIdentifier,
            pairingCode = pairingCode,
            expiresAt = expiresAt,
        )
    }

    /**
     * 校验移动端提交的会话标识与配对码，并在成功时分配设备身份。
     *
     * 判定顺序（存在 → 未过期 → 标识一致 → 配对码一致）本身是安全要求：任何一步被调换，
     * 都会让一个本应失效的会话有机会配对成功。
     *
     * 成功后本次会话立即被消费，同一张票据无法再次配对；被拒绝时则保留会话，让操作者
     * 可以重试，直到票据真正过期。配对成功后设备会立刻登记进 [pairedDeviceRegistry]，
     * 登记与返回之间不留空档——见方法末尾的说明。
     *
     * @param submittedChallengeIdentifier 移动端回传的会话标识，取自它所扫描的那张票据。
     * @param submittedPairingCode 移动端提交的配对码。
     * @return 配对结局。判定为「失败」的细节不会出现在返回值里——配对的失败原因只有拒绝，
     *   没有「差一点成功」这种状态。
     */
    fun redeemPairingCode(
        submittedChallengeIdentifier: PairingChallengeIdentifier,
        submittedPairingCode: PairingCode,
    ): PairingOutcome {
        val candidateSession =
            pendingPairingSession.get() ?: return PairingOutcome.Rejected(RejectionReason.PairingSessionNotAvailable)

        val rejectionReason =
            findRejectionReason(candidateSession, submittedChallengeIdentifier, submittedPairingCode)
        if (rejectionReason != null) {
            return PairingOutcome.Rejected(rejectionReason)
        }

        // 比较并交换就是「消费」：两个线程同时提交正确的配对码时，只有一个能把会话取走，
        // 另一个必然失败。先判断、再单独删除会留下一个能被并发穿过的窗口。
        if (!pendingPairingSession.compareAndSet(candidateSession, null)) {
            return PairingOutcome.Rejected(RejectionReason.PairingSessionNotAvailable)
        }

        val deviceIdentifier = issueDeviceIdentity()
        // 先登记再返回。身份一旦离开本方法，调用方就可能立刻拿它去执行控制命令；若登记放在
        // 返回之后，中间会存在一个「设备已被允许、登记处却查不到它」的窗口，而这个窗口恰好
        // 是审计最需要排除的东西。
        pairedDeviceRegistry.recordPairedDevice(deviceIdentifier, clock.instant())
        return PairingOutcome.Succeeded(deviceIdentifier)
    }

    /**
     * 依次判定这次提交是否必须被拒绝。
     *
     * @return 应当拒绝的原因；返回 null 表示这次提交可以继续走消费流程。
     */
    private fun findRejectionReason(
        candidateSession: PendingPairingSession,
        submittedChallengeIdentifier: PairingChallengeIdentifier,
        submittedPairingCode: PairingCode,
    ): RejectionReason? {
        if (hasExpired(candidateSession)) {
            return RejectionReason.PairingSessionExpired
        }
        if (candidateSession.challengeIdentifier != submittedChallengeIdentifier) {
            return RejectionReason.PairingSessionNotAvailable
        }
        if (!matchesInConstantTime(candidateSession.pairingCode, submittedPairingCode)) {
            return RejectionReason.PairingCodeMismatch
        }
        return null
    }

    /**
     * 判断会话是否已经超出有效期。读作一个是非问句，便于在条件里直接使用。
     */
    private fun hasExpired(pairingSession: PendingPairingSession): Boolean =
        clock.instant().isAfter(pairingSession.expiresAt)

    /**
     * 以与内容无关的耗时比较两个配对码。
     *
     * 直接用字符串相等会在第一个不同的字符处提前返回，攻击者据此可以逐位试探出正确的
     * 配对码——耗时差异本身就是信息泄漏。`MessageDigest.isEqual` 的比较耗时与内容无关，
     * 是 JDK 为校验码比较提供的恒定时间实现。
     */
    private fun matchesInConstantTime(
        expectedPairingCode: PairingCode,
        submittedPairingCode: PairingCode,
    ): Boolean =
        MessageDigest.isEqual(
            expectedPairingCode.value.toByteArray(Charsets.UTF_8),
            submittedPairingCode.value.toByteArray(Charsets.UTF_8),
        )

    /**
     * 生成一次性配对码。
     *
     * 字符集刻意剔除了 I、L、O、0、1：它们在多数无衬线字体下几乎无法区分。扫码是主通道，
     * 但人工抄写是必要的备用通道，一个看起来像 0 的 O 会让操作者反复失败却找不到原因。
     */
    private fun generatePairingCode(): PairingCode {
        val pairingCodeCharacters = CharArray(PAIRING_CODE_LENGTH)
        for (position in pairingCodeCharacters.indices) {
            pairingCodeCharacters[position] = PAIRING_CODE_ALPHABET[secureRandom.nextInt(PAIRING_CODE_ALPHABET.length)]
        }
        return PairingCode(String(pairingCodeCharacters))
    }

    /**
     * 为设备分配一个新的身份。
     *
     * 身份由插件生成而不是由移动端上报：由客户端自报身份，等于允许任意设备声称自己是
     * 已配对的那一台。
     */
    private fun issueDeviceIdentity(): DeviceIdentifier =
        DeviceIdentifier("device_${randomHexText(DEVICE_IDENTIFIER_BYTE_LENGTH)}")

    /**
     * 生成指定字节数的密码学随机数，并写成十六进制文本。
     *
     * 使用 [SecureRandom] 而不是普通随机数生成器：配对码与设备身份都是安全边界上的值，
     * 可预测的伪随机序列会让攻击者直接算出下一个身份。
     */
    private fun randomHexText(byteLength: Int): String {
        val randomBytes = ByteArray(byteLength)
        secureRandom.nextBytes(randomBytes)
        return HexFormat.of().formatHex(randomBytes)
    }

    private companion object {
        /**
         * 配对码字符集：26 个字母去掉 I、L、O，再加上 2 到 9。
         *
         * 8 位、31 个字符的可选空间约合 40 比特，在只有数分钟的有效期与传输层速率限制之下，
         * 穷举不可行；同时 8 位短到可以手工抄写。
         */
        private const val PAIRING_CODE_ALPHABET = "ABCDEFGHJKMNPQRSTVWXYZ23456789"

        private const val PAIRING_CODE_LENGTH = 8

        /**
         * 配对票据的有效期。
         *
         * 五分钟取自两个约束的交集：短到屏幕被旁人拍下也几乎没有利用价值，长到操作者
         * 有充裕时间拿起手机完成扫描。
         *
         * 这里用 `val` 而非 `const val`——[Duration] 不是编译期常量；按 ktlint 的命名约定，
         * 非 `const` 的成员值使用小驼峰，`const val` 才使用全大写。
         */
        private val pairingValidity: Duration = Duration.ofMinutes(5)

        /**
         * 配对会话标识的随机字节数。标识会出现在日志与审计记录里，因此它只需要唯一，
         * 不需要像凭证那样难以猜测。
         */
        private const val IDENTIFIER_BYTE_LENGTH = 12

        /** 设备身份的随机字节数。 */
        private const val DEVICE_IDENTIFIER_BYTE_LENGTH = 16

        /** 没有可用局域网地址时写入票据的回环地址。 */
        private const val LOOPBACK_HOST_ADDRESS = "127.0.0.1"
    }
}

/**
 * 一次尚未被消费的配对会话。
 *
 * 只保存判定配对成败所需的最小事实，并且只在 [DevicePairingService] 内部流转。这里刻意
 * 不保存设备信息：设备身份是在配对成功的那一刻才被分配的，提前放进来只会诱使实现
 * 在配对失败时也留下痕迹。
 *
 * @property challengeIdentifier 该会话的标识，用于识别回传的提交属于哪一次配对尝试。
 * @property pairingCode 该会话的一次性配对码。
 * @property expiresAt 该会话失效的绝对时刻。
 */
private data class PendingPairingSession(
    val challengeIdentifier: PairingChallengeIdentifier,
    val pairingCode: PairingCode,
    val expiresAt: Instant,
)
