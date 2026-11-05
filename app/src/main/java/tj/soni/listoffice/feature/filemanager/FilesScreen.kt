package tj.soni.listoffice.feature.filemanager

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import tj.soni.listoffice.MainViewModel
import tj.soni.listoffice.core.ui.PremiumCard
import tj.soni.listoffice.core.ui.ToolButton
import tj.soni.listoffice.core.ui.shareFile
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun FilesScreen(vm: MainViewModel) {
    val ctx = LocalContext.current
    val files by vm.files.collectAsState()
    var query by remember { mutableStateOf("") }
    var renameTarget by remember { mutableStateOf<File?>(null) }
    var deleteTarget by remember { mutableStateOf<File?>(null) }
    var exportTarget by remember { mutableStateOf<File?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("*/*")) { uri ->
        val f = exportTarget
        if (uri != null && f != null) vm.exportTo(f, uri)
        exportTarget = null
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) vm.importFrom(uri)
    }

    val shown = files.filter { it.name.contains(query, ignoreCase = true) }
    val fmt = remember { SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()) }

    Column(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Ҷустуҷӯ") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            ToolButton(Icons.Default.Add, "Ворид") { importLauncher.launch(arrayOf("*/*")) }
        }
        Text(
            "Чоп: файлро интихоб карда «Чоп»-ро пахш кунед; принтер, нусхаҳо, ранг ва ориентатсияро диалоги системаи Android нишон медиҳад.",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (shown.isEmpty()) {
            PremiumCard(Modifier.fillMaxWidth()) {
                Text(if (files.isEmpty()) "Файл нест. Ҳуҷҷат созед ё файл ворид кунед." else "Чизе ёфт нашуд.")
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(shown, key = { it.absolutePath }) { f ->
                    var menu by remember { mutableStateOf(false) }
                    PremiumCard(Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f).clickable { vm.openFile(ctx, f) }) {
                                Text(f.name, maxLines = 1)
                                Text(
                                    "${f.length() / 1024} KB • ${fmt.format(Date(f.lastModified()))}",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Box {
                                IconButton(onClick = { menu = true }) { Icon(Icons.Default.MoreVert, contentDescription = "Меню") }
                                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                                    DropdownMenuItem(text = { Text("Кушодан") }, onClick = { menu = false; vm.openFile(ctx, f) })
                                    DropdownMenuItem(text = { Text("Чоп") }, onClick = { menu = false; vm.printFile(ctx, f) })
                                    DropdownMenuItem(text = { Text("Мубодила") }, onClick = {
                                        menu = false
                                        if (!shareFile(ctx, f)) vm.message = "Мубодила муяссар нашуд"
                                    })
                                    DropdownMenuItem(text = { Text("Содирот") }, onClick = {
                                        menu = false
                                        exportTarget = f
                                        exportLauncher.launch(f.name)
                                    })
                                    DropdownMenuItem(text = { Text("Тағйири ном") }, onClick = { menu = false; renameTarget = f })
                                    DropdownMenuItem(text = { Text("Нест кардан") }, onClick = { menu = false; deleteTarget = f })
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    renameTarget?.let { f ->
        var name by remember(f) { mutableStateOf(f.nameWithoutExtension) }
        AlertDialog(
            onDismissRequest = { renameTarget = null },
            title = { Text("Тағйири ном") },
            text = { OutlinedTextField(name, { name = it }, singleLine = true) },
            confirmButton = {
                TextButton(enabled = name.isNotBlank(), onClick = { vm.rename(f, name); renameTarget = null }) { Text("Ҳифз") }
            },
            dismissButton = { TextButton(onClick = { renameTarget = null }) { Text("Бекор") } }
        )
    }

    deleteTarget?.let { f ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Нест кардан?") },
            text = { Text(f.name) },
            confirmButton = { TextButton(onClick = { vm.delete(f); deleteTarget = null }) { Text("Нест кун") } },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("Бекор") } }
        )
    }
}
