package pl.edu.agh.firevox.shared.model

import org.springframework.data.jpa.repository.JpaRepository

interface PhysicalMaterialRepository: JpaRepository<PhysicalMaterial, Int> {

    fun findByVoxelMaterial(voxelMaterial: VoxelMaterial): PhysicalMaterial

}