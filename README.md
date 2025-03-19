# LangGraph-Android Integration - Using langgraph and tool calls and RAG in SmolChat

This project demonstrates how to use LangGraph on Android, using SmolChat as a showcase platform for tool calls (ToolCalls).

## Key Changes
1. Added langgraph-android as a Git Submodule

    - Source: [LangGraph-Android](https://github.com/smithlai/Langgraph-Android.git)
    - Supports LangGraph tool calls (ToolCalls)
2. Added LangGraph Tool Examples
    - RagSearchTool: Retrieves content from the RAG database
    - SmolLMWithTools: Adapter for integrating LLMs with ToolCalls
    - UIBridgeTool: Controls UI actions (e.g., navigating to settings pages)

3. Project Configuration Changes
    - Added langgraph-android dependency in build.gradle.kts
    - Enabled Kotlin serialization support
4. Model Inference Adjustments

    - Set temperature to 0.0f
    - Enabled useMlock (true)
    - Integrated LangGraph for conversation processing
5. Added RAG-android as a Git Submodule

    - Source: [RAG-Android](https://github.com/smithlai/RAG-Android.git)
    - Provides Retrieval-Augmented Generation capabilities
    - Enables contextual search through document chunks
    - Enhances LLM responses with relevant information from stored documents
    
---

## Import `LangGraph-Android`

### 1. add `langgraph-android` Submodule


```sh
git submodule add https://github.com/smithlai/Langgraph-Android.git langgraph-android
git submodule update --init --recursive
```
#### /settings.gradle.kts
```kotlin
include(":langgraph-android")
```

#### app/build.gradle.kts
```kotlin
plugins {
    // fix:
    // Serializer for class 'XXXXXXXX' is not found.
    // Please ensure that class is marked as '@Serializable' and that the serialization compiler plugin is applied.
    id ("kotlinx-serialization")
}
dependencies {
    implementation(project(":langgraph-android"))
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.0")
}
```
### 2. Create SmolLMWithTools Adapter
建立 SmolLMWithTools.kt 來封裝 SmolLM，使其兼容 LangGraph：

```kotlin
class SmolLMWithTools(adapter: BaseLLMToolAdapter, val smolLM: SmolLM) : LLMWithTools(adapter) {

    override suspend fun init_model() {}

    override suspend fun close_model() {
        smolLM.close()
    }

    override fun addSystemMessage(content: String) {
        smolLM.addSystemPrompt(content)
    }

    override fun addUserMessage(content: String) {
        smolLM.addUserMessage(content)
    }

    override fun getResponse(query: String?): Flow<String> {
        return smolLM.getResponse(query ?: "")
    }
}

```
### 3. Setting up LangGraph
```kotlin
private var conversationGraph: LangGraph<CustomChatState>? = null
private var customState: CustomChatState = CustomChatState()
private var smolLMWithTools: SmolLMWithTools = SmolLMWithTools(Llama3_2_3B_LLMToolAdapter(), instance)

init {
    conversationGraph = createGraph(smolLMWithTools, listOf(RagSearchTool(), UIBridgeTool()))
}

private fun createGraph(
    model: SmolLMWithTools,
    tools: List<BaseTool<*, *>>
): LangGraph<CustomChatState> {

    model.bind_tools(tools) // <------important
    val graphBuilder = LangGraph<CustomChatState>()

    val llmNode = LLMNode<CustomChatState>(model)
    val toolNode = ToolNode<CustomChatState>(tools) // <----important

    graphBuilder.addStartNode()
    graphBuilder.addNode("llm", llmNode)
    graphBuilder.addNode(NodeNames.TOOLS, toolNode)

    graphBuilder.addConditionalEdges(
        "llm",
        mapOf(StateConditions.hasToolCalls<CustomChatState>() to NodeNames.TOOLS),
        defaultTarget = NodeNames.END
    )

    graphBuilder.addEdge(NodeNames.TOOLS, "llm")
    return graphBuilder.compile()
}

```
```kotlin
val userQuery = "The weather of tokyo?"
customState.addMessage(MessageRole.USER, userQuery)

val result = conversationGraph?.run(customState)
println("AI 回應: ${result?.getLastAssistantMessage()?.content}")
```

#### 4. Note
1. Why `temperature` set to `0.0f`:
    A higher temperature will make llm failed to generate correct tool format.
2. You can refer to `SmolLMWithTools.kt` and `Llama3_2_3B_LLMToolAdapter.kt` to  
   create custom adapters for different LLMs.

*app/build.gradle.kts*
```kotlin
//  Duplicate class org.intellij.lang.annotations.Flow found in modules annotations-23.0.0.jar -> annotations-23.0.0 (org.jetbrains:annotations:23.0.0) and annotations-java5-17.0.0.jar -> annotations-java5-17.0.0 (org.jetbrains:annotations-java5:17.0.0)
configurations {
    create("cleanedAnnotations")
    implementation {
        exclude(group = "org.jetbrains", module = "annotations")
    }
}
```


## Add rag-android Submodule

You can refer to `rag-android/Readme.md` as well
```sh
git submodule add https://github.com/smithlai/RAG-Android.git rag-android
git submodule update --init --recursive
```

#### /settings.gradle.kts
```kotlin
include(":rag-android")
```

#### app/build.gradle.kts
```kotlin
plugins {
    // fix:
    // Serializer for class 'XXXXXXXX' is not found.
    // Please ensure that class is marked as '@Serializable' and that the serialization compiler plugin is applied.
    id ("kotlinx-serialization")
}
dependencies {
    implementation(project(":rag-android"))
}
```
#### app/Application
```kotlin
class XXXApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@SmolChatApplication)
            modules(
                listOf(
                    KoinAppModule().module,
                    SmithRagModule().module // 添加 Android module 的 Koin module
                )
            )
        }
        ...
        ...
    }
    ObjectBoxStore.init(this)
    com.smith.smith_rag.data.ObjectBoxStore.init(this)
}
```


--------------------------------------------------

# SmolChat - On-Device Inference of SLMs in Android

<table>
<tr>
<td>
<img src="resources/app_screenshots/pic1.png" alt="app_img_01">
</td>
<td>
<img src="resources/app_screenshots/pic2.png" alt="app_img_02">
</td>
<td>
<img src="resources/app_screenshots/pic3.png" alt="app_img_03">
</td>
<td>
<img src="resources/app_screenshots/pic4.png" alt="app_img_03">
</td>
</tr>
<tr>
<td>
<img src="resources/app_screenshots/pic5.png" alt="app_img_04">
</td>
<td>
<img src="resources/app_screenshots/pic6.png" alt="app_img_05">
</td>
<td>
<img src="resources/app_screenshots/pic7.png" alt="app_img_06">
</td>
<td>
<img src="resources/app_screenshots/pic8.png" alt="app_img_07">
</td>
</tr>
</table>

## Project Goals

- Provide a usable user interface to interact with local SLMs (small language models) locally, on-device
- Allow users to add/remove SLMs (GGUF models) and modify their system prompts or inference parameters (temperature, 
  min-p)
- Allow users to create specific-downstream tasks quickly and use SLMs to generate responses
- Simple, easy to understand, extensible codebase

## Setup

1. Clone the repository with its submodule originating from llama.cpp,

```commandline
git clone --depth=1 https://github.com/shubham0204/SmolChat-Android
cd SmolChat-Android
git submodule update --init --recursive
```

2. Android Studio starts building the project automatically. If not, select **Build > Rebuild Project** to start a project build.

3. After a successful project build, [connect an Android device](https://developer.android.com/studio/run/device) to your system. Once connected, the name of the device must be visible in top menu-bar in Android Studio.

## Working

1. The application uses llama.cpp to load and execute GGUF models. As llama.cpp is written in pure C/C++, it is easy 
   to compile on Android-based targets using the [NDK](https://developer.android.com/ndk). 

2. The `smollm` module uses a `llm_inference.cpp` class which interacts with llama.cpp's C-style API to execute the 
   GGUF model and a JNI binding `smollm.cpp`. Check the [C++ source files here](https://github.com/shubham0204/SmolChat-Android/tree/main/smollm/src/main/cpp). On the Kotlin side, the [`SmolLM`](https://github.com/shubham0204/SmolChat-Android/blob/main/smollm/src/main/java/io/shubham0204/smollm/SmolLM.kt) class provides 
   the required methods to interact with the JNI (C++ side) bindings.

3. The `app` module contains the application logic and UI code. Whenever a new chat is opened, the app instantiates 
   the `SmolLM` class and provides it the model file-path which is stored by the [`LLMModel`](https://github.com/shubham0204/SmolChat-Android/blob/main/app/src/main/java/io/shubham0204/smollmandroid/data/DataModels.kt) entity in the ObjectBox.
   Next, the app adds messages with role `user` and `system` to the chat by retrieving them from the database and
   using `LLMInference::addChatMessage`.

4. For tasks, the messages are not persisted, and we inform to `LLMInference` by passing `_storeChats=false` to
   `LLMInference::loadModel`.

## Technologies

* [ggerganov/llama.cpp](https://github.com/ggerganov/llama.cpp) is a pure C/C++ framework to execute machine learning 
  models on multiple execution backends. It provides a primitive C-style API to interact with LLMs 
  converted to the [GGUF format](https://github.com/ggerganov/ggml/blob/master/docs/gguf.md) native to [ggml](https://github.com/ggerganov/ggml)/llama.cpp. The app uses JNI bindings to interact with a small class `smollm.
  cpp` which uses llama.cpp to load and execute GGUF models.

* [ObjectBox](https://objectbox.io) is a on-device, high-performance NoSQL database with bindings available in multiple 
  languages. The app 
  uses ObjectBox to store the model, chat and message metadata.

* [noties/Markwon](https://github.com/noties/Markwon) is a markdown rendering library for Android. The app uses 
  Markwon and [Prism4j](https://github.com/noties/Prism4j) (for code syntax highlighting) to render Markdown responses 
  from the SLMs.

## More On-Device ML Projects

- [shubham0204/Android-Doc-QA](https://github.com/shubham0204/Android-Document-QA): On-device RAG-based question 
  answering from documents
- [shubham0204/OnDevice-Face-Recognition-Android](https://github.com/shubham0204/OnDevice-Face-Recognition-Android): 
  Realtime face recognition with FaceNet, Mediapipe and ObjectBox's vector database
- [shubham0204/FaceRecognition_With_FaceNet_Android](https://github.com/shubham0204/OnDevice-Face-Recognition-Android):
  Realtime face recognition with FaceNet, MLKit
- [shubham0204/CLIP-Android](https://github.com/shubham0204/CLIP-Android): On-device CLIP inference in Android 
  (search images with textual queries)
- [shubham0204/Segment-Anything-Android](https://github.com/shubham0204/Segment-Anything-Android): Execute Meta's 
  SAM model in Android with onnxruntime
- [shubham0204/Depth-Anything-Android](https://github.com/shubham0204/Depth-Anything-Android): Execute the 
  Depth-Anything model in Android with onnxruntime for monocular depth estimation
- [shubham0204/Sentence-Embeddings-Android](https://github.com/shubham0204/Sentence-Embeddings-Android): Generate 
  sentence-embeddings (from models like `all-MiniLM-L6-V2`) in Android

## Future

The following features/tasks are planned for the future releases of the app:

- Assign names to chats automatically (just like ChatGPT and Claude)
- Add a search bar to the navigation drawer to search for messages within chats using ObjectBox's query capabilities
- Add a background service which uses BlueTooth/HTTP/WiFi to communicate with a desktop application to send queries 
  from the desktop to the mobile device for inference
- Enable auto-scroll when generating partial response in `ChatActivity`
- Measure RAM consumption
- Add [app shortcuts](https://developer.android.com/develop/ui/views/launch/shortcuts) for tasks
- Integrate [Android-Doc-QA](https://github.com/shubham0204/Android-Document-QA) for on-device RAG-based question answering from documents
- Check if llama.cpp can be compiled to use Vulkan for inference on Android devices (and use the mobile GPU)
- Check if multilingual GGUF models can be supported