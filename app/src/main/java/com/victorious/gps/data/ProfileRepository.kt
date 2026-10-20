package com.victorious.gps.data

import com.victorious.gps.domain.CoordinateResult
import com.victorious.gps.domain.CoordinateValidator
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

fun interface Clock { fun now(): Long }

class ProfileValidationException(message: String) : IllegalArgumentException(message)

@Singleton
class ProfileRepository @Inject constructor(
    private val dao: ProfileDao,
    private val clock: Clock,
) {
    fun observeProfiles(): Flow<List<Profile>> = dao.observeAll()

    suspend fun get(id: Long): Profile? = dao.getById(id)

    /** Inserts or updates after validating. Returns the stored id. */
    suspend fun save(profile: Profile): Long {
        val clean = normalize(profile)
        val clash = dao.getByName(clean.name)
        if (clash != null && clash.id != clean.id) {
            throw ProfileValidationException("A profile named \"${clean.name}\" already exists")
        }
        return if (clean.id == 0L) {
            dao.insert(clean)
        } else {
            dao.update(clean); clean.id
        }
    }

    suspend fun delete(profile: Profile) = dao.delete(profile)

    internal fun normalize(p: Profile): Profile {
        val name = p.name.trim()
        if (name.isEmpty()) throw ProfileValidationException("Profile name is required")
        if (name.length > MAX_NAME) throw ProfileValidationException("Profile name is too long")
        val r = CoordinateValidator.validate(p.latitude, p.longitude)
        if (r is CoordinateResult.Invalid) throw ProfileValidationException(r.reason)
        if (!p.accuracyMeters.isFinite() || p.accuracyMeters <= 0f) {
            throw ProfileValidationException("Accuracy must be greater than 0")
        }
        val pkg = p.targetPackage?.trim()?.takeIf { it.isNotEmpty() }
        if (pkg != null && !PACKAGE_RE.matches(pkg)) {
            throw ProfileValidationException("Target app must be a package name like com.example.app")
        }
        return p.copy(name = name, targetPackage = pkg, updatedAt = clock.now())
    }

    companion object {
        const val MAX_NAME = 60
        private val PACKAGE_RE = Regex("""^[a-zA-Z][a-zA-Z0-9_]*(\.[a-zA-Z][a-zA-Z0-9_]*)+$""")
    }
}
