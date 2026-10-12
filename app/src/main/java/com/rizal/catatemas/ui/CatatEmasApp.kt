package com.rizal.catatemas.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rizal.catatemas.AppContainer
import com.rizal.catatemas.ui.components.InfoSheet

private const val ROUTE_HOME = "home"
private const val ROUTE_NEW = "new"
private const val ROUTE_ZAKAT = "zakat"
private const val ROUTE_SECURITY = "security"
private const val PREFIX_EDIT = "edit:"

@Composable
fun CatatEmasRoot(container: AppContainer) {
    val viewModel: GoldViewModel = viewModel(factory = GoldViewModel.factory(container))
    val state by viewModel.state.collectAsStateWithLifecycle()
    val zakat by viewModel.zakat.collectAsStateWithLifecycle()
    val priceStore by viewModel.priceStore.collectAsStateWithLifecycle()
    val security by viewModel.securityState.collectAsStateWithLifecycle()
    val locked by viewModel.locked.collectAsStateWithLifecycle()
    val update by viewModel.update.collectAsStateWithLifecycle()
    val updateMessage by viewModel.updateMessage.collectAsStateWithLifecycle()

    var route by rememberSaveable { mutableStateOf(ROUTE_HOME) }
    var showInfo by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.onStart() }

    Box(Modifier.fillMaxSize()) {
        // konten tetap dikomposisi (tapi tidak digambar) saat terkunci,
        // supaya isian form yang sedang dibuka tidak hilang.
        Box(Modifier.alpha(if (locked) 0f else 1f)) {
            AnimatedContent(
                targetState = route,
                label = "route",
                transitionSpec = {
                    if (targetState == ROUTE_HOME) {
                        (slideInHorizontally { -it / 6 } + fadeIn()) togetherWith
                            (slideOutHorizontally { it / 6 } + fadeOut())
                    } else {
                        (slideInHorizontally { it } + fadeIn()) togetherWith
                            (slideOutHorizontally { -it / 6 } + fadeOut())
                    }
                },
            ) { target ->
                when {
                    target == ROUTE_HOME -> HomeScreen(
                        state = state,
                        zakat = zakat,
                        onAdd = { route = ROUTE_NEW },
                        onOpenItem = { id -> route = PREFIX_EDIT + id },
                        onOpenZakat = { route = ROUTE_ZAKAT },
                        onOpenSecurity = { route = ROUTE_SECURITY },
                        onOpenInfo = { showInfo = true },
                        onRefresh = { viewModel.refresh(force = true) },
                    )

                    target == ROUTE_ZAKAT -> ZakatScreen(
                        state = zakat,
                        priceDate = state.priceDate,
                        onToggleJewelry = viewModel::setIncludeJewelryInZakat,
                        onClose = { route = ROUTE_HOME },
                    )

                    target == ROUTE_SECURITY -> SecurityScreen(
                        state = security,
                        pinLength = viewModel.pinLength,
                        onClose = { route = ROUTE_HOME },
                        onVerifyPin = viewModel::verifyPin,
                        onSetPin = viewModel::setPin,
                        onClearPin = viewModel::clearPin,
                        onToggleBiometric = viewModel::setBiometricEnabled,
                        onToggleSecureScreen = viewModel::setSecureScreen,
                        onLockNow = {
                            route = ROUTE_HOME
                            viewModel.lockNow()
                        },
                    )

                    target == ROUTE_NEW -> EditItemScreen(
                        itemId = null,
                        viewModel = viewModel,
                        priceStore = priceStore,
                        onClose = { route = ROUTE_HOME },
                    )

                    else -> EditItemScreen(
                        itemId = target.removePrefix(PREFIX_EDIT),
                        viewModel = viewModel,
                        priceStore = priceStore,
                        onClose = { route = ROUTE_HOME },
                    )
                }
            }
        }

        if (locked) {
            LockScreen(
                pinLength = viewModel.pinLength,
                biometricUsable = security.biometricUsable,
                verifyPin = viewModel::verifyPin,
                onUnlock = viewModel::unlock,
            )
        }
    }

    if (showInfo) {
        InfoSheet(
            state = state,
            appVersion = viewModel.appVersion,
            updateMessage = updateMessage,
            onCheckUpdate = { viewModel.checkForUpdate(force = true) },
            onDismissUpdateMessage = viewModel::clearUpdateMessage,
            onDismiss = { showInfo = false },
        )
    }

    UpdateDialog(
        state = update,
        onDownload = viewModel::downloadUpdate,
        onInstall = viewModel::installUpdate,
        onDismiss = viewModel::dismissUpdate,
    )
}
