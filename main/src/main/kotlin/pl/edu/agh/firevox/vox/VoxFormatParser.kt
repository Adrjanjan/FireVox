package pl.edu.agh.firevox.vox

import com.google.common.io.LittleEndianDataInputStream
import com.google.common.io.LittleEndianDataOutputStream
import pl.edu.agh.firevox.vox.MaterialProperties.*
import pl.edu.agh.firevox.vox.chunks.*
import pl.edu.agh.firevox.vox.chunks.MainChunk.Builder.MainChunkBuilder
import java.io.*
import java.nio.charset.Charset

object VoxFormatParser {
    private const val TAG_FORMAT = "VOX "
    private const val VERSION = 200

    @Throws(IOException::class)
    fun write(model: VoxModel, outputStream: OutputStream) {
        val out = LittleEndianDataOutputStream(outputStream)

        out.writeTag(TAG_FORMAT)
        out.writeByte(VERSION)
        out.writeTag(ChunkTags.TAG_MAIN.tagValue)

        val mainBytes = ByteArrayOutputStream()
        val mainOut = LittleEndianDataOutputStream(mainBytes)

        // Size Chunk
        mainOut.writeTag(ChunkTags.TAG_SIZE.tagValue)
        // The size of this chunk
        mainOut.writeInt(12)
        mainOut.writeInt(0)
        mainOut.writeInt(model.sizeX)
        mainOut.writeInt(model.sizeY)
        mainOut.writeInt(model.sizeZ)

        // XYZI Chunk
        mainOut.writeTag(ChunkTags.TAG_XYZI.tagValue)
        val voxels = model.voxels
        val voxelChunkSize = (1 + voxels.size) * 4
        mainOut.writeInt(voxelChunkSize)
        mainOut.writeInt(0)
        mainOut.writeInt(voxels.size)
        for ((x, y, z, i) in voxels) {
            mainOut.writeInt(x)
            mainOut.writeInt(y)
            mainOut.writeInt(z)
            mainOut.writeInt(i)
        }

        // RGBA Chunk
        mainOut.writeTag(ChunkTags.TAG_RGBA.tagValue)
        mainOut.writeInt(1024)
        mainOut.writeInt(0)
        for (i in 1..255) {
            mainOut.writeInt(model.palette.getColor(i).toInt())
        }
        mainOut.writeInt(0)

        // MATT Chunks
        for (i in 1..255) {
            val material = model.getMaterial(i)
            if (material!!.type != 0) {
                mainOut.writeTag(ChunkTags.TAG_MATT.tagValue)
                val size: Int = 4 * (4 + (material.propertyValues?.size ?: 0))
                mainOut.writeInt(size)
                mainOut.writeInt(0)
                mainOut.writeInt(i)
                mainOut.writeInt(material.type ?: 0)
                mainOut.writeFloat(material.weight ?: 0.0f)
                mainOut.writeInt(material.propertiesBits ?: 0)
                material.propertiesBits?.let {
                    writePropertyBits(it, material.propertyValues ?: mapOf(), mainOut)
                }
            }
        }
        mainOut.flush()
        val main = mainBytes.toByteArray()
        out.write(0)
        out.write(main.size)
        out.write(main)
    }

    private fun writePropertyBits(
        propertyBits: Int,
        propertyValues: Map<MaterialProperties, Float>,
        outputStream: LittleEndianDataOutputStream
    ) {
        if (propertyBits and PLASTIC.propertyBit > 0) outputStream.writeFloat(
            propertyValues.getOrDefault(
                PLASTIC,
                0.0f
            )
        )
        if (propertyBits and ROUGHNESS.propertyBit > 0) outputStream.writeFloat(
            propertyValues.getOrDefault(
                ROUGHNESS,
                0.0f
            )
        )
        if (propertyBits and SPECULAR.propertyBit > 0) outputStream.writeFloat(
            propertyValues.getOrDefault(
                SPECULAR,
                0.0f
            )
        )
        if (propertyBits and IOR.propertyBit > 0) outputStream.writeFloat(propertyValues.getOrDefault(IOR, 0.0f))
        if (propertyBits and ATTENUATION.propertyBit > 0) outputStream.writeFloat(
            propertyValues.getOrDefault(
                ATTENUATION,
                0.0f
            )
        )
        if (propertyBits and POWER.propertyBit > 0) outputStream.writeFloat(propertyValues.getOrDefault(POWER, 0.0f))
        if (propertyBits and GLOW.propertyBit > 0) outputStream.writeFloat(propertyValues.getOrDefault(GLOW, 0.0f))
        if (propertyBits and IS_TOTAL_POWER.propertyBit > 0) outputStream.writeFloat(
            propertyValues.getOrDefault(
                IS_TOTAL_POWER,
                0.0f
            )
        )
    }

