package com.acilnakit

import com.acilnakit.data.model.Task
import com.acilnakit.util.toModel
import com.acilnakit.util.toListModel
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.QueryDocumentSnapshot
import com.google.firebase.firestore.QuerySnapshot
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

/**
 * Advanced Firestore Mapping Tests
 * Verifies that DocumentSnapshot to Model conversions work correctly,
 * including the automatic ID injection.
 */
class FirestoreMappingTest {

    @Test
    fun `toModel - Correctly converts and injects ID`() {
        // Preparation
        val mockSnapshot = mockk<DocumentSnapshot>()
        val mockId = "task_premium_id_123"
        val mockTask = Task(title = "Test Task")

        every { mockSnapshot.id } returns mockId
        every { mockSnapshot.toObject(Task::class.java) } returns mockTask

        // Execution
        val result = mockSnapshot.toModel<Task>()

        // Verification
        assertNotNull(result)
        assertEquals(mockId, result?.id)
        assertEquals("Test Task", result?.title)
    }

    @Test
    fun `toListModel - Correctly converts a list of documents`() {
        // Preparation
        val mockQuerySnapshot = mockk<QuerySnapshot>()
        val mockDoc1 = mockk<QueryDocumentSnapshot>()
        val mockDoc2 = mockk<QueryDocumentSnapshot>()
        
        val task1 = Task(title = "Task 1")
        val task2 = Task(title = "Task 2")

        every { mockDoc1.id } returns "id1"
        every { mockDoc1.toObject(Task::class.java) } returns task1
        
        every { mockDoc2.id } returns "id2"
        every { mockDoc2.toObject(Task::class.java) } returns task2

        every { mockQuerySnapshot.documents } returns listOf(mockDoc1, mockDoc2)

        // Execution
        val result = mockQuerySnapshot.toListModel<Task>()

        // Verification
        assertEquals(2, result.size)
        assertEquals("id1", result[0].id)
        assertEquals("id2", result[1].id)
    }
}
