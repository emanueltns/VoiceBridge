package com.k2fsa.sherpa.onnx.voicebridge.data.remote

import android.util.Log
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.Socket
import javax.inject.Inject

private const val TAG = "VpsClient"

class VpsClient @Inject constructor() {

    fun sendAndReceive(host: String, port: Int, text: String, timeoutMs: Int = 180_000): Result<String> {
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
}
