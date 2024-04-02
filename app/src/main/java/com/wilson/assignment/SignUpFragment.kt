package com.wilson.assignment

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.ScrollView
import androidx.fragment.app.activityViewModels
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class SignUpFragment : Fragment() {
    @Suppress("MemberVisibilityCanBePrivate")
    var eventListener: EventListener? = null

    private val userViewModel: UserViewModel by activityViewModels()

    private var fragmentView: View? = null

    private val username get() = fragmentView!!.findViewById<TextInputLayout>(R.id.newUsername)
    private val usernameText get() = fragmentView!!.findViewById<TextInputEditText>(R.id.newUsernameText)
    private val password get() = fragmentView!!.findViewById<TextInputLayout>(R.id.newPassword)
    private val passwordText get() = fragmentView!!.findViewById<TextInputEditText>(R.id.newPasswordText)
    private val firstName get() = fragmentView!!.findViewById<TextInputLayout>(R.id.firstName)
    private val firstNameText get() = fragmentView!!.findViewById<TextInputEditText>(R.id.firstNameText)
    private val lastName get() = fragmentView!!.findViewById<TextInputLayout>(R.id.lastName)
    private val lastNameText get() = fragmentView!!.findViewById<TextInputEditText>(R.id.lastNameText)
    private val signUpButton get() = fragmentView!!.findViewById<Button>(R.id.signUp)
    private val logInLinkButton get() = fragmentView!!.findViewById<Button>(R.id.logInLink)
    private val rememberMe get() = fragmentView!!.findViewById<CheckBox>(R.id.rememberMe2)
    private val scrollView get() = fragmentView!!.findViewById<ScrollView>(R.id.signUpScroll)

    fun interface EventListener {
        fun onGoToLogIn()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View?
        = inflater.inflate(R.layout.fragment_sign_up, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        fragmentView = view

        signUpButton.setOnClickListener {
            val user = User(
                usernameText.text.toString(),
                firstNameText.text!!.trimWhitespaces(),
                lastNameText.text!!.trimWhitespaces(),
            )
            val validation = user.validate(passwordText.text.toString())

            username.error = ""
            password.error = ""
            firstName.error = ""
            lastName.error = ""

            if (validation.isEmpty()) {
                userViewModel.signUp(user, passwordText.text.toString(), requireContext().dataStore.takeIf { rememberMe.isChecked })
            }
            else {
                var first: View? = null

                for (e in validation) {
                    when (e) {
                        is UsernameException -> username
                        is PasswordException -> password
                        is NameException -> if (e.isFirst) { firstName } else { lastName }
                        else -> null
                    }?.run {
                        first = this
                        error = e.message
                    }
                }

                first?.run {
                    scrollView.smoothScrollTo(0, view.top)
                }

                context?.shortToast("Invalid input")
            }
        }

        logInLinkButton.setOnClickListener {
            eventListener?.onGoToLogIn()
        }

        userViewModel.signUpResult.observe(viewLifecycleOwner) {
            it.exceptionOrNull()?.let { e ->
                when (e) {
                    is UserAlreadyExistsException -> {
                        username.error = "Username already taken."
                        scrollView.smoothScrollTo(0, username.top)
                    }
                }
            }
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        if (context is EventListener) {
            eventListener = context
        }
    }

    fun clearInput() {
        fragmentView?.run {
            usernameText?.text?.clear()
            passwordText?.text?.clear()
            firstNameText?.text?.clear()
            lastNameText?.text?.clear()
        }
    }
}