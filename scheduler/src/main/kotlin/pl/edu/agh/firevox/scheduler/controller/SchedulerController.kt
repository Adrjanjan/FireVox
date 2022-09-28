package pl.edu.agh.pwch.shop.scheduler.controller

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import pl.edu.agh.firevox.scheduler.service.SchedulerService
import pl.edu.agh.pwch.shop.shareddto.scheduler.SchedulerResult
import pl.edu.agh.pwch.shop.shareddto.scheduler.PlaceSchedulerRequest

@RestController
@RequestMapping("/scheduler")
class SchedulerController {

    @Autowired
    lateinit var schedulerService: SchedulerService

    @PostMapping
    fun placeScheduler(@RequestBody placeSchedulerRequest: PlaceSchedulerRequest): SchedulerResult = schedulerService.placeScheduler(placeSchedulerRequest)
}

