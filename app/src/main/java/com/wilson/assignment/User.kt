package com.wilson.assignment

import android.content.Context
import android.content.Intent
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.DocumentId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

fun FirestoreMap.toUser(username: String) = runCatching {
    User(
        username,
        getProp(User.FIRST_NAME_FIELD),
        getProp(User.LAST_NAME_FIELD),
    ).apply {
        try {
            for (notificationId in getListProp<String>(User.READ_NOTIFICATIONS_FIELD)) {
                readNotifications.add(notificationId)
            }

            for ((k, v) in getProp<FirestoreMap>(User.QUIZ_RESULTS_FIELD)) {
                (v as? Int)?.let { marks ->
                    results.put(k, marks)
                }
            }
        }
        catch (_: PropertyException) {}
    }
}

abstract class UsernameException(message: String): IllegalArgumentException(message)
class EmptyUsernameException: UsernameException("Username cannot be empty")
class InvalidCharUsernameException: UsernameException("Username must consist of only letters, digits and underscore")
abstract class PasswordException(message: String): IllegalArgumentException(message)
class PasswordLengthException: PasswordException("Password must consist of at least ${User.MIN_PASSWORD_LENGTH} characters")
class InvalidCharPasswordException: PasswordException("Password must not contain any whitespace")
abstract class NameException(message: String, val isFirst: Boolean): IllegalArgumentException(message)
class EmptyNameException(isFirst: Boolean): NameException("Name cannot be empty", isFirst)
class InvalidCharNameException(isFirst: Boolean): NameException("Name must only consist of only letters or space", isFirst)

data class User(@DocumentId val username: String = "", val firstName: String, val lastName: String) {
    val fullName get() = "$firstName $lastName"

    var readNotifications = mutableSetOf<String>()
    var results = mutableMapOf<String, Int>()

    fun validate(password: String? = null) = buildList {
        password?.let { validatePassword(it) }?.run { add(this) }
        validateUsername(username)?.run { add(this) }
        validateFirstName(firstName)?.run { add(this) }
        validateLastName(lastName)?.run { add(this) }
    }

    fun toMap(password: String? = null) = buildMap {
        password?.run { set(PASSWORD_FIELD, this) }
        set(FIRST_NAME_FIELD, firstName)
        set(LAST_NAME_FIELD, lastName)
        set(READ_NOTIFICATIONS_FIELD, readNotifications.toList())
        set(QUIZ_RESULTS_FIELD, results)
    }

    companion object {
        const val USER_COLLECTION = "users"
        const val PASSWORD_FIELD = "password"
        const val FIRST_NAME_FIELD = "firstName"
        const val LAST_NAME_FIELD = "lastName"
        const val READ_NOTIFICATIONS_FIELD = "read"
        const val QUIZ_RESULTS_FIELD = "results"

        const val MIN_PASSWORD_LENGTH = 8

        val collection get() = db.collection(USER_COLLECTION)

        fun hasUser(username: String) = collection.document(username).get().continueWith { it.result.exists() }

        fun getUser(username: String) = collection.document(username)
                    .get()
                    .continueWith { task ->
                        task.runCatching {
                            when {
                                !isSuccessful -> throw task.exception!!
                                !result.exists() -> throw UserNotFoundException(username)
                                else -> result.data!!.toUser(username).getOrThrow()
                            }
                        }
                    }

        fun validateUsername(username: String) = when {
            username.isEmpty() -> EmptyUsernameException()
            !username.all { it.isLetterOrDigit() || it == '_' } -> InvalidCharUsernameException()
            else -> null
        }

        fun validatePassword(password: String) = when {
            password.length < MIN_PASSWORD_LENGTH -> PasswordLengthException()
            password.any { it.isWhitespace() } -> InvalidCharPasswordException()
            else -> null
        }

        fun validateUsernameAndPassword(username: String, password: String) = buildList {
            validateUsername(username)?.run { add(this) }
            validatePassword(password)?.run { add(this) }
        }

        fun validateFirstName(firstName: String) = when {
            firstName.isEmpty() -> EmptyNameException(true)
            !firstName.all { it.isLetter() || it == ' ' } -> InvalidCharNameException(true)
            else -> null
        }

        fun validateLastName(lastName: String) = when {
            lastName.isEmpty() -> EmptyNameException(false)
            !lastName.all { it.isLetter() || it == ' ' } -> InvalidCharNameException(false)
            else -> null
        }
    }
}


class UserViewModel: ViewModel() {
    private val CREDENTIAL = stringPreferencesKey("credential")

    private val userLiveData = MutableLiveData<User?>()
    val user: LiveData<User?> get() = userLiveData

    private val signUpResultLiveData = MutableLiveData<Result<Unit>>()
    val signUpResult: LiveData<Result<Unit>> get() = signUpResultLiveData

