package com.victorious.gps

import com.victorious.gps.data.Clock
import com.victorious.gps.data.Profile
import com.victorious.gps.data.ProfileDao
import com.victorious.gps.data.ProfileRepository
import com.victorious.gps.data.ProfileValidationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

/** In-memory ProfileDao mirroring the Room DAO's contract (sorted by name, case-insensitive). */
private class FakeDao : ProfileDao {
    private val rows = MutableStateFlow<Map<Long, Profile>>(emptyMap())
    private var nextId = 1L
    override fun observeAll(): Flow<List<Profile>> =
        rows.map { m -> m.values.sortedBy { it.name.lowercase() } }
    override suspend fun getById(id: Long) = rows.value[id]
    override suspend fun getByName(name: String) = rows.value.values.firstOrNull { it.name.equals(name, true) }
    override suspend fun insert(profile: Profile): Long {
        val id = nextId++; rows.value = rows.value + (id to profile.copy(id = id)); return id
    }
    override suspend fun update(profile: Profile) { rows.value = rows.value + (profile.id to profile) }
    override suspend fun delete(profile: Profile) { rows.value = rows.value - profile.id }
}

class ProfileRepositoryTest {
    private val dao = FakeDao()
    private val repo = ProfileRepository(dao, Clock { 1234L })

    private fun p(name: String, lat: Double = 1.0, lng: Double = 2.0, pkg: String? = null) =
        Profile(name = name, latitude = lat, longitude = lng, targetPackage = pkg)

    @Test fun savesAndReadsBack() = runTest {
        val id = repo.save(p("  Home  ", pkg = " com.example.app "))
        val stored = repo.get(id)!!
        assertEquals("Home", stored.name)
        assertEquals("com.example.app", stored.targetPackage)
        assertEquals(1234L, stored.updatedAt)
    }

    @Test fun blankPackageBecomesNull() = runTest {
        val id = repo.save(p("Office", pkg = "   "))
        assertNull(repo.get(id)!!.targetPackage)
    }

    @Test fun listsSortedByName() = runTest {
        repo.save(p("zoo")); repo.save(p("Airport")); repo.save(p("beach"))
        assertEquals(listOf("Airport", "beach", "zoo"), repo.observeProfiles().first().map { it.name })
    }

    @Test fun updatesExisting() = runTest {
        val id = repo.save(p("Home"))
        repo.save(repo.get(id)!!.copy(latitude = 10.0, name = "Home"))
        assertEquals(10.0, repo.get(id)!!.latitude, 0.0)
        assertEquals(1, repo.observeProfiles().first().size)
    }

    @Test fun rejectsDuplicateNameCaseInsensitive() = runTest {
        repo.save(p("Home"))
        assertThrows(ProfileValidationException::class.java) { kotlinx.coroutines.runBlocking { repo.save(p("HOME")) } }
    }

    @Test fun deletes() = runTest {
        val id = repo.save(p("Gone"))
        repo.delete(repo.get(id)!!)
        assertNull(repo.get(id))
    }

    @Test fun rejectsInvalidInput() {
        val bad = listOf(
            p(""), p("   "), p("x".repeat(61)),
            p("lat", lat = 91.0), p("lng", lng = -181.0), p("nan", lat = Double.NaN),
            p("pkg", pkg = "not a package"), p("pkg2", pkg = "nodots"),
            Profile(name = "acc", latitude = 0.0, longitude = 0.0, accuracyMeters = 0f),
        )
        for (b in bad) assertThrows(ProfileValidationException::class.java) { repo.normalize(b) }
    }
}
