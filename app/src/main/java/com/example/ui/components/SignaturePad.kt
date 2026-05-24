package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke as CanvasStrokeStyle
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.ui.SignatureStroke

@Composable
fun SignaturePad(
    modifier: Modifier = Modifier,
    strokes: List<SignatureStroke>,
    onStrokesChanged: (List<SignatureStroke>) -> Unit,
    strokeColor: Color = Color.Black
) {
    var size by remember { mutableStateOf(IntSize.Zero) }

    // Keep track of the current in-progress stroke points
    val currentPoints = remember { mutableStateListOf<Offset>() }

    Box(
        modifier = modifier
            .background(Color.White)
            .clipToBounds()
            .onSizeChanged { size = it }
    ) {
        if (strokes.isEmpty() && currentPoints.isEmpty()) {
            Text(
                text = "Draw your signature here with your finger",
                color = Color.Gray.copy(alpha = 0.5f),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { startOffset ->
                            if (size.width > 0 && size.height > 0) {
                                currentPoints.clear()
                                val normX = startOffset.x / size.width
                                val normY = startOffset.y / size.height
                                currentPoints.add(Offset(normX, normY))
                            }
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            if (size.width > 0 && size.height > 0) {
                                val currentOffset = change.position
                                val normX = currentOffset.x / size.width
                                val normY = currentOffset.y / size.height
                                currentPoints.add(Offset(normX, normY))
                            }
                        },
                        onDragEnd = {
                            if (currentPoints.isNotEmpty()) {
                                val newStroke = SignatureStroke(currentPoints.toList())
                                onStrokesChanged(strokes + newStroke)
                                currentPoints.clear()
                            }
                        }
                    )
                }
        ) {
            // Draw already completed strokes
            for (stroke in strokes) {
                if (stroke.points.size > 1) {
                    val path = Path()
                    val p0 = stroke.points[0]
                    path.moveTo(p0.x * size.width, p0.y * size.height)
                    for (i in 1 until stroke.points.size) {
                        val pi = stroke.points[i]
                        path.lineTo(pi.x * size.width, pi.y * size.height)
                    }
                    drawPath(
                        path = path,
                        color = strokeColor,
                        style = CanvasStrokeStyle(
                            width = 4.dp.toPx(),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                } else if (stroke.points.size == 1) {
                    val p = stroke.points[0]
                    drawCircle(
                        color = strokeColor,
                        center = Offset(p.x * size.width, p.y * size.height),
                        radius = 3.dp.toPx()
                    )
                }
            }

            // Draw current active stroke points
            if (currentPoints.size > 1) {
                val path = Path()
                val p0 = currentPoints[0]
                path.moveTo(p0.x * size.width, p0.y * size.height)
                for (i in 1 until currentPoints.size) {
                    val pi = currentPoints[i]
                    path.lineTo(pi.x * size.width, pi.y * size.height)
                }
                drawPath(
                    path = path,
                    color = strokeColor,
                    style = CanvasStrokeStyle(
                        width = 4.dp.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            } else if (currentPoints.size == 1) {
                val p = currentPoints[0]
                drawCircle(
                    color = strokeColor,
                    center = Offset(p.x * size.width, p.y * size.height),
                    radius = 3.dp.toPx()
                )
            }
        }
    }
}
