package com.example.telecom

import android.telecom.*

class MyConnectionService : ConnectionService() {
    override fun onCreateOutgoingConnection(
        connectionManagerPhoneAccount: PhoneAccountHandle,
        request: ConnectionRequest
    ): Connection {
        val conn = object : Connection() {
            override fun onAnswer() { }
            override fun onDisconnect() {
                setDisconnected(DisconnectCause(DisconnectCause.LOCAL))
                destroy()
            }
        }
        conn.setAddress(request.address, TelecomManager.PRESENTATION_ALLOWED)
        conn.setInitializing()
        conn.setActive()
        return conn
    }

    override fun onCreateIncomingConnection(
        connectionManagerPhoneAccount: PhoneAccountHandle,
        request: ConnectionRequest
    ): Connection {
        val conn = object : Connection() {
            override fun onAnswer() {
                setActive()
            }
            override fun onDisconnect() {
                setDisconnected(DisconnectCause(DisconnectCause.REMOTE))
                destroy()
            }
        }
        conn.setAddress(request.address, TelecomManager.PRESENTATION_ALLOWED)
        conn.setRinging()
        return conn
    }
}
