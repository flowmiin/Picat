package com.tumblers.picat

import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.ActionBar
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.kakao.sdk.auth.model.OAuthToken
import com.kakao.sdk.common.model.AuthErrorCause
import com.kakao.sdk.user.UserApiClient
import com.tumblers.picat.databinding.ActivityLoginBinding
import com.kakao.sdk.common.model.ClientError
import com.kakao.sdk.common.model.ClientErrorCause
import com.kakao.sdk.talk.TalkApiClient
import com.tumblers.picat.dataclass.RequestInterface
import com.tumblers.picat.dataclass.SimpleResponseData
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class LoginActivity : AppCompatActivity() {

    lateinit var pref : SharedPreferences


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val actionbar: ActionBar? = supportActionBar
        actionbar?.hide()

        // val keyHash = Utility.getKeyHash(this)
        // println("해시 : ${keyHash}")
        pref = getPreferences(Context.MODE_PRIVATE)


        val callback: (OAuthToken?, Throwable?) -> Unit = { token, error ->
            if (error != null) {
                when {
                    error.toString() == AuthErrorCause.AccessDenied.toString() -> {
                        Toast.makeText(this, "접근이 거부 됨(동의 취소)", Toast.LENGTH_SHORT).show()
                    }
                    error.toString() == AuthErrorCause.InvalidClient.toString() -> {
                        Toast.makeText(this, "유효하지 않은 앱", Toast.LENGTH_SHORT).show()
                    }
                    error.toString() == AuthErrorCause.InvalidGrant.toString() -> {
                        Toast.makeText(this, "인증 수단이 유효하지 않아 인증할 수 없는 상태", Toast.LENGTH_SHORT).show()
                    }
                    error.toString() == AuthErrorCause.InvalidRequest.toString() -> {
                        Toast.makeText(this, "요청 파라미터 오류", Toast.LENGTH_SHORT).show()
                    }
                    error.toString() == AuthErrorCause.InvalidScope.toString() -> {
                        Toast.makeText(this, "유효하지 않은 scope ID", Toast.LENGTH_SHORT).show()
                    }
                    error.toString() == AuthErrorCause.Misconfigured.toString() -> {
                        Toast.makeText(this, "설정이 올바르지 않음(android key hash)", Toast.LENGTH_SHORT).show()
                    }
                    error.toString() == AuthErrorCause.ServerError.toString() -> {
                        Toast.makeText(this, "서버 내부 에러", Toast.LENGTH_SHORT).show()
                    }
                    error.toString() == AuthErrorCause.Unauthorized.toString() -> {
                        Toast.makeText(this, "앱이 요청 권한이 없음", Toast.LENGTH_SHORT).show()
                    }
    //                    else -> { // Unknown
    //                        Toast.makeText(this, "기타 에러", Toast.LENGTH_SHORT).show()
    //                    }
                }
            }
            else if (token != null) {
                Toast.makeText(this, "로그인에 성공하였습니다.", Toast.LENGTH_SHORT).show()

                var requestData = JsonObject()
                // 사용자 정보 요청 (기본)
                UserApiClient.instance.me { user, error ->
                    if (error != null) {
                        Log.e(TAG, "사용자 정보 요청 실패", error)
                    }
                    else if (user != null) {
                        Log.i(TAG, "사용자 정보 요청 성공" +
                                "\n회원번호: ${user.id}" +
                                "\n이메일: ${user.kakaoAccount?.email}" +
                                "\n닉네임: ${user.kakaoAccount?.profile?.nickname}" +
                                "\n프로필사진: ${user.kakaoAccount?.profile?.thumbnailImageUrl}")

                        requestData.addProperty("id", user.id)
                        requestData.addProperty("nickname", user.kakaoAccount?.profile?.nickname)
                        requestData.addProperty("picture", user.kakaoAccount?.profile?.profileImageUrl)
                        requestData.addProperty("email", user.kakaoAccount?.email)
                        Log.i(TAG, "결과1 $requestData")


                        // 카카오톡 친구 목록 가져오기 (기본)
                        TalkApiClient.instance.friends { friends, error ->
                            if (error != null) {
                                Log.e(TAG, "카카오톡 친구 목록 가져오기 실패", error)
                            }
                            else if (friends != null) {
                                Log.i(TAG, "카카오톡 친구 목록 가져오기 성공 \n${friends.elements?.joinToString("\n")}")
                                Log.i(TAG, "카카오톡 친구 목록 가져오기 성공2 \n${friends.elements}")
                                requestData.addProperty("total_count", friends.elements?.size)

                                val friendList = JsonArray()
                                if (friends.totalCount > 0){
                                    for (friend in friends.elements!!) {
                                        val friendObj = JsonObject()
                                        friendObj.addProperty("id", friend.id)
                                        friendObj.addProperty("uuid", friend.uuid)
                                        friendObj.addProperty("profile_nickname", friend.profileNickname)
                                        friendObj.addProperty("profile_thumbnail_image", friend.profileThumbnailImage)
                                        friendObj.addProperty("favorite", friend.favorite)
                                        friendObj.addProperty("allowedMsg", friend.allowedMsg)
                                        friendList.add(friendObj)
                                    }
                                }
                                requestData.add("elements", friendList)
                                Log.i(TAG, "결과2 $requestData")

                                apiRequest(requestData)

                            }

                        }
                    }

                }



                val intent = Intent(this, SharePictureActivity::class.java)
                startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
                finish()
            }
        }

        // 카카오톡으로 로그인
        binding.kakaoLoginButton.setOnClickListener{

            if (UserApiClient.instance.isKakaoTalkLoginAvailable(this)) {
                UserApiClient.instance.loginWithKakaoTalk(this) { token, error ->
                    if (error != null) {
                        Log.e(TAG, "카카오톡으로 로그인 실패", error)

                        // 사용자가 카카오톡 설치 후 디바이스 권한 요청 화면에서 로그인을 취소한 경우,
                        // 의도적인 로그인 취소로 보고 카카오계정으로 로그인 시도 없이 로그인 취소로 처리 (예: 뒤로 가기)
                        if (error is ClientError && error.reason == ClientErrorCause.Cancelled) {
                            return@loginWithKakaoTalk
                        }

                        // 카카오톡에 연결된 카카오계정이 없는 경우, 카카오계정으로 로그인 시도
                        UserApiClient.instance.loginWithKakaoAccount(this, callback = callback)

                    } else if (token != null) {
                        Log.i(TAG, "카카오톡으로 로그인 성공 ${token.accessToken}")
                        var requestData = JsonObject()
                        // 사용자 정보 요청 (기본)
                        UserApiClient.instance.me { user, error ->
                            if (error != null) {
                                Log.e(TAG, "사용자 정보 요청 실패", error)
                            }
                            else if (user != null) {
//                                Log.i(TAG, "사용자 정보 요청 성공" +
//                                        "\n회원번호: ${user.id}" +
//                                        "\n이메일: ${user.kakaoAccount?.email}" +
//                                        "\n닉네임: ${user.kakaoAccount?.profile?.nickname}" +
//                                        "\n프로필사진: ${user.kakaoAccount?.profile?.profileImageUrl}")

                                requestData.addProperty("id", user.id)
                                requestData.addProperty("nickname", user.kakaoAccount?.profile?.nickname)
                                requestData.addProperty("picture", user.kakaoAccount?.profile?.profileImageUrl)
                                requestData.addProperty("email", user.kakaoAccount?.email)
                                Log.i(TAG, "결과1 $requestData")


                                // 카카오톡 친구 목록 가져오기 (기본)
                                TalkApiClient.instance.friends { friends, error ->
                                    if (error != null) {
                                        Log.e(TAG, "카카오톡 친구 목록 가져오기 실패", error)
                                    }
                                    else if (friends != null) {
//                                        Log.i(TAG, "카카오톡 친구 목록 가져오기 성공 \n${friends.elements?.joinToString("\n")}")
//                                        Log.i(TAG, "카카오톡 친구 목록 가져오기 성공2 \n${friends.elements}")
                                        requestData.addProperty("total_count", friends.elements?.size)

                                        val friendList = JsonArray()
                                        if (friends.totalCount > 0){
                                            for (friend in friends.elements!!) {
                                                val friendObj = JsonObject()
                                                friendObj.addProperty("id", friend.id)
                                                friendObj.addProperty("uuid", friend.uuid)
                                                friendObj.addProperty("profile_nickname", friend.profileNickname)
                                                friendObj.addProperty("profile_thumbnail_image", friend.profileThumbnailImage)
                                                friendObj.addProperty("favorite", friend.favorite)
                                                friendObj.addProperty("allowedMsg", friend.allowedMsg)
                                                friendList.add(friendObj)
                                            }
                                        }
                                        requestData.add("elements", friendList)
                                        Log.i(TAG, "결과2 $requestData")

                                        apiRequest(requestData)

                                    }

                                }
                            }

                        }

                        val intent = Intent(this, SharePictureActivity::class.java)
                        startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
                        finish()
                    }
                }
            } else {
                UserApiClient.instance.loginWithKakaoAccount(this, callback = callback)
            }

        }
    }



    private fun apiRequest(requestData :JsonObject) {
        // retrofit 객체 생성
        val retrofit: Retrofit = Retrofit.Builder()
            .baseUrl(getString(R.string.picat_server))
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        // APIInterface 객체 생성
        var server: RequestInterface = retrofit.create(RequestInterface::class.java)
        server.postUser(requestData).enqueue(object : Callback<SimpleResponseData> {

            override fun onResponse(call: Call<SimpleResponseData>, response: Response<SimpleResponseData>) {
                if (response.isSuccessful){
                    Toast.makeText(applicationContext, "${response.body()?.message}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<SimpleResponseData>, t: Throwable) {
                Toast.makeText(applicationContext, "회원 가입 실패", Toast.LENGTH_SHORT).show()

            }
        })
    }
}