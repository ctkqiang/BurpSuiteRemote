package xin.ctkqiang.burpsuite.remote.mobileapp.feature.connection

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.camera.core.ImageProxy
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import xin.ctkqiang.burpsuite.remote.mobileapp.core.protocol.PairingTicket
import xin.ctkqiang.burpsuite.remote.mobileapp.core.protocol.PairingTicketDecoder
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.settings.ThemeMode
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component.BurpRemoteButton
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component.BurpRemoteButtonStyle
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component.BurpRemoteCard
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component.BurpRemoteEmptyState
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component.BurpRemoteScannerCloseButton
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component.BurpRemoteText
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component.BurpRemoteTextField
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.rememberBurpRemoteHaptics
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.BurpRemoteRadius
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.BurpRemoteSizing
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.BurpRemoteSpacing
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.BurpsuiteRemoteTheme
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.LocalBurpRemoteDesignTokens
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * 扫码配对弹窗的内容（plan §55）。
 *
 * 契约由装配层冻结：`BurpRemoteScannerDialog(onDismiss, onScannedTicketText)`。装配层用
 * `Dialog(usePlatformDefaultWidth = false)` 全屏承载它，因此这里**没有 Action Bar**——标题、返回
 * 与状态栏都由壳负责；它自己带的是：顶部的分段控件（相机 / 手输）与圆形关闭入口、取景遮罩
 * （四角括号与扫描线）、手电筒开关、各阶段状态文案，以及手输配对码这条兜底路径。
 *
 * 票据在本地先校验：解不出、协议版本不符、已过期三类各自给出可执行的中文原因，协议不符时
 * 两个版本号都写出来——等服务端拒一次再告诉用户「其实这张票早过期了」没有任何好处（plan §55）。
 * 本地通过才把票据文本交给 [onScannedTicketText]，之后怎么连、怎么落地身份是装配层的事。
 */
