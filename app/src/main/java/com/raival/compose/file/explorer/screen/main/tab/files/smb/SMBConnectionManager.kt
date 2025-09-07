package com.raival.compose.file.explorer.screen.main.tab.files.smb

import com.hierynomus.smbj.SMBClient
import com.hierynomus.smbj.session.Session
import com.hierynomus.smbj.auth.AuthenticationContext

object SMBConnectionManager {
    private val clients = mutableMapOf<String, Session>()

    fun getSession(
        host: String,
        username: String?,
        password: String?,
        domain: String?,
        anonymous: Boolean
    ): Session {
        val key = "$host|${username ?: "anon"}"
        return clients[key] ?: run {
            val client = SMBClient()
            val connection = client.connect(host)
            val session = if (anonymous || username.isNullOrBlank()) {
                connection.authenticate(null)
            } else {
                connection.authenticate(
                    AuthenticationContext(username, password?.toCharArray(), domain)
                )
            }
            clients[key] = session
            session
        }
    }
}
