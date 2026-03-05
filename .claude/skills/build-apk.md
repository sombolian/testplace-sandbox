# Skill: build-apk

## Description
Build the Android debug APK for this Nutrition Tracker project. Use when the user asks to build, compile, or generate an APK.

## Steps

### 1. Ensure Android SDK is installed
The environment does NOT come with an Android SDK pre-installed. You must set it up manually.

**SDK location:** `/home/user/android-sdk`
**local.properties** should contain: `sdk.dir=/home/user/android-sdk`

If the SDK directory doesn't exist or is missing components, install them:

```bash
mkdir -p /home/user/android-sdk/platforms /home/user/android-sdk/build-tools /home/user/android-sdk/cmdline-tools
```

#### Install command-line tools (needed for AGP plugin resolution)
```bash
cd /home/user/android-sdk/cmdline-tools
curl -sL "https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip" -o cmdline-tools.zip
unzip -q cmdline-tools.zip && mv cmdline-tools latest && rm cmdline-tools.zip
```

#### Install platform android-35
```bash
curl -sL "https://dl.google.com/android/repository/platform-35_r01.zip" -o /tmp/platform35.zip
unzip -q /tmp/platform35.zip -d /home/user/android-sdk/platforms/
rm /tmp/platform35.zip
```

IMPORTANT: This zip does NOT include a `package.xml`. You must create one at `/home/user/android-sdk/platforms/android-35/package.xml`:

```xml
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<ns2:repository xmlns:ns2="http://schemas.android.com/repository/android/common/02" xmlns:ns11="http://schemas.android.com/sdk/android/repo/repository2/03">
<localPackage path="platforms;android-35" obsolete="false">
<type-details xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:type="ns11:platformDetailsType">
<api-level>35</api-level>
<layoutlib api="15"/>
</type-details>
<revision><major>1</major></revision>
<display-name>Android SDK Platform 35</display-name>
</localPackage>
</ns2:repository>
```

#### Install build-tools 34.0.0 (required by AGP 8.7.3)
```bash
curl -sL "https://dl.google.com/android/repository/build-tools_r34-linux.zip" -o /tmp/bt34.zip
unzip -q /tmp/bt34.zip -d /tmp/bt34
mv /tmp/bt34/android-14 /home/user/android-sdk/build-tools/34.0.0
rm -rf /tmp/bt34.zip /tmp/bt34
```

Note: The zip extracts to a folder named `android-14`, which must be renamed to `34.0.0`.

#### Create local.properties
```bash
echo "sdk.dir=/home/user/android-sdk" > /home/user/testplace-sandbox/local.properties
```

### 2. Configure Gradle proxy (if needed)
The container environment uses an egress proxy. Gradle needs proxy settings in `~/.gradle/gradle.properties`:

```properties
systemProp.http.proxyHost=21.0.0.75
systemProp.http.proxyPort=15004
systemProp.https.proxyHost=21.0.0.75
systemProp.https.proxyPort=15004
systemProp.http.nonProxyHosts=localhost|127.0.0.1
android.useAndroidX=true
```

Note: the proxy host/port may change between sessions. Check JAVA_TOOL_OPTIONS env var for current values if the above doesn't work.

### 3. Build the APK
```bash
ANDROID_HOME=/home/user/android-sdk ./gradlew assembleDebug
```

Timeout: allow up to 10 minutes (600s). First build takes ~5 minutes, subsequent builds are faster.

### 4. Output
The APK will be at:
```
/home/user/testplace-sandbox/app/build/outputs/apk/debug/app-debug.apk
```

Size is approximately 19MB.

### 5. Deliver the APK to the user
The user cannot access localhost links. To give them a download link:

1. Copy the APK to the repo root:
```bash
cp /home/user/testplace-sandbox/app/build/outputs/apk/debug/app-debug.apk /home/user/testplace-sandbox/app-debug.apk
```

2. Commit and push to git:
```bash
git add app-debug.apk
git commit -m "Add built debug APK for download"
git push -u origin <current-branch-name>
```

3. Give the user the raw GitHub download link. The format is:
```
https://github.com/<owner>/<repo>/raw/<branch>/app-debug.apk
```

Get the remote URL and branch name to construct this:
```bash
git remote get-url origin
git branch --show-current
```

### Quick check before full setup
Before installing everything from scratch, check if the SDK already exists:
```bash
ls /home/user/android-sdk/platforms/android-35/android.jar 2>/dev/null && \
ls /home/user/android-sdk/build-tools/34.0.0/aapt2 2>/dev/null && \
echo "SDK ready" || echo "SDK needs setup"
```

If "SDK ready", skip directly to step 3.
