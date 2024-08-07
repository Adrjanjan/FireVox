package pl.edu.agh.firevox.shared.model.vox

import com.google.common.io.LittleEndianDataInputStream
import com.google.common.io.LittleEndianDataOutputStream
import pl.edu.agh.firevox.shared.model.VoxelKey
import pl.edu.agh.firevox.shared.model.simulation.Palette
import pl.edu.agh.firevox.shared.model.vox.chunks.*
import pl.edu.agh.firevox.shared.model.vox.chunks.ChunkTags.*
import pl.edu.agh.firevox.shared.model.x
import pl.edu.agh.firevox.shared.model.y
import pl.edu.agh.firevox.shared.model.z
import java.io.*
import java.nio.charset.StandardCharsets
import kotlin.math.ceil
import kotlin.math.min
import kotlin.math.round

object VoxFormatParser {
    private const val TAG_FORMAT = "VOX "
    private const val VERSION = 200
    private const val tileSize = 256

    /**
     * @param voxels - sorted map of voxel
     * @param palette - output palette
     * @param sizeX - size in X dimension
     * @param sizeY - size in Y dimension
     * @param sizeZ - size in Z dimension
     */
    @Throws(IOException::class)
    fun write(
        voxels: Map<VoxelKey, Int>,
        palette: Palette,
        sizeX: Int,
        sizeY: Int,
        sizeZ: Int,
        outputStream: OutputStream
    ) {
        val out = LittleEndianDataOutputStream(outputStream)
        out.writeTag(TAG_FORMAT)
        out.writeInt(VERSION)
        val main = createMainChunk(voxels, sizeX, sizeY, sizeZ, palette).toByteArray()
        out.writeTagStartData(TAG_MAIN.tagValue, 0, main.size)
        out.write(main)
    }

    /**
     * If you want to use different palette for the output the voxels here should be already mapped
     */
    private fun createMainChunk(
        voxels: Map<VoxelKey, Int>,
        sizeX: Int, sizeY: Int, sizeZ: Int,
        palette: Palette
    ): ByteArrayOutputStream {
        val mainBytes = ByteArrayOutputStream()
        val mainOut = LittleEndianDataOutputStream(mainBytes)

        val splitVoxels = splitVoxels(voxels, sizeX, sizeY, sizeZ)

        for (v in splitVoxels) {
            // Size Chunk
            mainOut.writeTagStartData(TAG_SIZE.tagValue, 12, 0)
            mainOut.writeInt(v.second.x)
            mainOut.writeInt(v.second.y)
            mainOut.writeInt(v.second.z)

            // XYZI Chunk
            mainOut.writeTagStartData(TAG_XYZI.tagValue, v.first.size * 4 + 4, 0) //4 ints per voxel + '0'

            mainOut.writeInt(v.first.size)
            for (voxel in v.first) {
                mainOut.writeByte(voxel.key.x)
                mainOut.writeByte(voxel.key.y)
                mainOut.writeByte(voxel.key.z)
                mainOut.writeByte(voxel.value)
            }
        }

        val modelIdToTranslation = splitVoxels.mapIndexed { i, triple ->
            i to triple.third
        }.associate { it.first to it.second }

        // Map<Int, Translation>
        // scene graph (chunks nGRP, nTRN, nSHP)
        writeSceneTree(mainOut, modelIdToTranslation)

        // RGBA Chunk
        mainOut.writeTagStartData(TAG_RGBA.tagValue, 1024, 0)
        palette.colours.sortedBy { it.index }.forEach {
            mainOut.writeByte(it.r)
            mainOut.writeByte(it.g)
            mainOut.writeByte(it.b)
            mainOut.writeByte(it.a)
        }
        mainOut.writeInt(0)
        mainOut.flush()
        return mainBytes
    }

