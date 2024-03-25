package com.wilson.assignment

import com.google.android.gms.tasks.Task
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore


data class User(val username: String, val password: String, val firstName: String, val lastName: String) {
    val fullName get() = "$firstName $lastName"

    fun validate(): List<Throwable> {
        val list = arrayListOf<Throwable>()

        list.addAll(validateUsernameAndPassword(username, password))
        validateFirstName(firstName).exceptionOrNull()?.let { list.add(it) }
        validateLastName(lastName).exceptionOrNull()?.let { list.add(it) }

        return list
    }

    @Suppress("MemberVisibilityCanBePrivate")
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

        fun hasUser(username: String): Task<Boolean?> {
            return collection.document(username)
                    .get()
                    .continueWith {
                        it.result?.exists()
                    }
        }

        fun addUser(user: User): Task<Boolean?> {
            return hasUser(user.username).continueWithTask {
                        it.result?.let { userExist ->
                            if (userExist) {
                                it.continueWith {
                                    false
                                }
                            }
                            else {
                                collection.document(user.username)
                                        .set(hashMapOf(
                                            PASSWORD_FIELD to user.password,
                                            FIRST_NAME_FIELD to user.firstName,
                                            LAST_NAME_FIELD to user.lastName,
                                        ))
                                        .continueWith {
                                            true
                                        }
                            }
                        }
                    }
        }

        fun getUser(username: String): Task<User?> {
            return collection.document(username)
                    .get()
                    .continueWith {
                        if (it.result?.exists() == true) {
                            val doc = it.result!!
                            val password = doc.getString(PASSWORD_FIELD) ?: return@continueWith null
                            val firstName = doc.getString(FIRST_NAME_FIELD) ?: return@continueWith null
                            val lastName = doc.getString(LAST_NAME_FIELD) ?: return@continueWith null

                            User(
                                username,
                                password,
                                firstName,
                                lastName,
                            )
                        }
                        else {
                            null
                        }
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

        fun validateUsernameAndPassword(username: String, password: String): List<Throwable> {
            val list = arrayListOf<Throwable>()

            validateUsername(username).exceptionOrNull()?.let { list.add(it) }
            validatePassword(password).exceptionOrNull()?.let { list.add(it) }

            return list
        }

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