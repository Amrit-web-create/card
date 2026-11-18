package com.amrit.mycard

import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.amrit.mycard.data.PhotoItem
import com.amrit.mycard.data.VaultViewModel
import com.amrit.mycard.ui.AccentColors
import com.amrit.mycard.ui.AddCardScreen
import com.amrit.mycard.ui.CardsScreen
import com.amrit.mycard.ui.CropRequest
import com.amrit.mycard.ui.CropResult
import com.amrit.mycard.ui.CropScreen
import com.amrit.mycard.ui.LocalImageStore
import com.amrit.mycard.ui.LockScreen
import com.amrit.mycard.ui.MyCardTheme
import com.amrit.mycard.ui.PasswordEditScreen
import com.amrit.mycard.ui.PasswordsScreen
import com.amrit.mycard.ui.PhotosScreen
import com.amrit.mycard.ui.PillNav
import com.amrit.mycard.ui.SettingsScreen
import com.amrit.mycard.ui.resolveDark
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

class MainActivity : ComponentActivity() {

    private val vm: VaultViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settings = vm.vault.settings
            val dark = resolveDark(settings.themeMode)
            val accent = AccentColors[settings.accent.coerceIn(0, AccentColors.lastIndex)]

            LaunchedEffect(settings.secureScreen) {
                if (settings.secureScreen) {
                    window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                }
            }

            CompositionLocalProvider(LocalImageStore provides vm.images) {
                MyCardTheme(
                    dark = dark,
                    black = settings.themeMode == 3,
                    cream = settings.themeMode == 4,
                    accentChoice = accent
                ) {
                    if (vm.unlocked) {
                        MainApp(vm)
                    } else {
                        LockScreen { pin -> vm.tryUnlock(pin) }
                    }
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        vm.lock()
    }
}

@Composable
private fun MainApp(vm: VaultViewModel) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var route by rememberSaveable { mutableStateOf("home") }
    var editingCardId by rememberSaveable { mutableStateOf<String?>(null) }
    var editingPasswordId by rememberSaveable { mutableStateOf<String?>(null) }
    var hideNav by remember { mutableStateOf(false) }
    var cropRequest by remember { mutableStateOf<CropRequest?>(null) }
    var pendingPhotoCallback by remember { mutableStateOf<((String) -> Unit)?>(null) }
    val scope = rememberCoroutineScope()
    val images = vm.images

    BackHandler(enabled = route != "home") { route = "home" }

    suspend fun askCrop(
        bmp: Bitmap,
        title: String,
        primary: String,
        secondary: String?,
        aspect: Int
    ): CropResult? {
        val d = CompletableDeferred<CropResult?>()
        cropRequest = CropRequest(bmp, title, primary, secondary, aspect, d)
        val r = d.await()
        cropRequest = null
        return r
    }

