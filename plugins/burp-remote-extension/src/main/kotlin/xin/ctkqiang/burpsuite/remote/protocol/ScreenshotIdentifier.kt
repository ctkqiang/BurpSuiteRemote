/**
 * Burp Remote —— 协议层 / 身份
 *
 * 声明「一张截图」的身份类型。截图既有原始形态也有美化后的产物，两者共用同一个身份，
 * 因此身份不能绑定到某一份具体文件上。
 *
 * @author 钟智强
 */

package xin.ctkqiang.burpsuite.remote.protocol

import kotlinx.serialization.Serializable

/**
 * 标识一张截图。
 *
 * 截图会经历「导入」与「美化」两次以上的状态迁移，中间还会产生不同格式与不同尺寸的产物。
 * 如果每一份产物各自持有一个标识符，客户端就必须自行维护它们之间的对应关系，而这份对应
 * 关系一旦在传输中丢失，用户看到的就是若干张互不相干的孤立图片。
 *
 * 因此身份归属于「这张截图」这一概念本身，原始图与处理图都是它的时序产物；处理版本由事件
 * 载荷单独记录，不参与身份构成。
 *
 * @property value 截图的唯一文本标识符，协议示例约定使用 `screenshot_` 前缀。
 */
@JvmInline
@Serializable
value class ScreenshotIdentifier(val value: String)
