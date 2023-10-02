package pl.edu.agh.firevox.shared.model.vox

import pl.edu.agh.firevox.shared.config.FireVoxProperties
import pl.edu.agh.firevox.shared.model.VoxelKey
import pl.edu.agh.firevox.shared.model.vox.VoxelsTransformations.rotateVoxels
import pl.edu.agh.firevox.shared.model.vox.VoxelsTransformations.sizeInDimension
import pl.edu.agh.firevox.shared.model.vox.VoxelsTransformations.translate
import pl.edu.agh.firevox.shared.model.vox.chunks.*

data class Model(
    val sizeChunk: SizeChunk,
    val voxelsChunk: VoxelsChunk
)

data class SceneTree(
    private val transformNodeChunks: Map<Int, TransformNodeChunk>,
    private val layerNodeChunks: Map<Int, LayerChunk>,
    private val groupNodeChunks: Map<Int, GroupNodeChunk>,
    private val shapeNodeChunks: Map<Int, ShapeNodeChunk>,
) {
    private val root = transformNodeChunks[0]

    /**
     * Used to construct correct voxels positions from scene tree
     * Example scene tree:
     *        T
     *        |
     *        G
     *       / \
     *      T   T
     *      |   |
     *      G   S
     *     / \
     *    T   T
     *    |   |
     *    S   S
     */
    fun constructScene(models: List<Model>, maxSize: Int): MutableMap<VoxelKey, VoxelMaterialId> {
        val root = this.root
            ?: if (models.size > 1) throw Exception("There is ${models.size} models in file but no scene tree") else models[0]
        return processNode(models, root as SceneNode, maxSize)
    }

    private fun processNode(
        models: List<Model>,
        node: SceneNode,
        maxSize: Int
    ): MutableMap<VoxelKey, VoxelMaterialId> =
        when (node) {
            is TransformNodeChunk -> {
                val voxels = processNode(models, this.findNode(node.childNodeId), maxSize)
                    .rotateVoxels(node.framesAttributes[0]?.rotation)
                    .translate(node.framesAttributes[0]?.translation)
                voxels
            }

            is GroupNodeChunk -> {
                val childVoxels: MutableMap<VoxelKey, VoxelMaterialId> = mutableMapOf()
                node.childNodeIds.forEach {
                    processNode(
                        models,
                        this.findNode(it),
                        maxSize
                    ).let { result -> childVoxels.putAll(result) }
                }
                childVoxels
            } // process all nodes in group
            is ShapeNodeChunk -> {
                val voxels: MutableMap<VoxelKey, VoxelMaterialId> = mutableMapOf()
                node.models.forEach { model ->
                    val voxelsBeforeMoveTo000 = models[model.modelId].voxelsChunk.voxels
                    val middle = VoxelKey(voxelsBeforeMoveTo000.maxOf { it.key.x } / 2,
                        voxelsBeforeMoveTo000.maxOf { it.key.y } / 2,
                        voxelsBeforeMoveTo000.maxOf { it.key.z } / 2
                    )
                    voxelsBeforeMoveTo000.filter { it.value != 0 }
                        .forEach { voxels[it.key - middle] = it.value }
                }
                voxels
            }  // add all models to list?
            else -> throw NotSupportedSceneNodeException("Scene node $node.")
        }

    private fun findNode(nodeId: Int): SceneNode =
        transformNodeChunks[nodeId] ?: groupNodeChunks[nodeId] ?: shapeNodeChunks[nodeId]
        ?: throw NodeNotFoundException("Node with id $nodeId was not present in Scene Tree")
}

class NodeNotFoundException(s: String) : Exception(s)

