package pl.edu.agh.firevox.vox

import java.io.*
import java.lang.Float.floatToIntBits
import java.lang.Float.intBitsToFloat
import java.nio.charset.Charset

object VoxFormatParser {
    private const val TAG_FORMAT = "VOX "
    private const val VERSION = 150

    @Throws(IOException::class)
    fun write(model: VoxModel, outputStream: OutputStream?) {
        val out = DataOutputStream(outputStream)

        out.writeTag(TAG_FORMAT)
        out.writeByte(VERSION)
        out.writeTag(ChunkTags.TAG_MAIN.tagValue)

        val mainBytes = ByteArrayOutputStream()
        val mainOut = DataOutputStream(mainBytes)

        // Size Chunk
        mainOut.writeTag(ChunkTags.TAG_SIZE.tagValue)
        // The size of this chunk
        mainOut.writeIntWithCorrectEndianness(12)
        mainOut.writeIntWithCorrectEndianness(0)
        mainOut.writeIntWithCorrectEndianness(model.sizeX)
        mainOut.writeIntWithCorrectEndianness(model.sizeY)
        mainOut.writeIntWithCorrectEndianness(model.sizeZ)

        // XYZI Chunk
        mainOut.writeTag(ChunkTags.TAG_XYZI.tagValue)
        val voxels = model.voxels
        val voxelChunkSize = (1 + voxels.size) * 4
        mainOut.writeIntWithCorrectEndianness(voxelChunkSize)
        mainOut.writeIntWithCorrectEndianness(0)
        mainOut.writeIntWithCorrectEndianness(voxels.size)
        for ((x, y, z, i) in voxels) {
            mainOut.writeBytes(x, y, z, i)
        }

        // RGBA Chunk
        mainOut.writeTag(ChunkTags.TAG_RGBA.tagValue)
        mainOut.writeIntWithCorrectEndianness(1024)
        mainOut.writeIntWithCorrectEndianness(0)
        for (i in 1..255) {
            mainOut.writeInt(model.palette.getColor(i).toInt())
        }
        mainOut.writeInt(0)

        // MATT Chunks
        for (i in 1..255) {
            val material = model.getMaterial(i)
            if (material!!.type != 0) {
                mainOut.writeTag(ChunkTags.TAG_MATT.tagValue)
                val size: Int = 4 * (4 + (material.values?.size ?: 0))
                mainOut.writeIntWithCorrectEndianness(size)
                mainOut.writeIntWithCorrectEndianness(0)
                mainOut.writeIntWithCorrectEndianness(i)
                mainOut.writeIntWithCorrectEndianness(material.type ?: 0)
                mainOut.writeFloatWithCorrectEndianness(material.weight ?: 0.0f)
                mainOut.writeIntWithCorrectEndianness(material.properties ?: 0)
                for (j in 0 until (material.values?.size ?: 0)) {
                    mainOut.writeFloatWithCorrectEndianness(material.values?.get(j) ?: 0.0f)
                }
            }
        }
        mainOut.flush()
        val main = mainBytes.toByteArray()
        out.writeIntWithCorrectEndianness(0)
        out.writeIntWithCorrectEndianness(main.size)
        out.write(main)
    }

    @Throws(IOException::class)
    fun read(inputStream: InputStream, maxSize: Int): VoxModel {
        val input = DataInputStream(inputStream)

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
        mainChunkBuilder: MainChunk.Builder.MainChunkBuilder,
        input: DataInputStream
    ) {
        mainChunkBuilder.paletteChunk = PaletteChunk.construct(input)
        readMaterialChunk(mainChunkBuilder, input, skipTag = false)
    }

    private fun readMaterialChunk(
        mainChunkBuilder: MainChunk.Builder.MainChunkBuilder,
        input: DataInputStream,
        skipTag: Boolean
    ) {
        if (!skipTag)
            try {
                val tag = input.readTag()
                if(tag.isBlank()) return
                if (tag != ChunkTags.TAG_MATT.tagValue) {
                    return
                }
            } catch (eof: EOFException) {
                // no material, finish
                return
            }
        mainChunkBuilder.materialChunk = MaterialChunk.construct(input)
    }

