package pl.edu.agh.firevox.vox.chunks

abstract class Chunk {
    abstract var tag: ChunkTags
    abstract val size: Int
    abstract val childSize: Int
}

enum class ChunkTags(val tagValue: String) {
    TAG_MAIN("MAIN"),
    TAG_PACK("PACK"),
    TAG_SIZE("SIZE"),
    TAG_XYZI("XYZI"),
    TAG_MATL("MATL"),
    TAG_RGBA("RGBA"),
    TAG_TRANSFORM_NODE("nTRN"),
    TAG_GROUP_NODE("nGRP"),
    TAG_SHAPE_NODE("nSHP"),
    TAG_LAYER("LAYR"),
    TAG_RENDER_OBJECTS("rOBJ"),
    TAG_RENDER_CAMERA("rCAM"),
    TAG_PALETTE_NOTE("NOTE"),
    TAG_INDEX_MAP("IMAP"),
}
