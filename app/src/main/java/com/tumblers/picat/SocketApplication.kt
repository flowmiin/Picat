package com.tumblers.picat

import io.socket.client.IO
import io.socket.client.Socket
import java.net.URISyntaxException

class SocketApplication {
    companion object {
        private lateinit var socket : Socket
        fun get(): Socket {
            try {
                //의균 서버
                //socket = IO.socket("http://13.124.148.41:3000/")
                //지우 서버
                socket = IO.socket("http://moonhwa-hs.shop:3000/")
            } catch (e: URISyntaxException) {
                e.printStackTrace()
            }
            return socket
        }
    }
}