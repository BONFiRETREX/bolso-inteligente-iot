package com.securitybag.app

import com.securitybag.app.data.fake.FakeBagStatusRepository
import com.securitybag.app.domain.model.AlarmType
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FakeBagStatusRepositoryTest {
    @Test
    fun initialScenarioIsArmedAndSecure() {
        val repository = FakeBagStatusRepository()

        assertTrue(repository.bagStatus.value.armed)
        assertTrue(repository.bagStatus.value.isSecure)
        assertFalse(repository.bagStatus.value.bagOpen)
    }

    @Test
    fun thirdScenarioKeepsOpeningAlarmLatched() {
        val repository = FakeBagStatusRepository()

        repository.advanceScenario()
        repository.advanceScenario()

        assertTrue(repository.bagStatus.value.bagOpen)
        assertTrue(repository.bagStatus.value.alarm.isLatched)
        assertTrue(repository.bagStatus.value.alarm.type == AlarmType.OPENING_DETECTED)
    }
}