    private fun writeSceneTree(
        mainOut: LittleEndianDataOutputStream,
        modelIdToTranslation: Map<Int, Translation>
    ) {
        val root = createTransformChunkBytes(
            nodeId = 0,
            nodeAttributes = mapOf(),
            childNodeId = 1,
            translation = null
        )

        val translateToShapeForModels = modelIdToTranslation.map {
            val legalIndex = it.key * 2 + 2 // since root and first group have id 0 and 1
            legalIndex to
                    (createTransformChunkBytes(
                        nodeId = legalIndex,
                        nodeAttributes = mapOf(),
                        childNodeId = legalIndex + 1,
                        translation = it.value
                    ) to createShapeChunkBytes(
                        legalIndex + 1,
                        mapOf(),
                        listOf(it.key to 0)
                    ))
        }

        val firstGroup = createGroupChunkBytes(
            1,
            mapOf(),
            translateToShapeForModels.map { it.first }
        )

        mainOut.write(root)
        mainOut.write(firstGroup)
        translateToShapeForModels.forEach {
            mainOut.write(it.second.first) // translate
            mainOut.write(it.second.second) // shape
        }

    }

    private fun createTransformChunkBytes(
        nodeId: Int,
        nodeAttributes: Map<String, String>,
        childNodeId: Int,
        reservedId: Int = -1,
        layerId: Int = -1,
        numberOfFrames: Int = 1,
        translation: Translation?
    ): ByteArray {
        val result = ByteArrayOutputStream()
        val resultOut = LittleEndianDataOutputStream(result)

        val content = ByteArrayOutputStream()
        val contentOut = LittleEndianDataOutputStream(content)

        contentOut.writeInt(nodeId)
        contentOut.writeVoxDict(nodeAttributes)
        contentOut.writeInt(childNodeId)
        contentOut.writeInt(reservedId)
        contentOut.writeInt(layerId)
        contentOut.writeInt(numberOfFrames)
        // frame attributes (only translation usable in this case)
        translation?.let {
            contentOut.writeVoxDict(
                mapOf("_t" to "${it.x} ${it.y} ${it.z}")
            )
        } ?: contentOut.writeVoxDict(
            mapOf()
        )

        resultOut.writeTagStartData(TAG_TRANSFORM_NODE.tagValue, content.size(), 0)
        resultOut.write(content.toByteArray())
        return result.toByteArray()
    }

    private fun createGroupChunkBytes(
        nodeId: Int,
        nodeAttributes: Map<String, String>,
        childNodeIds: List<Int>
    ): ByteArray {
        val result = ByteArrayOutputStream()
        val resultOut = LittleEndianDataOutputStream(result)

        val content = ByteArrayOutputStream()
        val contentOut = LittleEndianDataOutputStream(content)

        contentOut.writeInt(nodeId)
        contentOut.writeVoxDict(nodeAttributes)
        contentOut.writeInt(childNodeIds.size)
        childNodeIds.forEach { contentOut.writeInt(it) }

        resultOut.writeTagStartData(TAG_GROUP_NODE.tagValue, content.size(), 0)
        resultOut.write(content.toByteArray())
        return result.toByteArray()
    }

    private fun createShapeChunkBytes(
        nodeId: Int,
        nodeAttributes: Map<String, String>,
        models: List<Pair<Int, Int>> // modelId to frameIndex
    ): ByteArray {
        val result = ByteArrayOutputStream()
        val resultOut = LittleEndianDataOutputStream(result)

        val content = ByteArrayOutputStream()
        val contentOut = LittleEndianDataOutputStream(content)

        contentOut.writeInt(nodeId)
        contentOut.writeVoxDict(nodeAttributes)
        contentOut.writeInt(models.size)
        models.forEach {
            contentOut.writeInt(it.first) // modelId
            contentOut.writeVoxDict(mapOf())
        }

        resultOut.writeTagStartData(TAG_SHAPE_NODE.tagValue, content.size(), 0)
        resultOut.write(content.toByteArray())
        return result.toByteArray()
    }


