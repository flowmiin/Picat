package com.tumblers.picat

import android.app.Application
import com.kakao.sdk.common.KakaoSdk

class GlobalApplication: Application() {
    override fun onCreate() {
        super.onCreate()

        KakaoSdk.init(this, "3316a30016aae0ca0c99e236b9e7db5e")
    }
}