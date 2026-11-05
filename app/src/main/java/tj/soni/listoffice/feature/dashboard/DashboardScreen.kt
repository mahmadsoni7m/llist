package tj.soni.listoffice.feature.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import tj.soni.listoffice.MainViewModel
import tj.soni.listoffice.core.ui.PremiumCard
import tj.soni.listoffice.core.ui.ToolButton

@Composable
fun DashboardScreen(vm: MainViewModel) {
    val ctx = LocalContext.current
    val files by vm.files.collectAsState()
    fun count(ext: String) = files.count { it.extension.equals(ext, true) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("LIST OFFICE ULTRA", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
        Text("Реферат, КМД, PDF, DOCX ва дастнависи кабуд дар як барнома", color = MaterialTheme.colorScheme.onSurfaceVariant)

        PremiumCard(Modifier.fillMaxWidth()) {
            Text("Омор", style = MaterialTheme.typography.titleMedium)
            Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Stat("Ҳамаги", files.size)
                Stat("TXT", count("txt"))
                Stat("PDF", count("pdf"))
                Stat("DOCX", count("docx"))
            }
        }

        PremiumCard(Modifier.fillMaxWidth()) {
            Text("Амалҳои зуд", style = MaterialTheme.typography.titleMedium)
            Column(Modifier.padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ToolButton(Icons.Default.Description, "Ҳуҷҷати нав / шаблон") { vm.newDoc(); vm.tab = 1 }
                ToolButton(Icons.Default.Create, "Дастнависи кабуд") { vm.tab = 2 }
                ToolButton(Icons.Default.Folder, "Файлҳо ва чоп") { vm.tab = 3 }
            }
        }

        PremiumCard(Modifier.fillMaxWidth()) {
            Text("Ҳуҷҷатҳои охирин", style = MaterialTheme.typography.titleMedium)
            if (files.isEmpty()) {
                Text(
                    "Ҳоло файл нест. Ҳуҷҷати нав созед ё шаблони реферат/КМД-ро истифода баред.",
                    modifier = Modifier.padding(top = 8.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                files.take(4).forEach { f ->
                    Text(
                        f.name,
                        modifier = Modifier.fillMaxWidth().clickable { vm.openFile(ctx, f) }.padding(vertical = 10.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun Stat(label: String, value: Int) {
    Column {
        Text(value.toString(), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
