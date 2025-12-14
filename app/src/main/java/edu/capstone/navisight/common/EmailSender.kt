package edu.capstone.navisight.common

import android.content.Context
import android.util.Log
import edu.capstone.navisight.common.ConverterHelpers.convertMillisToDate
import edu.capstone.navisight.common.ConverterHelpers.convertMillisToDetailedDateTime
import edu.capstone.navisight.viu.model.Caregiver
import edu.capstone.navisight.viu.model.Viu
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.util.Properties
import javax.activation.DataHandler
import javax.activation.FileDataSource
import javax.mail.AuthenticationFailedException
import javax.mail.Authenticator
import javax.mail.Message
import javax.mail.MessagingException
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeBodyPart
import javax.mail.internet.MimeMessage
import javax.mail.internet.MimeMultipart

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

    // Use only for sending email from caregiver to VIU
    suspend fun sendEmergencyEmail(
        context: Context,
        viu: Viu,
        caregiver: Caregiver,
        attachments: List<File> = emptyList(),
        testEmail : String = "",
        lastLocationLongitude: String,
        lastLocationLatitude: String,
        lastLocationTimestamp: Long
    ) = withContext(Dispatchers.IO) {
        if (senderEmail == null || senderPassword == null) {
            loadCredentials(context)
            if (senderEmail == null || senderPassword == null) {
                throw Exception("Sender email or password is null after loading credentials.")
            }
        }

        val caregiverEmail = caregiver.email

        val to = if (testEmail.isEmpty()) caregiverEmail else testEmail

        // Safety checks ito
        if (caregiverEmail.isEmpty()) throw Exception("Caregiver email must not be empty")

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


        try {
            val message = MimeMessage(session).apply {
                setFrom(
                    InternetAddress(
                        senderEmail,
                        "NaviSight Emergency Alert"
                    )
                ) // Improved sender name
                setRecipients(Message.RecipientType.TO, InternetAddress.parse(to))
                setSubject("[ALERT - ACTION REQUIRED] Emergency Activated by ${viu.firstName} - donotreply")
                val multipart = MimeMultipart()


                val htmlContent = generateEmergencyHtml(
                    viu,
                    caregiver,
                    lastLocationLongitude,
                    lastLocationLatitude,
                    lastLocationTimestamp)

                val htmlBodyPart = MimeBodyPart().apply {
                    // Set content type to HTML
                    setContent(htmlContent, "text/html; charset=UTF-8")
                }
                multipart.addBodyPart(htmlBodyPart)

                if (attachments.isNotEmpty()) {
                    attachments.forEach { file ->
                        val attachmentPart = MimeBodyPart().apply {
                            val source = FileDataSource(file)
                            dataHandler = DataHandler(source)
                            fileName = file.name
                        }
                        multipart.addBodyPart(attachmentPart)
                        Log.d(TAG, "Attached file: ${file.name}")
                    }
                }
                // Set the content for the email message
                setContent(multipart)

                // Set importance flag for better visibility
                setHeader("X-Priority", "1")
            }
            Transport.send(message) //fire
            Log.i(TAG, "Email sent successfully to $caregiverEmail")

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


    private fun generateEmergencyHtml(
        viu: Viu,
        caregiver: Caregiver,
        lastLocationLongitude: String,
        lastLocationLatitude: String,
        lastLocationTimestamp: Long,

    ): String {
        val jetRed = "#c40e0e"
        val primaryColor = "#6A1B9A"      // Deep Purple (Emergency Focus)
        val secondaryColor = "#E1BEE7"    // Light Purple (Accent/Background Section)
        val softBlack = "#212121"         // Soft Black (Text)
        val softGrey = "#757575"          // Soft Grey (Notes)
        val white = "#FFFFFF"             // White (Main Background)

        val latDouble = lastLocationLatitude.toDoubleOrNull() ?: 0.0
        val latDirection = if (latDouble >= 0) "N" else "S"
        val formattedLatitude = "${String.format("%.6f", Math.abs(latDouble))} $latDirection"
        val lonDouble = lastLocationLongitude.toDoubleOrNull() ?: 0.0
        val lonDirection = if (lonDouble >= 0) "E" else "W"
        val formattedLongitude = "${String.format("%.6f", Math.abs(lonDouble))} $lonDirection"
        val dateTimeString = convertMillisToDetailedDateTime(lastLocationTimestamp)
        val mapsLink = "https://www.google.com/maps/search/?api=1&query=$latDouble,$lonDouble"
        val baseStyle = "font-family: 'Segoe UI', Arial, sans-serif; line-height: 1.6; color: $softBlack; background-color: $white;"

        return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <title>NaviSight Emergency Alert</title>
        </head>
        <body style="$baseStyle margin: 0; padding: 0;">
        
            <table role="presentation" cellspacing="0" cellpadding="0" border="0" width="100%" style="max-width: 600px; margin: auto;">
                <tr>
                    <td style="padding: 20px 30px;">
                    
                        <div style="text-align: center; background-color: $jetRed; color: $white; padding: 15px; border-radius: 8px 8px 0 0; margin-bottom: 20px;">
                            <h1 style="margin: 0; font-size: 24px; letter-spacing: 1px;">
                                EMERGENCY ALERT
                            </h1>
                        </div>
                        
                        <p style="font-size: 16px;">
                            Dear Caregiver <strong>${caregiver.firstName}</strong>,
                        </p>
                        
                        <p style="font-size: 18px; font-weight: bold; color: $softBlack;">
                            IMMEDIATE ACTION IS REQUIRED.
                        </p>
                        
                        <p>
                            Your registered visually impaired user (VIU), <strong>${viu.firstName} ${viu.lastName}</strong>, 
                            has manually triggered the emergency mode in the NaviSight app.
                        </p>
                        
                        <p>
                            Attached with this email are the quick snaps taken (both back and front) upon emergency trigger.
                        </p>
                        
                        <div style="background-color: $secondaryColor; padding: 20px; border-radius: 8px; margin: 25px 0; border: 1px solid ${primaryColor}40;">
                            <h3 style="color: $primaryColor; margin-top: 0; font-size: 18px; border-bottom: 2px solid $primaryColor; padding-bottom: 5px;">
                                📍 LAST KNOWN LOCATION:
                            </h3>
                            <p style="font-size: 1.1em; font-weight: bold; margin: 15px 0;"> 
                                Longitude: 
                                <span style="font-weight: normal; color: ${softBlack};">
                                    ${formattedLongitude}
                                </span>
                                <br>
                                
                                Latitude: 
                                <span style="font-weight: normal; color: ${softBlack};">
                                    ${formattedLatitude}
                                </span>
                                <br>
                                
                                Timestamp Taken: 
                                <span style="font-weight: normal; color: ${softBlack};">
                                    ${dateTimeString}
                                </span>
                            </p>
                            <p style="font-size: 14px; color: $softBlack;">
                                Please use the location data and contact the VIU immediately or check the NaviSight app directly for precise location.
                            </p>
                            <p style="text-align: center; margin-top: 20px; margin-bottom: 0;">
                                <a href="$mapsLink" 
                                   style="display: inline-block; padding: 10px 20px; background-color: $primaryColor; color: $white; text-decoration: none; border-radius: 6px; font-weight: bold; font-size: 16px;">
                                    🗺️ View Location on Google Maps
                                </a>
                            </p>
                        </div>
                        
                        <h3 style="color: $softBlack; font-size: 16px; margin-top: 30px; margin-bottom: 10px;">VIU Details:</h3>
                        <ul style="list-style-type: none; padding: 0; margin: 0;">
                            <li style="margin-bottom: 5px;"><strong>Name:</strong> ${viu.firstName} ${viu.lastName}</li>
                            <li><strong>App ID/Email:</strong> ${viu.email}</li>
                        </ul>
                        
                        <hr style="border: 0; border-top: 1px solid #eeeeee; margin: 30px 0;">

                        <p style="font-size: 13px; color: $softGrey; text-align: center;">
                            This is an automated emergency alert from NaviSight. Please do not reply to this email. 
                            For further information or remote actions, please open your NaviSight App.
                        </p>
                    </td>
                </tr>
            </table>

        </body>
        </html>
    """.trimIndent()
    }
}