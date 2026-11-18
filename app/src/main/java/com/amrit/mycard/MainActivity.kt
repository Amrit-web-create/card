package com.amrit.mycard

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.amrit.mycard.data.VaultViewModel
import com.amrit.mycard.ui.AccentColors
import com.amrit.mycard.ui.AddCardScreen
import com.amrit.mycard.ui.CardsScreen
import com.amrit.mycard.ui.LockScreen
import com.amrit.mycard.ui.MyCardTheme
import com.amrit.mycard.ui.PasswordEditScreen
import com.amrit.mycard.ui.PasswordsScreen
import com.amrit.mycard.ui.PillNav
import com.amrit.mycard.ui.SettingsScreen
import com.amrit.mycard.ui.resolveDark

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

            MyCardTheme(dark = dark, black = settings.themeMode == 3, accentChoice = accent) {
                if (vm.unlocked) {
                    MainApp(vm)
                } else {
                    LockScreen { pin -> vm.tryUnlock(pin) }
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

    BackHandler(enabled = route != "home") { route = "home" }

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
                    rotate360 = vm.vault.settings.rotate360,
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
                                rotate360 = vm.vault.settings.rotate360,
                                onAdd = {
                                    editingCardId = null
                                    route = "card"
                                },
                                onEdit = {
                                    editingCardId = it.id
                                    route = "card"
                                },
                                onDelete = { vm.deleteCard(it) },
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
                            else -> SettingsScreen(
                                settings = vm.vault.settings,
                                cards = vm.vault.cards,
                                onChange = { transform -> vm.updateSettings(transform) },
                                onSetPin = { vm.setPin(it) },
                                onEditCard = {
                                    editingCardId = it.id
                                    route = "card"
                                },
                                onArchiveCard = { id, value -> vm.setCardArchived(id, value) }
                            )
                        }
                    }
                    PillNav(
                        selected = tab,
                        onSelect = { tab = it },
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
                }
            }
        }
    }
}
