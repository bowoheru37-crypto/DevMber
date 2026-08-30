package com.example.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GenerateContentRequest(
    @property:Json(name = "contents") val contents: List<Content>,
    @property:Json(name = "generationConfig") val generationConfig: GenerationConfig? = null,
    @property:Json(name = "systemInstruction") val systemInstruction: Content? = null
)

@JsonClass(generateAdapter = true)
data class Content(
    @property:Json(name = "parts") val parts: List<Part>,
    @property:Json(name = "role") val role: String? = null
)

@JsonClass(generateAdapter = true)
data class Part(
    @property:Json(name = "text") val text: String? = null,
    @property:Json(name = "inlineData") val inlineData: InlineData? = null,
    @property:Json(name = "thought") val thought: Boolean? = null
)

@JsonClass(generateAdapter = true)
data class InlineData(
    @property:Json(name = "mimeType") val mimeType: String,
    @property:Json(name = "data") val data: String
)

@JsonClass(generateAdapter = true)
data class GenerationConfig(
    @property:Json(name = "temperature") val temperature: Float? = null,
    @property:Json(name = "topP") val topP: Float? = null,
    @property:Json(name = "topK") val topK: Int? = null,
    @property:Json(name = "thinkingConfig") val thinkingConfig: ThinkingConfig? = null,
    @property:Json(name = "imageConfig") val imageConfig: ImageConfig? = null,
    @property:Json(name = "responseModalities") val responseModalities: List<String>? = null
)

@JsonClass(generateAdapter = true)
data class ThinkingConfig(
    @property:Json(name = "thinkingLevel") val thinkingLevel: String // "high" or "low"
)

@JsonClass(generateAdapter = true)
data class ImageConfig(
    @property:Json(name = "aspectRatio") val aspectRatio: String = "1:1",
    @property:Json(name = "imageSize") val imageSize: String = "1K"
)

@JsonClass(generateAdapter = true)
data class GenerateContentResponse(
    @property:Json(name = "candidates") val candidates: List<Candidate>? = null
)

@JsonClass(generateAdapter = true)
data class Candidate(
    @property:Json(name = "content") val content: Content? = null,
    @property:Json(name = "finishReason") val finishReason: String? = null
)

@JsonClass(generateAdapter = true)
data class GenerateVideosRequest(
    @property:Json(name = "prompt") val prompt: String,
    @property:Json(name = "image") val image: InlineData? = null,
    @property:Json(name = "config") val config: VeoConfig? = null
)

@JsonClass(generateAdapter = true)
data class VeoConfig(
    @property:Json(name = "numberOfVideos") val numberOfVideos: Int = 1,
    @property:Json(name = "resolution") val resolution: String = "1080p",
    @property:Json(name = "aspectRatio") val aspectRatio: String = "16:9"
)

@JsonClass(generateAdapter = true)
data class VeoOperationResponse(
    @property:Json(name = "name") val name: String? = null,
    @property:Json(name = "done") val done: Boolean? = null
)

