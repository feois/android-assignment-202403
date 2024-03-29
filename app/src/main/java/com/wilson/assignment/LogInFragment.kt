package com.wilson.assignment

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import androidx.fragment.app.activityViewModels
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class LogInFragment : Fragment() {
    @Suppress("MemberVisibilityCanBePrivate")
    var eventListener: EventListener? = null

    private val userViewModel: UserViewModel by activityViewModels()

    private var fragmentView: View? = null

    private val username get() = fragmentView!!.findViewById<TextInputLayout>(R.id.username)
    private val usernameText get() = fragmentView!!.findViewById<TextInputEditText>(R.id.usernameText)
    private val password get() = fragmentView!!.findViewById<TextInputLayout>(R.id.password)
    private val passwordText get() = fragmentView!!.findViewById<TextInputEditText>(R.id.passwordText)
    private val loginButton get() = fragmentView!!.findViewById<Button>(R.id.login)
    private val signUpLinkButton get() = fragmentView!!.findViewById<Button>(R.id.signUpLink)
    private val rememberMe get() = fragmentView!!.findViewById<CheckBox>(R.id.rememberMe)

    fun interface EventListener {
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
        fragmentView = view

        loginButton.setOnClickListener {
            val validation = User.validateUsernameAndPassword(usernameText.text.toString(), passwordText.text.toString())

            username.error = ""
            password.error = ""

            logInfo("click")

            if (validation.isEmpty()) {
                userViewModel.login(usernameText.text.toString(), passwordText.text.toString(), context.takeIf { rememberMe.isChecked })
            }
            else {
                for (message in validation.mapNotNull { it.message }) {
                    User.errorType(message)?.let { errorType ->
                        when (errorType) {
                            User.ERR_USERNAME -> username.error = message
                            User.ERR_PASSWORD -> password.error = message
                        }
                    }
                }

                context?.shortToast("Invalid input")
            }
        }

        signUpLinkButton.setOnClickListener {
            eventListener?.onGoToSignUp()
        }

        userViewModel.loginResult.observe(viewLifecycleOwner) {
            it.exceptionOrNull()?.let { e ->
                when (e) {
                    is UserNotFoundException -> username.error = "User ${e.username} does not exist. Make sure your username is correct."
                    is IncorrectPasswordException -> password.error = "Password incorrect."
                }
            }
        }
    }

    fun clearInput() {
        fragmentView?.run {
            usernameText.text?.clear()
            passwordText.text?.clear()
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        if (context is EventListener) {
            eventListener = context
        }
    }
}