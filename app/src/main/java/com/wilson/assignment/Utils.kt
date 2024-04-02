@file:Suppress("unused")

package com.wilson.assignment

import android.content.Context
import android.text.Editable
import android.util.Log
import android.widget.Toast
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import java.security.MessageDigest
import kotlin.reflect.KClass

abstract class PropertyException(message: String): IllegalArgumentException(message)
class PropertyNotFoundException(val propertyName: String): PropertyException("Property $propertyName not found")
class PropertyIncompatibleTypeException(val propertyName: String, val type: KClass<*>): PropertyException("Property $propertyName is not ${type.qualifiedName}")
class UserNotFoundException(val username: String): Exception("User $username does not exist")
class IncorrectPasswordException: Exception("Incorrect password")
class UserAlreadyExistsException(val username: String): Exception("User $username already exists")

@Suppress("SpellCheckingInspection")
const val LOG_TAG = "myass"

fun logInfo(s: String) = Log.i(LOG_TAG, s)
fun logWarning(s: String, e: Throwable? = null) = Log.w(LOG_TAG, s, e)
fun logError(s: String, e: Throwable? = null) = Log.e(LOG_TAG, s, e)

fun Context.shortToast(s: String) = Toast.makeText(this, s, Toast.LENGTH_SHORT).show()
fun Context.longToast(s: String) = Toast.makeText(this, s, Toast.LENGTH_LONG).show()
fun Context.errorToast(s: String, e: Throwable) {
    logError(s, e)
    longToast("Error: ${e::class.simpleName}")
}

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

val db get() = Firebase.firestore
typealias FirestoreMap = Map<String, *>

inline fun <reified T> FirestoreMap.getProp(key: String) = when {
    !containsKey(key) -> throw PropertyNotFoundException(key)
    get(key) !is T -> throw PropertyIncompatibleTypeException(key, T::class)
    else -> get(key) as T
}

inline fun FirestoreMap.getIntProp(key: String) = getProp<Long>(key).toInt()

inline fun <reified T> FirestoreMap.getListProp(key: String) = getProp<List<*>>(key).filterIsInstance<T>()

fun Editable.setString(string: String) {
    clear()
    append(string)
}
fun Editable.trimWhitespaces(): String {
    val string = toString().trimWhitespaces()
    setString(string)
    return string
}
fun String.trimWhitespaces() = this.replace("\\s+".toRegex(), " ").trimStart().trimEnd()

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
