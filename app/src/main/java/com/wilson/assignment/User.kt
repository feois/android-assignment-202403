package com.wilson.assignment

import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore

inline fun <reified T> Map<String, *>.getProp(key: String) = when {
    !containsKey(key) -> throw PropertyNotFoundException(key)
    get(key) !is T -> throw PropertyIncompatibleTypeException(key, T::class)
    else -> get(key) as T
}

fun Map<String, *>.toUser(username: String) = runCatching {
    User(
        username,
        getProp<String>(User.FIRST_NAME_FIELD),
        getProp<String>(User.LAST_NAME_FIELD),
    )
}

data class User(val username: String, val firstName: String, val lastName: String) {
    val fullName get() = "$firstName $lastName"

    fun validate(password: String? = null) = buildList {
        password?.let { validatePassword(it) }?.run { add(this) }
        validateUsername(username)?.run { add(this) }
        validateFirstName(firstName)?.run { add(this) }
        validateLastName(lastName)?.run { add(this) }
    }

    fun toMap(password: String? = null) = buildMap<String, Any> {
        password?.run { set(PASSWORD_FIELD, this) }
        set(FIRST_NAME_FIELD, firstName)
        set(LAST_NAME_FIELD, lastName)
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

        fun hasUser(username: String) = collection.document(username).get().continueWith { it.result.exists() }

        private fun updateUser(user: User, password: String) = collection.document(user.username).update(user.toMap())

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

        fun errorType(error: String?) = when (error) {
            ERR_USERNAME_INVALID_CHARACTER, ERR_EMPTY_USERNAME -> ERR_USERNAME
            ERR_PASSWORD_WHITESPACE, ERR_MIN_PASSWORD_LENGTH -> ERR_PASSWORD
            ERR_EMPTY_FIRST_NAME, ERR_FIRST_NAME_INVALID_CHARACTER -> ERR_FIRST_NAME
            ERR_EMPTY_LAST_NAME, ERR_LAST_NAME_INVALID_CHARACTER -> ERR_LAST_NAME
            else -> null
        }

        fun validateUsername(username: String) = runCatching {
            require(username.isNotEmpty()) { ERR_EMPTY_USERNAME }
            require(username.all { it.isLetterOrDigit() || it == '_' }) { ERR_USERNAME_INVALID_CHARACTER }
        }.exceptionOrNull()

        fun validatePassword(password: String) = runCatching {
            require(password.length >= MIN_PASSWORD_LENGTH) { ERR_MIN_PASSWORD_LENGTH }
            require(password.none { it.isWhitespace() }) { ERR_PASSWORD_WHITESPACE }
        }.exceptionOrNull()

        fun validateUsernameAndPassword(username: String, password: String)
            = listOf(validateUsername(username), validatePassword(password)).filterNotNull()

        fun validateFirstName(firstName: String) = runCatching {
            require(firstName.isNotEmpty()) { ERR_EMPTY_FIRST_NAME }
            require(firstName.all { it.isLetter() || it == ' ' }) { ERR_FIRST_NAME_INVALID_CHARACTER }
        }.exceptionOrNull()

        fun validateLastName(lastName: String) = runCatching {
            require(lastName.isNotEmpty()) { ERR_EMPTY_LAST_NAME }
            require(lastName.all { it.isLetter() || it == ' ' }) { ERR_LAST_NAME_INVALID_CHARACTER }
        }.exceptionOrNull()
    }
}