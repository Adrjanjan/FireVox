package pl.edu.agh.firevox.worker.physics

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.data.*
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import pl.edu.agh.firevox.shared.model.*
import pl.edu.agh.firevox.shared.model.VoxelMaterial.*

class PhysicsCalculatorTest : ShouldSpec({

    val voxelRepository = mockk<CustomVoxelRepository>()
    val calculator = PhysicsCalculator(voxelRepository)
    var rowCount = 0

    fun row(
        v: VoxelMaterial,
        upper: VoxelMaterial = AIR,
        lower: VoxelMaterial = AIR,
        north: VoxelMaterial = AIR,
        south: VoxelMaterial = AIR,
        east: VoxelMaterial = AIR,
        west: VoxelMaterial = AIR,
        expected: VoxelMaterial
    ) = Row1(
        TestData(
            Voxel(VoxelKey(1, 1, 1), StateProperties(0, v, 0), StateProperties(1, v, 0)), listOf(
                Voxel(VoxelKey(1, 1, 2), StateProperties(1, upper, 0), StateProperties(1, upper, 0)),
                Voxel(VoxelKey(1, 1, 0), StateProperties(1, lower, 0), StateProperties(1, lower, 0)),
                Voxel(VoxelKey(1, 2, 1), StateProperties(1, north, 0), StateProperties(1, north, 0)),
                Voxel(VoxelKey(1, 0, 1), StateProperties(1, south, 0), StateProperties(1, south, 0)),
                Voxel(VoxelKey(2, 1, 1), StateProperties(1, east, 0), StateProperties(1, east, 0)),
                Voxel(VoxelKey(0, 1, 1), StateProperties(1, west, 0), StateProperties(1, west, 0)),
            ), expected, row = rowCount
        )
    ).also { rowCount += 1 }

//@formatter:off
    val testData: Table1<TestData> = Table1(
        headers = Headers1("Test voxels"),
        rows = listOf(
            // AIR
            row(AIR, expected = AIR),
            row(AIR, lower = HALF_SMOKE, expected = HALF_SMOKE),
            row(AIR, upper = CONCRETE, east = HALF_SMOKE, west = HALF_SMOKE, north = HALF_SMOKE, south = HALF_SMOKE, expected = HALF_SMOKE),
            row(AIR, upper = GLASS, east = HALF_SMOKE, west = HALF_SMOKE, north = HALF_SMOKE, south = HALF_SMOKE, lower = FULL_SMOKE, expected = HALF_SMOKE),
            // HALF_SMOKE
            row(HALF_SMOKE, expected = AIR),
            row(HALF_SMOKE, lower = FULL_SMOKE, expected = HALF_SMOKE),
            row(HALF_SMOKE, upper = WOOD, lower = FULL_SMOKE, expected = FULL_SMOKE),
            // FULL_SMOKE
            row(FULL_SMOKE, expected = HALF_SMOKE),
            row(FULL_SMOKE, upper = WOOD, lower = FULL_SMOKE, expected = FULL_SMOKE),
            row(FULL_SMOKE, upper = METAL, lower = FULL_SMOKE, expected = FULL_SMOKE),
            // CONDUCTION
            row(METAL, expected = METAL),
            row(METAL, lower = METAL_HOT, upper = METAL_HOT, north = METAL_HOT, east = METAL_HOT, west = METAL_HOT, south = METAL_HOT, expected = METAL_HEATED),
            row(METAL, lower = METAL_VERY_HOT, upper = METAL_VERY_HOT, north = METAL_VERY_HOT, east = METAL_VERY_HOT, west = METAL_VERY_HOT, south = METAL_VERY_HOT, expected = METAL_HOT),
            // FLAMMABLES
            row(WOOD, lower = METAL_VERY_HOT, upper = METAL_VERY_HOT, north = METAL_VERY_HOT, east = TEXTILE_BURNING, west = TEXTILE_BURNING, south = TEXTILE_BURNING, expected = WOOD_BURNING),
            row(PLASTIC_HEATED, lower = WOOD_BURNING, upper = WOOD_BURNING, north = PLASTIC_BURNING, east = PLASTIC_BURNING, west = GLASS_VERY_HOT, south = METAL_VERY_HOT, expected = PLASTIC_BURNING),
            row(CONCRETE, expected = CONCRETE),
        )
    )
//@formatter:on

    should("calculate correct next voxel material") {
        forAll(testData) { (voxel, neighbours, expectedMaterial, _) ->
            // given
            every {
                voxelRepository.findNeighbors(
                    voxel.voxelKey, NeighbourhoodType.N_E_W_S_U_L_, voxel.currentProperties.iterationNumber
                )
            } returns neighbours

            // when
            calculator.calculate(voxel)

            // then
            voxel.nextProperties.material shouldBe expectedMaterial
        }
    }

})


private data class TestData(
    val voxel: Voxel, val neighbours: List<Voxel>, val expectedMaterial: VoxelMaterial, val row: Int
)

