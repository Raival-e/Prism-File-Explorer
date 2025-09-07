package com.raival.compose.file.explorer.screen.main.tab.files.holder

import android.content.Context
import android.webkit.MimeTypeMap
import android.widget.Toast
import com.hierynomus.msdtyp.AccessMask
import com.hierynomus.msfscc.FileAttributes
import com.hierynomus.msfscc.fileinformation.FileIdBothDirectoryInformation
import com.hierynomus.mssmb2.SMB2CreateDisposition
import com.hierynomus.mssmb2.SMB2ShareAccess
import com.hierynomus.protocol.commons.EnumWithValue
import com.hierynomus.smbj.SMBClient
import com.hierynomus.smbj.connection.Connection
import com.hierynomus.smbj.share.DiskShare
import com.raival.compose.file.explorer.App.Companion.globalClass
import com.raival.compose.file.explorer.screen.main.tab.files.FilesTab
import com.raival.compose.file.explorer.screen.main.tab.files.misc.ContentCount
import com.raival.compose.file.explorer.screen.main.tab.files.misc.FileMimeType.anyFileType
import com.raival.compose.file.explorer.screen.main.tab.files.smb.SMBConnectionManager
import kotlinx.coroutines.runBlocking
import com.rapid7.client.dcerpc.transport.SMBTransportFactories
import com.rapid7.client.dcerpc.mssrvs.ServerService
import com.rapid7.client.dcerpc.mssrvs.dto.NetShareInfo0
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SMBFileHolder(
    val host: String,
    val username: String?,
    val password: String?,
    val anonymous: Boolean,
    val domain: String,
    val shareName: String = "",
    val pathInsideShare: String = "",
    private val _isFolder: Boolean = true
) : ContentHolder() {

    private var entry: FileIdBothDirectoryInformation? = null
    private var folderCount = 0
    private var fileCount = 0
    private var timestamp = -1L
    var details = ""

    override val displayName: String
        get() = when {
            shareName.isEmpty() -> host
            pathInsideShare.isEmpty() -> shareName
            else -> pathInsideShare.substringAfterLast("/")
        }

    override val isFolder: Boolean
        get() = _isFolder

    override val lastModified: Long
        get() = System.currentTimeMillis().also { if (timestamp == -1L) timestamp = it }

    override val size: Long
        get() = if (isFolder) 0L else entry?.endOfFile ?: 0L

    override val uniquePath: String
        get() = if (pathInsideShare.isEmpty()) "smb://$host/$shareName" else "smb://$host/$shareName/$pathInsideShare"

    override val extension: String by lazy {
        if (isFolder) ""
        else displayName.substringAfterLast('.', "").lowercase()
    }

    override val canAddNewContent: Boolean = true

    override val canRead: Boolean by lazy {
        if (isFolder) true
        else entry?.let { EnumWithValue.EnumUtils.isSet(it.fileAttributes, FileAttributes.FILE_ATTRIBUTE_READONLY).not() } ?: false
    }

    override val canWrite: Boolean by lazy {
        if (isFolder) false
        else entry?.let { EnumWithValue.EnumUtils.isSet(it.fileAttributes, FileAttributes.FILE_ATTRIBUTE_READONLY).not() } ?: false
    }

    val mimeType: String by lazy {
        if (isFolder) anyFileType
        else MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: anyFileType
    }

    override suspend fun getDetails(): String {
        if (details.isNotEmpty()) return details
        details = buildString {
            append("SMB Host: $host")
            if (!anonymous) append(" | User: $username")
        }
        return details
    }

    override suspend fun isValid(): Boolean = withContext(Dispatchers.IO) {
        if (host.isBlank()) return@withContext false

        val client = SMBClient()
        try {
            client.connect(host).use { connection ->
                val session = SMBConnectionManager.getSession(host, username, password, domain, anonymous)

                if (shareName.isNotBlank()) {
                    val share = session.connectShare(shareName) as DiskShare
                    share.list("").isNotEmpty()
                } else {
                    val transport = SMBTransportFactories.SRVSVC.getTransport(session)
                    val serverService = ServerService(transport)
                    val shares: List<NetShareInfo0> = serverService.shares0
                    shares.any { !it.netName.endsWith("$") }
                }
            }
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun listContent(): ArrayList<SMBFileHolder> {
        folderCount = 0
        fileCount = 0

        val result = arrayListOf<SMBFileHolder>()
        val client = SMBClient()

        try {
            client.connect(host).use { connection: Connection ->
                val session = SMBConnectionManager.getSession(host, username, password, domain, anonymous)

                if (shareName.isNullOrBlank() || shareName == "/") {
                    val transport = SMBTransportFactories.SRVSVC.getTransport(session)
                    val serverService = ServerService(transport)
                    val shares: List<NetShareInfo0> = serverService.shares0

                    for (share in shares) {
                        val name = share.netName
                        // filter administrative shares (C$, ADMIN$, IPC$, etc.)
                        if (!name.endsWith("$")) {
                            result.add(
                                SMBFileHolder(
                                    host = host,
                                    username = username,
                                    password = password,
                                    anonymous = anonymous,
                                    domain = domain,
                                    shareName = name
                                )
                            )
                            folderCount++
                        }
                    }
                } else {
                    val share: DiskShare = session.connectShare(shareName) as DiskShare
                    val listPath = pathInsideShare
                    for (entry in share.list(listPath)) {
                        // ignore virtual folders "." and ".."
                        if (entry.fileName == "." || entry.fileName == "..") continue

                        val isDir = EnumWithValue.EnumUtils.isSet(
                            entry.fileAttributes, FileAttributes.FILE_ATTRIBUTE_DIRECTORY
                        )
                        val childPath =
                            if (listPath.isEmpty()) entry.fileName else "$listPath/${entry.fileName}"

                        result.add(
                            SMBFileHolder(
                                host = host,
                                username = username,
                                password = password,
                                anonymous = anonymous,
                                domain = domain,
                                shareName = shareName,
                                pathInsideShare = childPath,
                                _isFolder = isDir
                            ).apply { this.entry = entry })

                        if (isDir) folderCount++ else fileCount++
                    }
                }
            }
        } catch (e: Exception) {
            globalClass.logger.logError(e)
        }

        return result
    }

    override suspend fun getParent(): SMBFileHolder? {
        if (pathInsideShare.isBlank()) return null //
        val parentPath = pathInsideShare.substringBeforeLast("/", "")
        return SMBFileHolder(host, username, password, anonymous, domain, shareName, parentPath)
    }

    override fun open(context: Context, anonymous: Boolean, skipSupportedExtensions: Boolean, customMimeType: String?) {
        
    }

    override suspend fun getContentCount(): ContentCount = ContentCount(fileCount, folderCount)

    override suspend fun createSubFile(name: String, onCreated: (ContentHolder?) -> Unit) {
        val client = SMBClient()
        try {
            client.connect(host).use { connection ->
                val session = SMBConnectionManager.getSession(host, username, password, domain, anonymous)

                val share = session.connectShare(shareName) as DiskShare
                val newFilePath = if (pathInsideShare.isBlank()) name else "$pathInsideShare/$name"

                val file = share.openFile(
                    newFilePath,
                    setOf(AccessMask.GENERIC_WRITE),
                    null,
                    SMB2ShareAccess.ALL,
                    SMB2CreateDisposition.FILE_CREATE,
                    null
                )
                file.close()

                onCreated(
                    SMBFileHolder(
                        host = host,
                        username = username,
                        password = password,
                        anonymous = anonymous,
                        domain = domain,
                        shareName = shareName,
                        pathInsideShare = newFilePath,
                        _isFolder = false
                    )
                )
            }
        } catch (e: Exception) {
            globalClass.logger.logError(e)
            onCreated(null)
        }
    }

    override suspend fun createSubFolder(name: String, onCreated: (ContentHolder?) -> Unit) {
        val client = SMBClient()
        try {
            client.connect(host).use { connection ->
                val session = SMBConnectionManager.getSession(host, username, password, domain, anonymous)

                val share = session.connectShare(shareName) as DiskShare

                val newFolderPath = if (pathInsideShare.isBlank()) name else "$pathInsideShare/$name"
                share.mkdir(newFolderPath)

                onCreated(
                    SMBFileHolder(
                        host = host,
                        username = username,
                        password = password,
                        anonymous = anonymous,
                        domain = domain,
                        shareName = shareName,
                        pathInsideShare = newFolderPath,
                        _isFolder = true
                    )
                )
            }
        } catch (e: Exception) {
            globalClass.logger.logError(e)
            onCreated(null)
        }
    }

    override suspend fun findFile(name: String): SMBFileHolder? {
        val client = SMBClient()
        try {
            client.connect(host).use { connection ->
                val session = SMBConnectionManager.getSession(host, username, password, domain, anonymous)

                val share = session.connectShare(shareName) as DiskShare
                for (entry in share.list(pathInsideShare)) {
                    if (entry.fileName == name) {
                        val isDir = EnumWithValue.EnumUtils.isSet(
                            entry.fileAttributes, FileAttributes.FILE_ATTRIBUTE_DIRECTORY
                        )
                        val fullPath = if (pathInsideShare.isBlank()) name else "$pathInsideShare/$name"

                        return SMBFileHolder(
                            host = host,
                            username = username,
                            password = password,
                            anonymous = anonymous,
                            domain = domain,
                            shareName = shareName,
                            pathInsideShare = fullPath,
                            _isFolder = isDir
                        )
                    }
                }
            }
        } catch (e: Exception) {
            globalClass.logger.logError(e)
        }
        return null
    }

    fun exists() = runBlocking { isValid() }
}