    private fun splitVoxels(
        voxels: Map<VoxelKey, Int>,
        sizeX: Int,
        sizeY: Int,
        sizeZ: Int
    ): List<Triple<Map<VoxelKey, Int>, ModelSize, Translation>> {
        val nx = ceil(sizeX.toDouble() / tileSize).toInt()
        val ny = ceil(sizeY.toDouble() / tileSize).toInt()
        val nz = ceil(sizeZ.toDouble() / tileSize).toInt()

        val results = mutableListOf<Triple<Map<VoxelKey, Int>, ModelSize, Translation>>()
        for (ix in 0 until nx) {
            val xStartIndex = ix * tileSize
            val xEndIndex = min(xStartIndex + tileSize, sizeX)
            for (iy in 0 until ny) {
                val yStartIndex = iy * tileSize
                val yEndIndex = min(yStartIndex + tileSize, sizeY)
                for (iz in 0 until nz) {
                    val zStartIndex = iz * tileSize
                    val zEndIndex = min(zStartIndex + tileSize, sizeZ)

                    val model = mutableMapOf<VoxelKey, Int>()
                    for ((key, value) in voxels) {
                        if (key.between(
                                xRange = xStartIndex until xEndIndex,
                                yRange = yStartIndex until yEndIndex,
                                zRange = zStartIndex until zEndIndex,
                            ) && value != 0
                        ) {
                            model[VoxelKey(key.x % tileSize, key.y % tileSize, key.z % tileSize)] = value + 1
                        }
                        if (model.size >= tileSize * tileSize * tileSize) break // single model cant be bigger than tileSize ^3
                    }
                    if (model.isEmpty()) continue

                    val modelXSize = xEndIndex - xStartIndex
                    val modelYSize = yEndIndex - yStartIndex
                    val modelZSize = zEndIndex - zStartIndex

                    results.add(
                        Triple(
                            model,
                            Triple(modelXSize, modelYSize, modelZSize),
                            Translation(
                                xStartIndex - tileSize + modelXSize / 2,
                                yStartIndex - tileSize + modelYSize / 2,
                                zStartIndex - tileSize + modelZSize / 2 + tileSize
                            )
                        )
                    )
                }
            }
        }
        return results
    }

    @Throws(IOException::class)
    fun read(inputStream: InputStream): ParsedVoxFile {
        val input = LittleEndianDataInputStream(inputStream)
        verifyVoxFormat(input)

        var mainChunk: MainChunk? = null
        val models = mutableListOf<Model>()
        val transformNodeChunks = mutableMapOf<Int, TransformNodeChunk>()
        val groupNodeChunks = mutableMapOf<Int, GroupNodeChunk>()
        val shapeNodeChunks = mutableMapOf<Int, ShapeNodeChunk>()
        val layerNodeChunks = mutableMapOf<Int, LayerChunk>()
        val materials = mutableListOf<MaterialChunk>()
        val cameras = mutableListOf<RenderCameraChunk>()
        val renderObjects = mutableListOf<RenderObjectsChunk>()
        var colorIndexMap: IndexMapChunk? = null
        var paletteChunk: PaletteChunk? = null
        var paletteNoteChunk: PaletteNoteChunk? = null


        while (input.available() != 0) {
            val tag = input.readTag()
            when (tag) {
                TAG_MAIN.tagValue -> mainChunk = MainChunk(input)
                TAG_PACK.tagValue -> PackChunk(input) // is deprecated in version 200
                TAG_SIZE.tagValue -> models.add(
                    Model(
                        SizeChunk(input),
                        input.readTag()
                            .let { if (it != TAG_XYZI.tagValue) throw UnexpectedChunk("XYZI should be here") }
                            .let { VoxelsChunk(input) }
                    )
                )

                TAG_XYZI.tagValue -> throw UnexpectedChunk("XYZI should be read with SIZE chunk")
                TAG_MATL.tagValue -> materials.add(MaterialChunk(input))
                TAG_RGBA.tagValue -> paletteChunk = PaletteChunk(input)
                TAG_TRANSFORM_NODE.tagValue -> TransformNodeChunk(input).let { transformNodeChunks[it.nodeId] = it }
                TAG_GROUP_NODE.tagValue -> GroupNodeChunk(input).let { groupNodeChunks[it.nodeId] = it }
                TAG_SHAPE_NODE.tagValue -> ShapeNodeChunk(input).let { shapeNodeChunks[it.nodeId] = it }
                TAG_LAYER.tagValue -> LayerChunk(input).let { layerNodeChunks[it.nodeId] = it }
                TAG_INDEX_MAP.tagValue -> colorIndexMap = IndexMapChunk(input)
                TAG_PALETTE_NOTE.tagValue -> paletteNoteChunk = PaletteNoteChunk(input)
                TAG_RENDER_OBJECTS.tagValue -> renderObjects.add(RenderObjectsChunk(input))
                TAG_RENDER_CAMERA.tagValue -> cameras.add(RenderCameraChunk(input))
                else -> throw UnexpectedChunk("Expected normal chunk, got $tag")
            }
        }
        return ParsedVoxFile(
            mainChunk!!,
            models,
            SceneTree(transformNodeChunks, layerNodeChunks, groupNodeChunks, shapeNodeChunks),
            paletteChunk!!,
            colorIndexMap,
            paletteNoteChunk,
            materials,
            renderObjects,
            cameras,
        )
    }

