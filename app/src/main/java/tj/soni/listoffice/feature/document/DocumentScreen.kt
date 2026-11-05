package tj.soni.listoffice.feature.document

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import tj.soni.listoffice.MainViewModel
import tj.soni.listoffice.core.ui.ChipRow
import tj.soni.listoffice.core.ui.ToolButton
import tj.soni.listoffice.domain.Lang
import tj.soni.listoffice.domain.ReportType
import tj.soni.listoffice.domain.TextTools

@Composable
fun DocumentScreen(vm: MainViewModel) {
    val ctx = LocalContext.current
    var showTemplate by remember { mutableStateOf(false) }
    var showSearch by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var summary by remember { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = vm.docName,
            onValueChange = { vm.docName = it },
            label = { Text("Номи ҳуҷҷат") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ToolButton(Icons.Default.Save, "Нигоҳ") { vm.saveDoc() }
            ToolButton(Icons.Default.Add, "Нав") { vm.newDoc() }
            ToolButton(Icons.Default.Description, "Шаблон") { showTemplate = true }
            ToolButton(Icons.Default.PictureAsPdf, "PDF") { vm.exportPdf { } }
            ToolButton(Icons.Default.Description, "DOCX") { vm.exportDocx() }
            ToolButton(Icons.Default.Print, "Чоп") { vm.printDocument(ctx) }
            ToolButton(Icons.Default.Create, "Дастнавис") { vm.toHandwriting() }
            ToolButton(Icons.Default.Info, "Хулоса") {
                val s = vm.summarize()
                summary = if (s.isBlank()) "Барои хулоса матни дарозтар лозим аст." else s
            }
            ToolButton(Icons.Default.Check, "Тоза") { vm.cleanText() }
            ToolButton(Icons.Default.Search, "Ҷустуҷӯ") { showSearch = !showSearch }
        }
        Text("Андозаи ҳарф дар PDF/DOCX:", color = MaterialTheme.colorScheme.onSurfaceVariant)
        ChipRow(vm.docFontSizes.map { "$it pt" }, vm.docFontSize) { vm.docFontSize = it }

        if (showSearch) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Ҷустуҷӯ дар ҳуҷҷат") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Text("Ёфт шуд: ${TextTools.countMatches(vm.docText, query)}")
        }

        OutlinedTextField(
            value = vm.docText,
            onValueChange = { vm.onDocTextChange(it) },
            label = { Text("Матн") },
            modifier = Modifier.fillMaxWidth().weight(1f)
        )
        Text(
            "Калимаҳо: ${TextTools.wordCount(vm.docText)}  •  Аломатҳо: ${vm.docText.length}  •  Автонигоҳдорӣ фаъол аст",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    if (showTemplate) {
        TemplateDialog(
            onDismiss = { showTemplate = false },
            onCreate = { type, lang, topic, author, org ->
                showTemplate = false
                vm.applyTemplate(type, lang, topic, author, org)
            }
        )
    }

    summary?.let { s ->
        AlertDialog(
            onDismissRequest = { summary = null },
            title = { Text("Хулосаи офлайн") },
            text = { Text(s, modifier = Modifier.verticalScroll(rememberScrollState())) },
            confirmButton = { TextButton(onClick = { summary = null }) { Text("Хуб") } }
        )
    }
}

@Composable
private fun TemplateDialog(
    onDismiss: () -> Unit,
    onCreate: (ReportType, Lang, String, String, String) -> Unit
) {
    var typeIdx by remember { mutableIntStateOf(0) }
    var langIdx by remember { mutableIntStateOf(0) }
    var topic by remember { mutableStateOf("") }
    var author by remember { mutableStateOf("") }
    var org by remember { mutableStateOf("") }
    val types = ReportType.values()
    val langs = Lang.values()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Шаблони ҳуҷҷат") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Намуд")
                ChipRow(types.map { it.tg }, typeIdx) { typeIdx = it }
                Text("Забон")
                ChipRow(langs.map { it.label }, langIdx) { langIdx = it }
                OutlinedTextField(topic, { topic = it }, label = { Text("Мавзӯъ") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(author, { author = it }, label = { Text("Муаллиф") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(org, { org = it }, label = { Text("Муассиса / донишгоҳ") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(
                enabled = topic.isNotBlank(),
                onClick = { onCreate(types[typeIdx], langs[langIdx], topic, author, org) }
            ) { Text("Сохтан") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Бекор") } }
    )
}
