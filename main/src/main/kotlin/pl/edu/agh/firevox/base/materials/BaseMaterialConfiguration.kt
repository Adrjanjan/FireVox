package pl.edu.agh.firevox.base.materials

import jakarta.annotation.PostConstruct
import org.springframework.context.annotation.Configuration
import pl.edu.agh.firevox.shared.model.PhysicalMaterialRepository

@Configuration
class BaseMaterialConfiguration(
    private val physicalMaterialRepository: PhysicalMaterialRepository
) {

//    @PostConstruct
//    fun defineBaseMaterials() {
//        val
//    }



}