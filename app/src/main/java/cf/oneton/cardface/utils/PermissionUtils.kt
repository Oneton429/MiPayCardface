package cf.oneton.cardface.utils

import android.util.Log

import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.IOException

object PermissionUtils {
    private const val TAG: String = "PermissionUtils"
    private var mRooted: Boolean = false

    suspend fun isRootAvailable(
        dispatcher: CoroutineDispatcher,
    ): Boolean = withContext(dispatcher) {
        if (mRooted) {
            return@withContext true
        }
        val libSuStatus = Shell.isAppGrantedRoot() ?: false
        if (libSuStatus) {
            Log.i(TAG, "Get root permission from isAppGrantedRoot")
            mRooted = true
            return@withContext true
        } else {
            val requestResult = requestRootPermission(dispatcher)
            if (requestResult) {
                mRooted = true
                Log.i(TAG, "Requested root permission from Shell.cmd(\"su -M\")")
                return@withContext true
            }
            val runtimeResult = requestRootInRuntime(dispatcher)
            if (runtimeResult) {
                mRooted = true
                Log.i(TAG, "Requested root permission from Runtime.getRuntime().exec(su -M)")
            }
            return@withContext runtimeResult
        }
    }

    private suspend fun requestRootPermission(dispatcher: CoroutineDispatcher): Boolean = withContext(dispatcher) {
        Shell.cmd("su -M").exec().isSuccess
    }

    private suspend fun requestRootInRuntime(dispatcher: CoroutineDispatcher): Boolean {
        // isAppGrantedRoot is always false on KernelSU and APatch.
        // This method looks for a file but su is not a real file in those
        return withContext(dispatcher) {
            try {
                val process = Runtime.getRuntime().exec("su -M")
                val exitValue = process.waitFor()
                val isSuccess = exitValue == 0
                if (isSuccess) {
                    Log.i(TAG, "Requested root permission from Runtime.getRuntime().exec(su)")
                } else {
                    Log.e(TAG, "Root unavailable: exitValue of the su command is not 0 ($exitValue)")
                }
                return@withContext isSuccess
            } catch (e: IOException) {
                Log.e(TAG, "Root unavailable: ${e.message}")
                return@withContext false
            }
        }
    }
}