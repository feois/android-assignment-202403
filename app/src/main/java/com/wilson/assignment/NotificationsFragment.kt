package com.wilson.assignment

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.toObject

class NotificationsAdapter(private val context: Context,
                           private val notifications: List<Notification>,
                           private val lifecycleOwner: LifecycleOwner,
                           private val userViewModel: UserViewModel,
                           private val readCallback: (Notification) -> Unit)
    : RecyclerView.Adapter<NotificationsAdapter.NotificationViewHolder>() {
    inner class NotificationViewHolder(private val view: View): RecyclerView.ViewHolder(view) {
        var notification: Notification? = null; private set

        private val titleText = view.findViewById<TextView>(R.id.notificationTitle)
        private val timeText = view.findViewById<TextView>(R.id.notificationTime)
        
        fun setNotification(n: Notification) {
            notification = n

            titleText.text = n.title
            timeText.text = formatTime(n.time)

            view.setOnClickListener {
                readCallback(n)
                setRead(true)
            }
        }

        fun setRead(status: Boolean) {
            titleText.setTextColor(if (status) { 0xFF000000 } else { 0xFF00FF00 }.toInt())
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = NotificationViewHolder(LayoutInflater.from(context)
            .inflate(R.layout.notification_item, parent, false))
            .apply {
                userViewModel.user.observe(lifecycleOwner) {
                    notification?.id?.run { setRead(userViewModel.hasRead(this)) }
                }
            }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) = holder.run {
        setNotification(notifications[position])
        setRead(userViewModel.hasRead(notifications[position].id))
    }

    override fun getItemCount() = notifications.size
}

class NotificationsFragment : Fragment() {
    private val userViewModel: UserViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View?
        = inflater.inflate(R.layout.fragment_notifications, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val recycler = view.findViewById<RecyclerView>(R.id.notifications)

        recycler.layoutManager = LinearLayoutManager(requireContext())

        Notification.collection.addSnapshotListener { value, error ->
            error?.run {
                return@addSnapshotListener requireContext().errorToast(
                    "Failed to update notifications",
                    error
                )
            }
            recycler.swapAdapter(getAdapter(value!!.mapNotNull { it.toObject<Notification>() }
                    .sortedByDescending { it.time }), true)
        }
    }

    private fun getAdapter(list: List<Notification>) = NotificationsAdapter(requireContext(), list, viewLifecycleOwner, userViewModel) {
        startActivity(Intent(requireContext(), NotificationActivity::class.java).apply {
            putExtra(NotificationActivity.INTENT_NOTIFICATION_ID, it.id)
        })
        userViewModel.readNotification(it.id)
    }
}