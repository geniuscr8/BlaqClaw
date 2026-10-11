package com.blackclaw.android.tool.impl

import com.blackclaw.android.assistant.VoiceInputManager
import com.blackclaw.android.assistant.VoskModelManager
import com.blackclaw.android.tool.BaseTool
import com.blackclaw.android.tool.ToolParameter
import com.blackclaw.android.tool.ToolResult

/**
 * Tools to control hands-free voice mode (wake word + STT).
 */
class VoiceModeTool : BaseTool() {
    override fun getName() = "voice_mode"
    override fun getDisplayName() = "Modo voz"
    override fun getDescriptionEN() =
        "Enable or disable hands-free voice mode. When on, say the wake word " +
        "('garra' offline, or 'Hey BlaqClaw' online) followed by a command and BlaqClaw listens " +
        "and acts. action: on | off | download_offline (downloads the offline model ~40MB so it " +
        "works with no beep and no internet). Optionally set a custom wake_word."
    override fun getDescriptionCN() = getDescriptionEN()
    override fun getBrief() = "enable/disable hands-free mode with a wake word"
    override fun getParameters() = listOf(
        ToolParameter("action", "string", "on | off | download_offline | status.", true),
        ToolParameter("wake_word", "string", "Optional custom wake word (online mode only).", false),
    )
    override fun execute(params: Map<String, Any>): ToolResult {
        val action = requireString(params, "action").lowercase()
        val wakeWord = optionalString(params, "wake_word", "")
        if (wakeWord.isNotBlank()) VoiceInputManager.wakeWord = wakeWord

        return when (action) {
            "download_offline" -> {
                if (VoskModelManager.isReady()) return ToolResult.success("The offline model is already installed. The offline wake word is 'claw'.")
                if (VoskModelManager.downloading) return ToolResult.success("The offline model download is already in progress…")
                VoskModelManager.download(onDone = { ok ->
                    com.blackclaw.android.utils.XLog.i("VoiceMode", "Offline model download done: $ok")
                })
                ToolResult.success("📥 Downloading the offline voice model (~40MB). When it finishes, voice mode will work without a beep or internet. I will enable it when it is ready.")
            }
            "status" -> {
                val sb = StringBuilder()
                sb.append("Voice mode: ${if (VoiceInputManager.wakeEnabled) "enabled" else "disabled"}\n")
                sb.append("Backend: ${if (VoskModelManager.isReady()) "offline (Vosk, wake='garra')" else "online (SpeechRecognizer, wake='${VoiceInputManager.wakeWord}')"}\n")
                sb.append("Reconocimiento disponible: ${VoiceInputManager.isAvailable()}")
                ToolResult.success(sb.toString())
            }
            "on" -> {
                VoiceInputManager.wakeEnabled = true
                runCatching { com.blackclaw.android.service.VoiceWakeService.start(com.blackclaw.android.ClawApplication.instance) }
                val backend = if (VoskModelManager.isReady())
                    "offline (di 'garra' + tu orden, sin beep ni internet, funciona en segundo plano)"
                else
                    "online (di '${VoiceInputManager.wakeWord}' + tu orden). Para el modo offline sin beep, usa download_offline."
                ToolResult.success("🎤 Voice mode enabled — $backend")
            }
            "off" -> {
                VoiceInputManager.wakeEnabled = false
                VoiceInputManager.stopWakeLoop()
                runCatching { com.blackclaw.android.service.VoiceWakeService.stop(com.blackclaw.android.ClawApplication.instance) }
                ToolResult.success("🔇 Voice mode disabled.")
            }
            else -> ToolResult.error("action: on | off | download_offline | status.")
        }
    }
}
