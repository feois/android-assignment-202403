package com.wilson.assignment

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.toObject
import java.text.SimpleDateFormat
import java.util.Locale

data class Notification(@DocumentId val id: String = "",
                        val title: String = "",
                        val content: String = "",
                        val time: Timestamp = Timestamp(0, 0)) {
    companion object {
        val collection = db.collection("notifications")

        fun getNotification(id: String) = collection.document(id).get().continueWith { doc ->
            doc.takeIf { it.isSuccessful }?.result?.toObject<Notification>()
        }
    }
}
