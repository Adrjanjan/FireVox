package pl.edu.agh.firevox.vox

class Palette {

//    fun merge(model: ParsedVoxFile) {
//        val otherPalette = model.palette
//        val map = createMap(this)
//        val missingEntries = mutableListOf<Material>()
//        val swaps = mutableMapOf<Int, Int>()
//        for (i in 1 until otherPalette..size) {
//            val material = otherPalette.materials[i]
//            if (material.used) {
//                val existingMaterial = map[material]
//                if (existingMaterial != null) {
//                    existingMaterial.used = true
//                    swaps[i] = existingMaterial.index
//                } else {
//                    missingEntries.add(material)
//                }
//            }
//        }
//
//        for (material in missingEntries) {
//            val candidate = materials.find { !it.used } ?: throw NoSpaceInPaletteException()
//            candidate.copyFrom(material)
//            candidate.used = true
//            swaps[material.index] = candidate.index
//        }
//
//        for (voxel in model.voxels) {
//            if (swaps.containsKey(voxel.colorIndex)) {
//                voxel.colorIndex = swaps[voxel.colorIndex]!!
//            }
//        }
//    }

}

class NoSpaceInPaletteException : Throwable()
