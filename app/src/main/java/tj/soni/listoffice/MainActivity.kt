package tj.soni.listoffice

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import tj.soni.listoffice.core.theme.ListOfficeTheme
import tj.soni.listoffice.feature.dashboard.DashboardScreen
import tj.soni.listoffice.feature.document.DocumentScreen
import tj.soni.listoffice.feature.filemanager.FilesScreen
import tj.soni.listoffice.feature.filemanager.PdfViewerScreen
import tj.soni.listoffice.feature.handwriting.HandwritingScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ListOfficeTheme {
                AppRoot()
            }
        }
    }
}

@Composable
fun AppRoot(vm: MainViewModel = viewModel()) {
    val snack = remember { SnackbarHostState() }
    val msg = vm.message
    LaunchedEffect(msg) {
        if (msg != null) {
            snack.showSnackbar(msg)
            vm.message = null
        }
    }

    val viewing = vm.viewing
    if (viewing != null) {
        PdfViewerScreen(viewing) { vm.viewing = null }
        return
    }

    val items = listOf(
        "Асосӣ" to Icons.Default.Home,
        "Ҳуҷҷат" to Icons.Default.Description,
        "Дастнавис" to Icons.Default.Create,
        "Файлҳо" to Icons.Default.Folder
    )

    Scaffold(
        snackbarHost = { SnackbarHost(snack) },
        bottomBar = {
            NavigationBar {
                items.forEachIndexed { i, (label, icon) ->
                    NavigationBarItem(
                        selected = vm.tab == i,
                        onClick = { vm.tab = i },
                        icon = { Icon(icon, contentDescription = null) },
                        label = { Text(label) }
                    )
                }
            }
        }
    ) { pad ->
        Box(Modifier.padding(pad).fillMaxSize()) {
            when (vm.tab) {
                0 -> DashboardScreen(vm)
                1 -> DocumentScreen(vm)
                2 -> HandwritingScreen(vm)
                else -> FilesScreen(vm)
            }
            if (vm.busy) {
                Box(
                    Modifier.fillMaxSize().background(Color(0x99000000)),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }
            }
        }
    }
}
