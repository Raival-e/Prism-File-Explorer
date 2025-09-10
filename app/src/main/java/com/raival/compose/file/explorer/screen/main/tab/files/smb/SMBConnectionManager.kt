package com.raival.compose.file.explorer.screen.main.tab.files.smb

import com.hierynomus.smbj.SMBClient
import com.hierynomus.smbj.session.Session
import com.hierynomus.smbj.auth.AuthenticationContext

object SMBConnectionManager {
    private val clients = mutableMapOf<String, Session>()

    fun getSession(
        host: String,
        port: Int = 445,
        username: String?,
        password: String?,
        domain: String?,
        anonymous: Boolean
    ): Session {
        val key = listOf(host, port, username ?: "anon", domain ?: "", anonymous, password?.hashCode() ?: 0)
            .joinToString("|")
        clients[key]?.let { return it }

        val client = SMBClient()
        val connection = client.connect(host, port)
        val session = try {
            if (anonymous || username.isNullOrBlank()) {
                connection.authenticate(null)
            } else {
                connection.authenticate(
                    AuthenticationContext(username, password?.toCharArray(), domain)
                )
            }
        } catch (e: Exception) {
            connection.close()
            throw RuntimeException("Authentication failed for $username@$host:$port", e)
        }

        try {
            val share = session.connectShare("IPC$")
            share.close()
        } catch (e: Exception) {
            session.logoff()
            throw RuntimeException("Session could not access share. Bad credentials?", e)
        }

        clients[key] = session
        return session
    }

}