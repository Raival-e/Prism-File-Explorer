package com.raival.compose.file.explorer.screen.main.tab.files.holder

import android.content.Context
import android.webkit.MimeTypeMap
import com.raival.compose.file.explorer.screen.main.tab.files.misc.ContentCount
import com.raival.compose.file.explorer.screen.main.tab.files.misc.FileMimeType.anyFileType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import jcifs.smb.SmbFile
import com.raival.compose.file.explorer.screen.main.tab.files.smb.SMB1ConnectionManager
import kotlinx.coroutines.runBlocking

class SMB1FileHolder(
    val host: String,
    val port: Int = 139,
    val username: String? = null,
    val password: String? = null,
    val anonymous: Boolean = false,
    val domain: String? = null,
    val shareName: String = "",
    val pathInsideShare: String = "",
    private val _isFolder: Boolean = true
) : ContentHolder() {

    private var folderCount = 0
    private var fileCount = 0
    var details = ""

    override val displayName: String
        get() = when {
            shareName.isEmpty() -> host // raíz del host
            pathInsideShare.isEmpty() -> shareName // share
            else -> {
                val file = getSmbFile()
                file.name.trimEnd('/').substringAfterLast('/') // nombre real del archivo/carpeta
            }
        }

    override val isFolder: Boolean
        get() = _isFolder

    override val lastModified: Long
        get() = System.currentTimeMillis() // jcifs-ng no expone lastModified fácilmente

    override val size: Long
        get() = if (isFolder) 0L else try { getSmbFile().length() } catch (e: Exception) { 0L }

    override val uniquePath: String
        get() = if (pathInsideShare.isEmpty()) "smb://$host/$shareName" else "smb://$host/$shareName/$pathInsideShare"

    override val extension: String by lazy {
        if (isFolder) ""
        else displayName.substringAfterLast('.', "").lowercase()
    }

    override val canAddNewContent: Boolean = true
    override val canRead: Boolean get() = true
    override val canWrite: Boolean get() = true

    val mimeType: String by lazy {
        if (isFolder) anyFileType
        else MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: anyFileType
    }

    private fun getSmbFile(): SmbFile = SMB1ConnectionManager.getFile(
        host = host,
        port = port,
        share = shareName,
        path = pathInsideShare,
        username = username,
        password = password,
        domain = domain,
        anonymous = anonymous
    )

    override suspend fun getDetails(): String {
        if (details.isNotEmpty()) return details
        details = buildString {
            append("SMB1 Host: $host")
            if (!anonymous) append(" | User: $username")
        }
        return details
    }

    override suspend fun isValid(): Boolean = withContext(Dispatchers.IO) {
        try {
            val file = getSmbFile()
            file.connect() // fuerza la conexión al servidor
            file.list()    // opcional: lista el directorio para asegurarte que existe
            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun listContent(): ArrayList<SMB1FileHolder> = withContext(Dispatchers.IO) {
        folderCount = 0
        fileCount = 0
        val result = arrayListOf<SMB1FileHolder>()
        try {
            val folder = getSmbFile()

            if (shareName.isEmpty()) {
                folder.listFiles()?.forEach { s ->
                    val shareNameClean = s.name.trimEnd('/')
                    if (!shareNameClean.endsWith("$")) {
                        result.add(
                            SMB1FileHolder(
                                host = host,
                                port = port,
                                username = username,
                                password = password,
                                anonymous = anonymous,
                                domain = domain,
                                shareName = shareNameClean,
                                _isFolder = true
                            )
                        )
                        folderCount++
                    }
                }
            } else if (folder.isDirectory) {
                folder.listFiles()?.forEach { f ->
                    if (f.name == "." || f.name == "..") return@forEach
                    val isDir = f.isDirectory
                    var rawName = f.name.trimEnd('/')

                    if (rawName.startsWith(shareName)) {
                        rawName = rawName.removePrefix(shareName)
                    }

                    val childPath = if (pathInsideShare.isEmpty()) rawName else "$pathInsideShare/$rawName"

                    result.add(
                        SMB1FileHolder(
                            host = host,
                            port = port,
                            username = username,
                            password = password,
                            anonymous = anonymous,
                            domain = domain,
                            shareName = shareName,
                            pathInsideShare = childPath,
                            _isFolder = isDir
                        )
                    )

                    if (isDir) folderCount++ else fileCount++
                }
            }
        } catch (_: Exception) {}
        result
    }

    override suspend fun getParent(): SMB1FileHolder? {
        return when {
            shareName.isEmpty() -> null
            pathInsideShare.isBlank() -> SMB1FileHolder(
                host = host,
                port = port,
                username = username,
                password = password,
                anonymous = anonymous,
                domain = domain,
                shareName = "",
                _isFolder = true
            )
            else -> {
                val parentPath = pathInsideShare.substringBeforeLast("/", "")
                SMB1FileHolder(
                    host = host,
                    port = port,
                    username = username,
                    password = password,
                    anonymous = anonymous,
                    domain = domain,
                    shareName = shareName,
                    pathInsideShare = parentPath,
                    _isFolder = true
                )
            }
        }
    }

    override fun open(context: Context, anonymous: Boolean, skipSupportedExtensions: Boolean, customMimeType: String?) {
        // abrir archivos no implementado todavía
    }

    override suspend fun getContentCount(): ContentCount = ContentCount(fileCount, folderCount)

    override suspend fun createSubFile(name: String, onCreated: (ContentHolder?) -> Unit) = withContext(Dispatchers.IO) {
        try {
            val parent = getSmbFile()
            if (parent.isDirectory) {
                val newFile = SmbFile("${parent.url}$name", SMB1ConnectionManager.getContext(host, port, shareName, username, password, domain, anonymous))
                newFile.createNewFile()
                onCreated(
                    SMB1FileHolder(
                        host = host,
                        port = port,
                        username = username,
                        password = password,
                        anonymous = anonymous,
                        domain = domain,
                        shareName = shareName,
                        pathInsideShare = if (pathInsideShare.isEmpty()) name else "$pathInsideShare/$name",
                        _isFolder = false
                    )
                )
            } else onCreated(null)
        } catch (_: Exception) {
            onCreated(null)
        }
    }

    override suspend fun createSubFolder(name: String, onCreated: (ContentHolder?) -> Unit) = withContext(Dispatchers.IO) {
        try {
            val parent = getSmbFile()
            if (parent.isDirectory) {
                val newFolder = SmbFile("${parent.url}$name/", SMB1ConnectionManager.getContext(host, port,shareName, username, password, domain, anonymous))
                newFolder.mkdir()
                onCreated(
                    SMB1FileHolder(
                        host = host,
                        port = port,
                        username = username,
                        password = password,
                        anonymous = anonymous,
                        domain = domain,
                        shareName = shareName,
                        pathInsideShare = if (pathInsideShare.isEmpty()) name else "$pathInsideShare/$name",
                        _isFolder = true
                    )
                )
            } else onCreated(null)
        } catch (_: Exception) {
            onCreated(null)
        }
    }

    override suspend fun findFile(name: String): SMB1FileHolder? = withContext(Dispatchers.IO) {
        try {
            val folder = getSmbFile()
            if (folder.isDirectory) {
                folder.listFiles()?.forEach { f ->
                    if (f.name == name) {
                        return@withContext SMB1FileHolder(
                            host = host,
                            port = port,
                            username = username,
                            password = password,
                            anonymous = anonymous,
                            domain = domain,
                            shareName = shareName,
                            pathInsideShare = if (pathInsideShare.isEmpty()) name else "$pathInsideShare/$name",
                            _isFolder = f.isDirectory
                        )
                    }
                }
            }
        } catch (_: Exception) {}
        null
    }

    fun exists() = runBlocking { isValid() }
}

