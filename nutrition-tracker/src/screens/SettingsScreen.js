import React, { useState, useCallback } from 'react';
import {
  View,
  Text,
  StyleSheet,
  TextInput,
  TouchableOpacity,
  ScrollView,
  Alert,
  Switch,
} from 'react-native';
import { useFocusEffect } from '@react-navigation/native';
import { COLORS, FONTS, RADIUS, SPACING, SHADOWS } from '../utils/theme';
import { getSettings, saveSettings, getStreak } from '../utils/storage';

export default function SettingsScreen() {
  const [settings, setLocalSettings] = useState({});
  const [streak, setStreak] = useState({ current: 0, best: 0 });
  const [saving, setSaving] = useState(false);
  const [showApiKey, setShowApiKey] = useState(false);

  useFocusEffect(
    useCallback(() => {
      loadSettings();
    }, [])
  );

  const loadSettings = async () => {
    const [s, st] = await Promise.all([getSettings(), getStreak()]);
    setLocalSettings(s);
    setStreak(st);
  };

  const handleSave = async () => {
    setSaving(true);
    await saveSettings(settings);
    setSaving(false);
    Alert.alert('Saved', 'Settings updated successfully!');
  };

  const updateField = (field, value) => {
    setLocalSettings((prev) => ({ ...prev, [field]: value }));
  };

  return (
    <View style={styles.container}>
      <View style={styles.header}>
        <Text style={styles.headerTitle}>⚙️ Settings</Text>
        <Text style={styles.headerSubtitle}>Configure your goals and AI</Text>
      </View>

      <ScrollView style={styles.scrollView} showsVerticalScrollIndicator={false}>
        {/* Streak Info */}
        <View style={styles.streakCard}>
          <View style={styles.streakRow}>
            <View style={styles.streakItem}>
              <Text style={styles.streakEmoji}>🔥</Text>
              <Text style={styles.streakValue}>{streak.current}</Text>
              <Text style={styles.streakLabel}>Current Streak</Text>
            </View>
            <View style={styles.streakDivider} />
            <View style={styles.streakItem}>
              <Text style={styles.streakEmoji}>🏆</Text>
              <Text style={styles.streakValue}>{streak.best}</Text>
              <Text style={styles.streakLabel}>Best Streak</Text>
            </View>
          </View>
        </View>

        {/* Daily Goals */}
        <View style={styles.section}>
          <Text style={styles.sectionTitle}>🎯 Daily Goals</Text>

          <View style={styles.inputGroup}>
            <Text style={styles.label}>Calorie Goal (kcal)</Text>
            <TextInput
              style={styles.input}
              value={String(settings.calorieGoal || '')}
              onChangeText={(v) => updateField('calorieGoal', parseInt(v) || 0)}
              keyboardType="numeric"
              placeholder="2000"
              placeholderTextColor={COLORS.textMuted}
            />
          </View>

          <View style={styles.inputGroup}>
            <Text style={styles.label}>Protein Goal (grams)</Text>
            <TextInput
              style={styles.input}
              value={String(settings.proteinGoal || '')}
              onChangeText={(v) => updateField('proteinGoal', parseInt(v) || 0)}
              keyboardType="numeric"
              placeholder="150"
              placeholderTextColor={COLORS.textMuted}
            />
          </View>

          <View style={styles.inputGroup}>
            <Text style={styles.label}>Water Goal (cups)</Text>
            <TextInput
              style={styles.input}
              value={String(settings.waterGoal || '')}
              onChangeText={(v) => updateField('waterGoal', parseInt(v) || 0)}
              keyboardType="numeric"
              placeholder="8"
              placeholderTextColor={COLORS.textMuted}
            />
          </View>
        </View>

        {/* Fitness Profile */}
        <View style={styles.section}>
          <Text style={styles.sectionTitle}>💪 Fitness Profile</Text>

          <View style={styles.inputGroup}>
            <Text style={styles.label}>Overall Goal</Text>
            <Text style={styles.labelHint}>e.g., חיטוב, muscle gain, weight loss, maintenance</Text>
            <TextInput
              style={styles.input}
              value={settings.overallGoal || ''}
              onChangeText={(v) => updateField('overallGoal', v)}
              placeholder="e.g., חיטוב, Lean muscle..."
              placeholderTextColor={COLORS.textMuted}
            />
          </View>

          <View style={styles.inputGroup}>
            <Text style={styles.label}>Gym Frequency</Text>
            <Text style={styles.labelHint}>How often do you work out?</Text>
            <TextInput
              style={styles.input}
              value={settings.gymFrequency || ''}
              onChangeText={(v) => updateField('gymFrequency', v)}
              placeholder="e.g., Every 2-3 days, 5x a week..."
              placeholderTextColor={COLORS.textMuted}
            />
          </View>
        </View>

        {/* AI Configuration */}
        <View style={styles.section}>
          <Text style={styles.sectionTitle}>🤖 AI Configuration</Text>

          <View style={styles.inputGroup}>
            <Text style={styles.label}>Gemini API Key</Text>
            <Text style={styles.labelHint}>
              Get your key from Google AI Studio
            </Text>
            <View style={styles.apiKeyRow}>
              <TextInput
                style={[styles.input, { flex: 1 }]}
                value={settings.geminiApiKey || ''}
                onChangeText={(v) => updateField('geminiApiKey', v)}
                placeholder="Enter your Gemini API key"
                placeholderTextColor={COLORS.textMuted}
                secureTextEntry={!showApiKey}
                autoCapitalize="none"
                autoCorrect={false}
              />
              <TouchableOpacity
                style={styles.showKeyBtn}
                onPress={() => setShowApiKey(!showApiKey)}>
                <Text style={styles.showKeyText}>{showApiKey ? '🙈' : '👁️'}</Text>
              </TouchableOpacity>
            </View>
          </View>

          <View style={styles.inputGroup}>
            <Text style={styles.label}>Coach AI Model</Text>
            <Text style={styles.labelHint}>Model for the nutrition coach chat</Text>
            <TextInput
              style={styles.input}
              value={settings.coachModel || ''}
              onChangeText={(v) => updateField('coachModel', v)}
              placeholder="gemini-2.0-flash"
              placeholderTextColor={COLORS.textMuted}
              autoCapitalize="none"
              autoCorrect={false}
            />
          </View>

          <View style={styles.inputGroup}>
            <Text style={styles.label}>Meal Analysis AI Model</Text>
            <Text style={styles.labelHint}>Model for analyzing meals/food photos</Text>
            <TextInput
              style={styles.input}
              value={settings.analyzeModel || ''}
              onChangeText={(v) => updateField('analyzeModel', v)}
              placeholder="gemini-2.0-flash"
              placeholderTextColor={COLORS.textMuted}
              autoCapitalize="none"
              autoCorrect={false}
            />
          </View>
        </View>

        {/* Info Section */}
        <View style={styles.infoSection}>
          <Text style={styles.infoTitle}>ℹ️ About</Text>
          <Text style={styles.infoText}>
            • Day resets at 5:00 AM (for night owls){'\n'}
            • 90%+ of goal counts as complete ✓{'\n'}
            • Going 20%+ over goal counts as a miss{'\n'}
            • ⭐ = Goal met (80-120%){'\n'}
            • ✨ = Close (60-80% or 120-140%){'\n'}
            • 💔 = Missed (&lt;60% or &gt;140%)
          </Text>
        </View>

        {/* Save Button */}
        <TouchableOpacity
          style={[styles.saveBtn, saving && styles.saveBtnDisabled]}
          onPress={handleSave}
          disabled={saving}>
          <Text style={styles.saveBtnText}>
            {saving ? 'Saving...' : '💾 Save Settings'}
          </Text>
        </TouchableOpacity>

        <View style={{ height: 100 }} />
      </ScrollView>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: COLORS.background,
  },
  header: {
    paddingTop: SPACING.xxl + 10,
    paddingHorizontal: SPACING.lg,
    paddingBottom: SPACING.md,
    backgroundColor: COLORS.background,
  },
  headerTitle: {
    ...FONTS.title,
    fontSize: 22,
  },
  headerSubtitle: {
    ...FONTS.small,
    color: COLORS.textSecondary,
    marginTop: 2,
  },
  scrollView: {
    flex: 1,
  },
  streakCard: {
    marginHorizontal: SPACING.md,
    marginBottom: SPACING.lg,
    backgroundColor: COLORS.backgroundCard,
    borderRadius: RADIUS.lg,
    padding: SPACING.lg,
    ...SHADOWS.small,
  },
  streakRow: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  streakItem: {
    flex: 1,
    alignItems: 'center',
  },
  streakDivider: {
    width: 1,
    height: 60,
    backgroundColor: COLORS.border,
  },
  streakEmoji: {
    fontSize: 32,
    marginBottom: 4,
  },
  streakValue: {
    ...FONTS.bold,
    fontSize: 28,
    color: COLORS.streak,
  },
  streakLabel: {
    ...FONTS.small,
    color: COLORS.textSecondary,
    marginTop: 2,
  },
  section: {
    marginHorizontal: SPACING.md,
    marginBottom: SPACING.lg,
    backgroundColor: COLORS.backgroundCard,
    borderRadius: RADIUS.lg,
    padding: SPACING.lg,
    ...SHADOWS.small,
  },
  sectionTitle: {
    ...FONTS.subtitle,
    marginBottom: SPACING.md,
  },
  inputGroup: {
    marginBottom: SPACING.md,
  },
  label: {
    ...FONTS.medium,
    color: COLORS.textSecondary,
    marginBottom: 4,
  },
  labelHint: {
    ...FONTS.tiny,
    color: COLORS.textMuted,
    marginBottom: SPACING.sm,
  },
  input: {
    backgroundColor: COLORS.backgroundInput,
    borderRadius: RADIUS.sm,
    padding: SPACING.md,
    color: COLORS.text,
    fontSize: 16,
    borderWidth: 1,
    borderColor: COLORS.border,
  },
  apiKeyRow: {
    flexDirection: 'row',
    gap: SPACING.sm,
  },
  showKeyBtn: {
    width: 48,
    backgroundColor: COLORS.backgroundInput,
    borderRadius: RADIUS.sm,
    alignItems: 'center',
    justifyContent: 'center',
    borderWidth: 1,
    borderColor: COLORS.border,
  },
  showKeyText: {
    fontSize: 20,
  },
  infoSection: {
    marginHorizontal: SPACING.md,
    marginBottom: SPACING.lg,
    backgroundColor: COLORS.backgroundCard,
    borderRadius: RADIUS.lg,
    padding: SPACING.lg,
    borderWidth: 1,
    borderColor: COLORS.border,
  },
  infoTitle: {
    ...FONTS.medium,
    marginBottom: SPACING.sm,
  },
  infoText: {
    ...FONTS.small,
    color: COLORS.textSecondary,
    lineHeight: 22,
  },
  saveBtn: {
    marginHorizontal: SPACING.md,
    backgroundColor: COLORS.primary,
    borderRadius: RADIUS.lg,
    padding: SPACING.md,
    alignItems: 'center',
    ...SHADOWS.medium,
  },
  saveBtnDisabled: {
    opacity: 0.7,
  },
  saveBtnText: {
    ...FONTS.bold,
    fontSize: 16,
    color: COLORS.text,
  },
});
