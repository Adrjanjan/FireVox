package pl.edu.agh.firevox.vox

import com.google.common.io.LittleEndianDataInputStream
import com.google.common.io.LittleEndianDataOutputStream
import pl.edu.agh.firevox.shared.model.VoxelKey
import pl.edu.agh.firevox.shared.model.simulation.Palette
import pl.edu.agh.firevox.shared.model.simulation.Simulation
import pl.edu.agh.firevox.shared.model.x
import pl.edu.agh.firevox.shared.model.y
import pl.edu.agh.firevox.shared.model.z
//import pl.edu.agh.firevox.vox.MaterialProperties.*
import pl.edu.agh.firevox.vox.chunks.*
import pl.edu.agh.firevox.vox.chunks.ChunkTags.*
import java.io.*
import java.nio.charset.Charset
import kotlin.math.ceil
import kotlin.math.min

object VoxFormatParser {
    private const val TAG_FORMAT = "VOX "
    private const val VERSION = 200

    /**
     * @param voxels - sorted map of voxel
     *
     * TODO - do przemyslenia - czy jak zapis będzie zbyt obciążający, to można voxele wyciągać po 256^3 i zapisywać częsciowe pliki a potem skleić tylko binarki
     *
     *
     */
    @Throws(IOException::class)
    fun write(voxels: Map<VoxelKey, Int>, palette: Palette, simulation: Simulation, outputStream: OutputStream) {
        val out = LittleEndianDataOutputStream(outputStream)

        out.writeTag(TAG_FORMAT)
        out.writeByte(VERSION)
        val main = createMainChunk(voxels, simulation, palette).toByteArray()

        out.writeTag(TAG_MAIN.tagValue)
        out.writeInt(0)
        out.writeInt(main.size)
        out.write(main)
    }

    private fun createMainChunk(
        voxels: Map<VoxelKey, Int>,
        simulation: Simulation,
        palette: Palette
    ): ByteArrayOutputStream {
        val mainBytes = ByteArrayOutputStream()
        val mainOut = LittleEndianDataOutputStream(mainBytes)

        val splitVoxels =
            splitVoxels(voxels, simulation.sizeX, simulation.sizeY, simulation.sizeZ)

        for (v in splitVoxels) {
            // Size Chunk
            mainOut.writeTag(TAG_SIZE.tagValue)
            // The size of this chunk
            mainOut.writeInt(12)
            mainOut.writeInt(0)
            mainOut.writeInt(v.second.x)
            mainOut.writeInt(v.second.y)
            mainOut.writeInt(v.second.z)

            // XYZI Chunk
            mainOut.writeTag(TAG_XYZI.tagValue)
            mainOut.writeInt(v.first.size * 4 + 4) //4 ints per voxel + '0'
            mainOut.writeInt(0)
            mainOut.writeInt(v.first.size)
            for (voxel in v.first) {
                mainOut.writeInt(voxel.key.x)
                mainOut.writeInt(voxel.key.y)
                mainOut.writeInt(voxel.key.z)
                mainOut.writeInt(voxel.value)
            }
        }

        // RGBA Chunk
        mainOut.writeTag(TAG_RGBA.tagValue)
        mainOut.writeInt(1024)
        mainOut.writeInt(0)
        palette.colours.sortedBy { it.index }.forEach {
            mainOut.writeInt(it.r)
            mainOut.writeInt(it.g)
            mainOut.writeInt(it.b)
            mainOut.writeInt(it.a)
        }
        mainOut.writeInt(0)
        mainOut.flush()
        return mainBytes
    }

    private fun splitVoxels(
        voxels: Map<VoxelKey, Int>,
        sizeX: Int,
        sizeY: Int,
        sizeZ: Int
    ): List<Pair<Map<VoxelKey, Int>, Triple<Int, Int, Int>>> {
        val tileSize = 255
        val nx = ceil(sizeX.toDouble() / tileSize).toInt()
        val ny = ceil(sizeY.toDouble() / tileSize).toInt()
        val nz = ceil(sizeZ.toDouble() / tileSize).toInt()

        val results = mutableListOf<Pair<Map<VoxelKey, Int>, Triple<Int, Int, Int>>>()
        for (ix in 0 until nx) {
            val xStartIndex = ix * tileSize
            val xEndIndex = min(xStartIndex + tileSize, sizeX)
            for (iy in 0 until ny) {
                val yStartIndex = iy * tileSize
                val yEndIndex = min(yStartIndex + tileSize, sizeY)
                for (iz in 0 until nz) {
                    val zStartIndex = iz * tileSize
                    val zEndIndex = min(zStartIndex + tileSize, sizeZ)

                    val model = voxels.filter {
                        it.key.between(
                            xRange = xStartIndex until xEndIndex,
                            yRange = yStartIndex until yEndIndex,
                            zRange = zStartIndex until zEndIndex,
                        )
                    }.mapKeys {
                        VoxelKey(
                            it.key.x % tileSize,
                            it.key.y % tileSize,
                            it.key.z % tileSize
                        )
                    }

                    val modelXSize = xEndIndex - xStartIndex
                    val modelYSize = yEndIndex - yStartIndex
                    val modelZSize = zEndIndex - zStartIndex

                    results.add(model to Triple(modelXSize, modelYSize, modelZSize))
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
            when (val tag = input.readTag()) {
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
            paletteNoteChunk!!,
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

}

class WrongFileVersionException(s: String) : Throwable(s)

class UnexpectedChunk(s: String) : Exception(s)

fun LittleEndianDataInputStream.readTag(size: Int = 4) =
    String(this.readNBytes(size), Charset.defaultCharset()).replace("\u0000", "")

fun LittleEndianDataOutputStream.writeTag(tag: String) = this.writeChars(tag)

fun LittleEndianDataInputStream.readVoxString() =
    (0 until this.readInt()).fold(StringBuffer()) { acc, _ ->
        acc.append(
            String(
                this.readNBytes(1),
                Charset.defaultCharset()
            ).replace("\u0000", "")
        )
    }.toString()

fun LittleEndianDataInputStream.readVoxDict(): Map<String, String> =
    (0 until this.readInt()).fold(mutableMapOf()) { acc, _ ->
        acc[this.readVoxString()] = this.readVoxString(); acc
    }
