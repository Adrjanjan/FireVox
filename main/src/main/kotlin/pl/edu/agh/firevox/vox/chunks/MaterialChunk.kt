package pl.edu.agh.firevox.vox.chunks

import com.google.common.io.LittleEndianDataInputStream
import pl.edu.agh.firevox.vox.Material
import pl.edu.agh.firevox.vox.MaterialProperties

data class MaterialChunk(
    val material: Material
) : Chunk(ChunkTags.TAG_MATT) {

    companion object {
        fun construct(input: LittleEndianDataInputStream): MaterialChunk {
            input.skip(8)
            val i = input.readInt()
            val type = input.readInt()
            val weight = input.readFloat()
            val propertyBits = input.readInt()

            val material = Material(
                index = i,
                used = true,
                color = -1,
                type = type,
                weight = weight,
                propertiesBits = propertyBits,
                propertyValues = calculateProperties(propertyBits, input)
            )
            return MaterialChunk(material)
        }

        private fun calculateProperties(
            propertyBits: Int,
            inputStream: LittleEndianDataInputStream
        ): MutableMap<MaterialProperties, Float> {
            val tmpProperties = mutableMapOf<MaterialProperties, Float>()
            if (propertyBits and MaterialProperties.PLASTIC.propertyBit > 0) tmpProperties[MaterialProperties.PLASTIC] =
                inputStream.readFloat()
            if (propertyBits and MaterialProperties.ROUGHNESS.propertyBit > 0) tmpProperties[MaterialProperties.ROUGHNESS] =
                inputStream.readFloat()
            if (propertyBits and MaterialProperties.SPECULAR.propertyBit > 0) tmpProperties[MaterialProperties.SPECULAR] =
                inputStream.readFloat()
            if (propertyBits and MaterialProperties.IOR.propertyBit > 0) tmpProperties[MaterialProperties.IOR] = inputStream.readFloat()
            if (propertyBits and MaterialProperties.ATTENUATION.propertyBit > 0) tmpProperties[MaterialProperties.ATTENUATION] =
                inputStream.readFloat()
            if (propertyBits and MaterialProperties.POWER.propertyBit > 0) tmpProperties[MaterialProperties.POWER] =
                inputStream.readFloat()
            if (propertyBits and MaterialProperties.GLOW.propertyBit > 0) tmpProperties[MaterialProperties.GLOW] =
                inputStream.readFloat()
            if (propertyBits and MaterialProperties.IS_TOTAL_POWER.propertyBit > 0) tmpProperties[MaterialProperties.IS_TOTAL_POWER] =
                inputStream.readFloat()
            return tmpProperties
        }
    }
}