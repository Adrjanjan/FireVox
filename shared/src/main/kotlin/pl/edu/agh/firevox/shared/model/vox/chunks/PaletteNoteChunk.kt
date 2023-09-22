package pl.edu.agh.firevox.shared.model.vox.chunks

import com.google.common.io.LittleEndianDataInputStream
import pl.edu.agh.firevox.shared.model.vox.readVoxDict
import pl.edu.agh.firevox.shared.model.vox.readVoxString

data class PaletteNoteChunk(
    val input: LittleEndianDataInputStream,
    override var tag: ChunkTags = ChunkTags.TAG_PALETTE_NOTE,
    override val size: Int = input.readInt(),
    override val childSize: Int = input.readInt(),
    val numOfColorNames: Int = input.readInt(),
    val colorNames: List<String> = readColoNames(input, numOfColorNames),
) : Chunk()

fun readColoNames(input: LittleEndianDataInputStream, numOfColorNames: Int): List<String> =
    (0 until numOfColorNames).fold(mutableListOf()) { acc, _ -> acc.add(input.readVoxString()); acc }
