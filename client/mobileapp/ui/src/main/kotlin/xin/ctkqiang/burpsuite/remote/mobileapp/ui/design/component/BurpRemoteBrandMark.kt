package xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke

/**
 * 品牌标记：一个右侧留缺口的方框，加一支从框内穿出缺口的箭头。
 *
 * 方框代表代理，箭头代表流量被转送出来——这个应用做的事就是这两件。标记是自画的，
 * 不用任何第三方产品的徽标：应用图标与品牌标记一旦借用别人的商标，用户就会以为这是官方出品。
 *
 * 几何与 `app/src/main/res/drawable/ic_launcher_foreground.xml` 是同一套（108 见方的画布）。
 * 两处各写一份是因为一处要平台资源、一处要 Compose 画布，而跨模块的 R 类不互通；改形状时两处都要改。
 *
 * @param colour 描边颜色；顶栏用主内容色，启动图标那份用白色。
 */
@Composable
fun BurpRemoteBrandMark(
    modifier: Modifier = Modifier,
    colour: Color,
) {
    Canvas(modifier = modifier) {
        val scale = size.minDimension / BRAND_MARK_VIEWPORT
        val originX = (size.width - BRAND_MARK_VIEWPORT * scale) / 2f
        val originY = (size.height - BRAND_MARK_VIEWPORT * scale) / 2f
        val stroke =
            Stroke(
                width = BRAND_MARK_STROKE_WIDTH * scale,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round,
            )

        drawBrandPath(
            points = BRAND_MARK_FRAME,
            colour = colour,
            stroke = stroke,
            originX = originX,
            originY = originY,
            scale = scale,
            isClosed = false,
        )
        drawBrandPath(
            points = listOf(ARROW_SHAFT_START, ARROW_SHAFT_END),
            colour = colour,
            stroke = stroke,
            originX = originX,
            originY = originY,
            scale = scale,
            isClosed = false,
        )
        drawBrandPath(
            points = BRAND_MARK_ARROW_HEAD,
            colour = colour,
            stroke = stroke,
            originX = originX,
            originY = originY,
            scale = scale,
            isClosed = false,
        )
    }
}

// 方框从右边缘缺口的上沿起笔，逆时针绕一圈回到缺口下沿，因此这段折线是开口的。
private fun DrawScope.drawBrandPath(
    points: List<Offset>,
    colour: Color,
    stroke: Stroke,
    originX: Float,
    originY: Float,
    scale: Float,
    isClosed: Boolean,
) {
    val path =
        Path().apply {
            points.forEachIndexed { index, point ->
                val x = originX + point.x * scale
                val y = originY + point.y * scale
                if (index == 0) moveTo(x, y) else lineTo(x, y)
            }
            if (isClosed) close()
        }
    drawPath(path = path, color = colour, style = stroke)
}

private const val BRAND_MARK_VIEWPORT = 108f
private const val BRAND_MARK_STROKE_WIDTH = 7f

private val BRAND_MARK_FRAME =
    listOf(
        Offset(76f, 47f),
        Offset(76f, 40f),
        Offset(68f, 32f),
        Offset(40f, 32f),
        Offset(32f, 40f),
        Offset(32f, 68f),
        Offset(40f, 76f),
        Offset(68f, 76f),
        Offset(76f, 68f),
        Offset(76f, 61f),
    )

private val ARROW_SHAFT_START = Offset(43f, 54f)

private val ARROW_SHAFT_END = Offset(78f, 54f)

private val BRAND_MARK_ARROW_HEAD =
    listOf(
        Offset(68f, 44f),
        Offset(78f, 54f),
        Offset(68f, 64f),
    )
