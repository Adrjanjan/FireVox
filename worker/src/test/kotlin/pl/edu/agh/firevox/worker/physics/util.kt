package pl.edu.agh.firevox.worker.physics

import io.kotest.core.spec.style.ShouldSpec
import org.springframework.core.io.ClassPathResource
import pl.edu.agh.firevox.shared.model.VoxelKey




fun ShouldSpec.getFile(name: String) = ClassPathResource(name).inputStream