package net.calvuz.qstore.backup.domain.usecase

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import net.calvuz.qstore.backup.domain.model.BackupOptions
import net.calvuz.qstore.backup.domain.model.BackupProgress
import net.calvuz.qstore.backup.domain.model.BackupResult
import net.calvuz.qstore.backup.domain.repository.BackupRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test
import java.io.File

class CreateBackupUseCaseTest {

    private val repository = mockk<BackupRepository>()
    private val useCase = CreateBackupUseCase(repository)

    @Test
    fun `invoke delegates to repository createBackup with given options`() = runTest {
        val options = BackupOptions(compressionLevel = 9)
        val progress = BackupProgress("test", 0.5f)
        every { repository.createBackup(options) } returns flowOf(progress)

        val result = useCase(options).let { flow ->
            var last: BackupProgress? = null
            flow.collect { last = it }
            last
        }

        assertEquals(progress, result)
    }

    @Test
    fun `invoke uses default options when none provided`() {
        every { repository.createBackup(BackupOptions()) } returns flowOf()

        useCase()

        // Il default di BackupOptions() deve arrivare invariato al repository.
        verify { repository.createBackup(BackupOptions()) }
    }

    @Test
    fun `sync delegates to repository createBackupSync and returns its result`() = runTest {
        val options = BackupOptions(includeFeatures = false)
        val expected = BackupResult.Success(File("backup.zip"), mockk(relaxed = true), 1024L)
        coEvery { repository.createBackupSync(options) } returns expected

        val result = useCase.sync(options)

        assertSame(expected, result)
        coVerify(exactly = 1) { repository.createBackupSync(options) }
    }

    @Test
    fun `estimateSize delegates to repository estimateBackupSize`() = runTest {
        coEvery { repository.estimateBackupSize() } returns 123456L

        val size = useCase.estimateSize()

        assertEquals(123456L, size)
    }
}
