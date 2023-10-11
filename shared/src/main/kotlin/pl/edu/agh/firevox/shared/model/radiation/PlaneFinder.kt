package pl.edu.agh.firevox.shared.model.radiation

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import pl.edu.agh.firevox.shared.model.VoxelKey
import pl.edu.agh.firevox.shared.model.VoxelRepository
import kotlin.math.*

@Service
class PlaneFinder(
    private val voxelRepository: VoxelRepository,
    @Value("\${firevox.voxel.size}") val voxelLength: Double = 0.01,
) {

    fun findPlanes(
        voxels: Array<Array<IntArray>>,
        pointsToNormals: List<Pair<VoxelKey, VoxelKey>>
    ): List<RadiationPlane> {
        val planes = mutableListOf<RadiationPlane>()
        pointsToNormals.forEach {
            planes.addAll(divideIntoPlanes(fullPlane(voxels, it), it.second, squareSize = 10))
        }
        val findRelationships = findRelationships(planes, voxels)
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
                    Triple(x + 1, y, z),
                    Triple(x - 1, y, z),
                    Triple(x, y + 1, z),
                    Triple(x, y - 1, z),
                )

                VoxelKey(0, 1, 0), VoxelKey(0, -1, 0) -> listOf(
                    Triple(x + 1, y, z), Triple(x - 1, y, z), Triple(x, y, z + 1), Triple(x, y, z - 1)
                )

                VoxelKey(1, 0, 0), VoxelKey(-1, 0, 0) -> listOf(
                    Triple(x, y + 1, z), Triple(x, y - 1, z), Triple(x, y, z + 1), Triple(x, y, z - 1)
                )

                else -> listOf()
            }

            for ((nx, ny, nz) in neighbors) {
                val key = VoxelKey(nx, ny, nz)
                if (isValidFaceElement(
                        nx, ny, nz, voxels
                    ) && voxels[nx][ny][nz] == voxels[startingPoint] && key !in visited
                ) {
                    stack.add(key)
                }
            }
        }

        return visited.toList()
    }

    fun isValidFaceElement(x: Int, y: Int, z: Int, matrix: Array<Array<IntArray>>): Boolean {
        return x >= 0 && x < matrix.size && y >= 0 && y < matrix[0].size && z >= 0 && z < matrix[0][0].size && matrix[x][y][z] != 0
    }

    fun divideIntoPlanes(
        fullPlane: List<VoxelKey>, normalVector: VoxelKey, squareSize: Int
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
        for (ix in 0 .. nx) {
            val xStartIndex = minX + ix * squareSize
            val xEndIndex = min(xStartIndex + squareSize, maxX + 1)
            if(xStartIndex > xEndIndex) break // only possible if end was truncated to maxX+1 but the iterations will make start bigger than end
            for (iy in 0 .. ny) {
                val yStartIndex = minY + iy * squareSize
                val yEndIndex = min(yStartIndex + squareSize, maxY + 1)
                if(yStartIndex > yEndIndex) break
                for (iz in 0 .. nz) {
                    val zStartIndex = minZ + iz * squareSize
                    val zEndIndex = min(zStartIndex + squareSize, maxZ + 1)
                    if(zStartIndex > zEndIndex) break

                    val voxels = mutableListOf<VoxelKey>()
                    for (key in fullPlane) {
                        if (
                            (key.x in (xStartIndex until xEndIndex) || key.x == xStartIndex) &&
                            (key.y in (yStartIndex until yEndIndex) || key.y == yStartIndex) &&
                            (key.z in (zStartIndex until zEndIndex) || key.z == zStartIndex)
                        ) {
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
                            a = voxels.find { it.x == xStartIndex + 1 && it.y == yStartIndex + 1 }!! // both start
                            b = voxels.find { it.x == xStartIndex + 1 && it.y == y }!! // second on start
                            c = voxels.find { it.x == x && it.y == yStartIndex + 1 }!! // one on end
                            d = voxels.find { it.x == x && it.y == y }!! // both end
                        }

                        normalVector.x != 0 -> { // plane in YZ
                            val y = voxels.maxOf { it.y }
                            val z = voxels.maxOf { it.z }
                            a = voxels.find { it.y == yStartIndex && it.z == zStartIndex }!! // both start
                            b = voxels.find { it.y == yStartIndex && it.z == z }!! // second on start
                            c = voxels.find { it.y == y && it.z == zStartIndex }!! // one on end
                            d = voxels.find { it.y == y && it.z == z }!! // both end
                        }

                        else -> { // plane in XZ
                            val x = voxels.maxOf { it.x }
                            val z = voxels.maxOf { it.z }
                            a = voxels.find { it.x == xStartIndex + 1 && it.z == zStartIndex + 1 }!! // both start
                            b = voxels.find { it.x == xStartIndex + 1 && it.z == z }!! // second on start
                            c = voxels.find { it.x == x && it.z == zStartIndex + 1 }!! // one on end
                            d = voxels.find { it.x == x && it.z == z }!! // both end
                        }
                    }

                    results.add(
                        RadiationPlane(
                            a = a,
                            b = b,
                            c = c,
                            d = d,
                            normalVector = normalVector,
                            voxels = voxels.map(voxelRepository::getReferenceById).toMutableSet(),
                            area = area(normalVector, a, b, c, d)
                        )
                    )
                }
            }
        }
        return results
    }

    fun findRelationships(planes: List<RadiationPlane>, voxels: Array<Array<IntArray>>): List<RadiationPlane> {
        for (first in planes) {
            for (second in planes) {
                if (second.childPlanes.map(PlanesConnection::child).contains(first)) continue

                if (canSeeEachOther(first, second)) {
                    if (!obstructedView(first.middle, second.middle, voxels)) {
                        val firstViewFactor = if (first.normalVector.dotProduct(second.normalVector) == 0) {
                            perpendicularViewFactor(first, second)
                        } else {
                            parallelViewFactor(first, second)
                        }
                        val secondViewFactor = first.area * firstViewFactor / second.area
                        first.childPlanes.add(
                            PlanesConnection(
                                parentPlane = first, child = second, viewFactor = firstViewFactor
                            )
                        )
                        second.childPlanes.add(
                            PlanesConnection(
                                parentPlane = first, child = second, viewFactor = secondViewFactor
                            )
                        )
                    }
                }
            }
        }
        return planes
    }

    private fun area(normalVector: VoxelKey, a: VoxelKey, b: VoxelKey, c: VoxelKey, d: VoxelKey): Double {
        val (m, n) = when {
            normalVector.z != 0 -> uniqueCoordinate(a, b, c, d) { it.x } to uniqueCoordinate(a, b, c, d) { it.y }
            normalVector.y != 0 -> uniqueCoordinate(a, b, c, d) { it.x } to uniqueCoordinate(a, b, c, d) { it.z }
            else -> uniqueCoordinate(a, b, c, d) { it.y } to uniqueCoordinate(a, b, c, d) { it.z }
        }
        return (abs(m[0] - m[1]) + 1) * (abs(n[0] - n[1]) + 1) * voxelLength.pow(2)
    }

    private fun obstructedView(start: VoxelKey, end: VoxelKey, voxels: Array<Array<IntArray>>): Boolean {
        for ((x, y, z) in ddaStep(start, end)) {
            if (voxels[x][y][z] != 0) return true
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
        da: Double,
        dc: Double,
        db: Double,
        a: Double,
        b: Double,
        c: Double,
    ) = (da != 0.0) // there is no step in this direction
            && ((dc != 0.0 && db != 0.0 && a <= c && a <= b) // 3d dda
            || (dc == 0.0 && a <= b) // 2d dda in AB plane
            || (db == 0.0 && a <= c) // 2d dda in AC plane
            )

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

        val d = 1 / (2 * PI * first.area) * (1..2).sumOf { i ->
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
        return d
    }

    private fun uniqueCoordinate(first: RadiationPlane, f: (VoxelKey) -> Int) =
        uniqueCoordinate(first.a, first.b, first.c, first.d, f)

    private fun uniqueCoordinate(a: VoxelKey, b: VoxelKey, c: VoxelKey, d: VoxelKey, f: (VoxelKey) -> Int) =
        listOf(f(a), f(b), f(c), f(d)).distinct().map(Int::toDouble).let { if(it.size == 1) listOf(it[0], it[0]) else it}

    private fun parallelIteratorFunction(x: Double, y: Double, n: Double, e: Double, z: Double): Double {
        val u = x - e
        val v = y - n
        val p = hypot(u, z)
        val q = hypot(v, z)
        return v * p * atan(v / p) + u * q * atan(u / q) - 0.5 * z * z * ln(u * u + v * v + z * z)
    }

    fun perpendicularViewFactor(first: RadiationPlane, second: RadiationPlane): Double {
        val x = mutableListOf<Double>()
        val y = mutableListOf<Double>()
        val n = mutableListOf<Double>()
        val e = mutableListOf<Double>()

        when {
            // first is horizontal with sides in XY, second is vertical with sides in YZ
            first.normalVector.z != 0 && second.normalVector.x != 0 -> {
                x.addAll(uniqueCoordinate(first) { it.x })
                y.addAll(uniqueCoordinate(first) { it.y })
                e.addAll(uniqueCoordinate(second) { it.z })
                n.addAll(uniqueCoordinate(second) { it.y })
            }
            // first is horizontal with sides in XZ, second is vertical with sides in XY
            first.normalVector.y != 0 && second.normalVector.z != 0 -> {
                x.addAll(uniqueCoordinate(first) { it.z })
                y.addAll(uniqueCoordinate(first) { it.x })
                e.addAll(uniqueCoordinate(second) { it.y })
                n.addAll(uniqueCoordinate(second) { it.x })
            }
            // first is horizontal with sides in YZ, second is vertical with sides in XZ
            first.normalVector.x != 0 && second.normalVector.y != 0 -> {
                x.addAll(uniqueCoordinate(first) { it.y })
                y.addAll(uniqueCoordinate(first) { it.z })
                e.addAll(uniqueCoordinate(second) { it.x })
                n.addAll(uniqueCoordinate(second) { it.z })
            }
        }

        return 1 / (2 * PI * first.area) * (1..2).sumOf { i ->
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
        if (C == 0.0) C = 10e-10 // to avoid dividing by zero
        val D = (y - n) / C
        return (y - n) * C * atan(D) - 0.25 * C * C * (1 - D * D) * ln(C * C * (1 + D * D))
    }

}

private operator fun Array<Array<IntArray>>.get(key: VoxelKey): Int = this[key.x][key.y][key.z]

private fun Int.sq() = this * this

data class Vector(var x: Double, var y: Double, var z: Double)