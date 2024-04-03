package com.wilson.assignment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.activityViewModels
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class UserProfileFragment : Fragment() {
    private val userViewModel: UserViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View?
        = inflater.inflate(R.layout.fragment_user_profile, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val logoutButton = view.findViewById<Button>(R.id.logout)
        val usernameLabel = view.findViewById<TextView>(R.id.profileUsername)
        val editFirstName = view.findViewById<TextInputLayout>(R.id.editFirstName)
        val editFirstNameText = view.findViewById<TextInputEditText>(R.id.editFirstNameText)
        val editLastName = view.findViewById<TextInputLayout>(R.id.editLastName)
        val editLastNameText = view.findViewById<TextInputEditText>(R.id.editLastNameText)
        val confirmEdit = view.findViewById<Button>(R.id.confirmEdit)

        logoutButton.setOnClickListener { userViewModel.logout(context) }

        userViewModel.user.observe(viewLifecycleOwner) {
            it?.run {
                usernameLabel.text = """
                    Username: $username
                    Name: $fullName
                """.trimIndent()
                editFirstNameText.text?.setString(firstName)
                editLastNameText.text?.setString(lastName)
            }
        }

        confirmEdit.setOnClickListener {
            val newFirstName = editFirstNameText.text!!.trimWhitespaces()
            val newLastName = editLastNameText.text!!.trimWhitespaces()

            val validation = buildList {
                User.validateFirstName(newFirstName)?.run { add(this) }
                User.validateLastName(newLastName)?.run { add(this) }
            }

            if (validation.isEmpty()) {
                userViewModel.updateUser(mapOf(
                    User.FIRST_NAME_FIELD to newFirstName,
                    User.LAST_NAME_FIELD to newLastName,
                ))
            }
            else {
                for (e in validation) {
                    when (e) {
                        is NameException -> if (e.isFirst) { editFirstName } else { editLastName }.error = e.message
                    }
                }

                context?.shortToast("Invalid input")
            }
        }
    }
}