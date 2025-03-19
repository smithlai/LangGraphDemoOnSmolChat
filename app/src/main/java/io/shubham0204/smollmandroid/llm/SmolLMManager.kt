/*
 * Copyright (C) 2025 Shubham Panchal
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.shubham0204.smollmandroid.llm

import android.util.Log
import com.smith.lai.toolcalls.langgraph.LangGraph
import com.smith.lai.toolcalls.langgraph.model.adapter.Llama3_2_3B_LLMToolAdapter
import com.smith.lai.toolcalls.langgraph.node.LLMNode
import com.smith.lai.toolcalls.langgraph.node.Node.Companion.NodeNames
import com.smith.lai.toolcalls.langgraph.node.ToolNode
import com.smith.lai.toolcalls.langgraph.state.GraphState
import com.smith.lai.toolcalls.langgraph.state.MessageRole
import com.smith.lai.toolcalls.langgraph.state.StateConditions
import com.smith.lai.toolcalls.langgraph.tools.BaseTool
import com.smith.lai.toolcalls.langgraph.tools.example_tools.CalculatorTool
import com.smith.lai.toolcalls.langgraph.tools.example_tools.ToolToday
import com.smith.lai.toolcalls.langgraph.tools.example_tools.WeatherTool
import io.shubham0204.smollm.SmolLM
import io.shubham0204.smollmandroid.data.Chat
import io.shubham0204.smollmandroid.data.MessagesDB
import io.shubham0204.smollmandroid.tools.SmolLMWithTools
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Single
import kotlin.time.measureTime

private const val LOGTAG = "[SmolLMManager-Kt]"
private val LOGD: (String) -> Unit = { Log.d(LOGTAG, it) }
/**
 * Custom state class for managing conversation state
 */
