package edu.capstone.navisight.auth.util

object LegalDocuments {
    const val TERMS_VERSION = "December 2025"

    const val TERMS_AND_CONDITIONS = """
Last Updated: December 2025

1. Acceptance of Terms By downloading, installing, or using the NaviSight mobile application , you agree to be bound by these Terms and Conditions. If you do not agree to these terms, do not use the App. These Terms constitute a legally binding agreement between you and the developers of NaviSight.

2. Separation of Privacy Policy Your use of the App is also governed by our Privacy Policy, which is a separate document outlining how we collect, use, and store your personal data and location information. By agreeing to these Terms, you acknowledge that you have reviewed the Privacy Policy.

3. Nature of Services (Assistive Tool Only) NaviSight is strictly a supplementary tool designed to assist Companions in monitoring the approximate location of associated Visually Impaired individuals .

NO GUARANTEE OF SAFETY: The App is NOT a substitute for direct human supervision, physical care, or professional medical monitoring.

NOT AN EMERGENCY SERVICE: In the event of a medical emergency or immediate danger, you must contact local emergency services (e.g., 911) immediately. The App cannot contact emergency services on your behalf.

4. GPS and Technology Limitations The App relies on third-party technologies (GPS, Cellular Data, Wi-Fi) that are subject to environmental interference and technical failure.

ACCURACY: We do not guarantee that location data will be accurate, real-time, or error-free. Tunnels, tall buildings, and weather conditions may cause location inaccuracies.

LATENCY: Notifications regarding the Viu's movements or geofence breaches may be delayed due to network connectivity issues.

5. LIMITATION OF LIABILITY TO THE MAXIMUM EXTENT PERMITTED BY APPLICABLE LAW, THE DEVELOPERS AND OWNERS OF NAVISIGHT SHALL NOT BE LIABLE FOR ANY DAMAGES, INJURIES, OR LOSSES ARISING FROM THE USE OF THIS APP. SPECIFICALLY:

VIU SAFETY DURING NAVIGATION: We are not liable for any accidents, falls, collisions, traffic incidents, or physical harm that occurs to the Visually Impaired Individual  while using the App for navigation or while being tracked. The Visually Impaired Individual uses the navigation features at their own risk.

COMPANION RELIANCE: We are not liable for any distress, loss, or harm resulting from the Companion's reliance on the App's location data. This includes instances where the App fails to report the Visually Impaired Individual's location, reports it broadly, or fails to send a geofence alert.

BY USING THE APP, YOU ASSUME FULL RESPONSIBILITY FOR THE SAFETY AND SUPERVISION OF THE INDIVIDUAL BEING TRACKED.

6. User Conduct You agree to use the App solely for lawful caregiving purposes. You represent and warrant that you have obtained all necessary consents to track the location of the Visually Impaired Individual. Unauthorized tracking or stalking is strictly prohibited.

7. Governing Law These Terms shall be governed by and construed in accordance with the laws of the Republic of the Philippines.

User Action: By clicking "I Agree" below, you acknowledge that you have read, understood, and accept the Terms and Conditions above.
    """

    const val PRIVACY_POLICY = """
Last Updated: December 2025

1. Introduction NaviSight Team  is committed to protecting your privacy. This Privacy Policy explains how we collect, use, and safeguard your information when you use our mobile application. We adhere to the principles of transparency, legitimate purpose, and proportionality as required by the Data Privacy Act of 2012 (Republic Act No. 10173) of the Philippines.

2. Information We Collect To provide our assistive services, we collect the following types of data:

Personal Information: When you create an account, we collect your name, email address, and contact number.

Location Data (Background Tracking):

For "Viu" (User) Accounts: NaviSight collects precise location data (GPS, Wi-Fi, Cellular) even when the app is closed or not in use. This is strictly required to enable the "Real-time Monitoring" and "Geofencing Alerts" features for the linked Companion.

For Companion Accounts: We may collect your location to calculate the distance between you and the Viu.

Camera & Images: We require camera access solely for scanning QR codes to pair a Companion with a Viu. We do not store or transmit raw video or images from your camera.

Device Information: We collect data about your device model, OS version, and battery level to inform the Companion of the Viu’s device status.

3. How We Use Your Data We use your data strictly for the following purposes:

To visualize the Viu's location on the Companion’s map.

To send push notifications (e.g., "Viu has left the safe zone").

To maintain a travel history log for safety review.

To authenticate your identity and secure your account.

4. Data Storage and Security

Third-Party Service: We use Google Firebase (Firestore and Realtime Database) to store and process your data. Firebase is a secure cloud platform compliant with global security standards.

Security: We implement industry-standard encryption to protect your data during transmission. However, no method of transmission over the internet is 100% secure.

5. Data Sharing and Disclosure We do not sell, trade, or rent your personal identification information to others.

Companion Sharing: Location data is shared only with the specific Companion account linked to the Viu via the unique QR pairing code.

Legal Requirements: We may disclose your information if required to do so by law or in response to valid requests by public authorities (e.g., a court order).

6. Your Rights (Data Privacy Act of 2012) As a data subject, you have the following rights:

Right to Access: You may request a copy of the personal data we hold about you.

Right to Rectification: You may ask us to correct any inaccurate data.

Right to Erasure (Right to be Forgotten): You may request that we delete your account and all associated location history from our servers.

7. Contact Us If you have questions about this Privacy Policy or wish to exercise your data privacy rights, please contact us at: navisightSupport@gmail.com
    """
}