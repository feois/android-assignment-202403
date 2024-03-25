package com.wilson.assignment

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class SignUpFragment : Fragment() {
    @Suppress("MemberVisibilityCanBePrivate")
    var eventListener: EventListener? = null

    private var username: TextInputLayout? = null
    private var password: TextInputLayout? = null
    private var firstName: TextInputLayout? = null
    private var lastName: TextInputLayout? = null
    private var signUpButton: Button? = null
    private var logInLinkButton: Button? = null
    private var usernameText: TextInputEditText? = null
    private var passwordText: TextInputEditText? = null
    private var firstNameText: TextInputEditText? = null
    private var lastNameText: TextInputEditText? = null
    private var rememberMe: CheckBox? = null

    interface EventListener {
        fun onGoToLogIn()
        fun onSignUp(user: User, password: String, remember: Boolean)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View?
        = inflater.inflate(R.layout.fragment_sign_up, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        username = view.findViewById(R.id.newUsername)
        password = view.findViewById(R.id.newPassword)
        firstName = view.findViewById(R.id.firstName)
        lastName = view.findViewById(R.id.lastName)
        signUpButton = view.findViewById(R.id.signUp)
        logInLinkButton = view.findViewById(R.id.logInLink)
        usernameText = view.findViewById(R.id.newUsernameText)
        passwordText = view.findViewById(R.id.newPasswordText)
        firstNameText = view.findViewById(R.id.firstNameText)
        lastNameText = view.findViewById(R.id.lastNameText)
        rememberMe = view.findViewById(R.id.rememberMe2)

        signUpButton?.setOnClickListener {
            val user = User(
                usernameText?.text.toString(),
                firstNameText?.text.toString(),
                lastNameText?.text.toString(),
            )
            val validation = user.validate()

            username?.error = ""
            password?.error = ""
            firstName?.error = ""
            lastName?.error = ""

            if (validation.isEmpty()) {
                eventListener?.onSignUp(user, passwordText?.text.toString(), rememberMe!!.isChecked)
            }
            else {
                for (message in validation.mapNotNull { it.message }) {
                    User.errorType(message)?.let { errorType ->
                        when (errorType) {
                            User.ERR_USERNAME -> username
                            User.ERR_PASSWORD -> password
                            User.ERR_FIRST_NAME -> firstName
                            User.ERR_LAST_NAME -> lastName
                            else -> null
                        }?.error = message
                    }
                }

                context?.shortToast("Invalid input")
            }
        }

        logInLinkButton?.setOnClickListener {
            eventListener?.onGoToLogIn()
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        if (context is EventListener) {
            eventListener = context
        }
    }

    fun clearInput() {
        usernameText?.text?.clear()
        passwordText?.text?.clear()
        firstNameText?.text?.clear()
        lastNameText?.text?.clear()
    }
}