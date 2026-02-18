# NutriTrack - AI-Powered Nutrition Tracker

A mobile-first nutrition tracking app with AI-powered meal analysis and coaching.

## Features

- **AI Meal Analysis**: Describe what you ate or take a photo — AI estimates calories & protein
- **Nutrition Coach**: Chat with an AI coach for personalized diet advice
- **Daily Tracking**: Track calories, protein, and water intake with visual progress rings
- **Streak System**: Build daily streaks to stay motivated
- **Calendar History**: View your history with star ratings (⭐ full, ✨ close, 💔 missed)
- **Smart Day Boundary**: Day resets at 5 AM (for night owls)
- **Water Tracking**: Track daily water intake with cup counter
- **Meal Management**: Edit or delete meals, manually adjust calories/protein
- **Offline First**: All data stored locally on device
- **Israeli Food AI**: AI recognizes Israeli supermarket products and homemade dishes

## Setup

### 1. Install dependencies

```bash
cd nutrition-tracker
npm install
```

### 2. Configure AI

Open the app → Settings → Enter your **Gemini API Key** from [Google AI Studio](https://aistudio.google.com/apikey)

You can customize the AI models:
- **Coach Model**: For the nutrition coach chat (default: `gemini-2.0-flash`)
- **Analyze Model**: For food analysis (default: `gemini-2.0-flash`)

### 3. Run the app

```bash
npx expo start
```

Scan the QR code with Expo Go on your phone.

## Build APK

### Using EAS Build (Recommended)

```bash
# Install EAS CLI
npm install -g eas-cli

# Login to Expo
eas login

# Build APK
eas build -p android --profile preview
```

The APK download link will be provided after the build completes.

### Local Build

```bash
# Generate native Android project
npx expo prebuild --platform android

# Build APK
cd android && ./gradlew assembleRelease
```

APK will be at `android/app/build/outputs/apk/release/app-release.apk`

## Goal System

- **90%+ of goal** = Shows checkmark (counts as met)
- **80-120% of goal** = ⭐ Full star
- **60-80% or 120-140%** = ✨ Half star (close)
- **Below 60% or above 140%** = 💔 Broken star (missed)

## Tech Stack

- React Native (Expo)
- AsyncStorage (local data)
- Gemini API (AI features)
- React Navigation
- react-native-svg (progress rings)
- react-native-markdown-display (coach chat)