class CustomChatState : GraphState() {
    // You can add custom properties here if needed
    var customProperty: String = ""
}
@Single
class SmolLMManager(
    private val messagesDB: MessagesDB,
) {
    private val HIDDEN_SIG = "(LANG_GRAPH)"
    private val instance = SmolLM()
    private var responseGenerationJob: Job? = null
    private var modelInitJob: Job? = null
    private var chat: Chat? = null
    private var isInstanceLoaded = false
    var isInferenceOn = false

    // LangGraph components
    private var conversationGraph: LangGraph<CustomChatState>? = null
    private var customState: CustomChatState = CustomChatState()
    private var smolLMWithTools: SmolLMWithTools = SmolLMWithTools(Llama3_2_3B_LLMToolAdapter(), instance)

    data class SmolLMInitParams(
        val chat: Chat,
        val modelPath: String,
        val minP: Float,
        val temperature: Float,
        val storeChats: Boolean,
        val contextSize: Long,
        val chatTemplate: String,
        val nThreads: Int,
        val useMmap: Boolean,
        val useMlock: Boolean,
    )

    data class SmolLMResponse(
        val response: String,
        val generationSpeed: Float,
        val generationTimeSecs: Int,
        val contextLengthUsed: Int,
    )
    private val tools: MutableList<BaseTool<*, *>> by lazy {
        mutableListOf(WeatherTool(), ToolToday())
    }
    fun bind_tools(new_tools: List<BaseTool<*, *>>){
        tools.addAll(new_tools)
        //update graph
        conversationGraph = createGraph(
            model = smolLMWithTools,
            tools = tools
        )
    }
    fun create(
        initParams: SmolLMInitParams,
        onError: (Exception) -> Unit,
        onSuccess: () -> Unit,
    ) {
        try {
            modelInitJob =
                CoroutineScope(Dispatchers.Default).launch {
                    chat = initParams.chat
                    if (isInstanceLoaded) {
                        close()
                    }
                    instance.create(
                        initParams.modelPath,
                        initParams.minP,
                        initParams.temperature,
                        initParams.storeChats,
                        initParams.contextSize,
                        initParams.chatTemplate,
                        initParams.nThreads,
                        initParams.useMmap,
                        initParams.useMlock,
                    )
                    LOGD("Model loaded")

//                    if (initParams.chat.systemPrompt.isNotEmpty()) {
//                        instance.fPrompt(initParams.chat.systemPrompt)
//                        LOGD("System prompt added")
//                    }
                    // Initialize the conversation graph with tools
                    conversationGraph = createGraph(
                        model = smolLMWithTools,
                        tools = tools
                    )
                    if (!initParams.chat.isTask) {
                        messagesDB.getMessagesForModel(initParams.chat.id).forEach { message ->
                            //don't put hidden message back
                            if (!message.message.startsWith(HIDDEN_SIG)) {
                                if (message.isUserMessage) {
                                    instance.addUserMessage(message.message)
                                    LOGD("User message added: ${message.message}")
                                } else {
                                    instance.addAssistantMessage(message.message)
                                    LOGD("Assistant message added: ${message.message}")
                                }
                            }
                        }
                    }
                    withContext(Dispatchers.Main) {
                        isInstanceLoaded = true
                        onSuccess()
                    }
                }
        } catch (e: Exception) {
            onError(e)
        }
    }

    fun getResponse(
        query: String,
        responseTransform: (String) -> String,
        onPartialResponseGenerated: (String) -> Unit,
        onSuccess: (SmolLMResponse) -> Unit,
        onCancelled: () -> Unit,
        onError: (Exception) -> Unit,
    ) {
        try {
            assert(chat != null) { "Please call SmolLMManager.create() first." }
            responseGenerationJob =
                CoroutineScope(Dispatchers.Default).launch {
                    isInferenceOn = true
                    var response = ""
                    val duration =
                        measureTime {
                            val usingGraph = true
                            if (usingGraph) {

                                conversationGraph?.setOnMessageCallback { message ->
                                    // run time update UI to display hidden messages in graph
                                    withContext(Dispatchers.Main) {
                                        when (message.role){
                                            MessageRole.ASSISTANT -> messagesDB.addAssistantMessage(chat!!.id, "$HIDDEN_SIG[${message.role}]"+message.content)
                                            MessageRole.TOOL -> messagesDB.addUserMessage(chat!!.id, "$HIDDEN_SIG[${message.role}]"+message.content)
                                            else ->{messagesDB.addAssistantMessage(chat!!.id, "$HIDDEN_SIG(Error)[${message.role}]"+message.content)}
                                        }
                                    }
                                }
                                // Add the user message to the state
                                customState.addMessage(MessageRole.USER, query)

                                // Run the graph with the current state
                                val result = conversationGraph?.run(customState)

                                // Update the current state with the result
                                result?.let {
                                    if (result.error?.isNotEmpty() == true) {
                                        throw Exception("${result.error}")
                                    } else {
                                        val assistantResponse = it.getLastAssistantMessage()
                                        val final_msg = assistantResponse?.content ?: ""
                                        LOGD("LangGraph generated response: ${final_msg.take(50)}...")
                                        response += responseTransform(final_msg)
                                        withContext(Dispatchers.Main) {
                                            onPartialResponseGenerated(response)
                                        }
                                    }
                                }
                            }else {
                                // Original
                                instance.getResponse(query).collect { piece ->
                                    response += responseTransform(piece)
                                    withContext(Dispatchers.Main) {
                                        onPartialResponseGenerated(response)
                                    }
                                }
                            }
                        }
                    // once the response is generated
                    // add it to the messages database
                    messagesDB.addAssistantMessage(chat!!.id, response)
                    withContext(Dispatchers.Main) {
                        isInferenceOn = false
                        onSuccess(
                            SmolLMResponse(
                                response = response,
                                generationSpeed = instance.getResponseGenerationSpeed(),
                                generationTimeSecs = duration.inWholeSeconds.toInt(),
                                contextLengthUsed = instance.getContextLengthUsed(),
                            ),
                        )
                    }
                }
        } catch (e: CancellationException) {
            isInferenceOn = false
            onCancelled()
        } catch (e: Exception) {
            isInferenceOn = false
            onError(e)
        }
    }

    fun stopResponseGeneration() {
        responseGenerationJob?.let { cancelJobIfActive(it) }
    }

    fun close() {
        stopResponseGeneration()
        modelInitJob?.let { cancelJobIfActive(it) }
        instance.close()
        isInstanceLoaded = false
    }

    private fun cancelJobIfActive(job: Job) {
        if (job.isActive) {
            job.cancel()
        }
    }
    /**
     * Creates a conversation graph with tools enabled
     */
    private fun createGraph(
        model: SmolLMWithTools,
        tools: List<BaseTool<*, *>>
    ): LangGraph<CustomChatState> {
        model.bind_tools(tools)
        // 创建图构建器
        val graphBuilder = LangGraph<CustomChatState>()

        // 创建节点
        val llmNode = LLMNode<CustomChatState>(model)
        val toolNode = ToolNode<CustomChatState>(tools)


        graphBuilder.addStartNode()
        graphBuilder.addEndNode()
        graphBuilder.addNode("llm", llmNode)
        graphBuilder.addNode(NodeNames.TOOLS, toolNode)

        // 添加边
        graphBuilder.addEdge(NodeNames.START, "llm")

        // 条件边
        graphBuilder.addConditionalEdges(
            "llm",
            mapOf(
                StateConditions.hasToolCalls<CustomChatState>() to NodeNames.TOOLS,
                StateConditions.isComplete<CustomChatState>() to NodeNames.END
            ),
            defaultTarget = NodeNames.END
        )

        graphBuilder.addEdge(NodeNames.TOOLS, "llm")

        // 编译并返回图
        return graphBuilder.compile()
    }
}
