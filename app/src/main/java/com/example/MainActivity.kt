package com.example

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mesh.MeshService
import com.example.ui.MainTab
import com.example.ui.MainViewModel
import com.example.ui.components.*
import com.example.ui.screens.*
import com.example.ui.theme.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MeshlineTheme {
                val viewModel: MainViewModel = viewModel()
                val context = LocalContext.current
                val bluetoothEnableRequest by viewModel.requestBluetoothEnable.collectAsStateWithLifecycle()

                val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    arrayOf(
                        Manifest.permission.BLUETOOTH_SCAN,
                        Manifest.permission.BLUETOOTH_CONNECT,
                        Manifest.permission.BLUETOOTH_ADVERTISE,
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.POST_NOTIFICATIONS
                    )
                } else {
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
                }

                val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { _ -> }
                val btLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { _: ActivityResult ->
                    viewModel.bluetoothEnableHandled()
                }

                LaunchedEffect(Unit) { launcher.launch(permissions) }
                
                LaunchedEffect(bluetoothEnableRequest) {
                    if (bluetoothEnableRequest) {
                        val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                        btLauncher.launch(enableBtIntent)
                    }
                }

                DisposableEffect(Unit) {
                    val connection = object : ServiceConnection {
                        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                            val binder = service as? MeshService.MeshBinder
                            binder?.getService()?.let { s: MeshService -> 
                                viewModel.setMeshService(s) 
                            }
                        }
                        override fun onServiceDisconnected(name: ComponentName?) {}
                    }
                    val intent = Intent(context, MeshService::class.java)
                    context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
                    onDispose { context.unbindService(connection) }
                }

                MeshlineApp(viewModel)
            }
        }
    }
}

@Composable
fun MeshlineApp(viewModel: MainViewModel) {
    val identity by viewModel.identity.collectAsStateWithLifecycle()
    val contacts by viewModel.contacts.collectAsStateWithLifecycle()
    val pendingJoinRequests by viewModel.pendingJoinRequests.collectAsStateWithLifecycle()
    val discoveredNodes by viewModel.discoveredNearbyNodes.collectAsStateWithLifecycle()
    val activeMessages by viewModel.activeMessages.collectAsStateWithLifecycle()
    val selectedId by viewModel.selectedConversationId.collectAsStateWithLifecycle()
    val activeTab by viewModel.activeTab.collectAsStateWithLifecycle()
    val showSecurity by viewModel.showSecurityPanel.collectAsStateWithLifecycle()
    val showPanic by viewModel.showPanicDialog.collectAsStateWithLifecycle()
    val showJoinReqs by viewModel.showJoinRequestsDialog.collectAsStateWithLifecycle()
    val showPair by viewModel.showPairNewNodeDialog.collectAsStateWithLifecycle()
    val peers by viewModel.meshPeers.collectAsStateWithLifecycle()
    val routes by viewModel.meshRoutes.collectAsStateWithLifecycle()
    val logs by viewModel.packetLogs.collectAsStateWithLifecycle()
    val isScanning by viewModel.isScanning.collectAsStateWithLifecycle()
    val isRelay by viewModel.isRelayMode.collectAsStateWithLifecycle()

    var showWT by remember { mutableStateOf(false) }
    var showCreateGrp by remember { mutableStateOf(false) }

    if (identity == null) {
        OnboardingScreen { u, p -> viewModel.createIdentity(u, p) }
        return
    }

    val selectedContact = contacts.find { it.id == selectedId }

    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                MainTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = activeTab == tab,
                        onClick = { viewModel.setActiveTab(tab) },
                        icon = { Icon(when(tab) {
                            MainTab.CHATS -> Icons.Default.ChatBubble
                            MainTab.NETWORK -> Icons.Default.Hub
                            MainTab.CONTACTS -> Icons.Default.People
                            MainTab.SETTINGS -> Icons.Default.Settings
                        }, null) },
                        label = { Text(tab.name, fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(selectedTextColor = MeshMintAccent, indicatorColor = MeshMintAccent)
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            when (activeTab) {
                MainTab.CHATS -> {
                    if (selectedId != null && selectedContact != null) {
                        ChatConversationScreen(
                            contact = selectedContact, messages = activeMessages,
                            onSendMessage = { t, h, r -> viewModel.sendMessage(t, h, r) },
                            onSendVoiceNote = { d -> viewModel.sendVoiceNote(d) },
                            onSendLocation = { viewModel.sendLocationMessage() },
                            onAddReaction = { id, e -> viewModel.addReaction(id, e) },
                            onTogglePinMessage = { id, t -> viewModel.togglePinMessage(id, t) },
                            onSetDisappearingTimer = { s -> viewModel.setDisappearingTimer(s) },
                            onDeleteMessage = { id -> viewModel.deleteMessage(id) },
                            onOpenWalkieTalkie = { showWT = true },
                            onBack = { viewModel.selectConversation(null) },
                            onToggleSecurityPanel = { viewModel.toggleSecurityPanel() }
                        )
                    } else {
                        ChatsListScreen(contacts, selectedId, { viewModel.selectConversation(it) }, { viewModel.setActiveTab(MainTab.NETWORK); viewModel.startBleScan() }, { showCreateGrp = true })
                    }
                }
                MainTab.NETWORK -> NetworkDashboardScreen(peers, routes, logs, pendingJoinRequests, isScanning, isRelay, { viewModel.startBleScan() }, { viewModel.toggleRelayMode() }, { viewModel.toggleJoinRequestsDialog() }, { viewModel.togglePairNewNodeDialog() })
                MainTab.CONTACTS -> ContactsScreen(contacts, pendingJoinRequests.size, { viewModel.toggleContactVerification(it) }, { viewModel.togglePairNewNodeDialog() }, { viewModel.toggleJoinRequestsDialog() }, { viewModel.selectConversation(it); viewModel.setActiveTab(MainTab.CHATS) })
                MainTab.SETTINGS -> SettingsScreen(
                    identity, showPanic, 
                    { viewModel.togglePanicDialog() }, 
                    { viewModel.confirmPanicWipe() },
                    { viewModel.getIdentityExportString() },
                    { viewModel.importIdentity(it) },
                    { u, a -> viewModel.updateProfile(u, a) }
                )
            }

            if (showJoinReqs) JoinRequestsDialog(pendingJoinRequests, { viewModel.toggleJoinRequestsDialog() }, { viewModel.acceptJoinRequest(it) }, { viewModel.rejectJoinRequest(it) })
            if (showPair) PairNewNodeDialog(identity, discoveredNodes, { viewModel.togglePairNewNodeDialog() }, { n, i, k -> viewModel.sendJoinRequest(n, i, k) })
            if (showWT && selectedContact != null) WalkieTalkieDialog(selectedContact, { showWT = false }, { d -> viewModel.sendVoiceNote(d) })
            if (showCreateGrp) CreateGroupDialog({ showCreateGrp = false }, { n, m -> viewModel.createGroupChannel(n, m) })
            if (showSecurity && selectedContact != null) SecurityDetailsSheet(selectedContact, { viewModel.toggleSecurityPanel() }, { viewModel.toggleContactVerification(it) })
        }
    }
}
