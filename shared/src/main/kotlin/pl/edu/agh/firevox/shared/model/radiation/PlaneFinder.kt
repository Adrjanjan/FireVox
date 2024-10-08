package pl.edu.agh.firevox.shared.model.radiation

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import pl.edu.agh.firevox.shared.model.PhysicalMaterialRepository
import pl.edu.agh.firevox.shared.model.VoxelKey
import java.lang.Math.pow
import java.util.stream.Collectors
import java.util.stream.IntStream
import kotlin.math.*

@Service
class PlaneFinder @Autowired constructor(
    @Value("\${firevox.voxel.size}") val voxelLength: Double = 0.01,
    @Value("\${firevox.plane.size}") val planeLength: Int = 10,
    private val physicalMaterialRepository: PhysicalMaterialRepository,
) {

    companion object {
        val log: Logger = LoggerFactory.getLogger(this::class.java)
    }

    @Transactional
    fun findPlanes(
        voxels: Array<Array<IntArray>>, pointsToNormals: List<Pair<VoxelKey, VoxelKey>>
    ): List<RadiationPlane> {
        log.info("Preprocessing radiation planes")
        var wallId = 0
        val planes = pointsToNormals.parallelStream().flatMap {
            log.info("Processing plane $it")
            val fullPlane = fullPlane(voxels, it)
            divideIntoPlanes(wallId++, fullPlane, it.second, planeLength).stream()
        }.collect(Collectors.toList()).toMutableList()

        val findRelationships = findRelationships(planes, voxels)
        log.info("Found ${planes.size} radiation planes")
        return findRelationships
    }

    fun fullPlane(voxels: Array<Array<IntArray>>, pointToNormal: Pair<VoxelKey, VoxelKey>): List<VoxelKey> {
        val (startingPoint, normalVector) = pointToNormal
        val stack = mutableListOf(pointToNormal.first)
        val visited = mutableSetOf<VoxelKey>()

        while (stack.isNotEmpty()) {
            val (x, y, z) = stack.removeAt(stack.size - 1).also(visited::add)

            // Check neighboring elements along the x, y, and z axes
            val neighbors = when (normalVector) {
                VoxelKey(0, 0, 1), VoxelKey(0, 0, -1) -> listOf(
                    VoxelKey(x + 1, y, z),
                    VoxelKey(x - 1, y, z),
                    VoxelKey(x, y + 1, z),
                    VoxelKey(x, y - 1, z),
                )

                VoxelKey(0, 1, 0), VoxelKey(0, -1, 0) -> listOf(
                    VoxelKey(x + 1, y, z), VoxelKey(x - 1, y, z), VoxelKey(x, y, z + 1), VoxelKey(x, y, z - 1)
                )

                VoxelKey(1, 0, 0), VoxelKey(-1, 0, 0) -> listOf(
                    VoxelKey(x, y + 1, z), VoxelKey(x, y - 1, z), VoxelKey(x, y, z + 1), VoxelKey(x, y, z - 1)
                )

                else -> listOf()
            }

            for (key in neighbors) {
                if (isValidFaceElement(
                        key, voxels, normalVector
                    ) && voxels[key] == voxels[startingPoint] && key !in visited
                ) {
                    stack.add(key)
                }
            }
        }

        return visited.toList()
    }

    fun isValidFaceElement(key: VoxelKey, matrix: Array<Array<IntArray>>, normalVector: VoxelKey) =
        matrix.contains(key) && matrix[key] != 0 && matrix.contains(normalVector + key) && matrix[normalVector + key] == 0

    private fun Array<Array<IntArray>>.contains(
        key: VoxelKey,
    ) = key.x in this.indices && key.y in this[0].indices && key.z in this[0][0].indices

    @Transactional
    fun divideIntoPlanes(
        wallId: Int, fullPlane: List<VoxelKey>, normalVector: VoxelKey, squareSize: Int
    ): List<RadiationPlane> {
        val minX = fullPlane.minOf { it.x }
        val minY = fullPlane.minOf { it.y }
        val minZ = fullPlane.minOf { it.z }
        val maxX = fullPlane.maxOf { it.x }
        val maxY = fullPlane.maxOf { it.y }
        val maxZ = fullPlane.maxOf { it.z }

        val nx = max(1, (maxX - minX) / squareSize)
        val ny = max(1, (maxY - minY) / squareSize)
        val nz = max(1, (maxZ - minZ) / squareSize)

        val results = mutableListOf<RadiationPlane>()
        for (ix in 0..nx) {
            val xStartIndex = minX + ix * squareSize
            val xEndIndex = min(xStartIndex + squareSize, maxX + 1)
            if (xStartIndex > xEndIndex) break // only possible if end was truncated to maxX+1 but the iterations will make start bigger than end
            for (iy in 0..ny) {
                val yStartIndex = minY + iy * squareSize
                val yEndIndex = min(yStartIndex + squareSize, maxY + 1)
                if (yStartIndex > yEndIndex) break
                for (iz in 0..nz) {
                    val zStartIndex = minZ + iz * squareSize
                    val zEndIndex = min(zStartIndex + squareSize, maxZ + 1)
                    if (zStartIndex > zEndIndex) break

                    val voxels = mutableListOf<VoxelKey>()
                    for (key in fullPlane) {
                        if ((key.x in (xStartIndex until xEndIndex) || key.x == xStartIndex) && (key.y in (yStartIndex until yEndIndex) || key.y == yStartIndex) && (key.z in (zStartIndex until zEndIndex) || key.z == zStartIndex)) {
                            voxels.add(key)
                        }
                        if (voxels.size >= squareSize * squareSize) break // single square cant be bigger than squareSize^2
                    }
                    if (voxels.isEmpty()) continue

                    val a: VoxelKey
                    val b: VoxelKey
                    val c: VoxelKey
                    val d: VoxelKey

                    when {
                        normalVector.z != 0 -> { // plane in XY
                            val x = voxels.maxOf { it.x }
                            val y = voxels.maxOf { it.y }
                            val f = voxels[0]

                            a = VoxelKey(xStartIndex, yStartIndex, f.z)
                            b = VoxelKey(xStartIndex, y, f.z)
                            c = VoxelKey(x, yStartIndex, f.z)
                            d = VoxelKey(x, y, f.z)
                        }

                        normalVector.x != 0 -> { // plane in YZ
                            val y = voxels.maxOf { it.y }
                            val z = voxels.maxOf { it.z }
                            val f = voxels[0]

                            a = VoxelKey(f.x, yStartIndex, zStartIndex)
                            b = VoxelKey(f.x, yStartIndex, z)
                            c = VoxelKey(f.x, y, zStartIndex)
                            d = VoxelKey(f.x, y, z)
                        }

                        else -> { // plane in XZ
                            val x = voxels.maxOf { it.x }
                            val z = voxels.maxOf { it.z }
                            val f = voxels[0]

                            a = VoxelKey(xStartIndex, f.y, zStartIndex)
                            b = VoxelKey(xStartIndex, f.y, z)
                            c = VoxelKey(x, f.y, zStartIndex)
                            d = VoxelKey(x, f.y, z)
                        }
                    }

                    results.add(
                        RadiationPlane(
                            wallId,
                            a = a,
                            b = b,
                            c = c,
                            d = d,
                            normalVector = normalVector,
                            voxels = voxels.toMutableSet(),
                            voxelsCount = voxels.size,
                            area = area(normalVector, a, b, c, d),
                            fullPlane = fullPlane,
                        )
                    )
                }
            }
        }
        return results
    }

    fun findRelationships(planes: List<RadiationPlane>, voxels: Array<Array<IntArray>>): List<RadiationPlane> {
        for (i in planes.indices) {
            IntStream.range(i + 1, planes.size).parallel().forEach { j ->
                val first = planes[i]
                val second = planes[j]

                if (canSeeEachOther(first, second)) {
                    if (!obstructedView(first, second, voxels)) {
                        val (firstViewFactor, secondViewFactor) = calculateViewFactors(first, second)

                        first.childPlanes.add(
                            PlanesConnection(
                                parent = first,
                                child = second,
                                parentVoxelsCount = first.voxelsCount,
                                childVoxelsCount = second.voxelsCount,
                                viewFactor = firstViewFactor
                            )
                        )
                        second.childPlanes.add(
                            PlanesConnection(
                                parent = second,
                                child = first,
                                parentVoxelsCount = second.voxelsCount,
                                childVoxelsCount = first.voxelsCount,
                                viewFactor = secondViewFactor
                            )
                        )
                    }
                }
            }
            if (planes[i].childPlanes.sumOf { it.viewFactor } > 1) {
                throw RuntimeException("View factors for plane (${planes[i].a}, ${planes[i].b}, ${planes[i].c}, ${planes[i].d}) was greater than 1")
            }
            addAmbienceConnection(planes[i])
        }
        return planes
    }

    private fun addAmbienceConnection(plane: RadiationPlane) {
        plane.resetLostRadiationPercentage()
        plane.childPlanes.add(
            PlanesConnection(
                parent = plane,
                child = null,
                viewFactor = plane.lostRadiationPercentage,
                parentVoxelsCount = plane.voxelsCount,
                childVoxelsCount = 0,
                isAmbient = true,
            )
        )
    }

    private fun calculateViewFactors(
        first: RadiationPlane, second: RadiationPlane
    ) = if (first.normalVector.dotProduct(second.normalVector) == 0) {
        perpendicularViewFactor(first, second) to perpendicularViewFactor(second, first)
    } else {
        parallelViewFactor(first, second) to parallelViewFactor(second, first)
    }

    private fun area(normalVector: VoxelKey, a: VoxelKey, b: VoxelKey, c: VoxelKey, d: VoxelKey): Double {
        val (m, n) = when {
            normalVector.z != 0 -> uniqueCoordinate(a, b, c, d) { it.x } to uniqueCoordinate(a, b, c, d) { it.y }
            normalVector.y != 0 -> uniqueCoordinate(a, b, c, d) { it.x } to uniqueCoordinate(a, b, c, d) { it.z }
            else -> uniqueCoordinate(a, b, c, d) { it.y } to uniqueCoordinate(a, b, c, d) { it.z }
        }
        return unitlessArea(m[0], m[1], n[0], n[1]) * voxelLength.pow(2)
    }

    private fun unitlessArea(a: Double, b: Double, c: Double, d: Double) = abs(a - b) * abs(c - d)

    private fun obstructedView(first: RadiationPlane, second: RadiationPlane, voxels: Array<Array<IntArray>>): Boolean {
        val firstKeys = first.voxels

        for (key in ddaStep(first.middle, second.middle)) {
            if (key == second.middle) return false
            if (key.x == -1 || key.y == -1 || key.z == -1 || !voxels.contains(key)) return true // out of the model
            if (voxels[key] != 0) {
                if (key in firstKeys || key in second.fullPlane) {
                    continue // ray still in first plane or is on the same level that the second see ie (55, 99, 5) -> (55, 5, 0)
                } else if (key in second.voxels) return false // ray in second plane
                return true
            }
        }
        return false
    }

    fun ddaStep(start: VoxelKey, end: VoxelKey) = sequence {
        val direction = end - start
        val dx = if (direction.x == 0) 0.0 else sqrt(1.0 + (direction.y.sq() + direction.z.sq()) / direction.x.sq())
        val dy = if (direction.y == 0) 0.0 else sqrt(1.0 + (direction.x.sq() + direction.z.sq()) / direction.y.sq())
        val dz = if (direction.z == 0) 0.0 else sqrt(1.0 + (direction.x.sq() + direction.y.sq()) / direction.z.sq())

        val current = start.copy()
        val rayLength = Vector(0.0, 0.0, 0.0)
        val stepX = direction.x.sign
        val stepY = direction.y.sign
        val stepZ = direction.z.sign

        while (current != end) {
            when {
                validMove(dx, dy, dz, rayLength.x, rayLength.z, rayLength.y) -> {
                    current.x += stepX
                    rayLength.x += dx
                }

                validMove(dy, dx, dz, rayLength.y, rayLength.z, rayLength.x) -> {
                    current.y += stepY
                    rayLength.y += dy
                }

                else -> {
                    current.z += stepZ
                    rayLength.z += dz
                }
            }
            yield(VoxelKey(current.x, current.y, current.z))
        }
    }

    private fun validMove(
        dirInA: Double,
        dirInC: Double,
        dirInB: Double,
        rayLengthA: Double,
        rayLengthB: Double,
        rayLengthC: Double,
    ) = (dirInA != 0.0) // there is no step in this direction
            && ((dirInC != 0.0 && dirInB != 0.0 && rayLengthA <= rayLengthC && rayLengthA <= rayLengthB) // 3d dda
            || (dirInC == 0.0 && rayLengthA <= rayLengthB) // 2d dda in AB plane
            || (dirInB == 0.0 && rayLengthA <= rayLengthC) // 2d dda in AC plane
            || (dirInC == 0.0 && dirInB == 0.0)) // 1d dda in A plane

    // https://gamedev.stackexchange.com/questions/185569/how-to-check-if-two-normals-directions-look-at-each-other
    fun canSeeEachOther(
        first: VoxelKey, firstNormalVector: VoxelKey, second: VoxelKey, secondNormalVector: VoxelKey
    ): Boolean {
        val delta = second - first
        val dp0 = delta.dotProduct(firstNormalVector)
        val dp1 = delta.dotProduct(secondNormalVector)
        return dp0 > 0 && dp1 < 0
    }

    private fun canSeeEachOther(first: RadiationPlane, second: RadiationPlane) = canSeeEachOther(
        first.middle, first.normalVector, second.middle, second.normalVector
    )

    //    based on http://imartinez.etsiae.upm.es/~isidoro/tc3/Radiation%20View%20factors.pdf
    fun parallelViewFactor(first: RadiationPlane, second: RadiationPlane): Double {
        val x = mutableListOf<Double>()
        val y = mutableListOf<Double>()
        val n = mutableListOf<Double>()
        val e = mutableListOf<Double>()
        val z: Double

        when {
            // squares have sides in XY axis (so normal.z !=)
            first.normalVector.z != 0 && second.normalVector.z != 0 -> {
                x.addAll(uniqueCoordinate(first) { it.x })
                y.addAll(uniqueCoordinate(first) { it.y })
                e.addAll(uniqueCoordinate(second) { it.x })
                n.addAll(uniqueCoordinate(second) { it.y })
                // all points in first/second are on the same Z
                z = abs(first.a.z - second.a.z).toDouble()
            }
            // squares have sides in YZ axis (so normal.x != 0)
            first.normalVector.x != 0 && second.normalVector.x != 0 -> {
                x.addAll(uniqueCoordinate(first) { it.y })
                y.addAll(uniqueCoordinate(first) { it.z })
                e.addAll(uniqueCoordinate(second) { it.y })
                n.addAll(uniqueCoordinate(second) { it.z })
                // all points in first/second are on the same X
                z = abs(first.a.x - second.a.x).toDouble()
            }
            // squares have sides in XZ axis (so normal.y != 0)
            first.normalVector.y != 0 && second.normalVector.y != 0 -> {
                x.addAll(uniqueCoordinate(first) { it.z })
                y.addAll(uniqueCoordinate(first) { it.x })
                e.addAll(uniqueCoordinate(second) { it.z })
                n.addAll(uniqueCoordinate(second) { it.x })
                // all points in first/second are on the same Y
                z = abs(first.a.y - second.a.y).toDouble()
            }

            else -> return 0.0
        }
        val A1 = 1 / (2 * PI * unitlessArea(x[0], x[1], y[0], y[1]))
        return A1 * (1..2).sumOf { i ->
            (1..2).sumOf { j ->
                (1..2).sumOf { k ->
                    (1..2).sumOf { l ->
                        (-1.0).pow(i + j + k + l.toDouble()) * parallelIteratorFunction(
                            x[i - 1], y[j - 1], n[k - 1], e[l - 1], z
                        )
                    }
                }
            }
        }
    }

    private fun uniqueCoordinate(first: RadiationPlane, f: (VoxelKey) -> Int) =
        uniqueCoordinate(first.a, first.b, first.c, first.d, f)

    private fun uniqueCoordinate(a: VoxelKey, b: VoxelKey, c: VoxelKey, d: VoxelKey, f: (VoxelKey) -> Int) =
        listOf(f(a), f(b), f(c), f(d)).distinct().let { if (it.size == 1) listOf(it[0], it[0]) else it }
            .let { if (it[0] > it[1]) listOf(it[1].toDouble(), it[0] + 1.0) else listOf(it[0].toDouble(), it[1] + 1.0) }

    private fun parallelIteratorFunction(x: Double, y: Double, n: Double, e: Double, z: Double): Double {
        val u = x - e
        val v = y - n
        val p = hypot(u, z)
        val q = hypot(v, z)
        return v * p * atan(v / p) + u * q * atan(u / q) - 0.5 * z * z * ln(u * u + v * v + z * z)
    }

    fun perpendicularViewFactor(first: RadiationPlane, second: RadiationPlane): Double {
        var x = mutableListOf<Double>()
        val y = mutableListOf<Double>()
        val n = mutableListOf<Double>()
        var e = mutableListOf<Double>()

        when {
            // first is horizontal with sides in XY, second is vertical with sides in YZ
            first.normalVector.z != 0 && second.normalVector.x != 0 -> {
                x.addAll(uniqueCoordinate(first) { it.x })
                y.addAll(uniqueCoordinate(first) { it.y })
                e.addAll(uniqueCoordinate(second) { it.z })
                n.addAll(uniqueCoordinate(second) { it.y })

                x = x.map { abs(it - second.a.x) }.sorted().toMutableList()
                e = e.map { abs(it - first.a.z) }.sorted().toMutableList()
            }
            // first is vertical with sides in XZ, second is horizontal with sides in XY
            first.normalVector.y != 0 && second.normalVector.z != 0 -> {
                x.addAll(uniqueCoordinate(first) { it.z })
                y.addAll(uniqueCoordinate(first) { it.x })
                e.addAll(uniqueCoordinate(second) { it.y })
                n.addAll(uniqueCoordinate(second) { it.x })

                x = x.map { abs(it - second.a.z) }.sorted().toMutableList()
                e = e.map { abs(it - first.a.y) }.sorted().toMutableList()
            }
            // first is horizontal with sides in YZ, second is vertical with sides in XZ
            first.normalVector.x != 0 && second.normalVector.y != 0 -> {
                x.addAll(uniqueCoordinate(first) { it.y })
                y.addAll(uniqueCoordinate(first) { it.z })
                e.addAll(uniqueCoordinate(second) { it.x })
                n.addAll(uniqueCoordinate(second) { it.z })

                x = x.map { abs(it - second.a.y) }.sorted().toMutableList() // - min(x[0], x[1], second.a.y)
                e = e.map { abs(it - first.a.x) }.sorted().toMutableList()
            }
            // and symmetry for cases above

            // second is horizontal with sides in XY
            // first is vertical with sides in YZ
            first.normalVector.x != 0 && second.normalVector.z != 0 -> {
                x.addAll(uniqueCoordinate(first) { it.z })
                y.addAll(uniqueCoordinate(first) { it.y })
                e.addAll(uniqueCoordinate(second) { it.x })
                n.addAll(uniqueCoordinate(second) { it.y })

                x = x.map { abs(it - second.a.z) }.sorted().toMutableList()
                e = e.map { abs(it - first.a.x) }.sorted().toMutableList()
            }
            // second is horizontal with sides in XZ
            // first is vertical with sides in XY
            first.normalVector.z != 0 && second.normalVector.y != 0 -> {
                x.addAll(uniqueCoordinate(first) { it.y })
                y.addAll(uniqueCoordinate(first) { it.x })
                e.addAll(uniqueCoordinate(second) { it.z })
                n.addAll(uniqueCoordinate(second) { it.x })

                x = x.map { abs(it - second.a.y) }.sorted().toMutableList()
                e = e.map { abs(it - first.a.z) }.sorted().toMutableList()
            }
            // second is horizontal with sides in YZ
            // first is vertical with sides in XZ
            first.normalVector.y != 0 && second.normalVector.x != 0 -> {
                x.addAll(uniqueCoordinate(first) { it.x })
                y.addAll(uniqueCoordinate(first) { it.z })
                e.addAll(uniqueCoordinate(second) { it.y })
                n.addAll(uniqueCoordinate(second) { it.z })

                x = x.map { abs(it - second.a.x) }.sorted().toMutableList()
                e = e.map { abs(it - first.a.y) }.sorted().toMutableList()
            }
        }

        val A1 = 1 / (2 * PI * unitlessArea(x[0], x[1], y[0], y[1]))
        return A1 * (1..2).sumOf { i ->
            (1..2).sumOf { j ->
                (1..2).sumOf { k ->
                    (1..2).sumOf { l ->
                        (-1.0).pow(i + j + k + l.toDouble()) * perpendicularIteratorFunction(
                            x[i - 1], y[j - 1], n[k - 1], e[l - 1]
                        )
                    }
                }
            }
        }
    }

    private fun perpendicularIteratorFunction(x: Double, y: Double, n: Double, e: Double): Double {
        var C = hypot(x, e)
        if (C == 0.0) C = 10e-6 // to avoid dividing by zero
        val D = (y - n) / C
        return (y - n) * C * atan(D) - 0.25 * C * C * (1 - D * D) * ln(C * C * (1 + D * D))
    }

    private fun perpendicularSecond(p: Double, q: Double, r: Double, s: Double): Double {
        val a = q - r
        if (a == 0.0) return perpendicularSharedSide(p, q, s)
        val b = q + r
        val c = hypot(p, s)
        val A = a / c
        assert(A != 0.0)
        val A2 = A * A
        val B = b / c
        val B2 = B * B
        val P = p / c
        val P2 = P * P
        val S = s / c
        val S2 = S * S

        val denom = PI * P * (A + B)
        val pt1 = atan(A) - P * atan(A / P) - S * atan(A / S)
        val pt2 = atan(B) - P * atan(B / P) - S * atan(B / S)

        val pt3_1 = ln((1 + A2) / (1 + B2))
        val pt3_2 = A2 * ln(((P2 + A2) * (S2 + A2)) / (A2 + A2 * A2))
        val pt3_3 = B2 * ln((B2 + B2 * B2) / ((P2 + B2) * (S2 + B2)))
        val pt3_4 = P2 * ln((P2 + B2) / (P2 + A2))
        val pt3_5 = S2 * ln((S2 + B2) / (S2 + A2))

        val pt3 = pt3_1 + pt3_2 + pt3_3 + pt3_4 + pt3_5

        return (A * pt1 - B * pt2 - pt3 / 4) / denom
    }

    private fun perpendicularSharedSide(X: Double, Y: Double, Z: Double): Double {
        val H = Z / Y
        val W = X / Y
        val H2 = H * H
        val W2 = W * W
        val C2 = H2 + W2
        val C = sqrt(C2)

        val denom = PI * W
        val pt1 = W * atan(1 / W) - C * atan(1 / C) + H * atan(1 / H)

        val pt2_1 = (1 + W2) * (1 + H2) / (1 + C2)
        val pt2_2 = pow((W2 * (1 + C2) / (1 + W2) * C2), W2)
        val pt2_3 = pow((H2 * (1 + C2) / (1 + H2) * C2), H2)

        val pt2 = 0.25 * ln(pt2_1 * pt2_2 * pt2_3)
        return (pt1 + pt2) / denom
    }

}

private operator fun Array<Array<IntArray>>.get(key: VoxelKey): Int = this[key.x][key.y][key.z]

private fun Int.sq() = this * this.toDouble()

data class Vector(var x: Double, var y: Double, var z: Double)