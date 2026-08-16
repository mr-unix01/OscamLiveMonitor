package com.mrunix.oscamlivemonitor

import com.jcraft.jsch.ChannelShell
import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.apache.commons.net.telnet.TelnetClient
import java.io.InputStream
import java.io.OutputStream

enum class TerminalProtocol {
    SSH,
    TELNET
}

class TerminalClient {

    private var sshSession: Session? = null
    private var sshChannel: ChannelShell? = null
    private var telnetClient: TelnetClient? = null

    private var input: InputStream? = null
    private var output: OutputStream? = null

    @Volatile
    var connected: Boolean = false
        private set

    suspend fun connect(
        protocol: TerminalProtocol,
        host: String,
        port: Int,
        username: String,
        password: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            disconnectInternal()

            when (protocol) {
                TerminalProtocol.SSH -> connectSsh(
                    host = host,
                    port = port,
                    username = username,
                    password = password
                )

                TerminalProtocol.TELNET -> connectTelnet(
                    host = host,
                    port = port,
                    username = username,
                    password = password
                )
            }

            connected = true
        }
    }

    private fun connectSsh(
        host: String,
        port: Int,
        username: String,
        password: String
    ) {
        val jsch = JSch()

        val session = jsch.getSession(
            username,
            host,
            port
        )

        if (password.isNotEmpty()) {
            session.setPassword(password.toByteArray())
        }

        session.setConfig(
            "StrictHostKeyChecking",
            "no"
        )

        session.connect(10_000)

        val channel =
            session.openChannel("shell") as ChannelShell

        channel.setPty(true)
        channel.setPtyType("xterm")

        input = channel.inputStream
        output = channel.outputStream

        channel.connect(10_000)

        sshSession = session
        sshChannel = channel
    }

    private suspend fun connectTelnet(
        host: String,
        port: Int,
        username: String,
        password: String
    ) {
        val client = TelnetClient("VT100")

        client.connect(host, port)

        input = client.inputStream
        output = client.outputStream
        telnetClient = client

        if (username.isNotBlank()) {
            delay(350)
            output?.write(
                "$username\r\n".toByteArray()
            )
            output?.flush()
        }

        if (password.isNotBlank()) {
            delay(350)
            output?.write(
                "$password\r\n".toByteArray()
            )
            output?.flush()
        }
    }

    suspend fun readLoop(
        onChunk: suspend (String) -> Unit
    ) = withContext(Dispatchers.IO) {

        val buffer = ByteArray(4096)

        try {
            while (connected) {
                val count =
                    input?.read(buffer) ?: -1

                if (count <= 0) {
                    break
                }

                onChunk(
                    String(
                        buffer,
                        0,
                        count,
                        Charsets.UTF_8
                    )
                )
            }
        } finally {
            connected = false
        }
    }

    suspend fun send(
        command: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            if (!connected) {
                error("Terminale non connesso")
            }

            val terminalOutput =
                output ?: error("Terminale non connesso")

            terminalOutput.write(
                "$command\r\n".toByteArray()
            )

            terminalOutput.flush()

            Unit
        }
    }

    fun disconnect() {
        disconnectInternal()
    }

    private fun disconnectInternal() {
        connected = false

        runCatching {
            sshChannel?.disconnect()
        }

        runCatching {
            sshSession?.disconnect()
        }

        runCatching {
            if (telnetClient?.isConnected == true) {
                telnetClient?.disconnect()
            }
        }

        sshChannel = null
        sshSession = null
        telnetClient = null
        input = null
        output = null
    }
}
