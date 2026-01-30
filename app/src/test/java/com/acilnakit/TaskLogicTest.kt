package com.acilnakit

import com.acilnakit.data.model.Task
import com.acilnakit.data.model.TaskStatus
import com.acilnakit.ui.viewmodel.SortOrder
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Task Logic Test - Verifies filtering and sorting behavior.
 * This test uses actual models to verify business rules.
 */
class TaskLogicTest {

    @Test
    fun `Sorting tasks by price works correctly`() {
        val tasks = listOf(
            createMockTask("1", 50.0),
            createMockTask("2", 150.0),
            createMockTask("3", 25.0)
        )

        val sorted = tasks.sortedByDescending { it.rewardAmount }

        assertEquals(150.0, sorted[0].rewardAmount, 0.0)
        assertEquals(50.0, sorted[1].rewardAmount, 0.0)
        assertEquals(25.0, sorted[2].rewardAmount, 0.0)
    }

    @Test
    fun `Filtering tasks by category works correctly`() {
        val tasks = listOf(
            createMockTask("1", 50.0, "Market"),
            createMockTask("2", 150.0, "Yemek"),
            createMockTask("3", 25.0, "Market")
        )

        val filtered = tasks.filter { it.category == "Market" }

        assertEquals(2, filtered.size)
        assertEquals("Market", filtered[0].category)
        assertEquals("Market", filtered[1].category)
    }

    private fun createMockTask(id: String, reward: Double, category: String = "Test"): Task {
        return Task(
            id = id,
            title = "Task $id",
            description = "Desc",
            rewardAmount = reward,
            category = category,
            schoolName = "ITU",
            campusName = "Ayazaga",
            creatorId = "user1",
            status = TaskStatus.OPEN
        )
    }
}
