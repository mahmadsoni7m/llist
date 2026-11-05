package tj.soni.listoffice.feature.handwriting

import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import tj.soni.listoffice.MainViewModel
import tj.soni.listoffice.core.ui.ChipRow
import tj.soni.listoffice.core.ui.PremiumCard
import tj.soni.listoffice.core.ui.ToolButton

@Composable
fun HandwritingScreen(vm: MainViewModel) {
    val ctx = LocalContext.current
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Handwriting Studio", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)

        vm.fontWarning?.let {
            PremiumCard(Modifier.fillMaxWidth()) {
                Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        OutlinedTextField(
            value = vm.hwText,
            onValueChange = { vm.hwText = it },
            label = { Text("Матн барои дастнавис") },
            minLines = 6,
            maxLines = 12,
            modifier = Modifier.fillMaxWidth()
        )

        Text("Услуб")
        ChipRow(listOf("Natural Student", "Beautiful", "Clean", "Custom"), vm.hwStyle) { vm.hwStyle = it }
        if (vm.hwStyle == 3) {
            Text("Андоза: ${vm.hwCustomSize.toInt()}")
            Slider(vm.hwCustomSize, { vm.hwCustomSize = it }, valueRange = 34f..64f)
            Text("Каҷии ҳарфҳо: ${"%.1f".format(vm.hwCustomRot)}°")
            Slider(vm.hwCustomRot, { vm.hwCustomRot = it }, valueRange = 0f..5f)
            Text("Нобаробарии хат: ${"%.1f".format(vm.hwCustomJitter)}")
            Slider(vm.hwCustomJitter, { vm.hwCustomJitter = it }, valueRange = 0f..6f)
        }

        Text("Қоғаз")
        ChipRow(Paper.values().map { it.label }, vm.hwPaper) { vm.hwPaper = it }
        if (vm.hwStyle != 3) {
            Text("Андозаи хат: ${(vm.hwSizeScale * 100).toInt()}%")
            Slider(vm.hwSizeScale, { vm.hwSizeScale = it }, valueRange = 0.6f..1.8f)
        }

        PremiumCard(Modifier.fillMaxWidth()) {
            Text("Ҳошияҳо (мм)", style = MaterialTheme.typography.titleMedium)
            Text("Чап: ${vm.hwLeft.toInt()}")
            Slider(vm.hwLeft, { vm.hwLeft = it }, valueRange = 0f..50f)
            Text("Рост: ${vm.hwRight.toInt()}")
            Slider(vm.hwRight, { vm.hwRight = it }, valueRange = 0f..50f)
            Text("Боло: ${vm.hwTop.toInt()}")
            Slider(vm.hwTop, { vm.hwTop = it }, valueRange = 0f..60f)
            Text("Поён: ${vm.hwBottom.toInt()}")
            Slider(vm.hwBottom, { vm.hwBottom = it }, valueRange = 0f..60f)
        }

        SwitchRow("Хати ҳошияи сурх", vm.hwMargin) { vm.hwMargin = it }
        SwitchRow("Сатри сурх (ақибнишинии аввали сатр)", vm.hwIndent) { vm.hwIndent = it }
        SwitchRow("Сарлавҳаҳои ҳарфи калон дар марказ", vm.hwCenter) { vm.hwCenter = it }
        SwitchRow("Рақами саҳифа", vm.hwPageNum) { vm.hwPageNum = it }

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            ToolButton(Icons.Default.Create, "Пешнамоиш") { vm.previewHandwriting() }
            ToolButton(Icons.Default.PictureAsPdf, "PDF") { vm.exportHandwritingPdf() }
            ToolButton(Icons.Default.Print, "Чоп") { vm.printHandwriting(ctx) }
        }

        if (vm.hwPreview.isEmpty()) {
            Text("Пешнамоиш пас аз пахши «Пешнамоиш» пайдо мешавад.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            vm.hwPreview.forEach { bmp ->
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Text("Пешнамоиш: то 3 саҳифаи аввал. PDF ҳамаи саҳифаҳоро дорад.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Switch(checked = checked, onCheckedChange = onChange)
        Text(label)
    }
}
