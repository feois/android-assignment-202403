package com.wilson.assignment

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class LogInFragment : Fragment() {
    @Suppress("MemberVisibilityCanBePrivate")
    var eventListener: EventListener? = null

    private var username: TextInputLayout? = null
    private var usernameText: TextInputEditText? = null
    private var password: TextInputLayout? = null
    private var passwordText: TextInputEditText? = null
    private var loginButton: Button? = null
    private var signUpLinkButton: Button? = null

    interface EventListener {
        fun onLogIn(username: String, password: String, newAccount: Boolean)
        fun onGoToSignUp()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View?
        = inflater.inflate(R.layout.fragment_log_in, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        username = view.findViewById(R.id.username)
        usernameText = view.findViewById(R.id.usernameText)
        password = view.findViewById(R.id.password)
        passwordText = view.findViewById(R.id.passwordText)
        loginButton = view.findViewById(R.id.login)
        signUpLinkButton = view.findViewById(R.id.signUpLink)

        loginButton?.setOnClickListener {
            val validation = User.validateUsernameAndPassword(usernameText?.text.toString(), passwordText?.text.toString())

            username?.error = ""
            password?.error = ""

            if (validation.isEmpty()) {
                eventListener?.onLogIn(usernameText?.text.toString(), passwordText?.text.toString(), false)
            }
            else {
                for (message in validation.mapNotNull { it.message }) {
                    User.errorType(message)?.let { errorType ->
                        when (errorType) {
                            User.ERR_USERNAME -> username?.error = message
                            User.ERR_PASSWORD -> password?.error = message
                        }
                    }
                }

                Toast.makeText(context, "Invalid input", Toast.LENGTH_SHORT).show()
            }
        }

        signUpLinkButton?.setOnClickListener {
            eventListener?.onGoToSignUp()
        }
    }

    fun clearInput() {
        usernameText?.text?.clear()
        passwordText?.text?.clear()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        if (context is EventListener) {
            eventListener = context
        }
    }
}