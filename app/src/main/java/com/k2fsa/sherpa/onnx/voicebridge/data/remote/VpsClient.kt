package com.k2fsa.sherpa.onnx.voicebridge.data.remote

import android.util.Log
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.Socket
import javax.inject.Inject

private const val TAG = "VpsClient"

class VpsClient @Inject constructor() {

    fun sendAndReceive(host: String, port: Int, text: String, timeoutMs: Int = 300_000): Result<String> {
        return try {
            val socket = Socket(host, port)
            socket.soTimeout = timeoutMs

            val writer = PrintWriter(socket.getOutputStream(), true)
            val reader = BufferedReader(InputStreamReader(socket.getInputStream()))

            writer.print(text)
            writer.flush()
            socket.shutdownOutput()

            val sb = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                if (line == "---END---") break
                sb.appendLine(line)
            }

            socket.close()
            Result.success(sb.toString().trim())
        } catch (e: Exception) {
            Log.e(TAG, "VPS communication error: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Streaming version: calls onChunk for each line as it arrives from the VPS.
     * Returns the full accumulated text at the end.
     */
    fun sendAndReceiveStreaming(
        host: String,
        port: Int,
        text: String,
        timeoutMs: Int = 300_000,
        onChunk: (accumulated: String) -> Unit,
    ): Result<String> {
        return try {
            val socket = Socket(host, port)
            socket.soTimeout = timeoutMs

            val writer = PrintWriter(socket.getOutputStream(), true)
            val reader = BufferedReader(InputStreamReader(socket.getInputStream()))

            writer.print(text)
            writer.flush()
            socket.shutdownOutput()

            val sb = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                if (line == "---END---") break
                if (sb.isNotEmpty()) sb.appendLine()
                sb.append(line)
                // Emit accumulated text so far
                onChunk(sb.toString())
            }

            socket.close()
            Result.success(sb.toString().trim())
        } catch (e: Exception) {
            Log.e(TAG, "VPS streaming error: ${e.message}")
            Result.failure(e)
        }
    }
}
