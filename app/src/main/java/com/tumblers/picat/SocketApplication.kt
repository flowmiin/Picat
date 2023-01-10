package com.tumblers.picat

import io.socket.client.IO
import io.socket.client.Socket
import java.net.URISyntaxException

class SocketApplication {
    companion object {
        private lateinit var socket : Socket
        fun get(): Socket {
            try {
                //클라이언트 개발용 서버
                socket = IO.socket("http://54.180.36.192:5000/")
                //본 서버
//                socket = IO.socket("http://43.200.93.112:5000/")

            } catch (e: URISyntaxException) {
                e.printStackTrace()
            }
            return socket
        }
    }
}