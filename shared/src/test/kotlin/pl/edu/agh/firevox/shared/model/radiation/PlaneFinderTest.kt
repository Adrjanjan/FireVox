package pl.edu.agh.firevox.shared.model.radiation

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import pl.edu.agh.firevox.shared.model.*
import kotlin.math.pow

class PlaneFinderTest : ShouldSpec({
    // given
    val radiationPlaneRepository = mockk<RadiationPlaneRepository>()
    val voxelRepository = mockk<VoxelRepository>()
    val planeFinder = PlaneFinder(radiationPlaneRepository, voxelRepository)

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
            VoxelKey(3, 0, 2),
        )
        val normalVector = VoxelKey(1, 0, 0)
        val squareSize = 3

        val material = PhysicalMaterial(
            VoxelMaterial.METAL,
            density = 2700.0,
            baseTemperature = 20.toKelvin(),
            thermalConductivityCoefficient = 235.0,
            convectionHeatTransferCoefficient = 0.0,
            specificHeatCapacity = 897.0,
            flashPointTemperature = 0.0.toKelvin(),
            burningTime = 0.0,
            generatedEnergyDuringBurning = 0.0,
            burntMaterial = null,
        )

        fullPlane.forEach {
            every { voxelRepository.getReferenceById(it) } returns Voxel(it, 0, material, 0.0, 0, material, 0.0)
        }

        val voxels = fullPlane.map {
            Voxel(it, 0, material, 0.0, 0, material, 0.0)
        }

        // when
        val planes = planeFinder.divideIntoPlanes(fullPlane, normalVector, squareSize)

        // then
        val firstPlane = planes[0]
        firstPlane.a shouldBe VoxelKey(3, 1, 1)
        firstPlane.b shouldBe VoxelKey(3, 1, 3)
        firstPlane.c shouldBe VoxelKey(3, 3, 1)
        firstPlane.d shouldBe VoxelKey(3, 3, 3)
        firstPlane.normalVector shouldBe VoxelKey(1, 0, 0)
        firstPlane.voxels shouldBe voxels.toMutableSet()
        firstPlane.area shouldBe 9 * 0.01.pow(2)
    }

    should("findRelationships") { TODO() }

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
                VoxelKey(1, 3, 4),
                VoxelKey(2, 3, 4),
                VoxelKey(2, 3, 5),
                VoxelKey(2, 3, 6),
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
                VoxelKey(1, 1, 2),
                VoxelKey(0, 1, 2),
                VoxelKey(0, 1, 1),
                VoxelKey(0, 1, 0),
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
                VoxelKey(0, 2, 4),
                VoxelKey(0, 3, 4),
                VoxelKey(0, 3, 5),
                VoxelKey(0, 3, 6),
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

    should("parallelViewFactor") { TODO() }

    should("perpendicularViewFactor") { TODO() }

})
