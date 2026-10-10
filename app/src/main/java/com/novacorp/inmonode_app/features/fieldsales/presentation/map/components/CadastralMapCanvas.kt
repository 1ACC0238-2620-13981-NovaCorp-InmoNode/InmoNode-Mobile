package com.novacorp.inmonode_app.features.fieldsales.presentation.map.components
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import com.novacorp.inmonode_app.core.designsystem.theme.*
import com.novacorp.inmonode_app.features.fieldsales.domain.model.*
import kotlin.math.*

/** Projection uses the downloaded cadastral polygons, including interior rings. */
@Composable
fun CadastralMapCanvas(lots: List<Lot>, matches: (Lot) -> Boolean, selectedId: Long?,
    modifier: Modifier = Modifier, reset: Int = 0, onSelect: (Long) -> Unit) {
    var zoom by remember(lots, reset) { mutableFloatStateOf(1f) }
    var pan by remember(lots, reset) { mutableStateOf(Offset.Zero) }
    val points = remember(lots) { lots.flatMap { it.polygon.flatten() } }
    val minX = points.minOfOrNull { it.longitude } ?: 0.0
    val maxX = points.maxOfOrNull { it.longitude } ?: 1.0
    val minY = points.minOfOrNull { it.latitude } ?: 0.0
    val maxY = points.maxOfOrNull { it.latitude } ?: 1.0
    val cosine = cos((minY + maxY) / 2 * PI / 180).coerceAtLeast(0.01)
    fun project(point: GeoPoint, width: Float, height: Float): Offset {
        val sx = max((maxX-minX)*cosine, 1e-9); val sy = max(maxY-minY, 1e-9)
        val scale = min((width-32)/sx, (height-32)/sy)
        val base = Offset(((point.longitude-minX)*cosine*scale+(width-sx*scale)/2).toFloat(),
            ((maxY-point.latitude)*scale+(height-sy*scale)/2).toFloat())
        val center = Offset(width/2,height/2)
        return (base-center)*zoom+center+pan
    }
    fun inside(point: Offset, ring: List<Offset>): Boolean {
        var result = false; var j = ring.lastIndex
        for (i in ring.indices) { val a=ring[i]; val b=ring[j]
            if ((a.y>point.y)!=(b.y>point.y) && point.x < (b.x-a.x)*(point.y-a.y)/(b.y-a.y)+a.x) result=!result
            j=i
        }; return result
    }
    val measurer = rememberTextMeasurer()
    Canvas(modifier.semantics {
        contentDescription="Mapa catastral"
        customActions=lots.filter(matches).map { lot -> CustomAccessibilityAction("${lot.code} · ${lot.dimensions.area} m²") { onSelect(lot.id);true } }
    }.pointerInput(lots, zoom, pan, matches) {
        detectTapGestures { tap -> lots.lastOrNull { lot ->
            val rings=lot.polygon.map { ring -> ring.map { project(it,size.width.toFloat(),size.height.toFloat()) } }
            matches(lot) && rings.isNotEmpty() && inside(tap,rings.first()) && rings.drop(1).none { inside(tap,it) }
        }?.let { onSelect(it.id) } }
    }.pointerInput(lots) { detectTransformGestures { _, delta, scale, _ ->
        zoom=(zoom*scale).coerceIn(0.5f,8f); pan+=delta
    } }) {
        drawRect(Color(0xFFE1E8DB))
        for (lot in lots) {
            val rings=lot.polygon.map { ring -> ring.map { project(it,size.width,size.height) } }
            val path=Path().apply { fillType=PathFillType.EvenOdd
                rings.forEach { ring -> ring.firstOrNull()?.let { moveTo(it.x,it.y) }; ring.drop(1).forEach { lineTo(it.x,it.y) }; close() } }
            val color=when { !matches(lot) -> Color(0xFFD3D8CF)
                lot.pendingReservationId!=null -> AlertYellow
                lot.status==LotStatus.AVAILABLE -> LightGreen
                lot.status==LotStatus.SOLD || lot.status==LotStatus.BLOCKED -> TerracottaOrange
                else -> AlertYellow }
            drawPath(path,color); drawPath(path,if(lot.id==selectedId) ForestGreen else Color.White,
                style=Stroke(if(lot.id==selectedId) 4.dp.toPx() else 1.5.dp.toPx()))
            rings.firstOrNull()?.takeIf { it.isNotEmpty() }?.let { ring ->
                val center=Offset(ring.map { it.x }.average().toFloat(),ring.map { it.y }.average().toFloat())
                val text=measurer.measure(lot.code,TextStyle(fontFamily=Montserrat,fontSize=10.sp,
                    color=if(color==TerracottaOrange) Color.White else PureBlack))
                drawText(text,topLeft=center-Offset(text.size.width/2f,text.size.height/2f))
            }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(widthDp=360,heightDp=440,showBackground=true)
@Composable
private fun CadastralMapCanvasPreview() {
 val lots=(0..5).map { index ->
  val x=(index%3).toDouble();val y=(index/3).toDouble()
  Lot(index.toLong(),1,"A-${index+1}",LotDimensions(java.math.BigDecimal.TEN,java.math.BigDecimal.TEN,java.math.BigDecimal("100")),
   listOf(listOf(GeoPoint(x,y),GeoPoint(x+0.9,y),GeoPoint(x+0.9,y+0.9),GeoPoint(x,y+0.9),GeoPoint(x,y))),
   java.math.BigDecimal("10000"),"PEN",if(index==1)LotStatus.SOLD else LotStatus.AVAILABLE)
 }
 InmoNodeAppTheme { CadastralMapCanvas(lots,{true},null,Modifier.fillMaxSize(),onSelect={}) }
}
