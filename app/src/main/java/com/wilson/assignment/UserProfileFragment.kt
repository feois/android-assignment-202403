package com.wilson.assignment

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button

class UserProfileFragment : Fragment() {
    @Suppress("MemberVisibilityCanBePrivate")
    var eventListener: EventListener? = null

    fun interface EventListener {
        fun onLogOut()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {

        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_user_profile, container, false)
        val logoutButton = view.findViewById<Button>(R.id.logout)

        logoutButton.setOnClickListener { eventListener?.onLogOut() }

        return view
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        if (context is EventListener) {
            eventListener = context
        }
    }
}