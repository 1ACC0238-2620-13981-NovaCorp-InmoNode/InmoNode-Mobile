package com.novacorp.inmonode_app.features.fieldsales.presentation.map.components

import com.novacorp.inmonode_app.features.fieldsales.domain.model.GeoPoint

/**
 * Where the map writes a lot code: the area centroid of its outer ring. A concave lot (a notched or U-shaped one) can
 * have its centroid outside, so then the code goes to the middle of the widest stretch of the lot at that latitude.
 * The map projection only scales and shifts, so a point inside on the plan stays inside on screen.
 */
internal fun labelPoint(ring: List<GeoPoint>): GeoPoint {
    val points = if (ring.size > 1 && ring.first() == ring.last()) ring.dropLast(1) else ring
    if (points.size < 3) return average(points)
    // Relative to the first vertex: lot sides are tiny next to the coordinates, so this keeps the precision.
    val origin = points.first()
    val local = points.map { GeoPoint(it.longitude - origin.longitude, it.latitude - origin.latitude) }
    var twiceArea = 0.0
    var x = 0.0
    var y = 0.0
    local.forEachIndexed { i, a ->
        val b = local[(i + 1) % local.size]
        val cross = a.longitude * b.latitude - b.longitude * a.latitude
        twiceArea += cross
        x += (a.longitude + b.longitude) * cross
        y += (a.latitude + b.latitude) * cross
    }
    if (twiceArea == 0.0) return average(points)
    val centroid = GeoPoint(origin.longitude + x / (3 * twiceArea), origin.latitude + y / (3 * twiceArea))
    if (contains(points, centroid)) return centroid
    val crossings = points.indices.mapNotNull { i ->
        val a = points[i]
        val b = points[(i + 1) % points.size]
        if ((a.latitude > centroid.latitude) != (b.latitude > centroid.latitude))
            a.longitude + (centroid.latitude - a.latitude) * (b.longitude - a.longitude) / (b.latitude - a.latitude)
        else null
    }.sorted()
    val widest = crossings.chunked(2).filter { it.size == 2 }.maxByOrNull { it[1] - it[0] } ?: return centroid
    return GeoPoint((widest[0] + widest[1]) / 2, centroid.latitude)
}

/** Even-odd rule, the same the map uses to pick the tapped lot. */
internal fun contains(ring: List<GeoPoint>, point: GeoPoint): Boolean {
    var inside = false
    var j = ring.lastIndex
    for (i in ring.indices) {
        val a = ring[i]
        val b = ring[j]
        if ((a.latitude > point.latitude) != (b.latitude > point.latitude) &&
            point.longitude < (b.longitude - a.longitude) * (point.latitude - a.latitude) / (b.latitude - a.latitude) + a.longitude
        ) inside = !inside
        j = i
    }
    return inside
}

private fun average(points: List<GeoPoint>) =
    GeoPoint(points.map { it.longitude }.average(), points.map { it.latitude }.average())
