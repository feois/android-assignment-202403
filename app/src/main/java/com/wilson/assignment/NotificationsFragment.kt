package com.wilson.assignment

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.toObject

class NotificationsAdapter(private val context: Context,
                           private val notifications: List<Notification>,
                           private val readCallback: (Notification) -> Unit,
                           private val hasReadCallback: (Notification) -> Boolean,)
    : RecyclerView.Adapter<NotificationsAdapter.NotificationViewHolder>() {
    inner class NotificationViewHolder(private val view: View): RecyclerView.ViewHolder(view) {
        private val titleText = view.findViewById<TextView>(R.id.notificationTitle)
        private val timeText = view.findViewById<TextView>(R.id.notificationTime)
        
        fun setNotification(notification: Notification) {
            titleText.text = notification.title
            setRead(hasReadCallback(notification))
            timeText.text = notification.formatTime()

            view.setOnClickListener {
                readCallback(notification)
                setRead(true)
            }
        }

        fun setRead(status: Boolean) {
            titleText.setTextColor(if (status) { 0xFF000000 } else { 0xFF00FF00 }.toInt())
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int)
        = NotificationViewHolder(LayoutInflater.from(context).inflate(R.layout.notification_item, parent, false))

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int)
        = holder.setNotification(notifications[position])

    override fun getItemCount() = notifications.size
}

class NotificationsFragment : Fragment() {
    private val userViewModel: UserViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View?
        = inflater.inflate(R.layout.fragment_notifications, container, false)

    @SuppressLint("NotifyDataSetChanged")
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

        userViewModel.user.observe(viewLifecycleOwner) {
            recycler.adapter?.notifyDataSetChanged()
        }
    }

    fun getAdapter(list: List<Notification>) = NotificationsAdapter(requireContext(), list,
        {
            startActivity(Intent(requireContext(), NotificationActivity::class.java).apply {
                putExtra(NotificationActivity.INTENT_NOTIFICATION_ID, it.id)
            })
            userViewModel.readNotification(it.id)
        },
        { userViewModel.hasRead(it.id) },)
}