    private val loginResultLiveData = MutableLiveData<Result<Unit>>()
    val loginResult: LiveData<Result<Unit>> get() = loginResultLiveData

    private val databaseExceptionLiveData = MutableLiveData<Throwable>()
    val databaseException: LiveData<Throwable> get() = databaseExceptionLiveData

    private val readNotifications = mutableSetOf<String>()

    private lateinit var passwordCache: String

    fun updateUser(map: FirestoreMap? = null) = user.value?.run {
        val userMap = toMap()
        val combinedMap = if (map != null) { userMap + map.filterKeys {
            when (it) {
                User.FIRST_NAME_FIELD, User.LAST_NAME_FIELD -> true
                else -> false
            }
        } } else { userMap }

        User.collection.document(username).update(combinedMap)
    }

    fun readCredential(dataStore: DataStore<Preferences>, successCallback: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            dataStore.data.map { it[CREDENTIAL] }.first()?.let { username ->
                User.collection.document(username).get()
                        .addOnSuccessListener {
                            if (it.exists()) {
                                it.data!!.runCatching {
                                    val password = getProp<String>(User.PASSWORD_FIELD)
                                    val user = toUser(username).getOrThrow()

                                    userLiveData.value = user
                                    passwordCache = password
                                    successCallback()
                                }.exceptionOrNull()?.run { databaseExceptionLiveData.value = this }
                            }
                            else {
                                databaseExceptionLiveData.value = UserNotFoundException(username)
                            }
                        }
                        .addOnFailureListener { databaseExceptionLiveData.value = it }
            }
        }
    }

    private suspend fun storeCredential(dataStore: DataStore<Preferences>) {
        withContext(Dispatchers.IO) {
            user.value?.run { dataStore.edit { it[CREDENTIAL] = username } }
        }
    }

    fun login(username: String, password: String, credentialDataStore: DataStore<Preferences>? = null) {
        User.collection.document(username).get()
                .addOnSuccessListener {
                    when {
                        !it.exists() -> loginResultLiveData.value = Result.failure(UserNotFoundException(username))
                        it.getString(User.PASSWORD_FIELD) != HashUtils.sha256(password)
                        -> loginResultLiveData.value = Result.failure(IncorrectPasswordException())
                        else -> it.data!!.toUser(username).fold({ user ->
                            userLiveData.value = user
                            credentialDataStore?.let { dataStore ->
                                viewModelScope.launch {
                                    storeCredential(dataStore)
                                }
                            }
                            loginResultLiveData.value = Result.success(Unit)
                            passwordCache = password
                        }, { e ->
                            databaseExceptionLiveData.value = e
                        })
                    }
                }
                .addOnFailureListener { databaseExceptionLiveData.value = it }
    }

    fun signUp(user: User, password: String, credentialDataStore: DataStore<Preferences>? = null) {
        user.readNotifications = readNotifications

        User.hasUser(user.username)
                .addOnSuccessListener { userExists ->
                    when {
                        userExists -> signUpResultLiveData.value = Result.failure(UserAlreadyExistsException(user.username))
                        else -> User.collection.document(user.username)
                                .set(user.toMap(HashUtils.sha256(password)))
                                .addOnSuccessListener {
                                    credentialDataStore?.let { dataStore ->
                                        viewModelScope.launch {
                                            storeCredential(dataStore)
                                        }
                                    }
                                    userLiveData.value = user
                                    signUpResultLiveData.value = Result.success(Unit)
                                    passwordCache = password
                                }
                                .addOnFailureListener { databaseExceptionLiveData.value = it }
                    }
                }
                .addOnFailureListener { databaseExceptionLiveData.value = it }
    }

    fun logout(credentialContext: Context? = null) {
        userLiveData.value = null
        readNotifications.clear()
        credentialContext?.apply {
            viewModelScope.launch {
                dataStore.edit { it.remove(CREDENTIAL) }
            }
        }
    }

    fun refresh() {
        user.value?.username?.let { username ->
            User.getUser(username)
                    .addOnSuccessListener { result ->
                        result.fold({
                            userLiveData.value = it
                        }, {
                            databaseExceptionLiveData.value = it
                        })
                    }
                    .addOnFailureListener { databaseExceptionLiveData.value = it }
        }
    }

    fun startQuiz(context: Context, quizId: String) {
        val intent = Intent(context, QuizActivity::class.java)

        intent.putExtra(QuizActivity.INTENT_QUIZ_ID, quizId)

        user.value?.run {
            intent.putExtra(QuizActivity.INTENT_USERNAME, username)
            intent.putExtra(QuizActivity.INTENT_PASSWORD, passwordCache)
        }

        context.startActivity(intent)
    }

    fun readNotification(id: String) {
        (user.value?.readNotifications ?: readNotifications).add(id)
        user.value?.run { updateUser() }
    }

    fun hasRead(id: String) = (user.value?.readNotifications ?: readNotifications).contains(id)
}
