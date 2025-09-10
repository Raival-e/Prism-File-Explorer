package com.raival.compose.file.explorer.screen.main.tab.files.smb

import jcifs.CIFSContext
import jcifs.context.BaseContext
import jcifs.context.SingletonContext
import jcifs.smb.NtlmPasswordAuthenticator
import jcifs.smb.SmbFile
import java.net.MalformedURLException

object SMB1ConnectionManager {

    private val contexts = mutableMapOf<String, CIFSContext>()

    init {
        System.setProperty("jcifs.smb.client.minVersion", "SMB1")
        System.setProperty("jcifs.smb.client.maxVersion", "SMB1")
        System.setProperty("jcifs.smb.client.responseTimeout", "5000")
        System.setProperty("jcifs.smb.client.soTimeout", "5000")
    }

    fun getOrCreateContext(
        host: String,
        port: Int,
        share: String,
        username: String?,
        password: String?,
        domain: String?,
        anonymous: Boolean
    ): CIFSContext {
        val key = listOf(host, port, share, username ?: "anon", domain ?: "", anonymous, password?.hashCode() ?: 0)
            .joinToString("|")

        contexts[key]?.let { return it }

        val baseContext = SingletonContext.getInstance() as BaseContext
        val context = if (anonymous || username.isNullOrBlank()) {
            baseContext.withAnonymousCredentials()
        } else {
            val auth = NtlmPasswordAuthenticator(domain ?: "", username, password)
            baseContext.withCredentials(auth)
        }

        val testUrl = "smb://$host:$port/$share/"
        try {
            val file = SmbFile(testUrl, context)
            file.connect()
        } catch (e: Exception) {
            throw RuntimeException("Unable to connect to SMB1 share: $testUrl", e)
        }

        contexts[key] = context
        return context
    }

    fun getFile(
        host: String,
        port: Int,
        share: String,
        path: String = "",
        username: String? = null,
        password: String? = null,
        domain: String? = null,
        anonymous: Boolean = false
    ): SmbFile {
        val context = getOrCreateContext(host, port, share, username, password, domain, anonymous)

        val url = buildString {
            append("smb://")
            append(host)
            append(":")
            append(port)
            append("/")
            append(share)
            if (path.isNotBlank()) {
                append("/")
                append(path.trimStart('/'))
            }
            if(share.isNotBlank())
                append("/")
        }

        return try {
            SmbFile(url, context)
        } catch (e: MalformedURLException) {
            throw RuntimeException("Invalid SMB URL: $url", e)
        }
    }


    fun getContext(
        host: String,
        port: Int,
        share: String,
        username: String? = null,
        password: String? = null,
        domain: String? = null,
        anonymous: Boolean = false
    ): CIFSContext {
        return getOrCreateContext(host, port, share, username, password, domain, anonymous)
    }
}
