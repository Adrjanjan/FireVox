package pl.edu.agh.firevox.shared.model.simulation.counters

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import pl.edu.agh.firevox.shared.model.radiation.RadiationPlane

interface CountersRepository : JpaRepository<Counter, CounterId>, JpaSpecificationExecutor<RadiationPlane> {

    @Query(
        """
            select c.count = :iteration from counters c where c.id = :id
        """, nativeQuery = true
    )
    fun canExecuteForIteration(iteration: Long, id: CounterId = CounterId.CURRENT_ITERATION) : Boolean

    @Query(
        """
           update Counter c set c.count = c.count+1 where c.id = (:id) 
        """
    )
    @Modifying
    fun increment(@Param("id") id: CounterId)

    @Query(
        """
            update Counter c set c.count = (:value) where c.id = (:id) 
        """
    )
    @Modifying
    fun set(id: CounterId, value: Long)

    fun reset(id: CounterId) = set(id, 0)

}

