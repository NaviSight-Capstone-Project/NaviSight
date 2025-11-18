package edu.capstone.navisight.utils

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.util.Properties
import javax.mail.*
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

object EmailSender {

    private const val TAG = "EmailSender"
    private const val SMTP_FILE = "smtp.properties"

    private var senderEmail: String? = null
    private var senderPassword: String? = null

    private fun loadCredentials(context: Context) {
        try {
            val props = Properties()
            val inputStream: InputStream = context.assets.open(SMTP_FILE)
            props.load(inputStream)

            senderEmail = props.getProperty("email")
            senderPassword = props.getProperty("password")

            Log.i(TAG, "SMTP credentials loaded successfully.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load SMTP credentials: ${e.message}", e)
        }
    }


    suspend fun sendVerificationEmail(
        context: Context,
        to: String,
        subject: String,
        body: String
    ) = withContext(Dispatchers.IO) {
        try {
            if (senderEmail == null || senderPassword == null) {
                loadCredentials(context)
            }

            val props = Properties().apply {
                put("mail.smtp.auth", "true")
                put("mail.smtp.starttls.enable", "true")
                put("mail.smtp.host", "smtp.gmail.com")
                put("mail.smtp.port", "587")
            }

            val session = Session.getInstance(props, object : Authenticator() {
                override fun getPasswordAuthentication(): PasswordAuthentication {
                    return PasswordAuthentication(senderEmail, senderPassword)
                }
            })

            val message = MimeMessage(session).apply {
                setFrom(InternetAddress(senderEmail, "NaviSight Support"))
                setRecipients(Message.RecipientType.TO, InternetAddress.parse(to))
                setSubject(subject)
                setText(
                    """
                    Hello,

                    $body

                    Thanks,
                    NaviSight Team
                    """.trimIndent()
                )
            }

            Transport.send(message)
            Log.i(TAG, "Email sent successfully to $to")

        } catch (e: AuthenticationFailedException) {
            Log.e(TAG, "Gmail authentication failed — check app password: ${e.message}")
            throw e
        } catch (e: MessagingException) {
            Log.e(TAG, "Messaging error: ${e.message}", e)
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "General error: ${e.message}", e)
            throw e
        }
    }
}
