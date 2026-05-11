package dev.prism.gallery

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.VideoFrameDecoder
import dagger.hilt.android.HiltAndroidApp
import dev.prism.gallery.worker.TrashPurgeWorker

@HiltAndroidApp
class PrismApplication : Application(), ImageLoaderFactory {

    override fun onCreate() {
        super.onCreate()
        TrashPurgeWorker.schedule(this)
    }

    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this)
            .components {
                add(VideoFrameDecoder.Factory())
            }
            .crossfade(true)
            .build()
}
