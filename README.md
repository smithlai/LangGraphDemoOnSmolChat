# LangGraph & RAG Integration for SmolChat Android

This project demonstrates how to use LangGraph and RAG (Retrieval Augmented Generation) on Android, using SmolChat as a platform for tool calls.

## Features

- **Tool Calls Support**: Use LangGraph for structured tool invocation in LLMs
- **RAG Integration**: Enhance LLM responses with contextual information from documents
- **UI Integration**: Access RAG functionality directly from the chat interface
- **Low-Resource Optimization**: Configured for efficient on-device operation


<div align="center">
  <img src="./screenshots/tool_calls1.png" width="45%">
  <p>Tool call demo 1 (query data)</p>
  
  <img src="./screenshots/tool_calls2.png" width="45%">
  <p>tool calls demo 2 (UI control)</p>
  <img src="./screenshots/rag_1.png" width="45%"><br/>
  <img src="./screenshots/rag_2.png" width="45%">
  <p>tool calls demo 3 (RAG)</p>
</div>

## Getting Started

### Prerequisites

- Android Studio Arctic Fox or newer
- Android device with API level 26+ (Android 8.0+)
- 4GB+ RAM recommended for model inference

### Setup

1. **Clone the repository with submodules**:
   ```bash
   git clone -b langgraph https://github.com/smithlai/LangGraphDemoOnSmolChat.git
   cd LangGraphDemoOnSmolChat
   git submodule update --init --recursive
   ```
   
The `git submodule update` command initializes both LangGraph and RAG modules automatically.


2. **Open in Android Studio**:
   - Open Android Studio
   - Select "Open an Existing Project"
   - Navigate to the cloned directory and open it

3. **Build the project**:
   - Android Studio should automatically sync and build the project
   - If it doesn't, select **Build > Rebuild Project**

4. **Run on a device**:
   - Connect an Android device to your computer
   - Select your device in the toolbar dropdown
   - Click the Run button (green triangle)


## Creating Your Own Tools & Graphs

To create your own custom tools and LangGraph configurations, please refer to the documentation in the [langgraph-android repository](https://github.com/smithlai/Langgraph-Android). 

The repository contains detailed instructions, examples, and best practices for:
- Creating custom tool classes
- Defining tool inputs and outputs
- Setting up LangGraph configurations
- Managing tool call state
- Implementing custom nodes and edges

For RAG-specific functionality, refer to the documentation in the [rag-android repository](https://github.com/smithlai/RAG-Android).

--------------------------------------------------
--------------------------------------------------
--------------------------------------------------
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