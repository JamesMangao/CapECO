/**
 * Firebase Cloud Function to send email using Brevo (Sendinblue)
 */

const { setGlobalOptions } = require("firebase-functions/v2");
const { onRequest } = require("firebase-functions/v2/https");
const logger = require("firebase-functions/logger");
const nodemailer = require("nodemailer");
const functions = require("firebase-functions");

// Limit concurrent containers for cost control
setGlobalOptions({ maxInstances: 5 });

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
      from: `"EcoCycle" <YOUR_BREVO_VERIFIED_EMAIL@example.com>`,
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