@Composable
fun BurpRemoteScannerDialog(
    onDismiss: () -> Unit,
    onScannedTicketText: (String) -> Unit,
) {
    val haptics = rememberBurpRemoteHaptics()
    val context = LocalContext.current
    val stateHolder = remember { mutableStateOf(BurpRemoteScannerDialogUserInterfaceState()) }
    val effectChannel = remember { Channel<BurpRemoteScannerDialogUserInterfaceEffect>(Channel.BUFFERED) }

    // 只认二维码一种码制：留着全部码制会让识别变慢，而这里只可能扫到票据。
    val barcodeScanner: BarcodeScanner =
        remember {
            BarcodeScanning.getClient(
                BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build(),
            )
        }
    val decodingGuard = remember { BarcodeDecodingGuard() }

    DisposableEffect(barcodeScanner) {
        onDispose { barcodeScanner.close() }
    }

    // 票据文本只交出去一次：识别成功那条路与手输那条路共用这里，因此不存在「扫码能连、手输连不上」。
    LaunchedEffect(effectChannel, onScannedTicketText) {
        effectChannel.receiveAsFlow().collect { effect ->
            when (effect) {
                is BurpRemoteScannerDialogUserInterfaceEffect.TicketAccepted ->
                    onScannedTicketText(effect.encodedTicketText)
            }
        }
    }

    fun decodeTicketOrNull(encodedTicketText: String): PairingTicket? =
        try {
            PairingTicketDecoder.decodeFromText(encodedTicketText)
        } catch (malformedTicket: IllegalArgumentException) {
            // 二维码里可能是任意一张名片、一条链接；解不出来是正常结果，不是故障。
            null
        }

    fun reject(
        rejection: PairingTicketRejection,
        ticket: PairingTicket?,
    ) {
        haptics.warning()
        stateHolder.value =
            stateHolder.value.copy(
                rejection = rejection,
                rejectionTicketProtocolVersion = ticket?.protocolVersion,
                rejectionTicketExpiresAt = ticket?.expiresAt,
            )
    }

    fun submitTicketText(encodedTicketText: String) {
        val ticket = decodeTicketOrNull(encodedTicketText)
        if (ticket == null) {
            reject(rejection = PairingTicketRejection.Undecodable, ticket = null)
            return
        }
        if (ticket.protocolVersion != stateHolder.value.supportedProtocolVersion) {
            reject(rejection = PairingTicketRejection.ProtocolVersionUnsupported, ticket = ticket)
            return
        }
        if (!ticket.expiresAt.isAfter(Instant.now())) {
            reject(rejection = PairingTicketRejection.Expired, ticket = ticket)
            return
        }
        stateHolder.value =
            stateHolder.value.copy(
                rejection = null,
                rejectionTicketProtocolVersion = null,
                rejectionTicketExpiresAt = null,
            )
        haptics.success()
        effectChannel.trySend(
            BurpRemoteScannerDialogUserInterfaceEffect.TicketAccepted(encodedTicketText),
        )
    }

    fun analyzeCameraFrame(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage == null || decodingGuard.isDecoding) {
            imageProxy.close()
            return
        }
        decodingGuard.isDecoding = true
        val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        barcodeScanner
            .process(inputImage)
            .addOnSuccessListener { barcodes ->
                barcodes.firstNotNullOfOrNull { barcode -> barcode.rawValue }?.let { encodedTicketText ->
                    submitTicketText(encodedTicketText)
                }
            }
            .addOnCompleteListener {
                imageProxy.close()
                decodingGuard.isDecoding = false
            }
    }

    fun handleIntent(intent: BurpRemoteScannerDialogUserInterfaceIntent) {
        val currentState = stateHolder.value
        when (intent) {
            is BurpRemoteScannerDialogUserInterfaceIntent.SelectMode -> {
                haptics.select()
                stateHolder.value =
                    currentState.copy(
                        mode = intent.mode,
                        // 离开取景面就把补光关掉：相机在别的屏上还开着灯是很显眼的现场事故。
                        isTorchEnabled = false,
                    )
            }

            is BurpRemoteScannerDialogUserInterfaceIntent.ToggleTorch -> {
                haptics.select()
                stateHolder.value = currentState.copy(isTorchEnabled = !currentState.isTorchEnabled)
            }

            is BurpRemoteScannerDialogUserInterfaceIntent.UpdateManualTicketText ->
                stateHolder.value = currentState.copy(manualTicketText = intent.manualTicketText)

            is BurpRemoteScannerDialogUserInterfaceIntent.SubmitManualTicketText ->
                if (currentState.manualTicketText.isNotBlank()) {
                    submitTicketText(currentState.manualTicketText)
                }

            is BurpRemoteScannerDialogUserInterfaceIntent.ReportCameraPermission -> {
                if (!intent.isGranted) haptics.failure()
                stateHolder.value =
                    currentState.copy(
                        cameraPermission =
                            if (intent.isGranted) {
                                PairingCameraPermission.Granted
                            } else {
                                PairingCameraPermission.Denied
                            },
                    )
            }
        }
    }

    val cameraPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            handleIntent(BurpRemoteScannerDialogUserInterfaceIntent.ReportCameraPermission(isGranted))
        }

    // 系统里已经授权时不必再弹一次窗：先把这个事实记下来，界面才能直接开预览。
    LaunchedEffect(context) {
        val isAlreadyGranted =
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        if (isAlreadyGranted) {
            handleIntent(BurpRemoteScannerDialogUserInterfaceIntent.ReportCameraPermission(true))
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    BurpRemoteScannerDialogContent(
        state = stateHolder.value,
        onIntent = ::handleIntent,
        onRequestCameraPermission = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) },
        onFrameCaptured = ::analyzeCameraFrame,
        onDismiss = onDismiss,
    )
}

