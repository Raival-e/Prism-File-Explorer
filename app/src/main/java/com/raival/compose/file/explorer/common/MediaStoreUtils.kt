package com.raival.compose.file.explorer.common

import android.content.Context
import android.media.MediaScannerConnection
import java.io.File

object MediaStoreUtils {
    /**
     * Notifies the MediaStore that one or more files have changed.
     *
     * @param context Context to use (applicationContext recommended)
     * @param files List of files to notify
     * @param mimeTypes Optional MIME types (should be same length as files array, or null)
     */
    @JvmOverloads
    fun notifyFileChanged(
        context: Context,
        files: List<File>,
        mimeTypes: List<String?>? = null
    ) {
        MediaScannerConnection.scanFile(
            context,
            files.map { it.absolutePath }.toTypedArray(),
            mimeTypes?.toTypedArray(),
            null
        )
    }

    /**
     * Notifies the MediaStore that a single file has changed.
     *
     * @param context Context to use (applicationContext recommended)
     * @param file The file that has changed.
     * @param mimeType Optional MIME type of the file.
     */
    @JvmOverloads
    fun notifyFileChanged(
        context: Context,
        file: File,
        mimeType: String? = null
    ) {
        notifyFileChanged(context, listOf(file), mimeTypes = mimeType?.let { listOf(it) })
    }
}