    val multiPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            scope.launch {
                for (u in uris) {
                    val bmp = withContext(Dispatchers.IO) { images.decodeUri(u) } ?: continue
                    val res = askCrop(bmp, "Add photo", "Crop and save", null, 0) ?: break
                    val id = UUID.randomUUID().toString()
                    withContext(Dispatchers.IO) { images.save(id, res.bytes) }
                    vm.addPhoto(PhotoItem(id = id, addedAt = System.currentTimeMillis()))
                }
            }
        }
    }

    val singlePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        val cb = pendingPhotoCallback
        pendingPhotoCallback = null
        if (uri != null && cb != null) {
            scope.launch {
                val bmp = withContext(Dispatchers.IO) { images.decodeUri(uri) } ?: return@launch
                val res = askCrop(bmp, "Crop photo", "Crop and save", null, 2) ?: return@launch
                val id = UUID.randomUUID().toString()
                withContext(Dispatchers.IO) { images.save(id, res.bytes) }
                cb(id)
            }
        }
    }

    val cropExisting: (PhotoItem) -> Unit = { p ->
        scope.launch {
            val bmp = withContext(Dispatchers.IO) { images.load(p.id, 4096) } ?: return@launch
            val res = askCrop(bmp, "Crop photo", "Replace photo", "Save as copy", 0) ?: return@launch
            if (res.secondary) {
                val id = UUID.randomUUID().toString()
                withContext(Dispatchers.IO) { images.save(id, res.bytes) }
                vm.addPhoto(PhotoItem(id = id, addedAt = System.currentTimeMillis()))
            } else {
                withContext(Dispatchers.IO) { images.save(p.id, res.bytes) }
                vm.bumpPhoto(p.id)
            }
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        AnimatedContent(
            targetState = route,
            transitionSpec = {
                if (targetState != "home") {
                    (slideInVertically(tween(380)) { it } + fadeIn(tween(300))) togetherWith fadeOut(tween(200))
                } else {
                    fadeIn(tween(260)) togetherWith (slideOutVertically(tween(320)) { it } + fadeOut(tween(300)))
                }
            },
            label = "route"
        ) { r ->
            when (r) {
                "card" -> AddCardScreen(
                    initial = vm.vault.cards.firstOrNull { it.id == editingCardId },
                    rotateMode = vm.vault.settings.rotateMode,
                    onPickPhoto = { cb ->
                        pendingPhotoCallback = cb
                        singlePicker.launch("image/*")
                    },
                    onSave = {
                        vm.saveCard(it)
                        route = "home"
                    },
                    onBack = { route = "home" }
                )
                "password" -> PasswordEditScreen(
                    initial = vm.vault.passwords.firstOrNull { it.id == editingPasswordId },
                    onSave = {
                        vm.savePassword(it)
                        route = "home"
                    },
                    onBack = { route = "home" }
                )
                else -> Box(Modifier.fillMaxSize()) {
                    AnimatedContent(
                        targetState = tab,
                        transitionSpec = {
                            (fadeIn(tween(260)) + scaleIn(initialScale = 0.96f, animationSpec = tween(260))) togetherWith fadeOut(tween(140))
                        },
                        label = "tab"
                    ) { t ->
                        when (t) {
                            0 -> CardsScreen(
                                cards = vm.vault.cards,
                                rotateMode = vm.vault.settings.rotateMode,
                                onAdd = {
                                    editingCardId = null
                                    route = "card"
                                },
                                onArchive = { id, value -> vm.setCardArchived(id, value) }
                            )
                            1 -> PasswordsScreen(
                                entries = vm.vault.passwords,
                                onAdd = {
                                    editingPasswordId = null
                                    route = "password"
                                },
                                onEdit = {
                                    editingPasswordId = it.id
                                    route = "password"
                                },
                                onDelete = { vm.deletePassword(it) },
                                onArchive = { id, value -> vm.setPasswordArchived(id, value) }
                            )
                            2 -> PhotosScreen(
                                photos = vm.vault.photos,
                                onAddPhotos = { multiPicker.launch("image/*") },
                                onCrop = cropExisting,
                                onArchive = { id, value -> vm.setPhotoArchived(id, value) },
                                onDelete = { vm.deletePhoto(it) },
                                onFullscreen = { hideNav = it }
                            )
                            else -> SettingsScreen(
                                settings = vm.vault.settings,
                                cards = vm.vault.cards,
                                onChange = { transform -> vm.updateSettings(transform) },
                                onSetPin = { vm.setPin(it) },
                                verifyPin = { vm.verifyPin(it) },
                                onEditCard = {
                                    editingCardId = it.id
                                    route = "card"
                                },
                                onDeleteCard = { vm.deleteCard(it) }
                            )
                        }
                    }
                    if (!hideNav) {
                        PillNav(
                            selected = tab,
                            onSelect = { tab = it },
                            modifier = Modifier.align(Alignment.BottomCenter)
                        )
                    }
                }
            }
        }

        val req = cropRequest
        if (req != null) {
            CropScreen(req)
        }
    }
}