    private fun hasMultipleModels(main: MainChunk.Builder.MainChunkBuilder, input: DataInputStream) {
        main.packChunk = PackChunk.construct(input)
        for (i in 0 until main.packChunk!!.numberOfChunks) {
            readSingleModel(main, input, false)
        }
    }

    private fun readSingleModel(
        main: MainChunk.Builder.MainChunkBuilder,
        input: DataInputStream,
        skipFirstTag: Boolean
    ) {
        val sizeChunk = SizeChunk.construct(input, skipFirstTag)
        main.childrenChunks.add(sizeChunk to VoxelsChunk.construct(input))
    }

    private fun verifyVoxFormat(input: DataInputStream) {
        input.readTag().let {
            if (it != TAG_FORMAT) {
                throw IOException("Doesn't appear to be in VOX format.")
            }
        }

        input.readIntWithCorrectEndianness().let {
            if (it != VERSION) {
                throw WrongFileVersionException("Warning: expecting version $VERSION but got $it.")
            }
        }
    }

}

class WrongFileVersionException(s: String) : Throwable(s)

abstract class Chunk(val id: ChunkTags)

class MainChunk private constructor(
    val contentSize: Int = 0,
    val overallChildrenByteSize: Int = 0,
    val packChunk: PackChunk?,
    val childrenChunks: MutableList<Pair<SizeChunk, VoxelsChunk>> = mutableListOf(),
    val paletteChunk: PaletteChunk?,
    val materialChunk: MaterialChunk?
) : Chunk(ChunkTags.TAG_MAIN) {

    companion object Builder {
        fun construct(input: DataInputStream): MainChunkBuilder {
            val chunkTag = input.readTag()
            if (chunkTag != ChunkTags.TAG_MAIN.tagValue) {
                throw IOException("Should be a ${ChunkTags.TAG_MAIN} tag here.")
            }
            return MainChunkBuilder(input.readIntWithCorrectEndianness(), input.readIntWithCorrectEndianness())
        }

        data class MainChunkBuilder(val contentSize: Int, val overallChildrenByteSize: Int) {
            var packChunk: PackChunk? = null
            var childrenChunks: MutableList<Pair<SizeChunk, VoxelsChunk>> = mutableListOf()
            var paletteChunk: PaletteChunk? = null
            var materialChunk: MaterialChunk? = null

            fun build(): MainChunk =
                MainChunk(contentSize, overallChildrenByteSize, packChunk, childrenChunks, paletteChunk, materialChunk)
        }
    }
}

data class PackChunk(val numberOfChunks: Int = 0) : Chunk(ChunkTags.TAG_PACK) {

    companion object {
        fun construct(input: DataInputStream): PackChunk {
            // tag was read in read method
            input.skipBytes(8)
            return PackChunk(input.readIntWithCorrectEndianness())
        }

    }
}

data class SizeChunk(val sizeX: Int, val sizeY: Int, val sizeZ: Int) : Chunk(ChunkTags.TAG_SIZE) {

    companion object {
        fun construct(input: DataInputStream, skipTag: Boolean): SizeChunk {
            if (!skipTag) {
                val tag = input.readTag()
                if (tag != ChunkTags.TAG_SIZE.tagValue) {
                    throw IOException("Should be a ${ChunkTags.TAG_SIZE.tagValue} tag here.")
                }
            }
            input.skipBytes(8)
            val sizeX = input.readIntWithCorrectEndianness()
            val sizeY = input.readIntWithCorrectEndianness()
            val sizeZ = input.readIntWithCorrectEndianness()
            return SizeChunk(sizeX, sizeY, sizeZ)
        }

    }
}

data class VoxelsChunk(val numberOfVoxels: Int, val voxels: List<Voxel>) : Chunk(ChunkTags.TAG_XYZI) {

    companion object {
        fun construct(input: DataInputStream): VoxelsChunk {
            // Voxel data chunk
            val xyziTag = input.readTag()
            if (xyziTag != ChunkTags.TAG_XYZI.tagValue) {
                throw IOException("Should be a ${ChunkTags.TAG_XYZI.tagValue} tag here.")
            }
            input.skipBytes(8)
            val numVoxels = input.readIntWithCorrectEndianness()
            val voxels = mutableListOf<Voxel>()
            for (i in 0 until numVoxels) {
                val x = input.readUnsignedByte()
                val y = input.readUnsignedByte()
                val z = input.readUnsignedByte()
                val colorIndex = input.readUnsignedByte()
                voxels.add(Voxel(x, y, z, colorIndex))
                println("Voxel x=$x, y=$y, z=$z, p=$colorIndex")
            }
            return VoxelsChunk(numVoxels, voxels)
        }
    }
}

