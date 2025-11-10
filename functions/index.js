/**
 * Firebase Cloud Function to send email using Brevo (Sendinblue)
 * and a callable function for user email lookup during login.
 */

const { setGlobalOptions } = require("firebase-functions/v2");
const { onRequest } = require("firebase-functions/v2/https");
const { onCall } = require("firebase-functions/v2/https"); // Import onCall for callable functions
const logger = require("firebase-functions/logger");
const nodemailer = require("nodemailer");
const functions = require("firebase-functions"); // Keep for functions.config()
const admin = require('firebase-admin'); // Import Firebase Admin SDK

// Initialize Firebase Admin SDK
admin.initializeApp();

// Limit concurrent containers for cost control
setGlobalOptions({ maxInstances: 5 });

// --- Brevo (Sendinblue) Email Function Setup ---
// Load your Brevo (Sendinblue) API key from Firebase config
const brevoKey = functions.config().brevo.key;

// Create transporter
const transporter = nodemailer.createTransport({
  host: "smtp-relay.brevo.com",
  port: 587,
  secure: false,
  auth: {
    user: "jubail.castro@cdsp.edu.ph", // must match a verified sender in Brevo
    pass: brevoKey,
  },
});

exports.sendEmail = onRequest(async (req, res) => {
  try {
    const { email, subject, message } = req.body;

    if (!email || !subject || !message) {
      return res.status(400).send("Missing email, subject, or message");
    }

    const mailOptions = {
      from: `"EcoCycle" jubail.castro@cdsp.edu.ph`, // IMPORTANT: Update this with your verified sender email
      to: email,
      subject,
      html: `<p>${message}</p>`,
    };

    await transporter.sendMail(mailOptions);
    logger.info(`✅ Email sent successfully to ${email}`);
    res.status(200).send("Email sent successfully!");
  } catch (error) {
    logger.error("❌ Error sending email:", error);
    res.status(500).send("Error sending email: " + error.message);
  }
});


// --- User Email Lookup Cloud Function for Login ---
exports.lookupUserEmailForLogin = onCall(async (request) => {
    const input = request.data.input; // This will be studentId or name from the app

    // Basic input validation
    if (!input) {
        logger.error("🚫 lookupUserEmailForLogin: Input field is missing.");
        throw new functions.https.HttpsError('invalid-argument', 'The input field is required.');
    }

    let userEmail = null;

    try {
        // Function to search a given collection
        const searchCollection = async (collectionName) => {
            logger.info(`🔍 lookupUserEmailForLogin: Searching collection '${collectionName}' for input: "${input}"`);

            // Try to find by studentId first in this collection
            let querySnapshot = await admin.firestore().collection(collectionName)
                .where('studentId', '==', input)
                .limit(1)
                .get();

            if (!querySnapshot.empty) {
                const email = querySnapshot.docs[0].data().email;
                logger.info(`✅ lookupUserEmailForLogin: Found email "${email}" by studentId in '${collectionName}'.`);
                return email;
            }

            // If not found by studentId, try to find by name
            // IMPORTANT: Ensure the 'name' field in Firestore exactly matches the input.
            // If case-insensitivity is needed, you might need to adjust this query
            // (e.g., by storing names in lowercase and querying with lowercase input).
            querySnapshot = await admin.firestore().collection(collectionName)
                .where('name', '==', input)
                .limit(1)
                .get();

            if (!querySnapshot.empty) {
                const email = querySnapshot.docs[0].data().email;
                logger.log(`✅ lookupUserEmailForLogin: Found email "${email}" by name in '${collectionName}'.`);
                return email;
            }

            return null; // Not found in this collection
        };

        // 1. Try to find in the 'users' collection
        userEmail = await searchCollection('users');

        // 2. If not found in 'users', try to find in the 'admins' collection
        if (!userEmail) {
            userEmail = await searchCollection('admins');
        }

        if (userEmail) {
            // Successfully found an email from either 'users' or 'admins'
            return { email: userEmail };
        } else {
            // If no user/admin found by either studentId or name in relevant collections
            logger.warn(`⚠️ lookupUserEmailForLogin: No user/admin found for input "${input}" (neither by studentId nor by name in 'users' or 'admins').`);
            throw new functions.https.HttpsError('not-found', 'No user found with the provided ID or name.');
        }
    } catch (error) {
        logger.error(`❌ lookupUserEmailForLogin: Error during email lookup for input "${input}": ${error.message}`, error);
        // Re-throw as HttpsError if not already one
        if (error instanceof functions.https.HttpsError) {
            throw error;
        } else {
            throw new functions.https.HttpsError('internal', 'An unexpected error occurred during email lookup.');
        }
    }
});
