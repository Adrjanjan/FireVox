package pl.edu.agh.firevox.vox

import com.google.common.io.LittleEndianDataInputStream
import com.google.common.io.LittleEndianDataOutputStream
//import pl.edu.agh.firevox.vox.MaterialProperties.*
import pl.edu.agh.firevox.vox.chunks.*
import pl.edu.agh.firevox.vox.chunks.ChunkTags.*
import java.io.*
import java.nio.charset.Charset

object VoxFormatParser {
    private const val TAG_FORMAT = "VOX "
    private const val VERSION = 200
//
//    @Throws(IOException::class)
//    fun write(model: ParsedVoxFile, outputStream: OutputStream) {
//        val out = LittleEndianDataOutputStream(outputStream)
//
//        out.writeTag(TAG_FORMAT)
//        out.writeByte(VERSION)
//        out.writeTag(ChunkTags.TAG_MAIN.tagValue)
//
//        val mainBytes = ByteArrayOutputStream()
//        val mainOut = LittleEndianDataOutputStream(mainBytes)
//
//        // Size Chunk
//        mainOut.writeTag(ChunkTags.TAG_SIZE.tagValue)
//        // The size of this chunk
//        mainOut.writeInt(12)
//        mainOut.writeInt(0)
//        mainOut.writeInt(model.sizeX)
//        mainOut.writeInt(model.sizeY)
//        mainOut.writeInt(model.sizeZ)
//
//        // XYZI Chunk
//        mainOut.writeTag(ChunkTags.TAG_XYZI.tagValue)
//        val voxels = model.voxels
//        val voxelChunkSize = (1 + voxels.size) * 4
//        mainOut.writeInt(voxelChunkSize)
//        mainOut.writeInt(0)
//        mainOut.writeInt(voxels.size)
//        for ((x, y, z, i) in voxels) {
//            mainOut.writeInt(x)
//            mainOut.writeInt(y)
//            mainOut.writeInt(z)
//            mainOut.writeInt(i)
//        }
//
//        // RGBA Chunk
//        mainOut.writeTag(ChunkTags.TAG_RGBA.tagValue)
//        mainOut.writeInt(1024)
//        mainOut.writeInt(0)
//        for (i in 1..255) {
//            mainOut.writeInt(model.palette.getColor(i).toInt())
//        }
//        mainOut.writeInt(0)
//
//        // MATT Chunks
//        for (i in 1..255) {
//            val material = model.getMaterial(i)
//            if (material!!.type != 0) {
//                mainOut.writeTag(ChunkTags.TAG_MATL.tagValue)
//                val size: Int = 4 * (4 + (material.propertyValues?.size ?: 0))
//                mainOut.writeInt(size)
//                mainOut.writeInt(0)
//                mainOut.writeInt(i)
//                mainOut.writeInt(material.type ?: 0)
//                mainOut.writeFloat(material.weight ?: 0.0f)
//                mainOut.writeInt(material.propertiesBits ?: 0)
//                material.propertiesBits?.let {
//                    writePropertyBits(it, material.propertyValues ?: mapOf(), mainOut)
//                }
//            }
//        }
//        mainOut.flush()
//        val main = mainBytes.toByteArray()
//        out.write(0)
//        out.write(main.size)
//        out.write(main)
//    }

    @Throws(IOException::class)
    fun read(inputStream: InputStream, maxSize: Int): ParsedVoxFile {
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
            maxSize,
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
