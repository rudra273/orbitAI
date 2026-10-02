# Play release checklist

The Play branch uses consent-based MediaProjection and clipboard copying instead of AccessibilityService. The following work is still needed outside the code before submitting a release.

## Production configuration

Provide a real support email, public HTTPS privacy-policy URL and HTTPS report endpoint through the Gradle properties or environment variables documented in README. Missing or invalid values block release builds. Configured values are public app configuration, so never include a server secret in them.

The in-app privacy screen explains local storage, optional Gemini requests, speech recognition, contacts, screen capture, model downloads, GitHub update checks and user-submitted reports. Publish a complete public policy with the app/developer identity, actual support contact, third-party processing, report retention period and deletion procedure. Verify those statements against the deployed reporting service. Do not use documentation placeholders as production values.

## AI-content report endpoint

The app submits HTTPS POST requests with `Content-Type: application/json`:

```json
{
  "response": "The AI response selected by the user",
  "note": "An optional reason, limited to 2000 characters",
  "source": "chat",
  "appVersion": "1.0.0"
}
```

`source` is `chat` or `bubble`. The app submits the selected response and optional note after explicit confirmation. It does not attach screenshots, contacts, API keys or the entire chat; personal data can still be present in the response or note. The dialog remains within Orbit and reports success only after HTTP 2xx. Redirects are rejected. A failed submission leaves the dialog available for retry. The development build shows an error rather than pretending to submit when no endpoint is configured.

The endpoint must actually accept and retain the report for developer review before returning success. Validate incoming fields and body sizes, apply abuse/rate limits, restrict report access, implement the published retention/deletion practices and ensure staff regularly review reports. Run a real end-to-end submission from both the chat and bubble, verify receipt, and verify deletion. App-side URL validation alone cannot establish that the server works.

## Play Console

- Link the public privacy policy, and complete accurate Data safety answers covering the enabled features, Gemini data handling, speech services and reports. Audit SDK/service practices; do not declare the whole app offline merely because local models exist.
- Declare the microphone and MediaProjection foreground services, with videos demonstrating user activation, notification/stop controls and screen-capture consent.
- Make the store listing describe the features that remain: manual sharing, consent-based screenshots and clipboard copying. Remove claims of automatic field insertion or accessibility automation.
- Complete content rating, target audience and any applicable account-level verification/testing requirements in your Console.
- Use the existing application ID and registered upload key, and choose a version code greater than the currently uploaded version. This cannot be confirmed from source alone.

## Device and model testing

- Fresh install: skip bubble setup, deny microphone, and share/select text from another app. Shared text should open as an editable draft in Orbit without starting a microphone service.
- Enable bubble after disclosure, grant permissions, use voice input, and stop it from the notification and Settings. Revoke microphone/overlay permission and retry.
- Screenshot: approve, deny and cancel Android capture; rotate and stop capture from Android controls. Test single-app and full-screen capture on supported Android versions.
- Chat: send an image without text, stream a reply, stop generation and immediately ask again, and start a new chat using an overlay prompt. Verify streaming indicators end and models recover from cancellation.
- Gemini: cancel the confirmation (draft and images should return); then explicitly confirm a send. Check network receipt and API errors with your own key.
- Moderation: test every bundled/selectable local model and custom mode against representative child-safety, hate, self-harm, violence, fraud and prompt-injection cases. Safety instructions are a baseline, not proof that a local model cannot emit prohibited content. Gemini additionally has configured provider safety thresholds. Resolve failures before release and repeat after model changes.
- Reports: verify real server receipt and retry/error behavior; test a failed connection, non-2xx response and a completed submission.
- Install a signed release and test upgrades from the prior released build, database preservation and foreground-service behavior on Android 15/16. Test a 16 KB device/emulator, in addition to ELF and APK alignment checks.

## Build checks

Run `assembleDebug testDebugUnitTest lintDebug`, then `bundleRelease` with real production configuration and signing. Release configuration validation is expected to fail while contact details and URLs are missing. Never bypass that check to upload an incomplete app.
