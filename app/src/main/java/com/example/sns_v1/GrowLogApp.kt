package com.example.sns_v1

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.example.sns_v1.auth.TokenManager
import com.example.sns_v1.network.ApiConfig
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient

/**
 * 画像配信が認証必須になったため、Coil が使う OkHttp に
 * Firebase の ID トークンを付けるインターセプタを差し込む。
 * ここを外すと、アバターや投稿画像がすべて 401 になる。
 */
class GrowLogApp : Application(), ImageLoaderFactory {

    override fun newImageLoader(): ImageLoader {
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request()
                // 自分の API から取る画像にだけ付ける。外部URLにトークンを漏らさないため
                if (request.url.host != ApiConfig.HOST) {
                    return@addInterceptor chain.proceed(request)
                }
                // インターセプタは同期的に動くので、ここだけ待つ。
                // Firebase 側でトークンはキャッシュされるため通常は即返る
                val token = runCatching { runBlocking { TokenManager.getIdToken() } }.getOrNull()
                    ?: return@addInterceptor chain.proceed(request)

                chain.proceed(
                    request.newBuilder()
                        .header("Authorization", "Bearer $token")
                        .build()
                )
            }
            .build()

        return ImageLoader.Builder(this)
            .okHttpClient(client)
            .build()
    }
}
