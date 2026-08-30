package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AiLogEntity
import com.example.data.local.AppDatabase
import com.example.data.local.AssetEntity
import com.example.data.local.ProjectEntity
import com.example.data.local.StudioRepository
import com.example.data.remote.AuthManager
import com.example.data.remote.CloudSyncManager
import com.example.data.remote.GeminiClient
import com.example.data.remote.SecurityManager
import com.example.data.remote.SyncState
import com.example.data.remote.UserSession
import com.example.engine.GameSandboxState
import com.example.engine.PhysicsEngine2D
import com.example.data.transfer.StudioTransferManager
import com.example.engine.ai.HardwareOptimizationProfile
import com.example.engine.ai.StudioAiDirector
import com.example.engine.web.WebBuilderEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class StudioTab {
    CODE_STUDIO,
    PHYSICS_SANDBOX,
    AI_CREATIVE,
    ANALYTICS_CLOUD
}

data class DeviceCustomizationState(
    val isCompactLayout: Boolean = false,
    val isLeftHandedTouch: Boolean = false,
    val targetFps: Int = 60,
    val selectedProfile: HardwareOptimizationProfile = StudioAiDirector.PROFILE_ITEL_A70,
    val isZeroAllocationActive: Boolean = true
)

data class TransferModalState(
    val isOpen: Boolean = false,
    val mode: String = "EXPORT", // EXPORT, IMPORT, AI_DIRECTOR
    val targetType: String = "GAME_LEVEL", // GAME_LEVEL, CODE_PROJECT, FULL_BUNDLE
    val payloadText: String = "",
    val isEncrypted: Boolean = false,
    val statusMessage: String = ""
)

data class AiDirectorState(
    val isGenerating: Boolean = false,
    val userPrompt: String = "Cyberpunk neon runner with high speed floating crystals and gravity flips",
    val lastGeneratedSummary: String = "",
    val optimizationStatus: String = "Profile itel A70 (Unisoc T603 / 4GB RAM) Ready"
)

data class CodeEditorState(
    val currentTitle: String = "Kinematics Engine Sandbox",
    val currentLanguage: String = "KOTLIN", // KOTLIN, JAVASCRIPT, GLSL, PYTHON, HTML5
    val currentCategory: String = "GAME", // GAME, APP, WEB, SHADER, MATH_PHYSICS
    val codeText: String = """
// Studio AI Engine - High Performance Mobile Sandbox
class VelocitySolver(val gravity: Float = 9.8f) {
    fun computeTrajectory(vx: Float, vy: Float, time: Float): Pair<Float, Float> {
        val posX = vx * time
        val posY = vy * time - 0.5f * gravity * time * time
        return Pair(posX, posY)
    }
}
    """.trimIndent(),
    val isExecuting: Boolean = false,
    val executionLog: String = "Ready for execution or AI High-Thinking refactor.",
    val isAiThinking: Boolean = false,
    val aiReasoningLog: String = ""
)

data class MediaStudioState(
    val imagePrompt: String = "Futuristic cybernetic city game level with neon glowing architecture",
    val selectedImageModel: String = "gemini-3.1-flash-image-preview", // or gemini-3-pro-image-preview
    val imageResolution: String = "2K", // 1K, 2K, 4K
    val imageAspectRatio: String = "1:1",
    val isGeneratingImage: Boolean = false,
    val generatedImageBase64: String = "",
    val imageStatus: String = "",
    // Veo Video Generator
    val videoPrompt: String = "Camera flythrough of cybernetic neural city at golden hour, high framerate cinematic lighting",
    val videoAspectRatio: String = "16:9", // 16:9 or 9:16
    val isGeneratingVideo: Boolean = false,
    val videoStatus: String = ""
)

data class AnalyticsState(
    val fps: Int = 60,
    val cpuSimLoad: Int = 18,
    val memoryUsageMb: Int = 42,
    val e2eEncryptionActive: Boolean = true,
    val notificationCount: Int = 3,
    val lastNotificationMessage: String = "Optimization: System memory compressed for high-FPS rendering."
)

class StudioViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    val repository = StudioRepository(database.studioDao())
    val syncManager = CloudSyncManager(repository)

    // Flow states
    val allProjects: StateFlow<List<ProjectEntity>> = repository.allProjects
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allAssets: StateFlow<List<AssetEntity>> = repository.allAssets
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userSession: StateFlow<UserSession?> = AuthManager.currentUser

    val syncState: StateFlow<SyncState> = syncManager.syncState

    // UI Tab & states
    private val _currentTab = MutableStateFlow(StudioTab.CODE_STUDIO)
    val currentTab: StateFlow<StudioTab> = _currentTab.asStateFlow()

    private val _codeEditorState = MutableStateFlow(CodeEditorState())
    val codeEditorState: StateFlow<CodeEditorState> = _codeEditorState.asStateFlow()

    private val _mediaStudioState = MutableStateFlow(MediaStudioState())
    val mediaStudioState: StateFlow<MediaStudioState> = _mediaStudioState.asStateFlow()

    private val _analyticsState = MutableStateFlow(AnalyticsState())
    val analyticsState: StateFlow<AnalyticsState> = _analyticsState.asStateFlow()

    // Device Customization & Performance Profiles
    private val _deviceCustomization = MutableStateFlow(DeviceCustomizationState())
    val deviceCustomization: StateFlow<DeviceCustomizationState> = _deviceCustomization.asStateFlow()

    // Transfer Modal (Import / Export / Backup)
    private val _transferModalState = MutableStateFlow(TransferModalState())
    val transferModalState: StateFlow<TransferModalState> = _transferModalState.asStateFlow()

    // AI Production Director
    private val _aiDirectorState = MutableStateFlow(AiDirectorState())
    val aiDirectorState: StateFlow<AiDirectorState> = _aiDirectorState.asStateFlow()

    // Floating Widget State
    private val _floatingWidgetOpen = MutableStateFlow(false)
    val floatingWidgetOpen: StateFlow<Boolean> = _floatingWidgetOpen.asStateFlow()

    // Engines
    val physicsEngine = PhysicsEngine2D()
    val gameSandbox = GameSandboxState()
    val oneButtonEngine = com.example.engine.graphics.OneButtonGameEngine()

    init {
        AuthManager.init()
        seedInitialSampleProjects()
    }

    fun selectTab(tab: StudioTab) {
        _currentTab.value = tab
    }

    fun toggleFloatingWidget() {
        _floatingWidgetOpen.value = !_floatingWidgetOpen.value
    }

    fun updateCodeText(newText: String) {
        _codeEditorState.value = _codeEditorState.value.copy(codeText = newText)
    }

    fun updateLanguage(lang: String) {
        val sampleCode = when (lang) {
            "GOLANG", "GO" -> """
package main

import (
    "fmt"
    "sync"
    "time"
)

type WorkerState struct {
    ID     int
    Cycles int
}

func process(id int, ch chan<- WorkerState, wg *sync.WaitGroup) {
    defer wg.Done()
    ch <- WorkerState{ID: id, Cycles: 1024}
}

func main() {
    fmt.Println("🐹 Golang 1.23 High-Performance Concurrency Engine Initialized")
    var wg sync.WaitGroup
    ch := make(chan WorkerState, 4)
    for i := 1; i <= 4; i++ {
        wg.Add(1)
        go process(i, ch, &wg)
    }
    wg.Wait()
    close(ch)
    for res := range ch {
        fmt.Printf("Worker #%d completed %d cycles\n", res.ID, res.Cycles)
    }
}
            """.trimIndent()
            "RUST", "RS" -> """
// Rust 2024 Edition SIMD Vector & Zero-Cost Abstraction
#[derive(Debug, Clone)]
pub struct Entity {
    pub id: u32,
    pub x: f32,
    pub y: f32,
}

impl Entity {
    pub fn update_position(&mut self, dx: f32, dy: f32) {
        self.x += dx;
        self.y += dy;
    }
}

fn main() {
    let mut player = Entity { id: 1, x: 100.0, y: 150.0 };
    player.update_position(12.5, -4.0);
    println!("🦀 Rust 2024: Entity updated safely. Pos: ({}, {})", player.x, player.y);
}
            """.trimIndent()
            "WASM", "WAT" -> """
(module
  (memory (export "memory") 1)
  (func (export "add") (param ${'$'}a i32) (param ${'$'}b i32) (result i32)
    local.get ${'$'}a
    local.get ${'$'}b
    i32.add
    return
  )
  (func (export "fastInverseSqrt") (param ${'$'}x f32) (result f32)
    local.get ${'$'}x
    f32.const 0.5
    f32.mul
    return
  )
)
            """.trimIndent()
            "JAVA" -> """
package com.example.game;

import java.util.concurrent.Executors;
import java.util.stream.IntStream;

public class GameSimulation {
    public record Player(int id, double x, double y) {}

    public static void main(String[] args) {
        System.out.println("☕ Java 21 LTS: Virtual Threads & Records Initialized");
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            IntStream.range(0, 100).forEach(i -> {
                executor.submit(() -> new Player(i, i * 1.5, 200.0));
            });
        }
        System.out.println("✔ 100 Virtual Threads simulated successfully.");
    }
}
            """.trimIndent()
            "REACT_TSX" -> """
import React, { useState, useEffect } from 'react';

export default function CyberApp() {
  const [count, setCount] = useState<number>(0);
  const [fps, setFps] = useState<number>(60);

  useEffect(() => {
    console.log("⚛️ React 19 Component Mounted!");
  }, []);

  return (
    <div className="p-4 bg-slate-900 text-cyan-400 rounded-xl">
      <h1 className="text-xl font-bold">⚡ React 19 TSX Sandbox</h1>
      <p className="text-sm text-slate-300">Stateful Fiber node: {count} clicks</p>
      <button onClick={() => setCount(c => c + 1)} className="mt-2 px-4 py-2 bg-cyan-500 text-black font-bold rounded">
        Dispatch State (+1)
      </button>
    </div>
  );
}
            """.trimIndent()
            "TYPESCRIPT" -> """
// TypeScript 5.5 Strict Engine
interface TelemetryPayload<T> {
  timestamp: number;
  data: T;
  checksum: string;
}

type DeviceStatus = 'ONLINE' | 'STANDBY' | 'OPTIMIZING';

function computeIntegrity<T>(payload: TelemetryPayload<T>): boolean {
  return payload.timestamp > 0 && payload.checksum.length > 0;
}
            """.trimIndent()
            "NODEJS" -> """
import express from 'express';
const app = express();
app.use(express.json());

app.get('/api/health', (req, res) => {
  res.json({ status: 'ok', engine: 'Node.js v22-LTS', uptime: process.uptime() });
});

app.listen(3000, () => console.log('🚀 Express Server bound to port 3000'));
            """.trimIndent()
            "JAVASCRIPT" -> "// JS Canvas Engine\nfunction renderFrame(ctx) {\n    ctx.fillStyle = '#00F0FF';\n    ctx.fillRect(10, 10, 80, 80);\n}"
            "GLSL" -> "// GLSL Fragment Shader\nprecision mediump float;\nvarying vec2 vUv;\nvoid main() {\n    gl_FragColor = vec4(vUv.x, vUv.y, 1.0, 1.0);\n}"
            "PYTHON" -> "# Python Physics Vector & Autograd\nimport math\ndef vector_dot(v1, v2):\n    return sum(x*y for x, y in zip(v1, v2))\n\nprint('Python 3.12 Engine Initialized')"
            "HTML5" -> "<!-- HTML5 Game Container -->\n<canvas id='viewport' width='400' height='300'></canvas>"
            else -> "// Kotlin High-Performance Engine\nfun main() {\n    println(\"Engine running smoothly at 60 FPS\")\n}"
        }
        _codeEditorState.value = _codeEditorState.value.copy(
            currentLanguage = lang,
            codeText = sampleCode
        )
    }

    fun loadPreset(presetName: String) {
        val (lang, title, code) = when (presetName) {
            "GOLANG_CONCURRENCY" -> {
                val (t, c) = WebBuilderEngine.getWebPreset("GOLANG_CONCURRENCY")
                Triple("GOLANG", t, c)
            }
            "RUST_BORROW_SIMD" -> {
                val (t, c) = WebBuilderEngine.getWebPreset("RUST_BORROW_SIMD")
                Triple("RUST", t, c)
            }
            "WASM_WAT_STACK_VM" -> {
                val (t, c) = WebBuilderEngine.getWebPreset("WASM_WAT_STACK_VM")
                Triple("WASM", t, c)
            }
            "JAVA_VIRTUAL_THREADS" -> {
                val (t, c) = WebBuilderEngine.getWebPreset("JAVA_VIRTUAL_THREADS")
                Triple("JAVA", t, c)
            }
            "PYTHON_AUTOGRAD_AI" -> {
                val (t, c) = WebBuilderEngine.getWebPreset("PYTHON_AUTOGRAD_AI")
                Triple("PYTHON", t, c)
            }
            "REACT_TSX_CYBER" -> {
                val (t, c) = WebBuilderEngine.getWebPreset("REACT_TSX_CYBER")
                Triple("REACT_TSX", t, c)
            }
            "NODEJS_EXPRESS_API" -> {
                val (t, c) = WebBuilderEngine.getWebPreset("NODEJS_EXPRESS_API")
                Triple("NODEJS", t, c)
            }
            "REACT_THREE_FIBER" -> {
                val (t, c) = WebBuilderEngine.getWebPreset("REACT_THREE_FIBER")
                Triple("REACT_TSX", t, c)
            }
            "TYPESCRIPT_WASM_DSP" -> {
                val (t, c) = WebBuilderEngine.getWebPreset("TYPESCRIPT_WASM_DSP")
                Triple("TYPESCRIPT", t, c)
            }
            "KINEMATICS_SOLVER" -> Triple(
                "KOTLIN",
                "High-Speed Kinematics Integrator",
                """
// Low-Entry Device Zero-Alloc Kinematics Solver (itel A70)
class KinematicSolver {
    var posX = 0f
    var posY = 0f
    var velX = 120f
    var velY = -250f
    val gravity = 980f

    fun step(dt: Float) {
        velY += gravity * dt
        posX += velX * dt
        posY += velY * dt
    }
}
                """.trimIndent()
            )
            "GLSL_RAYMARCHER" -> Triple(
                "GLSL",
                "Raymarching Sphere Shader",
                """
// Fragment Shader for 60 FPS Canvas
precision mediump float;
uniform vec2 u_resolution;
uniform float u_time;

float sphereSDF(vec3 p) {
    return length(p) - 1.0;
}

void main() {
    vec2 uv = (gl_FragCoord.xy - 0.5 * u_resolution) / u_resolution.y;
    vec3 ro = vec3(0.0, 0.0, -3.0);
    vec3 rd = normalize(vec3(uv, 1.0));
    float d = sphereSDF(ro + rd * 2.0);
    gl_FragColor = vec4(vec3(step(0.0, -d)), 1.0);
}
                """.trimIndent()
            )
            "PARTICLE_JS" -> Triple(
                "JAVASCRIPT",
                "Retro 2D Particle Matrix",
                """
// Lightweight HTML5 / JS 2D Particle Loop
class ParticleSystem {
    constructor(count = 50) {
        this.particles = Array.from({ length: count }, () => ({
            x: Math.random() * 400,
            y: Math.random() * 600,
            vx: (Math.random() - 0.5) * 4,
            vy: (Math.random() - 0.5) * 4
        }));
    }
    update(dt) {
        for (let p of this.particles) {
            p.x += p.vx * dt;
            p.y += p.vy * dt;
        }
    }
}
                """.trimIndent()
            )
            "FAST_MATH_ASM" -> Triple(
                "KOTLIN",
                "Zero-Alloc Fast Math & Bitwise Ops",
                """
// Binary & Bitwise Math Optimizer for Low-Power SoC (Unisoc T603)
object FastMathEngine {
    fun fastInverseSqrt(x: Float): Float {
        val xhalf = 0.5f * x
        var i = java.lang.Float.floatToIntBits(x)
        i = 0x5f3759df - (i shr 1)
        var y = java.lang.Float.intBitsToFloat(i)
        y *= (1.5f - xhalf * y * y)
        return y
    }
}
                """.trimIndent()
            )
            "WEB_CANVAS_ARCADE" -> {
                val (t, c) = WebBuilderEngine.getWebPreset("WEB_CANVAS_ARCADE")
                Triple("HTML5", t, c)
            }
            "WEB_AUDIO_VISUALIZER" -> {
                val (t, c) = WebBuilderEngine.getWebPreset("WEB_AUDIO_VISUALIZER")
                Triple("HTML5", t, c)
            }
            "WEB_CYBER_LAYOUT" -> {
                val (t, c) = WebBuilderEngine.getWebPreset("WEB_CYBER_LAYOUT")
                Triple("HTML5", t, c)
            }
            else -> Triple(
                "PYTHON",
                "PID Controller & Newton Solver",
                """
# PID Velocity & Trajectory Controller
class PIDController:
    def __init__(self, kp=1.2, ki=0.05, kd=0.1):
        self.kp, self.ki, self.kd = kp, ki, kd
        self.prev_error = 0.0
        self.integral = 0.0
    def update(self, target, current, dt):
        error = target - current
        self.integral += error * dt
        derivative = (error - self.prev_error) / max(dt, 0.001)
        self.prev_error = error
        return self.kp * error + self.ki * self.integral + self.kd * derivative
                """.trimIndent()
            )
        }

        _codeEditorState.value = _codeEditorState.value.copy(
            currentTitle = title,
            currentLanguage = lang,
            codeText = code,
            executionLog = "Loaded algorithm preset: '$title'"
        )
    }

    fun executeCode() {
        val current = _codeEditorState.value
        _codeEditorState.value = current.copy(
            isExecuting = true,
            executionLog = "Parsing syntax and benchmarking ${current.currentLanguage} bytecode..."
        )

        viewModelScope.launch {
            kotlinx.coroutines.delay(120)
            val polyReport = com.example.engine.polyglot.PolyglotEngineHub.executePolyglotCode(
                current.currentLanguage,
                current.codeText
            )
            val checksum = SecurityManager.computeSha256Checksum(current.codeText)

            val statusReport = StringBuilder().apply {
                append("${polyReport.statusMessage}\n")
                append("• Language Engine: ${polyReport.language.displayName} (${polyReport.language.iconLabel})\n")
                append("• Lines: ${polyReport.linesCount} | Execution Time: ${polyReport.executionTimeMicros} μs\n")
                append("• Memory Footprint: ${polyReport.memoryFootprintKb} KB | SHA-256: ${checksum.take(16)}...\n")
                append("• Execution Logs:\n")
                polyReport.detailedLogs.forEach { log ->
                    append("  $log\n")
                }
                append("• Mobile Hardware Target: itel A70 (Unisoc T603 / Android 5+) 60 FPS Zero-GC")
            }.toString()

            _codeEditorState.value = _codeEditorState.value.copy(
                isExecuting = false,
                executionLog = statusReport
            )
        }
    }

    /**
     * Request High-Thinking code generation from gemini-3.1-pro-preview with thinkingLevel = "high"
     */
    fun requestAiHighThinking(prompt: String) {
        val current = _codeEditorState.value
        _codeEditorState.value = current.copy(
            isAiThinking = true,
            aiReasoningLog = "Initiating gemini-3.1-pro-preview with ThinkingLevel.HIGH (Deep algorithmic analysis)..."
        )

        viewModelScope.launch {
            val startTime = System.currentTimeMillis()
            val result = GeminiClient.generateHighThinkingCode(
                prompt = prompt,
                contextCode = current.codeText
            )
            val duration = System.currentTimeMillis() - startTime

            result.onSuccess { thinkingResult ->
                _codeEditorState.value = _codeEditorState.value.copy(
                    isAiThinking = false,
                    aiReasoningLog = thinkingResult.thinkingProcess,
                    codeText = thinkingResult.outputCode,
                    executionLog = "✔ AI High Thinking completed in ${duration}ms"
                )

                repository.logAiInteraction(
                    AiLogEntity(
                        prompt = prompt,
                        response = thinkingResult.outputCode.take(200),
                        thinkingProcess = thinkingResult.thinkingProcess,
                        model = "gemini-3.1-pro-preview (ThinkingLevel.HIGH)",
                        durationMs = duration
                    )
                )
            }.onFailure { err ->
                _codeEditorState.value = _codeEditorState.value.copy(
                    isAiThinking = false,
                    aiReasoningLog = "Error occurred: ${err.message}",
                    executionLog = "Thinking request failed: ${err.message}"
                )
            }
        }
    }

    fun saveCurrentProject() {
        val current = _codeEditorState.value
        viewModelScope.launch {
            val project = ProjectEntity(
                title = current.currentTitle,
                category = current.currentCategory,
                language = current.currentLanguage,
                codeContent = current.codeText,
                description = "Saved on ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date())}",
                isSynced = false
            )
            val id = repository.saveProject(project)
            _codeEditorState.value = _codeEditorState.value.copy(
                executionLog = "Project #$id '${current.currentTitle}' saved to local Room Database."
            )

            // Trigger sync if user is logged in
            val user = AuthManager.currentUser.value
            if (user != null && !user.isAnonymous) {
                syncManager.syncAll(user.uid)
            }
        }
    }

    fun loadProject(project: ProjectEntity) {
        _codeEditorState.value = _codeEditorState.value.copy(
            currentTitle = project.title,
            currentCategory = project.category,
            currentLanguage = project.language,
            codeText = project.codeContent,
            executionLog = "Loaded project '${project.title}'"
        )
        _currentTab.value = StudioTab.CODE_STUDIO
    }

    fun deleteProject(id: Long) {
        viewModelScope.launch {
            repository.deleteProject(id)
        }
    }

    // Media Studio actions
    fun updateImagePrompt(prompt: String) {
        _mediaStudioState.value = _mediaStudioState.value.copy(imagePrompt = prompt)
    }

    fun updateImageModel(model: String) {
        _mediaStudioState.value = _mediaStudioState.value.copy(selectedImageModel = model)
    }

    fun updateImageResolution(res: String) {
        _mediaStudioState.value = _mediaStudioState.value.copy(imageResolution = res)
    }

    fun updateVideoPrompt(prompt: String) {
        _mediaStudioState.value = _mediaStudioState.value.copy(videoPrompt = prompt)
    }

    fun updateVideoAspectRatio(ratio: String) {
        _mediaStudioState.value = _mediaStudioState.value.copy(videoAspectRatio = ratio)
    }

    /**
     * Generate or Edit Image using gemini-3.1-flash-image-preview or gemini-3-pro-image-preview
     */
    fun generateImage() {
        val state = _mediaStudioState.value
        _mediaStudioState.value = state.copy(
            isGeneratingImage = true,
            imageStatus = "Generating image with ${state.selectedImageModel} (${state.imageResolution})..."
        )

        viewModelScope.launch {
            val result = if (state.selectedImageModel == "gemini-3-pro-image-preview") {
                GeminiClient.generateHighQualityImage(
                    prompt = state.imagePrompt,
                    imageSize = state.imageResolution,
                    aspectRatio = state.imageAspectRatio
                )
            } else {
                GeminiClient.createOrEditImage(
                    prompt = state.imagePrompt,
                    aspectRatio = state.imageAspectRatio
                )
            }

            result.onSuccess { res ->
                _mediaStudioState.value = _mediaStudioState.value.copy(
                    isGeneratingImage = false,
                    generatedImageBase64 = res.imageUrlOrBase64,
                    imageStatus = res.description
                )

                // Save to local assets
                repository.saveAsset(
                    AssetEntity(
                        title = state.imagePrompt.take(30),
                        assetType = "IMAGE",
                        prompt = state.imagePrompt,
                        dataUriOrBase64 = res.imageUrlOrBase64,
                        resolution = state.imageResolution,
                        aspectRatio = state.imageAspectRatio,
                        modelUsed = state.selectedImageModel
                    )
                )
            }.onFailure { err ->
                _mediaStudioState.value = _mediaStudioState.value.copy(
                    isGeneratingImage = false,
                    imageStatus = "Image generation error: ${err.message}"
                )
            }
        }
    }

    /**
     * Animate Image into Video using veo-3.1-fast-generate-preview
     */
    fun generateVeoVideo() {
        val state = _mediaStudioState.value
        _mediaStudioState.value = state.copy(
            isGeneratingVideo = true,
            videoStatus = "Starting Veo Video generation (model: veo-3.1-fast-generate-preview, ratio: ${state.videoAspectRatio})..."
        )

        viewModelScope.launch {
            val result = GeminiClient.generateVeoVideo(
                prompt = state.videoPrompt,
                photoBase64 = state.generatedImageBase64.ifEmpty { null },
                aspectRatio = state.videoAspectRatio
            )

            result.onSuccess { res ->
                _mediaStudioState.value = _mediaStudioState.value.copy(
                    isGeneratingVideo = false,
                    videoStatus = "✔ ${res.status}"
                )

                repository.saveAsset(
                    AssetEntity(
                        title = state.videoPrompt.take(30),
                        assetType = "VIDEO",
                        prompt = state.videoPrompt,
                        dataUriOrBase64 = res.operationName,
                        resolution = "1080p",
                        aspectRatio = state.videoAspectRatio,
                        modelUsed = "veo-3.1-fast-generate-preview"
                    )
                )
            }.onFailure { err ->
                _mediaStudioState.value = _mediaStudioState.value.copy(
                    isGeneratingVideo = false,
                    videoStatus = "Veo generation error: ${err.message}"
                )
            }
        }
    }

    // Google Sign in / Guest Auth
    fun signInGoogle(context: Context) {
        viewModelScope.launch {
            AuthManager.signInWithGoogle(context)
            val user = AuthManager.currentUser.value
            if (user != null) {
                syncManager.syncAll(user.uid)
            }
        }
    }

    fun continueAsGuest() {
        AuthManager.continueAsGuest()
    }

    fun signOut() {
        AuthManager.signOut()
    }

    fun triggerSyncNow() {
        val user = AuthManager.currentUser.value
        if (user != null) {
            syncManager.syncAll(user.uid)
        }
    }

    fun triggerLocalNotification(message: String) {
        _analyticsState.value = _analyticsState.value.copy(
            notificationCount = _analyticsState.value.notificationCount + 1,
            lastNotificationMessage = message
        )
    }

    // ----------------------------------------------------
    // Transfer, Import/Export, and Backup Operations
    // ----------------------------------------------------

    fun openExportModal(targetType: String = "GAME_LEVEL") {
        val payload = when (targetType) {
            "GAME_LEVEL" -> StudioTransferManager.exportLevelToJson(oneButtonEngine, "Studio 1-Button Level")
            "CODE_PROJECT" -> {
                val cur = _codeEditorState.value
                StudioTransferManager.exportEncryptedProject(
                    title = cur.currentTitle,
                    category = cur.currentCategory,
                    language = cur.currentLanguage,
                    code = cur.codeText
                )
            }
            else -> StudioTransferManager.exportLevelToJson(oneButtonEngine, "Full Studio Package")
        }

        _transferModalState.value = TransferModalState(
            isOpen = true,
            mode = "EXPORT",
            targetType = targetType,
            payloadText = payload,
            isEncrypted = targetType == "CODE_PROJECT",
            statusMessage = "Payload generated (${payload.length} chars) with SHA-256 integrity digest."
        )
    }

    fun openImportModal(targetType: String = "GAME_LEVEL") {
        _transferModalState.value = TransferModalState(
            isOpen = true,
            mode = "IMPORT",
            targetType = targetType,
            payloadText = "",
            isEncrypted = false,
            statusMessage = "Paste JSON or .studiopkg package to import."
        )
    }

    fun closeTransferModal() {
        _transferModalState.value = _transferModalState.value.copy(isOpen = false)
    }

    fun updateTransferPayloadText(text: String) {
        _transferModalState.value = _transferModalState.value.copy(payloadText = text)
    }

    fun performImportAction(context: Context) {
        val state = _transferModalState.value
        val payload = state.payloadText.trim()
        if (payload.isBlank()) {
            _transferModalState.value = state.copy(statusMessage = "❌ Error: Payload text is empty.")
            return
        }

        if (state.targetType == "GAME_LEVEL") {
            val result = StudioTransferManager.importLevelFromJson(payload, oneButtonEngine)
            result.onSuccess { msg ->
                _transferModalState.value = state.copy(statusMessage = msg)
                triggerLocalNotification(msg)
            }.onFailure { err ->
                _transferModalState.value = state.copy(statusMessage = "❌ Import Failed: ${err.message}")
            }
        } else {
            val result = StudioTransferManager.importEncryptedProject(payload)
            result.onSuccess { proj ->
                viewModelScope.launch {
                    repository.saveProject(proj)
                    loadProject(proj)
                    _transferModalState.value = state.copy(statusMessage = "✔ Project '${proj.title}' imported & loaded!")
                    triggerLocalNotification("Project '${proj.title}' successfully imported.")
                }
            }.onFailure { err ->
                _transferModalState.value = state.copy(statusMessage = "❌ Decrypt/Import Failed: ${err.message}")
            }
        }
    }

    fun copyTransferPayloadToClipboard(context: Context) {
        val payload = _transferModalState.value.payloadText
        if (payload.isNotBlank()) {
            StudioTransferManager.copyToClipboard(context, "Studio Payload", payload)
            _transferModalState.value = _transferModalState.value.copy(
                statusMessage = "✔ Copied to clipboard (${payload.length} chars)!"
            )
        }
    }

    fun pasteFromClipboardToTransfer(context: Context) {
        val text = StudioTransferManager.readFromClipboard(context)
        if (text.isNotBlank()) {
            _transferModalState.value = _transferModalState.value.copy(
                payloadText = text,
                statusMessage = "✔ Pasted ${text.length} chars from clipboard."
            )
        }
    }

    // ----------------------------------------------------
    // AI Production Director & Level Generator
    // ----------------------------------------------------

    fun updateAiDirectorPrompt(prompt: String) {
        _aiDirectorState.value = _aiDirectorState.value.copy(userPrompt = prompt)
    }

    fun generateAiLevelBlueprint() {
        val prompt = _aiDirectorState.value.userPrompt
        _aiDirectorState.value = _aiDirectorState.value.copy(
            isGenerating = true,
            optimizationStatus = "AI Director crafting game world and 1-button mechanics..."
        )

        viewModelScope.launch {
            val result = StudioAiDirector.generateLevelFromPrompt(prompt, oneButtonEngine)
            _aiDirectorState.value = _aiDirectorState.value.copy(
                isGenerating = false,
                lastGeneratedSummary = result.summary,
                optimizationStatus = "✔ Level '${result.levelTitle}' generated and loaded into Engine!"
            )
            triggerLocalNotification("AI Director: Level '${result.levelTitle}' generated!")
        }
    }

    // ----------------------------------------------------
    // Mobile Layout Customization & Low-Entry Optimization (itel A70)
    // ----------------------------------------------------

    fun setCompactLayout(isCompact: Boolean) {
        _deviceCustomization.value = _deviceCustomization.value.copy(isCompactLayout = isCompact)
    }

    fun setLeftHandedTouch(isLeft: Boolean) {
        _deviceCustomization.value = _deviceCustomization.value.copy(isLeftHandedTouch = isLeft)
    }

    fun applyHardwareProfile(profile: HardwareOptimizationProfile) {
        val report = StudioAiDirector.applyHardwareOptimization(oneButtonEngine, physicsEngine, profile)
        _deviceCustomization.value = _deviceCustomization.value.copy(
            selectedProfile = profile,
            targetFps = profile.targetFps,
            isZeroAllocationActive = profile.zeroAllocationActive
        )
        _aiDirectorState.value = _aiDirectorState.value.copy(optimizationStatus = report)
        triggerLocalNotification("Optimization Applied: ${profile.profileName}")
    }

    private fun seedInitialSampleProjects() {
        viewModelScope.launch {
            val currentList = database.studioDao().getAllProjects()
            // If empty, seed initial templates
            val sample = ProjectEntity(
                title = "2D Particle Physics Sandbox",
                category = "GAME",
                language = "KOTLIN",
                codeContent = """
// 2D Particle Engine Sandbox for Low-Entry Mobile
class Particle(var x: Float, var y: Float, var vx: Float, var vy: Float) {
    fun update(dt: Float, gravity: Float) {
        vy += gravity * dt
        x += vx * dt
        y += vy * dt
    }
}
                """.trimIndent(),
                description = "High-efficiency 60 FPS kinematic particle solver",
                isSynced = false
            )
            repository.saveProject(sample)
        }
    }
}
