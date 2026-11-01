package com.example.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.util.OfflineShieldRemover
import java.io.File
import java.io.FileOutputStream

@Composable
fun ShieldEditorDialog(
    initialBitmap: Bitmap,
    onDismiss: () -> Unit,
    onSaveShield: (Uri) -> Unit
) {
    val context = LocalContext.current
    val baseBitmap = remember(initialBitmap) {
        OfflineShieldRemover.scaleBitmapPreservingRatio(initialBitmap, 480)
    }

    var currentBitmap by remember {
        mutableStateOf(baseBitmap.copy(Bitmap.Config.ARGB_8888, true))
    }

    val undoStack = remember { mutableStateListOf<Bitmap>() }

    var tolerancePercentage by remember { mutableFloatStateOf(25f) }
    var brushRadius by remember { mutableFloatStateOf(16f) }
    var zoomScale by remember { mutableFloatStateOf(1.0f) }
    var selectedTool by remember { mutableStateOf("ERASER") } // ERASER, PENCIL, WAND
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    fun pushUndo() {
        if (undoStack.size >= 5) {
            undoStack.removeAt(0)
        }
        undoStack.add(currentBitmap.copy(Bitmap.Config.ARGB_8888, true))
    }

    fun applyAutoTolerance(tol: Float) {
        try {
            pushUndo()
            val processed = OfflineShieldRemover.removeColorBackground(
                source = baseBitmap,
                targetColor = baseBitmap.getPixel(0, 0),
                tolerancePercentage = tol.toInt()
            )
            currentBitmap = processed
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(12.dp),
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Editor y Recorte de Escudo",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Varita mágica, zoom y lápiz restaurador",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar")
                    }
                }

                // Interactive Canvas on Checkered Background with Zoom
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1.1f)
                        .clip(RoundedCornerShape(14.dp))
                        .border(1.5.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
                ) {
                    var renderTrigger by remember { androidx.compose.runtime.mutableIntStateOf(0) }

                    // Checkered pattern indicating transparency
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val dummy = renderTrigger
                        canvasSize = IntSize(size.width.toInt(), size.height.toInt())
                        val checkSize = 16.dp.toPx()
                        val cols = (size.width / checkSize).toInt() + 1
                        val rows = (size.height / checkSize).toInt() + 1

                        for (r in 0 until rows) {
                            for (c in 0 until cols) {
                                val isEven = (r + c) % 2 == 0
                                drawRect(
                                    color = if (isEven) Color(0xFFE2E8F0) else Color(0xFFCBD5E1),
                                    topLeft = Offset(c * checkSize, r * checkSize),
                                    size = Size(checkSize, checkSize)
                                )
                            }
                        }

                        // Draw current processed bitmap scaled to canvas and zoom
                        val w = size.width * zoomScale
                        val h = size.height * zoomScale
                        val offsetX = (size.width - w) / 2f
                        val offsetY = (size.height - h) / 2f
                        drawImage(
                            image = currentBitmap.asImageBitmap(),
                            dstOffset = IntOffset(offsetX.toInt(), offsetY.toInt()),
                            dstSize = IntSize(w.toInt(), h.toInt())
                        )
                    }

                    // Touch overlay with fluid stroke drag gestures and wand click
                    var lastTouchX by remember { mutableFloatStateOf(-1f) }
                    var lastTouchY by remember { mutableFloatStateOf(-1f) }

                    fun mapTouchToBitmap(touchX: Float, touchY: Float): Pair<Float, Float>? {
                        if (canvasSize.width <= 0 || canvasSize.height <= 0) return null
                        val w = canvasSize.width * zoomScale
                        val h = canvasSize.height * zoomScale
                        val offsetX = (canvasSize.width - w) / 2f
                        val offsetY = (canvasSize.height - h) / 2f

                        val relX = (touchX - offsetX) / w
                        val relY = (touchY - offsetY) / h
                        if (relX !in 0f..1f || relY !in 0f..1f) return null

                        val bmpX = relX * currentBitmap.width
                        val bmpY = relY * currentBitmap.height
                        return Pair(bmpX, bmpY)
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(selectedTool, brushRadius, zoomScale, tolerancePercentage) {
                                detectDragGestures(
                                    onDragStart = { offset ->
                                        pushUndo()
                                        lastTouchX = offset.x
                                        lastTouchY = offset.y
                                        val mapped = mapTouchToBitmap(offset.x, offset.y)
                                        if (mapped != null) {
                                            val (bmpX, bmpY) = mapped
                                            val scaledRadius = (brushRadius / canvasSize.width) * currentBitmap.width / zoomScale

                                            if (selectedTool == "WAND") {
                                                OfflineShieldRemover.magicWandFloodFill(
                                                    bitmap = currentBitmap,
                                                    touchX = bmpX,
                                                    touchY = bmpY,
                                                    tolerancePercentage = tolerancePercentage.toInt()
                                                )
                                                renderTrigger++
                                            } else if (selectedTool == "PENCIL") {
                                                OfflineShieldRemover.restoreOriginalPixels(
                                                    bitmap = currentBitmap,
                                                    originalBitmap = baseBitmap,
                                                    cx = bmpX,
                                                    cy = bmpY,
                                                    radius = scaledRadius.coerceAtLeast(2f)
                                                )
                                                renderTrigger++
                                            } else {
                                                OfflineShieldRemover.applyBrush(
                                                    bitmap = currentBitmap,
                                                    x = bmpX,
                                                    y = bmpY,
                                                    radius = scaledRadius.coerceAtLeast(2f),
                                                    isErase = true
                                                )
                                                renderTrigger++
                                            }
                                        }
                                    },
                                    onDragEnd = {
                                        lastTouchX = -1f
                                        lastTouchY = -1f
                                    },
                                    onDragCancel = {
                                        lastTouchX = -1f
                                        lastTouchY = -1f
                                    },
                                    onDrag = { change, _ ->
                                        change.consume()
                                        val touchX = change.position.x
                                        val touchY = change.position.y

                                        if (selectedTool != "WAND") {
                                            val mapped = mapTouchToBitmap(touchX, touchY)
                                            val mappedPrev = if (lastTouchX >= 0) mapTouchToBitmap(lastTouchX, lastTouchY) else mapped

                                            if (mapped != null && mappedPrev != null) {
                                                val (bmpX, bmpY) = mapped
                                                val (prevBmpX, prevBmpY) = mappedPrev
                                                val scaledRadius = (brushRadius / canvasSize.width) * currentBitmap.width / zoomScale

                                                if (selectedTool == "PENCIL") {
                                                    OfflineShieldRemover.restoreOriginalPixelsLine(
                                                        bitmap = currentBitmap,
                                                        originalBitmap = baseBitmap,
                                                        startX = prevBmpX,
                                                        startY = prevBmpY,
                                                        endX = bmpX,
                                                        endY = bmpY,
                                                        radius = scaledRadius.coerceAtLeast(2f)
                                                    )
                                                } else {
                                                    OfflineShieldRemover.applyBrushLine(
                                                        bitmap = currentBitmap,
                                                        startX = prevBmpX,
                                                        startY = prevBmpY,
                                                        endX = bmpX,
                                                        endY = bmpY,
                                                        radius = scaledRadius.coerceAtLeast(2f),
                                                        isErase = true
                                                    )
                                                }

                                                lastTouchX = touchX
                                                lastTouchY = touchY
                                                renderTrigger++
                                            }
                                        }
                                    }
                                )
                            }
                    )
                }

                // Zoom and Precision Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Zoom: ${(zoomScale * 100).toInt()}%", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Slider(
                        value = zoomScale,
                        onValueChange = { zoomScale = it },
                        valueRange = 1.0f..3.0f,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedButton(
                        onClick = { zoomScale = 1.0f },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("100%", fontSize = 10.sp)
                    }
                }

                // Tolerance Slider with auto color removal
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    )
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AutoFixHigh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Tolerancia de fondo", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                            Text("${tolerancePercentage.toInt()}%", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Slider(
                            value = tolerancePercentage,
                            onValueChange = {
                                tolerancePercentage = it
                                applyAutoTolerance(it)
                            },
                            valueRange = 0f..80f
                        )
                    }
                }

                // Brush radius slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Grosor: ${brushRadius.toInt()}px", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    Slider(
                        value = brushRadius,
                        onValueChange = { brushRadius = it },
                        valueRange = 6f..48f,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Manual Tools: Eraser vs Pencil (Restores original) vs Wand, Undo and Reset
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = selectedTool == "ERASER",
                        onClick = { selectedTool = "ERASER" },
                        label = { Text("Goma", fontSize = 11.sp) },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(13.dp)) }
                    )
                    FilterChip(
                        selected = selectedTool == "PENCIL",
                        onClick = { selectedTool = "PENCIL" },
                        label = { Text("Lápiz (Restaura)", fontSize = 11.sp) },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(13.dp)) }
                    )
                    FilterChip(
                        selected = selectedTool == "WAND",
                        onClick = { selectedTool = "WAND" },
                        label = { Text("Varita", fontSize = 11.sp) },
                        leadingIcon = { Icon(Icons.Default.AutoFixHigh, contentDescription = null, modifier = Modifier.size(13.dp)) }
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    // Undo button
                    IconButton(
                        onClick = {
                            if (undoStack.isNotEmpty()) {
                                currentBitmap = undoStack.removeAt(undoStack.lastIndex)
                            }
                        },
                        enabled = undoStack.isNotEmpty()
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Deshacer")
                    }

                    // Reset button
                    IconButton(
                        onClick = {
                            pushUndo()
                            currentBitmap = baseBitmap.copy(Bitmap.Config.ARGB_8888, true)
                            tolerancePercentage = 0f
                        }
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reiniciar")
                    }
                }

                // Actions: Cancel & Save
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancelar")
                    }

                    Button(
                        onClick = {
                            try {
                                val dir = File(context.filesDir, "team_shields").apply { if (!exists()) mkdirs() }
                                val file = File(dir, "shield_edited_${System.currentTimeMillis()}.png")
                                FileOutputStream(file).use { out ->
                                    currentBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                                }
                                onSaveShield(Uri.fromFile(file))
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Aplicar Escudo")
                    }
                }
            }
        }
    }
}
