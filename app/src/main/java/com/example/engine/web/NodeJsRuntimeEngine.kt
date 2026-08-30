package com.example.engine.web

import com.example.data.remote.SecurityManager

/**
 * Node.js 22 LTS Core Runtime & NPM Ecosystem Simulator.
 * Provides virtualized server-side execution, REST/Express routing, EventEmitter,
 * In-memory Virtual FileSystem, and NPM dependency manager.
 */
object NodeJsRuntimeEngine {

    data class NpmPackage(
        val name: String,
        val version: String,
        val description: String,
        val sizeKb: Float,
        val isInstalled: Boolean = false,
        val category: String = "CORE" // FRAMEWORK, STATE, 3D, UTILS, BACKEND
    )

    data class ServerRoute(
        val method: String, // GET, POST, PUT, DELETE, WS
        val path: String,
        val handlerName: String,
        val responseJson: String
    )

    data class TerminalOutput(
        val command: String,
        val logs: List<String>,
        val exitCode: Int = 0,
        val executionTimeMs: Long
    )

    // Virtual In-Memory Filesystem
    private val virtualFileSystem = mutableMapOf(
        "package.json" to """
        {
          "name": "studio-fullstack-app",
          "version": "1.0.0",
          "type": "module",
          "scripts": {
            "dev": "vite",
            "build": "tsc && vite build",
            "server": "node server.js",
            "test": "vitest run"
          },
          "dependencies": {
            "react": "^19.0.0",
            "react-dom": "^19.0.0",
            "express": "^4.21.0",
            "zustand": "^4.5.2",
            "three": "^0.165.0"
          },
          "devDependencies": {
            "typescript": "^5.5.3",
            "@types/react": "^18.3.3",
            "vite": "^5.3.1"
          }
        }
        """.trimIndent(),
        "server.js" to """
        import express from 'express';
        const app = express();
        app.use(express.json());

        app.get('/api/health', (req, res) => {
          res.json({ status: 'ok', uptime: process.uptime(), engine: 'Node.js v22-LTS' });
        });

        app.post('/api/game/sync', (req, res) => {
          const { score, playerClass } = req.body;
          res.json({ success: true, savedScore: score, syncId: 'sync_' + Date.now() });
        });

        app.listen(3000, () => console.log('🚀 Server listening on port 3000'));
        """.trimIndent()
    )

    // Standard pre-configured modern NPM package registry
    val npmRegistry = mutableListOf(
        NpmPackage("react", "^19.0.0", "The library for web and native user interfaces", 8.2f, isInstalled = true, "FRAMEWORK"),
        NpmPackage("react-dom", "^19.0.0", "React package for working with the DOM", 42.1f, isInstalled = true, "FRAMEWORK"),
        NpmPackage("typescript", "^5.5.3", "Language for application scale JavaScript", 68.0f, isInstalled = true, "FRAMEWORK"),
        NpmPackage("express", "^4.21.0", "Fast, unopinionated, minimalist web framework", 54.3f, isInstalled = true, "BACKEND"),
        NpmPackage("zustand", "^4.5.2", "Bear necessities for state management in React", 3.1f, isInstalled = true, "STATE"),
        NpmPackage("three", "^0.165.0", "JavaScript 3D library for WebGL rendering", 142.0f, isInstalled = true, "3D"),
        NpmPackage("@react-three/fiber", "^8.16.8", "A React renderer for Threejs on Web and Mobile", 28.5f, isInstalled = false, "3D"),
        NpmPackage("tailwindcss", "^3.4.4", "A utility-first CSS framework for rapid UI development", 35.0f, isInstalled = true, "UTILS"),
        NpmPackage("socket.io", "^4.7.5", "Real-time bidirectional event-based communication", 44.0f, isInstalled = false, "BACKEND"),
        NpmPackage("howler", "^2.2.4", "Audio library for modern web and game development", 9.8f, isInstalled = true, "UTILS"),
        NpmPackage("matter-js", "^0.19.0", "2D rigid body physics engine for the web", 46.2f, isInstalled = false, "UTILS"),
        NpmPackage("vite", "^5.3.1", "Next Generation Frontend Tooling", 18.4f, isInstalled = true, "UTILS")
    )

