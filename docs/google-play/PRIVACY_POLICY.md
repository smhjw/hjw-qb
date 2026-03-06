# Privacy Policy for qbremote

Last updated: 2026-03-06

`qbremote` is an Android client for managing qBittorrent through the qBittorrent WebUI API.

## 1. Data We Collect

We do not collect personal data on our own servers.

The app stores connection settings locally on your device, including:
- Host / port / protocol settings
- Username
- Password (stored locally using Android encrypted storage)

## 2. How Data Is Used

Connection data is used only to authenticate and communicate with the qBittorrent server you configure.

The app sends requests directly from your device to your configured qBittorrent WebUI endpoint to:
- Sign in
- Fetch torrent and transfer status
- Execute torrent management actions

## 3. Data Sharing

We do not sell, rent, or share your personal data with advertising networks, analytics providers, or data brokers.

## 4. Third-Party Services

The app currently does not include third-party analytics, crash reporting, or advertising SDKs.

## 5. Data Security

- Local password storage uses encrypted Android storage (`EncryptedSharedPreferences`).
- Transmission security depends on your server configuration:
  - HTTPS is supported and recommended.
  - HTTP may be used if you configure it (for example, in trusted LAN environments).

## 6. Data Retention and Deletion

All app data is controlled by you on your device.
You can remove server profiles and uninstall the app at any time to delete local data.

## 7. Children

This app is not specifically directed to children.

## 8. Changes to This Policy

We may update this policy when app behavior or legal requirements change.
The latest version will be published in this repository.

## 9. Contact

For support or privacy questions, replace this placeholder with your public contact email before publishing:

`[YOUR_PUBLIC_SUPPORT_EMAIL]`