    private fun verifyVoxFormat(input: LittleEndianDataInputStream) {
        if (input.readTag() != TAG_FORMAT) {
            throw IOException("Doesn't appear to be in VOX format.")
        }
        input.readInt().let {
            if (it != VERSION) {
                throw WrongFileVersionException("Warning: expecting version $VERSION but got $it.")
            }
        }
    }

    fun toPaletteLinear(value: Double, min: Double, max: Double, numBuckets: Int = 253) =
        round((value - min) * (numBuckets / (max - min))).toInt() + 1

}

class WrongFileVersionException(s: String) : Throwable(s)

class UnexpectedChunk(s: String) : Exception(s)

fun LittleEndianDataInputStream.readTag(size: Int = 4): String {
    val bytes = this.readNBytes(size)
    return String(bytes, StandardCharsets.UTF_8).replace("\u0000", "")
}

fun LittleEndianDataInputStream.readVoxString() =
    (0 until this.readInt()).fold(StringBuffer()) { acc, _ ->
        acc.append(
            String(
                this.readNBytes(1),
                StandardCharsets.UTF_8
            ).replace("\u0000", "")
        )
    }.toString()

fun LittleEndianDataInputStream.readVoxDict(): Map<String, String> =
    (0 until this.readInt()).fold(mutableMapOf()) { acc, _ ->
        acc[this.readVoxString()] = this.readVoxString(); acc
    }

fun LittleEndianDataOutputStream.writeTagStartData(
    tag: String,
    numberOfBytesOfChunkContent: Int,
    numberOfChunksOfChildrenChunks: Int
) = this.writeTag(tag)
    .also { this.writeInt(numberOfBytesOfChunkContent) }
    .also { this.writeInt(numberOfChunksOfChildrenChunks) }

fun LittleEndianDataOutputStream.writeTag(tag: String) =
    this.write(tag.toByteArray(StandardCharsets.UTF_8))

fun LittleEndianDataOutputStream.writeVoxDict(dict: Map<String, String>) {
    this.writeInt(dict.size)
    dict.forEach {
        this.writeVoxString(it.key)
        this.writeVoxString(it.value)
    }
}

fun LittleEndianDataOutputStream.writeVoxString(s: String) {
    this.writeInt(s.length)
    this.write(s.toByteArray(StandardCharsets.UTF_8))
}


typealias ModelSize = Triple<Int, Int, Int>