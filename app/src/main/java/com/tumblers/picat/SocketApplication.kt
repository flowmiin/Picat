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
                socket = IO.socket("http://43.200.93.112:5000/")
                //지우 서버
//                socket = IO.socket("http://moonhwa-hs.shop:3000/")
                //의균 로컬
//                socket = IO.socket("http://192.249.30.129:5000/")

            } catch (e: URISyntaxException) {
                e.printStackTrace()
            }
            return socket
        }
    }
}