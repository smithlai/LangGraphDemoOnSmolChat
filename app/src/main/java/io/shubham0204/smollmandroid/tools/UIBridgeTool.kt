package io.shubham0204.smollmandroid.tools

import android.util.Log
import com.smith.lai.toolcalls.langgraph.response.ToolFollowUpMetadata
import com.smith.lai.toolcalls.langgraph.tools.BaseTool
import com.smith.lai.toolcalls.langgraph.tools.ToolAnnotation
import kotlinx.serialization.Serializable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


@Serializable
data class UIBridgeInput(val command: String = "")

@ToolAnnotation(
    name = "screen_control",
    description = """
Manipulating screen with specific command.
acceptable command value:
    "chat_setting": open chat setting Screen
    "rag_setting": open Rag setting Screen


"""
)
class UIBridgeTool : BaseTool<UIBridgeInput, Unit>() {

    // 保存跳轉回調
    private var screenControlCallback: ((cmd:String) -> Unit)? = null

    fun setNavigateCallback(callback: (cmd:String) -> Unit) {
        screenControlCallback = callback
    }

    override suspend fun invoke(input: UIBridgeInput): Unit = withContext(Dispatchers.Main) {
        Log.d("UIBridgeTool","Executing command: ${input.command}")
        screenControlCallback?.invoke(input.command)
    }

    override fun getFollowUpMetadata(response: Unit): ToolFollowUpMetadata {
        return ToolFollowUpMetadata(
            requiresFollowUp = false,
//            shouldTerminateFlow = false,
            customFollowUpPrompt = ""
        )
    }
}