class ParsedVoxFile(
    val mainChunk: MainChunk,
    val models: List<Model>, //size to xyzi
    val sceneTree: SceneTree,
    val palette: PaletteChunk,
    val colorIndexMap: IndexMapChunk?,
    val paletteNoteChunk: PaletteNoteChunk,
    val materials: List<MaterialChunk>,
    val renderObjects: List<RenderObjectsChunk>,
    val cameras: List<RenderCameraChunk>,
    private val maxSize: Int = FireVoxProperties.maxSize
) {

    val voxels = sceneTree.constructScene(models, maxSize).minimaliseKeys()

    var sizeX: Int = voxels.sizeInDimension { it.x }
    var sizeY: Int = voxels.sizeInDimension { it.y }
    var sizeZ: Int = voxels.sizeInDimension { it.z }

    private fun MutableMap<VoxelKey, VoxelMaterialId>.addVoxel(x: Int, y: Int, z: Int, i: VoxelMaterialId) {
        if (x in 0 until maxSize && (y in 0 until maxSize) && (z in 0 until maxSize)) {
            this[VoxelKey(x, y, z)] = i
        }
    }
//
//    fun scale(f: Int): ParsedVoxFile {
//        if (f == 1) {
//            return this;
//        } else {
//            val oldVoxels = voxels
//            val newVoxels = mutableListOf<Voxel>()
//            for ((x1, y1, z1, i) in oldVoxels) {
//                val x = x1 * f
//                val y = y1 * f
//                val z = z1 * f
//                for (dx in 0 until f) {
//                    for (dy in 0 until f) {
//                        for (dz in 0 until f) {
//                            newVoxels.add(Voxel(x + dx, y + dy, z + dz, i))
//                        }
//                    }
//                }
//            }
//            // TODO maybe replace mutability to new Model creation
//            voxels = newVoxels
//            sizeX = (sizeX * f).coerceAtMost(maxSize)
//            sizeY = (sizeY * f).coerceAtMost(maxSize)
//            sizeZ = (sizeZ * f).coerceAtMost(maxSize)
//            return this;
//        }
//    }
//

    /**
     * Adds already parsed .vox file - all Scene Nodes have to be already processes
     */
    fun addModel(
        model: ParsedVoxFile,
        offsetX: Int,
        offsetY: Int,
        offsetZ: Int,
        centerX: Boolean = false,
        centerY: Boolean = false,
        centerZ: Boolean = false,
        flipX: Boolean = false,
        flipY: Boolean = false,
        flipZ: Boolean = false,
        rotateX: Int = 0,
        rotateY: Int = 0,
        rotateZ: Int = 0,
    ) {
        val rot = Array(3) { FloatArray(3) }
        rot[0][0] = (cos(rotateY) * cos(rotateZ)).toFloat()
        rot[0][1] = (cos(rotateZ) * sin(rotateX) * sin(rotateY) - cos(rotateX) * sin(rotateZ)).toFloat()
        rot[0][2] = (cos(rotateX) * cos(rotateZ) * sin(rotateY) + sin(rotateX) * sin(rotateZ)).toFloat()
        rot[1][0] = (cos(rotateY) * sin(rotateZ)).toFloat()
        rot[1][1] = (cos(rotateX) * cos(rotateZ) + sin(rotateX) * sin(rotateY) * sin(rotateZ)).toFloat()
        rot[1][2] = (cos(rotateX) * sin(rotateY) * sin(rotateZ) - cos(rotateZ) * sin(rotateX)).toFloat()
        rot[2][0] = -sin(rotateY).toFloat()
        rot[2][1] = (cos(rotateY) * sin(rotateX)).toFloat()
        rot[2][2] = (cos(rotateX) * cos(rotateY)).toFloat()

        for (entry in model.voxels.entries) {
            val vx = if (flipX) model.sizeX - entry.key.x - 1 else entry.key.x
            val vy = if (flipY) model.sizeY - entry.key.y - 1 else entry.key.y
            val vz = if (flipZ) model.sizeZ - entry.key.z - 1 else entry.key.z
            val fx = vx - model.sizeX / 2f
            val fy = vy - model.sizeY / 2f
            val fz = vz - model.sizeZ / 2f
            val rx: Int =
                (rot[0][0] * fx + rot[0][1] * fy + rot[0][2] * fz + (if (centerX) 1f else model.sizeX / 2f) + offsetX).toInt()
            val ry: Int =
                (rot[1][0] * fx + rot[1][1] * fy + rot[1][2] * fz + (if (centerY) 1f else model.sizeY / 2f) + offsetY).toInt()
            val rz =
                (rot[2][0] * fx + rot[2][1] * fy + rot[2][2] * fz + (if (centerZ) 1f else model.sizeZ / 2f) + offsetZ).toInt()
            voxels.addVoxel(rx, ry, rz, entry.value)
        }
    }

}

private fun MutableMap<VoxelKey, Int>.minimaliseKeys(): MutableMap<VoxelKey, Int> {
    val xMin = this.keys.minOf { k -> k.x }
    val yMin = this.keys.minOf { k -> k.y }
    val zMin = this.keys.minOf { k -> k.z }
    this.keys.forEach {
        it.x -= xMin;
        it.y -= yMin;
        it.z -= zMin;
    }
    return this
}

class NotSupportedSceneNodeException(s: String) : Exception(s)

fun sin(angle: Int) = when (angle % 360) {
    90 -> 1
    180 -> 0
    270 -> -1
    else -> 0
}


fun cos(angle: Int) = when (angle % 360) {
    90, 270 -> 0
    180 -> -1
    else -> 1
}

