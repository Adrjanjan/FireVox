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
    val childNodeId: Int = input.readInt().also { input.skip(4) },  // reservedId, skipped
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

fun constructRotationFromBits(rotation: Int): List<List<Int>> {
    val firstIndex = rotation and 0b0011
    val secondIndex = rotation and 0b1100 shr 2
    val array = mutableListOf(0, 1, 2)
    array.remove(firstIndex)
    array.remove(secondIndex)
    val thirdIndex = array[0]
    val negativeFirst = ((rotation and 0b0010000) shr 4) == 1
    val negativeSecond = ((rotation and 0b0100000) shr 5) == 1
    val negativeThird = ((rotation and 0b1000000) shr 6) == 1

    return mutableListOf(
        constructRow(firstIndex, if (negativeFirst) -1 else 1),
        constructRow(secondIndex, if (negativeSecond) -1 else 1),
        constructRow(thirdIndex, if (negativeThird) -1 else 1)
    )
}

private fun constructRow(position: Int, value: Int) = mutableListOf(0, 0, 0).also { it[position] = value }.subList(0, 3)

data class Translation(val x: Int, val y: Int, val z: Int) {
    constructor(xyz: String) : this(
        xyz.split(" ")[0].toInt(),
        xyz.split(" ")[1].toInt(),
        xyz.split(" ")[2].toInt(),
    )
}

