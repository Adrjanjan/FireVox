package pl.edu.agh.firevox.shared.model.radiation

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.doubles.shouldBeLessThan
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import pl.edu.agh.firevox.shared.model.*
import kotlin.math.pow

class PlaneFinderTest : ShouldSpec({
    // given
    val voxelRepository = mockk<VoxelRepository>()
    val physicalMaterialRepository = mockk<PhysicalMaterialRepository>()
    val planeFinder = PlaneFinder(0.01, 10, physicalMaterialRepository)

    val material = PhysicalMaterial(
        VoxelMaterial.METAL,
        density = 2700.0,
        baseTemperature = 20.toKelvin(),
        thermalConductivityCoefficient = 235.0,
        convectionHeatTransferCoefficient = 0.0,
        specificHeatCapacity = 897.0,
        ignitionTemperature = null,
        timeToIgnition = null,
        autoignitionTemperature = null,
        burningTime = null,
        effectiveHeatOfCombustion = null,
        smokeEmissionPerSecond = null,
        deformationTemperature = 700.toKelvin()
    )
    var wallId = 0

    should("findPlanes") {
        // given 7x7x7 matrix with walls at
        //1 x == 0 -> 49 voxels
        //2 x == 3 -> 49
        //3 x == 5 && z == 0 -> 7
        //4 x == 5 && z == 6 -> 7
        //5 y == 6 && 4<x<6 && 2<z<4 ->
        val size = 7
        val matrix = Array(size) { i ->
            Array(size) { j ->
                IntArray(size) { k ->
                    when {
                        i == 0 -> 1
                        i == 3 -> 2
                        i == 5 && (k == 0) -> 3
                        i == 5 && k == 6 -> 4
                        j == 6 && i in 4..6 && k in 2..4 -> 5
                        else -> 0
                    }.also {
                        every { voxelRepository.getReferenceById(VoxelKey(i, j, k)) } returns Voxel(
                            VoxelKey(i, j, k),
                            material,
                            0.0,
                            material,
                            0.0
                        )
                    }
                }
            }
        }

        // when
        val planes = planeFinder.findPlanes(
            matrix, listOf(
                VoxelKey(0, 0, 0) to VoxelKey(1, 0, 0),   // 1
                VoxelKey(3, 0, 0) to VoxelKey(-1, 0, 0),  // 2
                VoxelKey(5, 0, 0) to VoxelKey(0, 0, 1),   // 3
                VoxelKey(5, 0, 6) to VoxelKey(0, 0, -1),  // 4
                VoxelKey(6, 6, 2) to VoxelKey(0, -1, 0),  // 5
            )
        )

        // then
        // should find connections 1 <-> 2,  3 <-> 4, 3 <-> 5, 4 <-> 5
        planes.size shouldBe 5

        planes[0].childPlanes[0].child shouldBe planes[1]
        planes[1].childPlanes[0].child shouldBe planes[0]

        planes[2].childPlanes[0].child shouldBe planes[3]
        planes[2].childPlanes[1].child shouldBe planes[4]

        planes[3].childPlanes[0].child shouldBe planes[2]
        planes[3].childPlanes[1].child shouldBe planes[4]

        planes[4].childPlanes[0].child shouldBe planes[2]
        planes[4].childPlanes[1].child shouldBe planes[3]
    }

    context("fullPlane") {
        should("find wall of cube ") {
            // given 5x5x5 matrix with 3x3x3 cube in the middle
            val size = 5
            val middleSize = 3
            val matrix = Array(size) { i ->
                Array(size) { j ->
                    IntArray(size) { k ->
                        if (i in (size - middleSize) / 2 until (size + middleSize) / 2 &&
                            j in (size - middleSize) / 2 until (size + middleSize) / 2 &&
                            k in (size - middleSize) / 2 until (size + middleSize) / 2
                        ) {
                            1
                        } else {
                            0
                        }
                    }
                }
            }
            // when
            val eastWall = planeFinder.fullPlane(matrix, VoxelKey(3, 1, 3) to VoxelKey(1, 0, 0))
            // then
            eastWall.toSet() shouldBe setOf(
                VoxelKey(3, 1, 1),
                VoxelKey(3, 2, 1),
                VoxelKey(3, 3, 1),
                VoxelKey(3, 1, 2),
                VoxelKey(3, 2, 2),
                VoxelKey(3, 3, 2),
                VoxelKey(3, 1, 3),
                VoxelKey(3, 2, 3),
                VoxelKey(3, 3, 3),
            )

            // when
            val westWall = planeFinder.fullPlane(matrix, VoxelKey(1, 1, 3) to VoxelKey(-1, 0, 0))
            // then
            westWall.toSet() shouldBe setOf(
                VoxelKey(1, 1, 1),
                VoxelKey(1, 2, 1),
                VoxelKey(1, 3, 1),
                VoxelKey(1, 1, 2),
                VoxelKey(1, 2, 2),
                VoxelKey(1, 3, 2),
                VoxelKey(1, 1, 3),
                VoxelKey(1, 2, 3),
                VoxelKey(1, 3, 3),
            )
        }
        should("find irregular face") {
            // given 5x5x5 matrix with 3x3x3 cube in the middle
            val size = 5
            val middleSize = 3
            val matrix = Array(size) { i ->
                Array(size) { j ->
                    IntArray(size) { k ->
                        if (i in (size - middleSize) / 2 until (size + middleSize) / 2 &&
                            j in (size - middleSize) / 2 until (size + middleSize) / 2 &&
                            k in (size - middleSize) / 2 until (size + middleSize) / 2
                        ) {
                            1
                        } else {
                            if (i == 3 && j == 0 && k == 2) 1 else 0
                        }
                    }
                }
            }
            // when
            val eastWall = planeFinder.fullPlane(matrix, VoxelKey(3, 1, 3) to VoxelKey(1, 0, 0))
            // then
            eastWall.toSet() shouldBe setOf(
                VoxelKey(3, 1, 1),
                VoxelKey(3, 2, 1),
                VoxelKey(3, 3, 1),
                VoxelKey(3, 1, 2),
                VoxelKey(3, 2, 2),
                VoxelKey(3, 3, 2),
                VoxelKey(3, 1, 3),
                VoxelKey(3, 2, 3),
                VoxelKey(3, 3, 3),
                VoxelKey(3, 0, 2),
            )
        }
    }

    should("divideIntoPlanes") {
        val fullPlane = listOf(
            VoxelKey(3, 1, 1),
            VoxelKey(3, 2, 1),
            VoxelKey(3, 3, 1),
            VoxelKey(3, 1, 2),
            VoxelKey(3, 2, 2),
            VoxelKey(3, 3, 2),
            VoxelKey(3, 1, 3),
            VoxelKey(3, 2, 3),
            VoxelKey(3, 3, 3),
            VoxelKey(3, 1, 4),
            VoxelKey(3, 2, 4),
            VoxelKey(3, 3, 4),
            VoxelKey(3, 1, 5),
            VoxelKey(3, 2, 5),
            VoxelKey(3, 3, 5),
            VoxelKey(3, 1, 6),
            VoxelKey(3, 2, 6),
            VoxelKey(3, 3, 6),
        )
        val normalVector = VoxelKey(1, 0, 0)
        val squareSize = 2


        fullPlane.forEach {
            every { voxelRepository.getReferenceById(it) } returns Voxel(it, material, 0.0, material, 0.0)
        }

        // when
        val air = PhysicalMaterial(
            voxelMaterial = VoxelMaterial.AIR,
            density = 1.204,
            baseTemperature = 20.0,
            thermalConductivityCoefficient = 25.87,
            convectionHeatTransferCoefficient = 0.0,
            specificHeatCapacity = 1.0061,
            ignitionTemperature = null,
            timeToIgnition = null,
            autoignitionTemperature = null,
            burningTime = null,
            effectiveHeatOfCombustion = null,
            smokeEmissionPerSecond = null,
            deformationTemperature = null
        )

        every { physicalMaterialRepository.findAll() } returns listOf(air)

        val planes = planeFinder.divideIntoPlanes(
            wallId++,
            fullPlane,
            normalVector,
            squareSize
        )

        // then
        val firstPlane = planes[0]
        firstPlane.a shouldBe VoxelKey(3, 1, 1)
        firstPlane.b shouldBe VoxelKey(3, 1, 2)
        firstPlane.c shouldBe VoxelKey(3, 2, 1)
        firstPlane.d shouldBe VoxelKey(3, 2, 2)
        firstPlane.normalVector shouldBe VoxelKey(1, 0, 0)
        firstPlane.voxels.size shouldBe squareSize * squareSize
        firstPlane.area shouldBe 4 * 0.01.pow(2)
    }

    context("ddaStep") {
        should("in all dimensions") {
            val start = VoxelKey(0, 0, 0)
            val end = VoxelKey(2, 4, 6)
            val results = mutableListOf<VoxelKey>()
            for (i in planeFinder.ddaStep(start, end)) {
                results.add(i)
            }
            results shouldBe listOf(
                VoxelKey(1, 0, 0),
                VoxelKey(1, 1, 0),
                VoxelKey(1, 1, 1),
                VoxelKey(1, 1, 2),
                VoxelKey(1, 2, 2),
                VoxelKey(1, 2, 3),
                VoxelKey(1, 2, 4),
                VoxelKey(2, 2, 4),
                VoxelKey(2, 3, 4),
                VoxelKey(2, 3, 5),
                VoxelKey(2, 4, 5),
                VoxelKey(2, 4, 6),
            )
        }

        should("reversed") {
            val start = VoxelKey(2, 4, 6)
            val end = VoxelKey(0, 0, 0)

            val results = mutableListOf<VoxelKey>()
            for (i in planeFinder.ddaStep(start, end)) {
                results.add(i)
            }
            results shouldBe listOf(
                VoxelKey(1, 4, 6),
                VoxelKey(1, 3, 6),
                VoxelKey(1, 3, 5),
                VoxelKey(1, 3, 4),
                VoxelKey(1, 2, 4),
                VoxelKey(1, 2, 3),
                VoxelKey(1, 2, 2),
                VoxelKey(0, 2, 2),
                VoxelKey(0, 1, 2),
                VoxelKey(0, 1, 1),
                VoxelKey(0, 0, 1),
                VoxelKey(0, 0, 0),
            )
        }

        should("in 1d") {
            val start = VoxelKey(0, 0, 6)
            val end = VoxelKey(0, 0, 0)

            val results = mutableListOf<VoxelKey>()
            for (i in planeFinder.ddaStep(start, end)) {
                results.add(i)
            }
            results shouldBe listOf(
                VoxelKey(0, 0, 5),
                VoxelKey(0, 0, 4),
                VoxelKey(0, 0, 3),
                VoxelKey(0, 0, 2),
                VoxelKey(0, 0, 1),
                VoxelKey(0, 0, 0),
            )
        }

        should("in 2d") {
            val start = VoxelKey(0, 0, 0)
            val end = VoxelKey(0, 4, 6)

            val results = mutableListOf<VoxelKey>()
            for (i in planeFinder.ddaStep(start, end)) {
                results.add(i)
            }
            results shouldBe listOf(
                VoxelKey(0, 1, 0),
                VoxelKey(0, 1, 1),
                VoxelKey(0, 1, 2),
                VoxelKey(0, 2, 2),
                VoxelKey(0, 2, 3),
                VoxelKey(0, 3, 3),
                VoxelKey(0, 3, 4),
                VoxelKey(0, 3, 5),
                VoxelKey(0, 4, 5),
                VoxelKey(0, 4, 6),
            )
        }


    }

    should("canSeeEachOther") {
        // "v----------"
        // "^----------"
        planeFinder.canSeeEachOther(
            VoxelKey(5, 5, 0), VoxelKey(0, 0, 1),
            VoxelKey(5, 5, 15), VoxelKey(0, 0, -1)
        ) shouldBe true

        // "^---------"
        // "v---------"
        planeFinder.canSeeEachOther(
            VoxelKey(5, 5, 0), VoxelKey(0, 0, -1),
            VoxelKey(5, 5, 15), VoxelKey(0, 0, 1)
        ) shouldBe false

        // ">---------"
        // "-<--------"
        planeFinder.canSeeEachOther(
            VoxelKey(5, 5, 5), VoxelKey(0, 1, 0),
            VoxelKey(5, 15, 0), VoxelKey(0, -1, 0)
        ) shouldBe true

        // "<---------"
        // "->--------"
        planeFinder.canSeeEachOther(
            VoxelKey(5, 5, 5), VoxelKey(0, -1, 0),
            VoxelKey(5, 15, 0), VoxelKey(0, 1, 0)
        ) shouldBe false

        // ">---------"
        // "-^--------"
        planeFinder.canSeeEachOther(
            VoxelKey(5, 5, 5), VoxelKey(0, 1, 0),
            VoxelKey(10, 10, 0), VoxelKey(0, 0, 1)
        ) shouldBe true

        // "<---------"
        // "-^--------"
        planeFinder.canSeeEachOther(
            VoxelKey(5, 5, 5), VoxelKey(0, -1, 0),
            VoxelKey(10, 10, 0), VoxelKey(0, 0, 1)
        ) shouldBe false

    }

    should("parallelViewFactor") {
        val fullPlane = listOf(
            VoxelKey(0, 0, 0),
            VoxelKey(0, 1, 0),
            VoxelKey(1, 0, 0),
            VoxelKey(1, 1, 0),
        )

        val voxels = fullPlane.map {
            Voxel(it, material, 0.0, oddIterationMaterial = material, oddIterationTemperature = 0.0)
        }

        val firstPlane = RadiationPlane(
            wallId++,
            a = VoxelKey(0, 0, 0),
            b = VoxelKey(0, 1, 0),
            c = VoxelKey(1, 0, 0),
            d = VoxelKey(1, 1, 0),
            normalVector = VoxelKey(0, 0, 1),
            voxels = voxels.map(Voxel::key).toMutableSet(),
            voxelsCount = voxels.size,
            area = 0.04,
            fullPlane = fullPlane,
        )

        val secondPlane = RadiationPlane(
            wallId++,
            a = VoxelKey(0, 0, 1),
            b = VoxelKey(0, 1, 1),
            c = VoxelKey(1, 0, 1),
            d = VoxelKey(1, 1, 1),
            normalVector = VoxelKey(0, 0, -1),
            voxels = voxels.map(Voxel::key).toMutableSet(),
            voxelsCount = voxels.size,
            area = 4.0,
            fullPlane = fullPlane,
        )

        // when
        val result = planeFinder.parallelViewFactor(firstPlane, secondPlane)

        // then
//        https://kanamesasaki.github.io/viewfactor/
        result shouldBeGreaterThan 0.4152531
        result shouldBeLessThan 0.4152533
    }

    should("perpendicularViewFactor") {
        val fullPlane = listOf(
            VoxelKey(0, 0, 0),
            VoxelKey(0, 1, 0),
            VoxelKey(1, 0, 0),
            VoxelKey(1, 1, 0),
        )
        val voxels = fullPlane.map {
            Voxel(it, material, 0.0, oddIterationMaterial = material, oddIterationTemperature = 0.0)
        }

        val firstPlane = RadiationPlane(
            wallId++,
            a = VoxelKey(0, 0, 0),
            b = VoxelKey(0, 1, 0),
            c = VoxelKey(1, 0, 0),
            d = VoxelKey(1, 1, 0),
            normalVector = VoxelKey(0, 0, 1),
            voxels = voxels.map(Voxel::key).toMutableSet(),
            voxelsCount = voxels.size,
            area = 1.0,
            fullPlane = fullPlane,
        )

        val secondPlane = RadiationPlane(
            wallId++,
            a = VoxelKey(0, 0, 0),
            b = VoxelKey(0, 0, 1),
            c = VoxelKey(0, 1, 0),
            d = VoxelKey(0, 1, 1),
            normalVector = VoxelKey(1, 0, 0),
            voxels = voxels.map(Voxel::key).toMutableSet(),
            voxelsCount = voxels.size,
            area = 4.0,
            fullPlane = fullPlane,
        )

        // when
        val result = planeFinder.perpendicularViewFactor(firstPlane, secondPlane)

        // then
        result shouldBeGreaterThan 0.20004
        result shouldBeLessThan 0.20005
    }

    should("perpendicularViewFactor2") {
        val fullPlane = listOf(
            VoxelKey(0, 0, 0),
            VoxelKey(0, 10, 0),
            VoxelKey(0, 0, 1),
            VoxelKey(0, 10, 11),
        )
        val voxels = fullPlane.map {
            Voxel(it, material, 0.0, oddIterationMaterial = material, oddIterationTemperature = 0.0)
        }

        val firstPlane = RadiationPlane(
            wallId++,
            a = VoxelKey(0, 0, 1),
            b = VoxelKey(0, 0, 10),
            c = VoxelKey(0, 9, 1),
            d = VoxelKey(0, 9, 10),
            normalVector = VoxelKey(1, 0, 0),
            voxels = voxels.map(Voxel::key).toMutableSet(),
            voxelsCount = voxels.size,
            area = 0.01,
            fullPlane = fullPlane,
        )

        val secondPlane = RadiationPlane(
            wallId++,
            a = VoxelKey(1, 99, 1),
            b = VoxelKey(1, 99, 10),
            c = VoxelKey(10, 99, 1),
            d = VoxelKey(10, 99, 10),
            normalVector = VoxelKey(0, -1, 0),
            voxels = voxels.map(Voxel::key).toMutableSet(),
            voxelsCount = voxels.size,
            area = 0.01,
            fullPlane = fullPlane,
        )

        // when
        val result = planeFinder.perpendicularViewFactor(firstPlane, secondPlane)

        // then
        result shouldBeGreaterThan 0.162822719236895
        result shouldBeLessThan 0.162822719236897
    }

    should("perpendicularViewFactor3") {
        val fullPlane = listOf(
            VoxelKey(0, 0, 1),
            VoxelKey(0, 0, 10),
            VoxelKey(0, 9, 1),
            VoxelKey(0, 9, 10),
        )
        val voxels = fullPlane.map {
            Voxel(it, material, 0.0, oddIterationMaterial = material, oddIterationTemperature = 0.0)
        }

        val firstPlane = RadiationPlane(
            wallId++,
            a = VoxelKey(0, 0, 1),
            b = VoxelKey(0, 0, 10),
            c = VoxelKey(0, 9, 1),
            d = VoxelKey(0, 9, 10),
            normalVector = VoxelKey(1, 0, 0),
            voxels = voxels.map(Voxel::key).toMutableSet(),
            voxelsCount = voxels.size,
            area = 0.01,
            fullPlane = fullPlane,
        )

        val secondPlane = RadiationPlane(
            wallId++,
            a = VoxelKey(99, 0, 1),
            b = VoxelKey(99, 0, 98),
            c = VoxelKey(99, 98, 1),
            d = VoxelKey(99, 98, 98),
            normalVector = VoxelKey(-1, 0, 0),
            voxels = voxels.map(Voxel::key).toMutableSet(),
            voxelsCount = voxels.size,
            area = 0.97,
            fullPlane = fullPlane,
        )

        // when
        val result = planeFinder.parallelViewFactor(firstPlane, secondPlane)

        // then
        result shouldBeGreaterThan 0.20004
        result shouldBeLessThan 0.20005
    }

    should("ZFaulty") {
        val fullPlane = listOf<VoxelKey>()
        val voxels = fullPlane.map {
            Voxel(it, material, 0.0, oddIterationMaterial = material, oddIterationTemperature = 0.0)
        }

        val firstPlane = RadiationPlane(
            wallId++,
            a = VoxelKey(1, 99, 91),
            b = VoxelKey(1, 99, 98),
            c = VoxelKey(10, 99, 91),
            d = VoxelKey(10, 99, 98),
            normalVector = VoxelKey(0, -1, 0),
            voxels = voxels.map(Voxel::key).toMutableSet(),
            voxelsCount = voxels.size,
            area = 0.01,
            fullPlane = fullPlane,
        )

        val secondPlane = RadiationPlane(
            wallId++,
            a = VoxelKey(11, 0, 0),
            b = VoxelKey(11, 9, 0),
            c = VoxelKey(20, 0, 0),
            d = VoxelKey(20, 9, 0),
            normalVector = VoxelKey(0, 0, 1),
            voxels = voxels.map(Voxel::key).toMutableSet(),
            voxelsCount = voxels.size,
            area = 0.97,
            fullPlane = fullPlane,
        )

        // when
        val result = planeFinder.perpendicularViewFactor(firstPlane, secondPlane)

        // then
        result shouldBeGreaterThan 0.20004
        result shouldBeLessThan 0.20005
    }

    should("ZFaulty2") {
        val fullPlane = listOf<VoxelKey>()
        val voxels = fullPlane.map {
            Voxel(it, material, 0.0, oddIterationMaterial = material, oddIterationTemperature = 0.0)
        }

        val secondPlane = RadiationPlane(
            wallId++,
            a = VoxelKey(1, 99, 91),
            b = VoxelKey(1, 99, 98),
            c = VoxelKey(10, 99, 91),
            d = VoxelKey(10, 99, 98),
            normalVector = VoxelKey(0, -1, 0),
            voxels = voxels.map(Voxel::key).toMutableSet(),
            voxelsCount = voxels.size,
            area = 0.01,
            fullPlane = fullPlane,
        )

        val firstPlane = RadiationPlane(
            wallId++,
            a = VoxelKey(11, 0, 0),
            b = VoxelKey(11, 9, 0),
            c = VoxelKey(20, 0, 0),
            d = VoxelKey(20, 9, 0),
            normalVector = VoxelKey(0, 0, 1),
            voxels = voxels.map(Voxel::key).toMutableSet(),
            voxelsCount = voxels.size,
            area = 0.97,
            fullPlane = fullPlane,
        )

        // when
        val result = planeFinder.perpendicularViewFactor(firstPlane, secondPlane)

        // then
        result shouldBeGreaterThan 0.20004
        result shouldBeLessThan 0.20005
    }

})
