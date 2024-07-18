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

        Executors.newFixedThreadPool(6)
        builder.setDiskCache(InternalCacheDiskCacheFactory(context, 50 * 1024 * 1024))
        builder.setMemoryCache(LruResourceCache(10 * 1024 * 1024))
    }
}
