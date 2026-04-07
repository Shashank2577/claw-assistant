package com.openclaw.ai.common

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.coroutines.CompletableDeferred

interface LatencyProvider {
  val latencyMs: Float
}

data class Classification(val label: String, val score: Float, val color: Color)

data class JsonObjAndTextContent<T>(val jsonObj: T, val textContent: String)

class AudioClip(val audioData: ByteArray, val sampleRate: Int)

open class AgentAction(val name: AgentActionName)

class CallJsAgentAction(
  val url: String,
  val data: String,
  val secret: String = "",
  val result: CompletableDeferred<String> = CompletableDeferred(),
) : AgentAction(name = AgentActionName.CALL_JS_SKILL) {}

class AskInfoAgentAction(
  val dialogTitle: String,
  val fieldLabel: String,
  val result: CompletableDeferred<String> = CompletableDeferred(),
) : AgentAction(name = AgentActionName.ASK_INFO)

class SkillProgressAgentAction(
  val label: String,
  val inProgress: Boolean,
  val addItemTitle: String = "",
  val addItemDescription: String = "",
  val customData: Any? = null,
) : AgentAction(name = AgentActionName.SKILL_PROGRESS)

enum class AgentActionName() {
  CALL_JS_SKILL,
  SKILL_PROGRESS,
  ASK_INFO,
}

data class SkillTryOutChip(
  val icon: ImageVector,
  val label: String,
  val prompt: String,
  val skillName: String,
)

data class SkillInfo(
  val skillMd: String,
  val skillUrl: String? = null,
  val tryoutChip: SkillTryOutChip? = null,
)

data class SkillsIndex(val skills: List<SkillInfo>)

data class CallJsSkillResult(
  val result: String?,
  val error: String?,
  val image: CallJsSkillResultImage?,
  val webview: CallJsSkillResultWebview?,
)

data class CallJsSkillResultImage(val base64: String?)

data class CallJsSkillResultWebview(
  val url: String?,
  val iframe: Boolean?,
  val aspectRatio: Float?,
)
