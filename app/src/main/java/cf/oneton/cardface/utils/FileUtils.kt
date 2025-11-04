package cf.oneton.cardface.utils

import android.annotation.SuppressLint
import android.util.Log
import com.topjohnwu.superuser.io.SuFile
import com.topjohnwu.superuser.io.SuFileInputStream
import com.topjohnwu.superuser.io.SuFileOutputStream
import java.io.File

object FileUtils {
    private const val TAG: String = "FileUtils"

    @SuppressLint("SdCardPath")
    private val baseFolder = SuFile("/data/data/com.miui.tsmclient/cache/image_manager_disk_cache/")

    @JvmStatic
    fun read(name: String): ByteArray {
        val path = baseFolder.getChildFile(name)
        SuFileInputStream.open(path).use {
            return it.readBytes()
        }
    }

    @JvmStatic
    fun read(name: String, limit: Int): ByteArray {
        val path = baseFolder.getChildFile(name)
        SuFileInputStream.open(path).use {
            var buffer = ByteArray(limit)
            it.read(buffer, 0, limit)
            return buffer
        }
    }

    @JvmStatic
    fun listFiles(): List<String> {
        if (!baseFolder.exists()) {
            Log.w(TAG, "File $baseFolder not exists")
            return ArrayList()
        }
        return baseFolder.list()?.toList() ?: ArrayList()
    }

    @JvmStatic
    fun copy(source: String, dest: String): File {
        val sourceFile = baseFolder.getChildFile(source)
        val destFile = baseFolder.getChildFile(dest)
        SuFileOutputStream.open(destFile).use { output ->
            SuFileInputStream.open(sourceFile).use { input ->
                input.copyTo(output)
            }
            return destFile
        }
    }

    @JvmStatic
    fun write(name: String, bytes: ByteArray): File {
        val path = baseFolder.getChildFile(name)
        SuFileOutputStream.open(path).use {
            it.write(bytes)
            return path
        }
    }

    @JvmStatic
    fun swap(source1: String, source2: String): ByteArray {
        val path1 = baseFolder.getChildFile(source1)
        val path2 = baseFolder.getChildFile(source2)
        if (!path1.exists()) {
            Log.w(TAG, "File $path1 not exists")
            return ByteArray(0)
        }
        if (!path2.exists()) {
            Log.w(TAG, "File $path2 not exists")
            return ByteArray(0)
        }
        var tmpName: String
        var tmpFile: SuFile
        do {
            tmpName = ".tmp_swap_${System.currentTimeMillis()}_$source1"
            tmpFile = baseFolder.getChildFile(tmpName)
        } while (tmpFile.exists())

        path1.renameTo(tmpFile)
        path2.renameTo(path1)
        tmpFile.renameTo(path2)

        Log.i(TAG, "Swap $source1 and $source2 via $tmpName")
        return read(source1) // Bytes of original $source2
    }
}
