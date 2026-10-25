package com.rupleide.netfix.core.dpibypass

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.File
import java.util.concurrent.TimeUnit

internal class RootDpiSession private constructor(private val process: Process, private val script: File) {
    val isAlive: Boolean get() = process.isAlive

    fun disconnect() {
        process.outputStream.close()
        script.delete()
    }

    suspend fun close() = withContext(Dispatchers.IO) {
        disconnect()
        if (!process.waitFor(10, TimeUnit.SECONDS)) {
            process.destroy()
            check(process.waitFor(10, TimeUnit.SECONDS)) { "Root firewall cleanup did not finish" }
        }
        script.delete()
        check(process.exitValue() == 0) { "Root firewall cleanup failed" }
    }

    companion object {
        suspend fun open(context: Context, rules: RootDpiRules): RootDpiSession {
            var pending: RootDpiSession? = null
            try {
                withContext(Dispatchers.IO) {
                    val script = File.createTempFile("root-dpi-", ".sh", context.cacheDir)
                    script.writeText(rules.guardian(File(context.filesDir, "root-dpi.lock").absolutePath))
                    val process = try {
                        ProcessBuilder("su", "-c", "sh '${script.absolutePath.replace("'", "'\\''")}'")
                            .redirectErrorStream(true).start()
                    } catch (e: Exception) {
                        script.delete()
                        throw e
                    }
                    val session = RootDpiSession(process, script)
                    pending = session
                    val reader = process.inputStream.bufferedReader()
                    val output = StringBuilder()
                    withTimeout(30_000) {
                        while (true) {
                            if (reader.ready()) {
                                val line = reader.readLine() ?: error("Root process exited: $output")
                                if (line == "READY") break
                                if (output.length < 4096) output.appendLine(line)
                            } else {
                                check(process.isAlive) { "Root access or firewall setup failed: $output" }
                                delay(50)
                            }
                        }
                    }
                }
                return checkNotNull(pending).also { pending = null }
            } catch (e: Throwable) {
                try {
                    withContext(NonCancellable) { pending?.close() }
                } catch (cleanupError: Exception) { e.addSuppressed(cleanupError) }
                throw e
            }
        }
    }
}
