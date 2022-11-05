package pl.edu.agh.firevox.vox.chunks

abstract class Chunk(val id: ChunkTags)

enum class ChunkTags(val tagValue: String) {
    TAG_MAIN("MAIN"),
    TAG_PACK("PACK"),
    TAG_SIZE("SIZE"),
    TAG_XYZI("XYZI"),
    TAG_MATT("MATL"),
    TAG_RGBA("RGBA"),
    TAG_TRANSFORM("nTRN"),
    TAG_GROUP("nGRP"),
    TAG_SHAPE("nSHP"),
    TAG_LAYER("LAYR"),
    TAG_RENDER_OBJECTS("rOBJ"),
    TAG_RENDER_CAMERA("rCAM"),
    TAG_PALETTE_NOTE("NOTE"),
    TAG_INDEX_MAP("IMAP"),
}
