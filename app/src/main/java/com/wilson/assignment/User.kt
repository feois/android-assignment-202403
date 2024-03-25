package com.wilson.assignment

import android.content.Context
import androidx.datastore.preferences.core.edit
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.security.MessageDigest

/**
 * Hashing Utils
 * @author Sam Clarke <www.samclarke.com>
 * @license MIT
 */
object HashUtils {
    fun sha512(input: String) = hashString("SHA-512", input)

    fun sha256(input: String) = hashString("SHA-256", input)

    fun sha1(input: String) = hashString("SHA-1", input)

    /**
     * Supported algorithms on Android:
     *
     * Algorithm	Supported API Levels
     * MD5          1+
     * SHA-1	    1+
     * SHA-224	    1-8,22+
     * SHA-256	    1+
     * SHA-384	    1+
     * SHA-512	    1+
     */
    private fun hashString(type: String, input: String): String {
        val HEX_CHARS = "0123456789ABCDEF"
        val bytes = MessageDigest
                .getInstance(type)
                .digest(input.toByteArray())
        val result = StringBuilder(bytes.size * 2)

        bytes.forEach {
            val i = it.toInt()
            result.append(HEX_CHARS[i shr 4 and 0x0f])
            result.append(HEX_CHARS[i and 0x0f])
        }

        return result.toString()
    }
}

data class User(val username: String, val firstName: String, val lastName: String) {
    val fullName get() = "$firstName $lastName"

    fun validate() = listOf(
        validateUsername(username),
        validateFirstName(firstName),
        validateLastName(lastName),
    ).mapNotNull { it.exceptionOrNull() }

    fun verify(password: String) = collection.document(username).get().continueWith {
        it.result?.let { doc ->
            doc.takeIf { doc.exists() }?.let {
                val verified = doc.getString(PASSWORD_FIELD) == HashUtils.sha256(password)

                if (verified) {
                    session = this
                }

                verified
            }
        }
    }

    companion object {
        const val USER_COLLECTION = "users"
        const val PASSWORD_FIELD = "password"
        const val FIRST_NAME_FIELD = "first name"
        const val LAST_NAME_FIELD = "last name"

        const val MIN_PASSWORD_LENGTH = 8

        const val ERR_USERNAME = "Invalid username"
        const val ERR_EMPTY_USERNAME = "Username cannot be empty"
        const val ERR_USERNAME_INVALID_CHARACTER = "Username must consist of only letters, digits and underscore"
        const val ERR_PASSWORD = "Invalid password"
        const val ERR_MIN_PASSWORD_LENGTH = "Password must consist of at least $MIN_PASSWORD_LENGTH characters"
        const val ERR_PASSWORD_WHITESPACE = "Password must not contain any whitespace"
        const val ERR_FIRST_NAME = "Invalid first name"
        const val ERR_EMPTY_FIRST_NAME = "First name cannot be empty"
        const val ERR_FIRST_NAME_INVALID_CHARACTER = "First Name must only consist of only letters and space"
        const val ERR_LAST_NAME = "Invalid last name"
        const val ERR_EMPTY_LAST_NAME = "Last name cannot be empty"
        const val ERR_LAST_NAME_INVALID_CHARACTER = "Last Name must only consist of only letters and space"

        val db get() = Firebase.firestore
        val collection get() = db.collection(USER_COLLECTION)

        var session: User? = null; internal set

        suspend fun readCredential(context: Context) =
            context.dataStore.data.map { it[CREDENTIAL] }.first()?.let { username ->
                getUser(username).continueWith {
                    if (it.result == null) {
                        false
                    }
                    else {
                        session = it.result
                        true
                    }
                }
            }

        suspend fun storeCredential(context: Context) = session?.apply { context.dataStore.edit { it[CREDENTIAL] = username } }

        suspend fun clearCredential(context: Context) = context.dataStore.edit { it.remove(CREDENTIAL) }

        fun hasUser(username: String) = collection.document(username)
                    .get()
                    .continueWith {
                        it.result?.exists()
                    }

        fun addUser(user: User, password: String) = hasUser(user.username).continueWithTask {
            it.result?.let { userExist ->
                if (userExist) {
                    it.continueWith {
                        false
                    }
                }
                else {
                    collection.document(user.username)
                            .set(hashMapOf(
                                PASSWORD_FIELD to HashUtils.sha256(password),
                                FIRST_NAME_FIELD to user.firstName,
                                LAST_NAME_FIELD to user.lastName,
                            ))
                            .continueWith { true }
                }
            } ?: it
        }

        fun getUser(username: String) = collection.document(username)
                    .get()
                    .continueWith {
                        it.result?.takeIf { it.exists() }?.let { doc ->
                            val firstName =
                                doc.getString(FIRST_NAME_FIELD) ?: return@continueWith null
                            val lastName =
                                doc.getString(LAST_NAME_FIELD) ?: return@continueWith null

                            User(
                                username,
                                firstName,
                                lastName,
                            )
                        }
                    }

        fun errorType(error: String) = when (error) {
            ERR_USERNAME_INVALID_CHARACTER, ERR_EMPTY_USERNAME -> ERR_USERNAME
            ERR_PASSWORD_WHITESPACE, ERR_MIN_PASSWORD_LENGTH -> ERR_PASSWORD
            ERR_EMPTY_FIRST_NAME, ERR_FIRST_NAME_INVALID_CHARACTER -> ERR_FIRST_NAME
            ERR_EMPTY_LAST_NAME, ERR_LAST_NAME_INVALID_CHARACTER -> ERR_LAST_NAME
            else -> null
        }

        fun validateUsername(username: String) = runCatching {
            require(username.isNotEmpty()) { ERR_EMPTY_USERNAME }
            require(username.all { it.isLetterOrDigit() || it == '_' }) { ERR_USERNAME_INVALID_CHARACTER }
        }

        fun validatePassword(password: String) = runCatching {
            require(password.length >= MIN_PASSWORD_LENGTH) { ERR_MIN_PASSWORD_LENGTH }
            require(password.none { it.isWhitespace() }) { ERR_PASSWORD_WHITESPACE }
        }

        fun validateUsernameAndPassword(username: String, password: String)
            = listOf(validateUsername(username), validatePassword(password)).mapNotNull { it.exceptionOrNull() }

        fun validateFirstName(firstName: String) = runCatching {
            require(firstName.isNotEmpty()) { ERR_EMPTY_FIRST_NAME }
            require(firstName.all { it.isLetter() || it == ' ' }) { ERR_FIRST_NAME_INVALID_CHARACTER }
        }

        fun validateLastName(lastName: String) = runCatching {
            require(lastName.isNotEmpty()) { ERR_EMPTY_LAST_NAME }
            require(lastName.all { it.isLetter() || it == ' ' }) { ERR_LAST_NAME_INVALID_CHARACTER }
        }
    }
}