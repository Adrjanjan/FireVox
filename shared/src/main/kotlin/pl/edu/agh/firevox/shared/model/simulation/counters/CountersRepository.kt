package pl.edu.agh.firevox.shared.model.simulation.counters

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.transaction.annotation.Transactional
import pl.edu.agh.firevox.shared.model.radiation.RadiationPlane

@Transactional
interface CountersRepository : JpaRepository<Counter, CounterId>, JpaSpecificationExecutor<RadiationPlane> {

    @Query(
        """
            select c.count = :iteration from counters c where c.id = 'CURRENT_ITERATION'
        """, nativeQuery = true
    )
    fun canExecuteForIteration(iteration: Long) : Boolean

    @Query(
        """
           update Counter c set c.count = c.count+1 where c.id = (:id) 
        """
    )
    @Modifying
    fun increment(@Param("id") id: CounterId)

    @Query(
        """
           update Counter c set c.count = c.count + :add where c.id = (:id) 
        """
    )
    @Modifying
    fun add(@Param("id") id: CounterId, add: Int)

    @Query(
        """
            update Counter c set c.count = (:value) where c.id = (:id) 
        """
    )
    @Modifying
    fun set(id: CounterId, value: Long)

    @Query(
        """
            update Counter c set c.count = 0 where c.id = (:id) 
        """
    )
    @Modifying
    fun reset(id: CounterId)

}

