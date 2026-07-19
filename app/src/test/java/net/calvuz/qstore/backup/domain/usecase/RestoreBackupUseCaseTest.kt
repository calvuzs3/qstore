package net.calvuz.qstore.backup.domain.usecase

import android.net.Uri
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import net.calvuz.qstore.backup.domain.model.BackupMetadata
import net.calvuz.qstore.backup.domain.model.BackupProgress
import net.calvuz.qstore.backup.domain.model.RestoreOptions
import net.calvuz.qstore.backup.domain.model.RestoreResult
import net.calvuz.qstore.backup.domain.repository.BackupRepository
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class RestoreBackupUseCaseTest {

    private val repository = mockk<BackupRepository>()
    private val useCase = RestoreBackupUseCase(repository)

    @Test
    fun `invoke with File delegates to repository restoreBackup(File)`() {
        val file = File("backup.zip")
        val options = RestoreOptions(verifyChecksums = false)
        val progress = flowOf(BackupProgress("test", 1.0f))
        every { repository.restoreBackup(file, options) } returns progress

        val result = useCase(file, options)

        assertSame(progress, result)
    }

    @Test
    fun `invoke with Uri delegates to repository restoreBackup(Uri)`() {
        val uri = mockk<Uri>()
        val options = RestoreOptions()
        val progress = flowOf(BackupProgress("test", 1.0f))
        every { repository.restoreBackup(uri, options) } returns progress

        val result = useCase(uri, options)

        assertSame(progress, result)
    }

    @Test
    fun `sync delegates to repository restoreBackupSync`() = runTest {
        val file = File("backup.zip")
        val expected = RestoreResult.Success(mockk(relaxed = true), mockk(relaxed = true))
        coEvery { repository.restoreBackupSync(file, RestoreOptions()) } returns expected

        val result = useCase.sync(file)

        assertSame(expected, result)
        coVerify(exactly = 1) { repository.restoreBackupSync(file, RestoreOptions()) }
    }

    @Test
    fun `validate File delegates to repository validateBackup(File)`() = runTest {
        val file = File("backup.zip")
        val metadata = mockk<BackupMetadata>()
        coEvery { repository.validateBackup(file) } returns Result.success(metadata)

        val result = useCase.validate(file)

        assertTrue(result.isSuccess)
        assertSame(metadata, result.getOrNull())
    }

    @Test
    fun `validate Uri delegates to repository validateBackup(Uri)`() = runTest {
        val uri = mockk<Uri>()
        coEvery { repository.validateBackup(uri) } returns Result.failure(Exception("boom"))

        val result = useCase.validate(uri)

        assertTrue(result.isFailure)
        coVerify(exactly = 1) { repository.validateBackup(uri) }
    }
}
