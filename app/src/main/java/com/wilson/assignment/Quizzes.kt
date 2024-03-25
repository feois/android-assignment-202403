package com.wilson.assignment

import android.text.InputType
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await

data class Quiz internal constructor(val id: String, val name: String, val allowReorder: Boolean, val questions: List<Question>) {
    abstract class Question {
        abstract val question: String

        companion object {
            fun fromMap(map: Map<*, *>): Question? {
                return when {
                    map.containsKey("type") -> SubjectiveQuestion(
                        map["question"] as? String ?: return null,
                        map["type"] as? String ?: return null,
                        map["answer"] as? String
                    )

                    map.containsKey("answer") -> UniselectionalObjectiveQuestion(
                        map["question"] as? String ?: return null,
                        (map["answer"] as? Long)?.toInt() ?: return null,
                        (map["options"] as? List<*>)?.filterIsInstance<String>() ?: return null,
                        map["allow reorder"] as? Boolean ?: true,
                    )

                    map.containsKey("count") -> MultiselectionalObjectiveQuestion(
                        map["question"] as? String ?: return null,
                        (map["count"] as? Long)?.toInt() ?: return null,
                        (map["answers"] as? List<*>)?.filterIsInstance<Long>()?.map { it.toInt() } ?: return null,
                        (map["options"] as? List<*>)?.filterIsInstance<String>() ?: return null,
                        map["allow reorder"] as? Boolean ?: true,
                    )

                    else -> null
                }
            }
        }
    }

    abstract class ObjectiveQuestion: Question() {
        abstract val allowReorder: Boolean
        abstract val options: List<String>
    }

    data class SubjectiveQuestion internal constructor(override val question: String,
                                                       val type: String,
                                                       val answer: String? = null): Question() {
        val inputType get() = when (type) {
            "integer" -> InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_SIGNED
            "decimal" -> InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL or InputType.TYPE_NUMBER_FLAG_SIGNED
            "text" -> InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_AUTO_CORRECT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            "long text" -> InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_AUTO_CORRECT or InputType.TYPE_TEXT_FLAG_MULTI_LINE or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            else -> InputType.TYPE_CLASS_TEXT
        }
    }
    data class UniselectionalObjectiveQuestion(override val question: String,
                                               val answer: Int,
                                               override val options: List<String>,
                                               override val allowReorder: Boolean = true): ObjectiveQuestion()
    data class MultiselectionalObjectiveQuestion(override val question: String = "",
                                                 val count: Int = 0,
                                                 val answers: List<Int> = listOf(),
                                                 override val options: List<String>,
                                                 override val allowReorder: Boolean = true): ObjectiveQuestion()
}

val quizzes by lazy {
    runBlocking {
        (Firebase.firestore.collection("quizzes").get().await()?.documents ?: listOf())
                .mapNotNull { doc -> doc?.takeIf { it.exists() }?.let {
                    Quiz(
                        it.id,
                        it.getString("name") ?: return@let null,
                        it.getBoolean("allow reorder") ?: true,
                        (it.get("questions") as List<*>).filterIsInstance<Map<*, *>>()
                                .mapNotNull { question -> Quiz.Question.fromMap(question) }
                    )
                } }
    }
}
val quizzesMap by lazy {
    quizzes.associateBy { it.id }
}