/**
 * 叠在取景画面上的控件要躲开的顶部安全区：状态栏，以及刘海、挖孔装置所占的那一条。
 *
 * 返回的是这一块留白的并集。取景画面本身铺满整屏（相机画面是背景，缩进去就会在状态栏与手势条
 * 两侧露出黑边），因此这份留白不施加在弹窗根节点上，只由控件层消费；`Modifier.windowInsetsPadding`
 * 会把它施加过的 inset 对子树标记为已消费，内层再取一次只会拿到零，不会叠成两倍。
 */
@Composable
private fun scannerControlsTopSafeArea(): WindowInsets =
    WindowInsets.systemBars
        .only(WindowInsetsSides.Top)
        .union(WindowInsets.displayCutout)

/**
 * 手输面板整条流程要躲开的安全区：顶部同 [scannerControlsTopSafeArea]，底部是手势条与键盘的并集。
 *
 * 返回的是三者的并集。键盘弹起时 `ime` 比 `navigationBars` 高，取并集即取两者中更高的那一个，
 * 因此面板底部的配对按钮在手势条上不会被压住，键盘弹出时也会跟着抬起来。
 */
@Composable
private fun manualTicketTextPanelSafeArea(): WindowInsets =
    scannerControlsTopSafeArea()
        .union(WindowInsets.navigationBars)
        .union(WindowInsets.ime)

/**
 * 弹窗内容的无状态形式。
 *
 * 单独拆出来是为了两件事：一是相机那一支要拿去跑 @Preview 会真的开相机，于是预览只覆盖能安全
 * 渲染的处境；二是「哪些控件在哪些状态下出现」这件事可以在这里一眼看完，不必穿过状态持有看。
 *
 * 取景面是沉浸式的：相机那一支把控件叠在预览之上（顶部一行、手电筒在底部），手输那一支则是
 * 一条正常的纵向流程——输入框要能被键盘推上来，叠在取景框上的做法在这里只会挡住自己。两处的
 * 留白来源也不同：取景面铺满整屏，只有叠在它上面的控件消费顶部安全区；手输面没有背景画面，
 * 于是整条流程直接消费顶部、手势条与键盘三处，同一次 inset 不会在别处被加第二遍。
 */
@Composable
internal fun BurpRemoteScannerDialogContent(
    state: BurpRemoteScannerDialogUserInterfaceState,
    onIntent: (BurpRemoteScannerDialogUserInterfaceIntent) -> Unit,
    onRequestCameraPermission: () -> Unit,
    onFrameCaptured: (ImageProxy) -> Unit,
    onDismiss: () -> Unit,
) {
    when (state.mode) {
        PairingScannerMode.ManualTicketText ->
            ManualTicketTextPanel(
                state = state,
                onSelectMode = { mode ->
                    onIntent(BurpRemoteScannerDialogUserInterfaceIntent.SelectMode(mode))
                },
                onTicketTextChange = { manualTicketText ->
                    onIntent(
                        BurpRemoteScannerDialogUserInterfaceIntent.UpdateManualTicketText(manualTicketText),
                    )
                },
                onSubmit = { onIntent(BurpRemoteScannerDialogUserInterfaceIntent.SubmitManualTicketText) },
                onDismiss = onDismiss,
            )

        PairingScannerMode.Camera ->
            Box(modifier = Modifier.fillMaxSize()) {
                PairingScannerPanel(
                    cameraPermission = state.cameraPermission,
                    isTorchEnabled = state.isTorchEnabled,
                    statusText = stringResource(scannerStatusResource(state = state)),
                    onFrameCaptured = onFrameCaptured,
                    onToggleTorch = { onIntent(BurpRemoteScannerDialogUserInterfaceIntent.ToggleTorch) },
                    onRequestPermission = onRequestCameraPermission,
                )
                Column(
                    modifier =
                        Modifier
                            .align(Alignment.TopStart)
                            .windowInsetsPadding(scannerControlsTopSafeArea())
                            .padding(BurpRemoteSpacing.Large),
                    verticalArrangement = Arrangement.spacedBy(BurpRemoteSpacing.Small),
                ) {
                    ScannerControlRow(
                        selectedMode = state.mode,
                        onSelectMode = { mode ->
                            onIntent(BurpRemoteScannerDialogUserInterfaceIntent.SelectMode(mode))
                        },
                        onDismiss = onDismiss,
                    )
                    if (state.rejection != null) {
                        TicketRejectionCard(state = state, rejection = state.rejection)
                    }
                }
            }
    }
}

