package com.example.cameraprovider

import android.content.Context
import com.bumptech.glide.GlideBuilder
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.load.engine.cache.InternalCacheDiskCacheFactory
import com.bumptech.glide.load.engine.cache.LruResourceCache
import com.bumptech.glide.module.AppGlideModule
import java.util.concurrent.Executors


@GlideModule
class MyAppGlideModule : AppGlideModule() {
    override fun applyOptions(context: Context, builder: GlideBuilder) {

        val executor = Executors.newFixedThreadPool(10)
        // Thêm các tùy chọn cấu hình Glide toàn cầu tại đây
        builder.setDiskCache(InternalCacheDiskCacheFactory(context, 50 * 1024 * 1024)) // 50 MB disk cache
        builder.setMemoryCache(LruResourceCache(10 * 1024 * 1024)) // 10 MB memory cache
    }
}
