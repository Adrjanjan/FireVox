package pl.edu.agh.firevox.shared.model.vox.chunks

import com.google.common.io.LittleEndianDataInputStream
import pl.edu.agh.firevox.shared.model.vox.readVoxDict

data class TransformNodeChunk(
    val input: LittleEndianDataInputStream,
    override var tag: ChunkTags = ChunkTags.TAG_TRANSFORM_NODE,
    override val size: Int = input.readInt(),
    override val childSize: Int = input.readInt(),
    override val nodeId: Int = input.readInt(),
    val nodeAttributes: Map<String, String> = input.readVoxDict(),
    val childNodeId: Int = input.readInt()
        .also { input.skip(4) },  // reservedId, skipped
    val layerId: Int = input.readInt(),
    val numOfFrames: Int = input.readInt(),
    val framesAttributes: Map<Int, TransformProperties> = readFrameAttributes(input, numOfFrames),
) : Chunk(), SceneNode

data class TransformProperties(val rotation: Rotation?, val translation: Translation?, val frameIndex: Int?) {
    companion object {
        const val rotationKey = "_r"
        const val translationKey = "_t"
        const val indexKey = "_f"
    }

    constructor(dict: Map<String, String>) : this(
        dict[rotationKey]?.let { Rotation(it.toInt()) },
        dict[translationKey]?.let { Translation(it) },
        dict[indexKey]?.toInt()
    )
}

data class Rotation(val rot: List<List<Int>>) {
    constructor(bits: Int) : this(constructRotationFromBits(bits))
}

fun readFrameAttributes(input: LittleEndianDataInputStream, numOfFrames: Int): Map<Int, TransformProperties> =
    (0 until numOfFrames).fold(mutableMapOf()) { acc, i -> acc[i] = readTransformProperties(input); acc }

private fun readTransformProperties(input: LittleEndianDataInputStream) = TransformProperties(input.readVoxDict())

fun constructRotationFromBits(bits: Int): List<List<Int>> {
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

data class Translation(val x: Int, val y: Int, val z: Int) {
    constructor(xyz: String) : this(
        xyz.split(" ")[0].toInt(),
        xyz.split(" ")[1].toInt(),
        xyz.split(" ")[2].toInt(),
    )
}