/**
 * 弹窗顶部的一行控件：左边是模式切换，右边是关闭。
 *
 * 两者不做进同一个分段控件：分段控件是「这份输入用哪种方式」，关闭是「离开这里」，语义不同层，
 * 混在一起用户会以为关闭也是第三种输入方式。
 */
@Composable
private fun ScannerControlRow(
    selectedMode: PairingScannerMode,
    onSelectMode: (PairingScannerMode) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(BurpRemoteSpacing.Medium),
    ) {
        PairingScannerModeSelector(
            selectedMode = selectedMode,
            onSelectMode = onSelectMode,
            modifier = Modifier.weight(weight = 1f, fill = true),
        )
        BurpRemoteScannerCloseButton(
            contentDescription = stringResource(R.string.connection_scanner_close_action),
            onClick = onDismiss,
        )
    }
}

/**
 * 相机与手输是同一件事的两种输入，放在同一个分段控件里二选一。
 *
 * 做成两枚并排按钮时，未选中那枚是透明底加强调色文字，与关闭连在一起就读成一条 Action Bar；
 * 分段控件只有一个容器与一个选中胶囊，选中的是哪一个不必再猜。
 */
@Composable
private fun PairingScannerModeSelector(
    selectedMode: PairingScannerMode,
    onSelectMode: (PairingScannerMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalBurpRemoteDesignTokens.current
    val haptics = rememberBurpRemoteHaptics()
    val containerShape = RoundedCornerShape(BurpRemoteRadius.Control)
    val segmentShape = RoundedCornerShape(BurpRemoteRadius.Capsule)

    Row(
        modifier =
            modifier
                .height(BurpRemoteSizing.MinimumTouchTarget)
                .clip(containerShape)
                .background(color = tokens.colourScheme.surfaceElevated, shape = containerShape)
                .border(width = BurpRemoteSizing.Divider, color = tokens.colourScheme.outline, shape = containerShape)
                .padding(BurpRemoteSpacing.ExtraSmall),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PairingScannerMode.entries.forEach { mode ->
            val isSelected = mode == selectedMode
            Box(
                modifier =
                    Modifier
                        .weight(weight = 1f, fill = true)
                        .fillMaxHeight()
                        .clip(segmentShape)
                        .background(
                            color = if (isSelected) tokens.colourScheme.accent else Color.Transparent,
                            shape = segmentShape,
                        )
                        .clickable {
                            haptics.select()
                            onSelectMode(mode)
                        },
                contentAlignment = Alignment.Center,
            ) {
                BurpRemoteText(
                    text = stringResource(mode.labelResource),
                    style = tokens.typography.label,
                    colour =
                        if (isSelected) {
                            tokens.colourScheme.onAccent
                        } else {
                            tokens.colourScheme.contentSecondary
                        },
                    maxLines = 1,
                )
            }
        }
    }
}

/**
 * 手输配对文本：一条与分段控件同风格的纵向流程。
 *
 * 输入框、主操作、一行说明，前两件是用户要做的，第三件只在按钮不可用时出来说一句为什么；
 * 兜底路径不需要一段居中空状态来解释自己存在的原因。
 *
 * 整条流程按安全区内缩一次（顶部状态栏与挖孔、底部手势条与键盘），这次内缩施加在流程根节点上，
 * 控件本身不再各自取一遍，因此底部的主操作既不会压在手势条上，也不会被键盘挡住。
 */
@Composable
private fun ManualTicketTextPanel(
    state: BurpRemoteScannerDialogUserInterfaceState,
    onSelectMode: (PairingScannerMode) -> Unit,
    onTicketTextChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onDismiss: () -> Unit,
) {
    val tokens = LocalBurpRemoteDesignTokens.current
    val isSubmittable = state.manualTicketText.isNotBlank()

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(manualTicketTextPanelSafeArea())
                .padding(BurpRemoteSpacing.Large),
        verticalArrangement = Arrangement.spacedBy(BurpRemoteSpacing.Medium),
    ) {
        ScannerControlRow(
            selectedMode = state.mode,
            onSelectMode = onSelectMode,
            onDismiss = onDismiss,
        )
        if (state.rejection != null) {
            TicketRejectionCard(state = state, rejection = state.rejection)
        }
        BurpRemoteTextField(
            value = state.manualTicketText,
            onValueChange = onTicketTextChange,
            label = stringResource(R.string.connection_manual_pairing_text_label),
            // 配对文本要能逐字符比对，比例字体做不到这件事。
            isMonospace = true,
        )
        BurpRemoteButton(
            text = stringResource(R.string.connection_manual_pairing_action),
            onClick = onSubmit,
            style = BurpRemoteButtonStyle.Primary,
            isEnabled = isSubmittable,
        )
        if (!isSubmittable) {
            BurpRemoteText(
                text = stringResource(R.string.connection_pairing_text_required_hint),
                style = tokens.typography.caption,
                colour = tokens.colourScheme.contentSecondary,
            )
        }
    }
}