data class PaletteChunk(val colors: MutableList<Long?>) : Chunk(ChunkTags.TAG_RGBA) {

    companion object {
        fun construct(input: DataInputStream): PaletteChunk {
            // Palette chunk
            val colors = MutableList<Long?>(256) { null }
            input.skipBytes(8)
            for (i in 1..255) {
                val r = input.readUnsignedByte()
                val g = input.readUnsignedByte()
                val b = input.readUnsignedByte()
                val a = input.readUnsignedByte()
                colors[i] = fromRgba(r, g, b, a)
            }
            return PaletteChunk(colors)
        }

        private fun fromRgba(r: Int, g: Int, b: Int, a: Int) = String.format("%02x%02x%02x%02x", r, g, b, a).toLong(16)
    }
}

data class MaterialChunk(
    val index: Int,
    val materialType: MaterialType,
    val materialWeight: Float,
    val properties: Map<String, Float>
) : Chunk(ChunkTags.TAG_MATT) {

    companion object {
        fun construct(input: DataInputStream): MaterialChunk {
            input.skipBytes(8)
            val index = input.readIntWithCorrectEndianness()
            val type = input.readIntWithCorrectEndianness()
            val weight = input.readFloatWithCorrectEndianness()
            val propertyBits = input.readIntWithCorrectEndianness()
            return MaterialChunk(index, type, weight, calculateProperties(propertyBits, input))
        }

        private fun calculateProperties(propertyBits: Int, inputStream: DataInputStream): MutableMap<String, Float> {
            val tmpProperties = mutableMapOf<String, Float>()
            if (propertyBits and 1 > 0) tmpProperties["plastic"] = inputStream.readFloatWithCorrectEndianness()
            if (propertyBits and 2 > 0) tmpProperties["roughness"] = inputStream.readFloatWithCorrectEndianness()
            if (propertyBits and 4 > 0) tmpProperties["specular"] = inputStream.readFloatWithCorrectEndianness()
            if (propertyBits and 8 > 0) tmpProperties["ior"] = inputStream.readFloatWithCorrectEndianness()
            if (propertyBits and 16 > 0) tmpProperties["attenuation"] = inputStream.readFloatWithCorrectEndianness()
            if (propertyBits and 32 > 0) tmpProperties["power"] = inputStream.readFloatWithCorrectEndianness()
            if (propertyBits and 64 > 0) tmpProperties["glow"] = inputStream.readFloatWithCorrectEndianness()
            if (propertyBits and 128 > 0) tmpProperties["isTotalPower"] =
                inputStream.readFloatWithCorrectEndianness()
            return tmpProperties
        }
    }
}


enum class ChunkTags(val tagValue: String) {
    TAG_MAIN("MAIN"),
    TAG_PACK("PACK"),
    TAG_SIZE("SIZE"),
    TAG_XYZI("XYZI"),
    TAG_RGBA("RGBA"),
    TAG_MATT("MATT"),
}


private fun DataInputStream.readTag(size: Int = 4) = String(this.readNBytes(size), Charset.defaultCharset()).replace("\u0000", "")

private fun DataOutputStream.writeTag(tag: String) = this.writeBytes(tag)

private fun DataOutputStream.writeIntWithCorrectEndianness(i: Int) = this.writeInt(changeEndianness(i))

private fun DataInputStream.readIntWithCorrectEndianness() = changeEndianness(this.readInt())

private fun DataInputStream.readFloatWithCorrectEndianness() = intBitsToFloat(changeEndianness(this.readInt()))

private fun DataOutputStream.writeFloatWithCorrectEndianness(v: Float) =
    this.writeInt(changeEndianness(floatToIntBits(v)))

private fun DataOutputStream.writeBytes(vararg ints: Int) = ints.forEach { this.write(it and 0xff) }

private fun changeEndianness(i: Int) =
    i and 0xff shl 24 or (i and 0xff00 shl 8) or (i and 0xff0000 shr 8) or (i shr 24 and 0xff)