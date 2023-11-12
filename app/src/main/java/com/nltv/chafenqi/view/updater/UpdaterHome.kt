package com.nltv.chafenqi.view.updater

import android.app.Activity
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.alorma.compose.settings.ui.SettingsMenuLink
import com.alorma.compose.settings.ui.SettingsSwitch
import com.michaelflisar.composepreferences.core.PreferenceDivider
import com.michaelflisar.composepreferences.core.PreferenceInfo
import com.michaelflisar.composepreferences.core.PreferenceScreen
import com.michaelflisar.composepreferences.core.PreferenceSectionHeader
import com.michaelflisar.composepreferences.core.classes.PreferenceSettingsDefaults
import com.michaelflisar.composepreferences.core.classes.asPreferenceData
import com.michaelflisar.composepreferences.core.hierarchy.PreferenceRootScope
import com.michaelflisar.composepreferences.screen.bool.PreferenceBool
import com.michaelflisar.composepreferences.screen.button.PreferenceButton
import dev.burnoo.compose.rememberpreference.rememberBooleanPreference
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdaterHomePage(navController: NavController) {
    val model: UpdaterViewModel = viewModel()
    val scrollState = rememberScrollState()

    LaunchedEffect(Unit) {
        while (true) {
            // Log.i("Updater", "Fetching stats...")
            model.updateServerStat()
            model.updateUploadStat()
            delay(5000)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "传分") },
                scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(),
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary
                ),
                actions = {
                    IconButton(onClick = {  }) {
                        Icon(imageVector = Icons.Default.Help, contentDescription = "传分帮助")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box {
            PreferenceScreen (
                settings = PreferenceSettingsDefaults.settings(),
                scrollable = true,
                modifier = Modifier.padding(paddingValues)
            ) {
                UpdaterProxyGroup()
                PreferenceDivider()
                UpdaterClipboardGroup()
                PreferenceDivider()
                UpdaterSettingsGroup()
                PreferenceDivider()
            }
            if (model.shouldShowQRCode) {
                UpdaterQRCodePage()
            }
        }
    }
}

@Composable
fun PreferenceRootScope.UpdaterProxyGroup() {
    val model: UpdaterViewModel = viewModel()
    val uiState by model.uiState.collectAsState()

    PreferenceSectionHeader(title = { Text(text = "代理") })
    ProxyToggle()
    PreferenceInfo(
        title = { Text(text = "传分状态") },
        subtitle = { Text(text = "舞萌DX: ${uiState.maiUploadStat}\n" + "中二节奏: ${uiState.chuUploadStat}") },
        icon = { Icon(imageVector = Icons.Default.Info, contentDescription = "传分状态") }
    )
    PreferenceInfo(
        title = { Text(text = "服务器状态") },
        subtitle = { Text(text = "舞萌DX: ${uiState.maiServerStat}\n" + "中二节奏: ${uiState.chuServerStat}") },
        icon = { Icon(imageVector = Icons.Default.AccessTime, contentDescription = "服务器状态") }
    )
    UpdaterWechatActions()
}

@Composable
fun PreferenceRootScope.ProxyToggle() {
    var isVpnOn by remember {
        mutableStateOf(false)
    }

    val model: UpdaterViewModel = viewModel()
    val context = LocalContext.current

    val vpnLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = {
            if (it.resultCode == Activity.RESULT_OK) {
                model.startVPN(context)
                isVpnOn = true
            }
        })
    PreferenceBool(
        value = isVpnOn,
        onValueChange = { value -> 
            isVpnOn = value
            if (isVpnOn) {
                val intent = model.prepareVPN(context)
                intent?.also { vpnLauncher.launch(it) } ?: run {
                    model.startVPN(context)
                }
            } else {
                model.stopVPN(context)
            }
        },
        title = { Text(text = "开关") }
    )
}

@Composable
fun PreferenceRootScope.UpdaterClipboardGroup() {
    val model: UpdaterViewModel = viewModel()
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    val shouldForward by rememberBooleanPreference(
        keyName = "shouldForward",
        initialValue = false,
        defaultValue = false
    )

    fun makeToast() {
        Toast.makeText(context, "已复制到剪贴板", Toast.LENGTH_SHORT).show()
    }

    PreferenceSectionHeader(title = { Text(text = "链接和二维码") })

    PreferenceButton(
        onClick = {
            clipboardManager.setText(AnnotatedString(model.buildUri(1, shouldForward)))
            makeToast()
        },
        title = { Text(text = "复制舞萌DX链接") },
        icon = { Icon(imageVector = Icons.Default.Link, contentDescription = "复制舞萌DX链接") }
    )
    PreferenceButton(
        onClick = {
            clipboardManager.setText(AnnotatedString(model.buildUri(0, shouldForward)))
            makeToast()
        },
        title = { Text(text = "复制中二节奏链接") },
        icon = { Icon(imageVector = Icons.Default.Link, contentDescription = "复制中二节奏链接") }
    )
    PreferenceButton(
        onClick = { model.shouldShowQRCode = true },
        title = { Text(text = "生成二维码") },
        icon = { Icon(imageVector = Icons.Default.QrCode, contentDescription = "生成二维码") }
    )
}

@Composable
fun PreferenceRootScope.UpdaterWechatActions() {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val model: UpdaterViewModel = viewModel()

    PreferenceButton(
        onClick = { model.openWeChat(context, uriHandler) },
        title = { Text(text = "跳转到微信") },
        icon = { Icon(imageVector = Icons.Default.OpenInNew, contentDescription = "跳转到微信") }
    )
}

@Composable
fun PreferenceRootScope.UpdaterSettingsGroup() {
    var shouldForward by rememberBooleanPreference(
        keyName = "shouldForward",
        initialValue = false,
        defaultValue = false
    )

    PreferenceSectionHeader(title = { Text(text = "设置") })
    PreferenceBool(
        value = shouldForward,
        onValueChange = { selected -> shouldForward = selected },
        title = { Text(text = "同步到水鱼网") },
        subtitle = { Text(text = "需要在设置中绑定账号") }
    )
}