    // Simulated Express Routes
    val registeredRoutes = listOf(
        ServerRoute("GET", "/api/health", "HealthCheckController", "{\"status\":\"ok\",\"engine\":\"Node.js v22\",\"fps\":60}"),
        ServerRoute("GET", "/api/game/leaderboard", "LeaderboardController", "[{\"rank\":1,\"name\":\"CyberNinja\",\"score\":4890},{\"rank\":2,\"name\":\"SolarKnight\",\"score\":3920}]"),
        ServerRoute("POST", "/api/game/save", "GameSaveController", "{\"success\":true,\"savedAt\":\"2026-08-25T08:30:00Z\"}"),
        ServerRoute("GET", "/api/telemetry/metrics", "TelemetryController", "{\"cpuLoad\":18,\"memoryMb\":28.4,\"p99LatencyMs\":1.8}"),
        ServerRoute("WS", "ws://localhost:3000/game-sync", "MultiplayerWebSocketHandler", "{\"connected\":true,\"channel\":\"room_cyber_01\"}")
    )

    /**
     * Executes a virtual terminal command (e.g. "npm install", "node server.js", "npm run build").
     */
    fun runTerminalCommand(cmd: String): TerminalOutput {
        val startNanos = System.nanoTime()
        val logs = mutableListOf<String>()
        val trimmed = cmd.trim()

        when {
            trimmed.startsWith("npm install") || trimmed.startsWith("npm i") -> {
                val pkgName = trimmed.removePrefix("npm install").removePrefix("npm i").trim()
                if (pkgName.isEmpty()) {
                    logs.add("📦 Restoring dependencies from package.json...")
                    logs.add("✔ audited 12 packages in 42ms (zero vulnerabilities found)")
                    logs.add("✔ node_modules synced (Virtual In-Memory Cache)")
                } else {
                    val target = npmRegistry.find { it.name.equals(pkgName, ignoreCase = true) }
                    if (target != null) {
                        val idx = npmRegistry.indexOf(target)
                        npmRegistry[idx] = target.copy(isInstalled = true)
                        logs.add("📦 Fetching $pkgName@${target.version} from registry...")
                        logs.add("✔ +$pkgName@${target.version} added (${target.sizeKb} KB)")
                        logs.add("✔ package.json updated successfully")
                    } else {
                        logs.add("📦 Searching registry for $pkgName...")
                        logs.add("✔ +$pkgName@latest installed into virtual container")
                    }
                }
            }

            trimmed == "npm run build" || trimmed == "vite build" -> {
                logs.add("vite v5.3.1 building for production...")
                logs.add("✓ 24 modules transformed.")
                logs.add("dist/index.html                   0.45 kB │ gzip:  0.28 kB")
                logs.add("dist/assets/index-D7h3B1.css      1.82 kB │ gzip:  0.89 kB")
                logs.add("dist/assets/index-kL89qW.js      24.60 kB │ gzip:  8.12 kB")
                logs.add("✓ built in 14ms (Tree-Shaking: 94.2% dead code eliminated)")
            }

            trimmed == "node server.js" || trimmed == "npm run server" -> {
                logs.add("🚀 [Node.js v22.4.0 Engine Active]")
                logs.add("📡 Express server bound to http://localhost:3000")
                logs.add("🔗 Routes mounted: GET /api/health, GET /api/game/leaderboard, POST /api/game/save")
                logs.add("⚡ WebSocket sync listener active on ws://localhost:3000/game-sync")
            }

            trimmed.startsWith("curl ") -> {
                val url = trimmed.removePrefix("curl ").trim()
                logs.add("HTTP/1.1 200 OK")
                logs.add("Content-Type: application/json; charset=utf-8")
                logs.add("X-Powered-By: Express/Node.js")
                logs.add("Response Payload: {\"status\":\"ok\",\"timestamp\":${System.currentTimeMillis()},\"endpoint\":\"$url\"}")
            }

            trimmed == "ls" || trimmed == "dir" -> {
                logs.add("📂 Virtual Workspace:")
                virtualFileSystem.keys.forEach { logs.add("  📄 $it (${virtualFileSystem[it]?.length ?: 0} bytes)") }
            }

            else -> {
                logs.add("Executing: $trimmed")
                logs.add("✔ Output: Process finished with exit code 0 (Elapsed: 4ms)")
            }
        }

        val elapsedMs = (System.nanoTime() - startNanos) / 1_000_000

        return TerminalOutput(
            command = trimmed,
            logs = logs,
            exitCode = 0,
            executionTimeMs = elapsedMs
        )
    }

    /**
     * Reads a virtual file from the in-memory FS.
     */
    fun readFile(fileName: String): String? = virtualFileSystem[fileName]

    /**
     * Writes or updates a virtual file.
     */
    fun writeFile(fileName: String, content: String) {
        virtualFileSystem[fileName] = content
    }
}
