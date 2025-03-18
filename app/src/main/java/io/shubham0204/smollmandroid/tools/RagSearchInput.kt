package io.shubham0204.smollmandroid.tools

//import android.util.Log
//import com.smith.lai.smithtoolcalls.langgraph.response.ToolFollowUpMetadata
//import com.smith.lai.smithtoolcalls.langgraph.tools.BaseTool
//import com.smith.lai.smithtoolcalls.langgraph.tools.ToolAnnotation
//import com.smith.smith_rag.data.ChunksDB
//import com.smith.smith_rag.data.RetrievedContext
//import com.smith.smith_rag.embeddings.SentenceEmbeddingProvider
//import kotlinx.serialization.Serializable
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.withContext
//import kotlin.time.measureTimedValue


//@Serializable
//data class RagSearchInput(val query: String = "")
//
//@ToolAnnotation(
//    name = "rag_serach",
//    description = """
//search TC series device data from rag database with specified query keyword
//"""
//)
//class RagSearchTool(chunksDB: ChunksDB, sentenceEncoder: SentenceEmbeddingProvider) : BaseTool<RagSearchInput, String>() {
//    val chunksDB: ChunksDB = chunksDB
//    val sentenceEncoder:SentenceEmbeddingProvider = sentenceEncoder
//
//
//    override suspend fun invoke(input: RagSearchInput): String = withContext(Dispatchers.Main) {
//        val query = input.query
//        Log.d("RagSearchTool","RAG Search: ${query}")
//        val retrieveDuration = measureTimedValue {
//            var jointContext = ""
//            val retrievedContextList = ArrayList<RetrievedContext>()
//            val queryEmbedding = sentenceEncoder.encodeText(query)
//            chunksDB.getSimilarChunks(queryEmbedding)
//                .forEach {
//                    jointContext += "\n" + it.second.chunkData
//                    retrievedContextList.add(
//                        RetrievedContext(
//                            it.second.docFileName,
//                            it.second.chunkData
//                        )
//                    )
//                }
//            jointContext
//        }
//        return@withContext retrieveDuration.value
//    }
//    override fun getFollowUpMetadata(response: String): ToolFollowUpMetadata {
//        val customPrompt = "I found this information in the database:\n\n```\n$response\n```\n\n" +
//                "Based on this information, please answer the original question."
//
//        return ToolFollowUpMetadata(
//            requiresFollowUp = true,
////            shouldTerminateFlow = false,
//            customFollowUpPrompt = customPrompt
//        )
//    }
//}