/**
 * 票据不可用时的具体原因。
 *
 * 三类原因给的下一步完全不同：换一张码、去插件重新生成、升级其中一端；因此不能合并成一句
 * 「扫码失败」。协议不符时两个版本号都写出来，用户才知道该升级哪一端。
 */
@Composable
private fun TicketRejectionCard(
    state: BurpRemoteScannerDialogUserInterfaceState,
    rejection: PairingTicketRejection?,
) {
    BurpRemoteCard {
        BurpRemoteEmptyState(
            headline = stringResource(ticketRejectionHeadlineResource(rejection)),
            detail = ticketRejectionDetail(state = state, rejection = rejection),
        )
    }
}

private fun scannerStatusResource(state: BurpRemoteScannerDialogUserInterfaceState): Int =
    when (state.cameraPermission) {
        PairingCameraPermission.Denied -> R.string.connection_scanner_status_permission_denied
        PairingCameraPermission.Unknown -> R.string.connection_scanner_status_pending
        PairingCameraPermission.Granted ->
            when (state.rejection) {
                PairingTicketRejection.Undecodable -> R.string.connection_scanner_status_undecodable
                PairingTicketRejection.Expired -> R.string.connection_scanner_status_expired
                PairingTicketRejection.ProtocolVersionUnsupported -> R.string.connection_scanner_status_protocol
                null -> R.string.connection_scanner_status_searching
            }
    }

private fun ticketRejectionHeadlineResource(rejection: PairingTicketRejection?): Int =
    when (rejection) {
        PairingTicketRejection.Undecodable -> R.string.connection_ticket_rejection_undecodable_headline
        PairingTicketRejection.Expired -> R.string.connection_ticket_rejection_expired_headline
        PairingTicketRejection.ProtocolVersionUnsupported ->
            R.string.connection_ticket_rejection_protocol_headline

        null -> R.string.connection_scanner_status_searching
    }

@Composable
private fun ticketRejectionDetail(
    state: BurpRemoteScannerDialogUserInterfaceState,
    rejection: PairingTicketRejection?,
): String =
    when (rejection) {
        PairingTicketRejection.Undecodable ->
            stringResource(R.string.connection_ticket_rejection_undecodable_detail)

        PairingTicketRejection.Expired ->
            stringResource(
                R.string.connection_ticket_rejection_expired_detail,
                state.rejectionTicketExpiresAt?.let { expiresAt ->
                    TICKET_EXPIRY_FORMATTER.format(expiresAt)
                } ?: stringResource(R.string.connection_absent_value),
            )

        PairingTicketRejection.ProtocolVersionUnsupported ->
            stringResource(
                R.string.connection_ticket_rejection_protocol_detail,
                state.rejectionTicketProtocolVersion ?: state.supportedProtocolVersion,
                state.supportedProtocolVersion,
            )

        null -> stringResource(R.string.connection_ticket_rejection_undecodable_detail)
    }

