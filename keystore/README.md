# Keystores

`victorgps-debug.jks` is a **debug-only** PKCS12 keystore. It is committed on purpose so every
machine and CI run produce APKs with the same debug signing cert.

- alias / store password / key password: `victorgps-debug`
- SHA-256: `38:AE:B4:F2:B9:A6:40:C5:46:FB:5D:DC:45:EA:79:BE:8E:89:BC:2C:76:9D:3C:87:B6:4B:F9:7A:27:B0:38:49`

**The release key is never committed.** `.gitignore` blocks `*.jks`, `*.keystore` and `*.p12` except this
file. Release builds read the key from CI secrets (`VG_RELEASE_KEYSTORE_B64`, `VG_RELEASE_STORE_PASSWORD`,
`VG_RELEASE_KEY_ALIAS`, `VG_RELEASE_KEY_PASSWORD`). Without them, release signing isn't configured.
