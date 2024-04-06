package com.wilson.assignment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.activityViewModels
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class OldPasswordException(val passwordException: PasswordException)
class NewPasswordException(val passwordException: PasswordException)
class ConfirmPasswordException(val passwordException: PasswordException)
class OldPasswordNotProvidedException: PasswordException("Old password field is empty")
class ConfirmPasswordNotProvidedException: PasswordException("Confirm password field is empty")
class WrongOldPasswordException: PasswordException("Old password is wrong")
class WrongConfirmPasswordException: PasswordException("Confirm password does not match")

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
        val oldPassword = view.findViewById<TextInputLayout>(R.id.oldPassword)
        val oldPasswordText = view.findViewById<TextInputEditText>(R.id.oldPasswordText)
        val newPassword = view.findViewById<TextInputLayout>(R.id.newPassword)
        val newPasswordText = view.findViewById<TextInputEditText>(R.id.newPasswordText)
        val confirmPassword = view.findViewById<TextInputLayout>(R.id.confirmPassword)
        val confirmPasswordText = view.findViewById<TextInputEditText>(R.id.confirmPasswordText)
        val warnBlankSwitch = view.findViewById<SwitchCompat>(R.id.warnBlank)
        val confirmEdit = view.findViewById<Button>(R.id.confirmEdit)

        logoutButton.setOnClickListener { userViewModel.logout(context) }

        userViewModel.user.observe(viewLifecycleOwner) {
            oldPasswordText.text?.clear()
            newPasswordText.text?.clear()
            confirmPasswordText.text?.clear()

            it?.run {
                usernameLabel.text = """
                    Username: $username
                    Name: $fullName
                """.trimIndent()
                editFirstNameText.text?.setString(firstName)
                editLastNameText.text?.setString(lastName)
                warnBlankSwitch.isChecked = warnBlank
            }
        }

        confirmEdit.setOnClickListener {
            val newFirstName = editFirstNameText.text!!.trimWhitespaces()
            val newLastName = editLastNameText.text!!.trimWhitespaces()
            val oldP = oldPasswordText.text!!.toString().takeIf { it.isNotEmpty() }
            val newP = newPasswordText.text!!.toString().takeIf { it.isNotEmpty() }
            val conP = confirmPasswordText.text!!.toString().takeIf { it.isNotEmpty() }

            editFirstName.error = ""
            editLastName.error = ""
            oldPassword.error = ""
            newPassword.error = ""
            confirmPassword.error = ""

            val validation = buildList {
                User.validateFirstName(newFirstName)?.run { add(this) }
                User.validateLastName(newLastName)?.run { add(this) }
                oldP?.run { User.validatePassword(this)?.run { add(OldPasswordException(this)) } }
                newP?.run { User.validatePassword(this)?.run { add(NewPasswordException(this)) } }
                conP?.run { User.validatePassword(this)?.run { add(ConfirmPasswordException(this)) } }

                if (newP != null) {
                    if (oldP == null) {
                        add(OldPasswordNotProvidedException())
                    }

                    if (conP == null) {
                        add(ConfirmPasswordNotProvidedException())
                    }
                    else if (newP != conP) {
                        add(WrongConfirmPasswordException())
                    }
                }
            }

            if (validation.isEmpty()) {
                userViewModel.updateUser(mapOf(
                    User.FIRST_NAME_FIELD to newFirstName,
                    User.LAST_NAME_FIELD to newLastName,
                    User.WARN_BLANK_FIELD to warnBlankSwitch.isChecked,
                ))?.addOnSuccessListener {
                    requireContext().shortToast("Successfully updated profile information")
                }

                newP?.run {
                    userViewModel.updatePassword(oldP!!, this, requireContext().dataStore).fold({
                        it?.addOnSuccessListener {
                            requireContext().shortToast("Successfully updated password")
                        }
                    }) {
                        if (it is WrongOldPasswordException) {
                            oldPassword.error = it.message
                        }
                    }
                }
            }
            else {
                for (e in validation) {
                    when (e) {
                        is NameException -> if (e.isFirst) { editFirstName } else { editLastName }.error = e.message
                        is OldPasswordException -> oldPassword.error = e.passwordException.message
                        is NewPasswordException -> newPassword.error = e.passwordException.message
                        is ConfirmPasswordException -> confirmPassword.error = e.passwordException.message
                        is OldPasswordNotProvidedException -> oldPassword.error = e.message
                        is ConfirmPasswordNotProvidedException -> confirmPassword.error = e.message
                        is WrongConfirmPasswordException -> confirmPassword.error = e.message
                    }
                }

                context?.shortToast("Invalid input")
            }
        }
    }
}