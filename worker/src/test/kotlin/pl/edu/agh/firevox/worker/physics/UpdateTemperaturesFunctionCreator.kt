package pl.edu.agh.firevox.worker.physics

import jakarta.persistence.EntityManager
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UpdateTemperaturesFunctionCreator(
    private val entityManager: EntityManager
) {
    @Transactional
    fun createUpdateTemperatures(dynamicSql: String = """
CREATE
    OR REPLACE FUNCTION update_temperatures(iteration integer, volume numeric) RETURNS VOID AS
${'$'}${'$'}
DECLARE
    pc_id                        integer;
    pc_child_id                  integer;
    pc_q_net                     numeric;
    v_even_iteration_temperature numeric;
    m_density                    numeric;
    m_specific_heat_capacity     numeric;
    voxels_count                 integer;
    v_x                          integer;
    v_y                          integer;
    v_z                          integer;
    temp_increase                numeric;
BEGIN
    IF iteration % 2 = 0 THEN -- even
        FOR pc_id, pc_q_net, pc_child_id IN (SELECT pc.id, pc.q_net, pc.child_id
                                             FROM planes_connections pc
                                             where pc.q_net > 0.0)
            LOOP
                FOR v_x, v_y, v_z, v_even_iteration_temperature, m_density, m_specific_heat_capacity, voxels_count IN
                    (SELECT v.x, v.y, v.z, v.even_iteration_temperature, m.density, m.specific_heat_capacity
                     from voxels v
                              join materials m on m.id = v.even_iteration_material_id
                     where (v.x, v.y, v.z) in
                           (select voxel_key_x, voxel_key_y, voxel_key_z
                            from plane_voxels
                            where plane_id = pc_child_id))
                    LOOP
                        select count(*) into voxels_count from plane_voxels where plane_id = pc_child_id;
                        temp_increase \:= pc_q_net / (volume * m_density * m_specific_heat_capacity * voxels_count);

                        UPDATE voxels v
                        SET even_iteration_temperature = v.even_iteration_temperature + temp_increase
                        WHERE (x, y, z) = (v_x, v_y, v_z);
                    END LOOP;
            END LOOP;
    ELSE -- odd
        FOR pc_id, pc_q_net, pc_child_id IN (SELECT pc.id, pc.q_net, pc.child_id
                                             FROM planes_connections pc
                                             where pc.q_net > 0.0)
            LOOP
                FOR v_x, v_y, v_z, v_even_iteration_temperature, m_density, m_specific_heat_capacity, voxels_count IN
                    (SELECT v.x, v.y, v.z, v.odd_iteration_temperature, m.density, m.specific_heat_capacity
                     from voxels v
                              join materials m on m.id = v.even_iteration_material_id
                     where (v.x, v.y, v.z) in
                           (select voxel_key_x, voxel_key_y, voxel_key_z
                            from plane_voxels
                            where plane_id = pc_child_id))
                    LOOP
                        select count(*) into voxels_count from plane_voxels where plane_id = pc_child_id;
                        temp_increase \:= pc_q_net / (volume * m_density * m_specific_heat_capacity * voxels_count);

                        UPDATE voxels v
                        SET odd_iteration_temperature = v.odd_iteration_temperature + temp_increase
                        WHERE (x, y, z) = (v_x, v_y, v_z);
                    END LOOP;
            END LOOP;
    END IF;
END;
${'$'}${'$'} LANGUAGE plpgsql;
    """) {
        entityManager.createNativeQuery(dynamicSql).executeUpdate()
    }

}