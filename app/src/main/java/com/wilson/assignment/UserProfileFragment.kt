package com.wilson.assignment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.activityViewModels

class UserProfileFragment : Fragment() {
    private val userViewModel: UserViewModel by activityViewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {

        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View?
        = inflater.inflate(R.layout.fragment_user_profile, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val logoutButton = view.findViewById<Button>(R.id.logout)
        val usernameLabel = view.findViewById<TextView>(R.id.profileUsername)

        logoutButton.setOnClickListener { userViewModel.logout(context) }

        userViewModel.user.observe(viewLifecycleOwner) {
            it?.run {
                usernameLabel.text = """
                    Username: ${username}
                    Name: ${fullName}
                """.trimIndent()
            }
        }
    }
}