@get:StringRes
private val PairingScannerMode.labelResource: Int
    get() =
        when (this) {
            PairingScannerMode.Camera -> R.string.connection_scanner_mode_camera
            PairingScannerMode.ManualTicketText -> R.string.connection_scanner_mode_manual
        }

// 识别是异步的：上一帧还没出结果时再送一帧，只会把同一张码识别多次并重复交付票据。
private class BarcodeDecodingGuard {
    var isDecoding: Boolean = false
}

// 票据失效时刻按设备时区与当前语言格式化：用户要拿它跟插件界面上显示的时间对齐。
private val TICKET_EXPIRY_FORMATTER: DateTimeFormatter =
    DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).withZone(ZoneId.systemDefault())

// rules.md §8.3：每个屏幕都要有浅色与深色两套预览，否则深色下配色失衡只有装到机器上才发现。
// 相机那一支会真的去开相机，因此预览只覆盖手输与票据被拒两种可以安全渲染的处境。
@Preview(name = "手输浅色", showBackground = true)
@Composable
private fun BurpRemoteScannerDialogManualLightPreview() {
    BurpsuiteRemoteTheme(themeMode = ThemeMode.Light) {
        BurpRemoteScannerDialogContent(
            state =
                BurpRemoteScannerDialogUserInterfaceState(
                    mode = PairingScannerMode.ManualTicketText,
                    cameraPermission = PairingCameraPermission.Granted,
                ),
            onIntent = {},
            onRequestCameraPermission = {},
            onFrameCaptured = {},
            onDismiss = {},
        )
    }
}

@Preview(name = "手输深色", showBackground = true)
@Composable
private fun BurpRemoteScannerDialogManualDarkPreview() {
    BurpsuiteRemoteTheme(themeMode = ThemeMode.Dark) {
        BurpRemoteScannerDialogContent(
            state =
                BurpRemoteScannerDialogUserInterfaceState(
                    mode = PairingScannerMode.ManualTicketText,
                    cameraPermission = PairingCameraPermission.Granted,
                ),
            onIntent = {},
            onRequestCameraPermission = {},
            onFrameCaptured = {},
            onDismiss = {},
        )
    }
}

@Preview(name = "票据被拒浅色", showBackground = true)
@Composable
private fun BurpRemoteScannerDialogRejectedLightPreview() {
    BurpsuiteRemoteTheme(themeMode = ThemeMode.Light) {
        BurpRemoteScannerDialogContent(
            state =
                BurpRemoteScannerDialogUserInterfaceState(
                    mode = PairingScannerMode.ManualTicketText,
                    cameraPermission = PairingCameraPermission.Granted,
                    rejection = PairingTicketRejection.ProtocolVersionUnsupported,
                    rejectionTicketProtocolVersion = 2,
                ),
            onIntent = {},
            onRequestCameraPermission = {},
            onFrameCaptured = {},
            onDismiss = {},
        )
    }
}

@Preview(name = "票据被拒深色", showBackground = true)
@Composable
private fun BurpRemoteScannerDialogRejectedDarkPreview() {
    BurpsuiteRemoteTheme(themeMode = ThemeMode.Dark) {
        BurpRemoteScannerDialogContent(
            state =
                BurpRemoteScannerDialogUserInterfaceState(
                    mode = PairingScannerMode.ManualTicketText,
                    cameraPermission = PairingCameraPermission.Granted,
                    rejection = PairingTicketRejection.ProtocolVersionUnsupported,
                    rejectionTicketProtocolVersion = 2,
                ),
            onIntent = {},
            onRequestCameraPermission = {},
            onFrameCaptured = {},
            onDismiss = {},
        )
    }
}
