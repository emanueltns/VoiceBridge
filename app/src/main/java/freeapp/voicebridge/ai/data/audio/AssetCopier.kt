package freeapp.voicebridge.ai.data.audio

import android.app.Application
import android.util.Log
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "AssetCopier"

@Singleton
class AssetCopier @Inject constructor(
    private val application: Application,
) {
    fun copyDataDir(dataDir: String): String {
        copyAssets(dataDir)
        val extDir = application.getExternalFilesDir(null) ?: application.filesDir
        return extDir.absolutePath
    }

    private fun copyAssets(path: String) {
        try {
            val assets = application.assets.list(path)
            if (assets.isNullOrEmpty()) {
                copyFile(path)
            } else {
                val extDir = application.getExternalFilesDir(null) ?: application.filesDir
                val fullPath = "$extDir/$path"
                File(fullPath).mkdirs()
                for (asset in assets) {
                    val p = if (path.isEmpty()) "" else "$path/"
                    copyAssets(p + asset)
                }
            }
        } catch (ex: IOException) {
            Log.e(TAG, "Failed to copy $path: $ex")
        }
    }

    private fun copyFile(filename: String) {
        try {
            val istream = application.assets.open(filename)
            val extDir = application.getExternalFilesDir(null) ?: application.filesDir
            val newFilename = "$extDir/$filename"
            val ostream = java.io.FileOutputStream(newFilename)
            val buffer = ByteArray(1024)
            var read: Int
            while (istream.read(buffer).also { read = it } != -1) {
                ostream.write(buffer, 0, read)
            }
            istream.close()
            ostream.flush()
            ostream.close()
        } catch (ex: Exception) {
            Log.e(TAG, "Failed to copy $filename: $ex")
        }
    }
}
