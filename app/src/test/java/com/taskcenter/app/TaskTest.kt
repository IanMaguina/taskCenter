package com.taskcenter.app

import com.taskcenter.app.data.database.entity.Task
import com.taskcenter.app.data.database.entity.TaskStatus
import org.junit.Assert.*
import org.junit.Test

class TaskTest {

    @Test
    fun task_defaultStatus_isPending() {
        val task = Task(
            id = "1",
            spaceId = "space1",
            name = "Test Task",
            estimatedTime = 30
        )
        assertEquals(TaskStatus.PENDING, task.status)
    }

    @Test
    fun task_realTime_calculatedCorrectly() {
        val startedAt = 1000L
        val completedAt = 61000L  // 60 seconds later = 1 minute
        val realTime = (completedAt - startedAt) / 60000  // in minutes
        assertEquals(1L, realTime)
    }

    @Test
    fun task_withReward_hasCorrectValue() {
        val task = Task(
            id = "2",
            spaceId = "space1",
            name = "Rewarded Task",
            reward = 50
        )
        assertEquals(50, task.reward)
        assertTrue(task.reward > 0)
    }

    @Test
    fun task_takeTask_changesStatus() {
        val task = Task(
            id = "3",
            spaceId = "space1",
            name = "Take Task Test",
            status = TaskStatus.PENDING
        )
        val updated = task.copy(
            status = TaskStatus.IN_PROGRESS,
            assignedToId = "user1",
            assignedToName = "Alice",
            startedAt = System.currentTimeMillis()
        )
        assertEquals(TaskStatus.IN_PROGRESS, updated.status)
        assertEquals("user1", updated.assignedToId)
        assertNotNull(updated.startedAt)
    }

    @Test
    fun task_completeTask_changesStatus() {
        val startedAt = System.currentTimeMillis() - 120000L  // 2 minutes ago
        val task = Task(
            id = "4",
            spaceId = "space1",
            name = "Complete Task Test",
            status = TaskStatus.IN_PROGRESS,
            startedAt = startedAt
        )
        val completedAt = System.currentTimeMillis()
        val realTimeMinutes = (completedAt - startedAt) / 60000
        val updated = task.copy(
            status = TaskStatus.COMPLETED,
            completedAt = completedAt,
            realTime = realTimeMinutes
        )
        assertEquals(TaskStatus.COMPLETED, updated.status)
        assertTrue(updated.realTime >= 2L)
    }
}
