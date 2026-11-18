package com.amrit.mycard.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.amrit.mycard.data.ImageStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

val LocalImageStore = staticCompositionLocalOf<ImageStore?> { null }

/** Loads (and caches) a decrypted photo off the main thread. */
@Composable
fun rememberVaultImage(id: String, rev: Int, maxDim: Int): State<ImageBitmap?> {
    val store = LocalImageStore.current
    return produceState<ImageBitmap?>(initialValue = null, id, rev, maxDim) {
        value = if (store == null || id.isEmpty()) {
            null
        } else {
            withContext(Dispatchers.IO) { store.load(id, maxDim)?.asImageBitmap() }
        }
    }
}