    @Throws(IOException::class)
    fun read(inputStream: InputStream, maxSize: Int): VoxModel {
        val input = LittleEndianDataInputStream(inputStream)

        verifyVoxFormat(input)
        val mainChunkBuilder = MainChunk.construct(input)

        when (input.readTag()) {
            ChunkTags.TAG_PACK.tagValue -> hasMultipleModels(mainChunkBuilder, input)
            ChunkTags.TAG_SIZE.tagValue -> readSingleModel(mainChunkBuilder, input, skipFirstTag = true)
            else -> throw IOException("Should be a ${ChunkTags.TAG_SIZE.tagValue} tag here.")
        }

        val paletteOrMaterialTag = try {
            input.readTag()
        } catch (eof: EOFException) {
            // no palette or material, finish
            return VoxModel.construct(mainChunkBuilder.build(), maxSize)
        }
        when (paletteOrMaterialTag) {
            ChunkTags.TAG_RGBA.tagValue -> readPaletteChunkAndMaterialChunk(mainChunkBuilder, input)
            ChunkTags.TAG_MATT.tagValue -> readMaterialChunk(mainChunkBuilder, input, skipTag = true)
            else -> throw IOException("Should be a ${ChunkTags.TAG_MATT} or ${ChunkTags.TAG_RGBA} tag here.")
        }

        return VoxModel.construct(mainChunkBuilder.build(), maxSize)
    }

    private fun readPaletteChunkAndMaterialChunk(
        mainChunkBuilder: MainChunkBuilder,
        input: LittleEndianDataInputStream
    ) {
        mainChunkBuilder.paletteChunk = PaletteChunk.construct(input)
        readMaterialChunk(mainChunkBuilder, input, skipTag = false)
    }

    private fun readMaterialChunk(
        mainChunkBuilder: MainChunkBuilder,
        input: LittleEndianDataInputStream,
        skipTag: Boolean
    ) {
        if (!skipTag)
            try {
                val tag = input.readTag()
                if (tag.isBlank()) return
                if (tag != ChunkTags.TAG_MATT.tagValue) {
                    return
                }
            } catch (eof: EOFException) {
                // no material, finish
                return
            }
        mainChunkBuilder.materialChunk = MaterialChunk.construct(input)
    }

    private fun hasMultipleModels(main: MainChunkBuilder, input: LittleEndianDataInputStream) {
        main.packChunk = PackChunk.construct(input)
        for (i in 0 until main.packChunk!!.numberOfChunks) {
            readSingleModel(main, input, false)
        }
    }

    private fun readSingleModel(
        main: MainChunkBuilder,
        input: LittleEndianDataInputStream,
        skipFirstTag: Boolean
    ) {
        val sizeChunk = SizeChunk.construct(input, skipFirstTag)
        main.childrenChunks.add(sizeChunk to VoxelsChunk.construct(input))
    }

    private fun verifyVoxFormat(input: LittleEndianDataInputStream) {
        input.readTag().let {
            if (it != TAG_FORMAT) {
                throw IOException("Doesn't appear to be in VOX format.")
            }
        }

        input.readInt().let {
            if (it != VERSION) {
                throw WrongFileVersionException("Warning: expecting version $VERSION but got $it.")
            }
        }
    }

}

class WrongFileVersionException(s: String) : Throwable(s)


fun LittleEndianDataInputStream.readTag(size: Int = 4) =
    String(this.readNBytes(size), Charset.defaultCharset()).replace("\u0000", "")

fun LittleEndianDataOutputStream.writeTag(tag: String) = this.writeChars(tag)

fun LittleEndianDataInputStream.readVoxString() =
    (0..this.readInt()).fold(StringBuffer()) { acc, _ ->
        acc.append(
            String(
                this.readNBytes(1),
                Charset.defaultCharset()
            ).replace("\u0000", "")
        )
    }.toString()

fun LittleEndianDataInputStream.readVoxDict(): Map<String, String> =
    (0..this.readInt()).fold(mutableMapOf()) { acc, _ ->
        acc[this.readVoxString()] = this.readVoxString(); acc
    }

fun LittleEndianDataInputStream.readRotation(): List<List<Int>> {
    val bits = this.readByte().toInt()
    return constructRotationFromBits(bits)
}

fun constructRotationFromBits(bits: Int): List<MutableList<Int>> {
    val nonZeroIndexRow1 = (bits and 3)
    val nonZeroIndexRow2 = (bits and (3 shl 2)) shr 2
    val row1Value = signAtPosition(bits, 4)
    val row2Value = signAtPosition(bits, 5)
    val row3Value = signAtPosition(bits, 6)

    fun constructRow(position: Int, value: Int) = mutableListOf(0, 0, 0).also { it[position] = value }.subList(0, 3)

    return listOf(
        constructRow(nonZeroIndexRow1, row1Value),
        constructRow(nonZeroIndexRow2, row2Value),
        constructRow(0, row3Value)
    )
}

private fun signAtPosition(bits: Int, position: Int) = if (bits and (1 shl position) == 0) 1 else -1

fun readSceneGraph(input: LittleEndianDataInputStream, mainChunkBuilder: MainChunkBuilder) {
    when(val tag = input.readTag()){
        ChunkTags.TAG_TRANSFORM.tagValue -> TODO()
        ChunkTags.TAG_GROUP.tagValue -> TODO()
        ChunkTags.TAG_SHAPE.tagValue -> TODO()
        else -> throw UnexpectedChunk("Expected read scene chunk, got $tag")
    }
}

class UnexpectedChunk(s: String) : Exception(s)
