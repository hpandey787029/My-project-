# Android Screen Recorder — build the APK in the cloud (no Android Studio)

## 1. Create a GitHub repo
1. Go to github.com → sign in (or create a free account).
2. Click **New repository**. Name it e.g. `screen-recorder`. Keep it **Public** or **Private**, either works. Don't add a README (we already have one).
3. Click **Create repository**.

## 2. Upload this project
On the new repo's page:
1. Click **"uploading an existing file"** (on the quick-setup screen), or **Add file → Upload files**.
2. Drag in **every file and folder** from this ScreenRecorder folder, keeping the same structure (including the hidden `.github` folder — if your upload UI hides dotfiles, see the "zip trick" below).
3. Commit directly to the `main` branch.

**Zip trick for the `.github` folder:** GitHub's drag-and-drop sometimes skips folders starting with a dot. If `.github/workflows/build-apk.yml` doesn't appear after upload, go to your repo → **Add file → Create new file**, name it `.github/workflows/build-apk.yml` (typing the slashes creates the folders), and paste in the contents of that file from this project.

## 3. Run the build
1. In your repo, click the **Actions** tab.
2. You should see "Build APK" listed. Click it, then click **Run workflow → Run workflow** (green button).
3. Wait 2–4 minutes for it to finish (green checkmark).

## 4. Download the APK to your phone
1. Click the finished run → scroll to **Artifacts** → tap **ScreenRecorder-debug-apk** to download a `.zip`.
2. On your phone (or download on a computer and AirDrop/transfer/email it to your phone), unzip it to get `app-debug.apk`.
3. Tap the APK file to install. Android will ask to allow installs from this source (Files app or your browser) — allow it once.
4. Open the app, tap **Start recording**, approve the system prompt. Done — this is a real installed app, so it uses Android's native screen-capture API and works fully on your phone.

## Updating the app later
Any time you change the code and push to `main` (or re-upload changed files), the Action re-runs automatically and a fresh APK artifact appears under **Actions**.
