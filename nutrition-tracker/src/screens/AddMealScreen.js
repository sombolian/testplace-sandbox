import React, { useState } from 'react';
import {
  View,
  Text,
  StyleSheet,
  TextInput,
  TouchableOpacity,
  ActivityIndicator,
  Alert,
  ScrollView,
  KeyboardAvoidingView,
  Platform,
  Image,
} from 'react-native';
import * as ImagePicker from 'expo-image-picker';
import { COLORS, FONTS, RADIUS, SPACING, SHADOWS } from '../utils/theme';
import { addMeal, addWater, getEffectiveDate } from '../utils/storage';
import { analyzeMeal } from '../utils/ai';

export default function AddMealScreen({ navigation, route }) {
  const targetDate = route?.params?.date || null;
  const [description, setDescription] = useState('');
  const [imageUri, setImageUri] = useState(null);
  const [imageBase64, setImageBase64] = useState(null);
  const [loading, setLoading] = useState(false);
  const [result, setResult] = useState(null);

  const pickImage = async () => {
    const permResult = await ImagePicker.requestMediaLibraryPermissionsAsync();
    if (!permResult.granted) {
      Alert.alert('Permission needed', 'Please allow access to your photo library');
      return;
    }

    const pickerResult = await ImagePicker.launchImageLibraryAsync({
      mediaTypes: ['images'],
      allowsEditing: true,
      quality: 0.7,
      base64: true,
    });

    if (!pickerResult.canceled && pickerResult.assets[0]) {
      setImageUri(pickerResult.assets[0].uri);
      setImageBase64(pickerResult.assets[0].base64);
    }
  };

  const takePhoto = async () => {
    const permResult = await ImagePicker.requestCameraPermissionsAsync();
    if (!permResult.granted) {
      Alert.alert('Permission needed', 'Please allow access to your camera');
      return;
    }

    const pickerResult = await ImagePicker.launchCameraAsync({
      allowsEditing: true,
      quality: 0.7,
      base64: true,
    });

    if (!pickerResult.canceled && pickerResult.assets[0]) {
      setImageUri(pickerResult.assets[0].uri);
      setImageBase64(pickerResult.assets[0].base64);
    }
  };

  const handleAnalyze = async () => {
    if (!description.trim() && !imageBase64) {
      Alert.alert('Input needed', 'Please describe what you ate or take a photo');
      return;
    }

    setLoading(true);
    try {
      const analysis = await analyzeMeal(description.trim(), imageBase64);
      setResult(analysis);
    } catch (e) {
      Alert.alert('Error', e.message || 'Failed to analyze meal. Check your API key in Settings.');
    }
    setLoading(false);
  };

  const handleAdd = async () => {
    if (!result) return;

    try {
      if (result.isWater && result.waterCups > 0) {
        await addWater(result.waterCups, targetDate);
      }

      await addMeal(
        {
          name: result.name || description,
          description: result.description || '',
          calories: result.calories || 0,
          protein: result.protein || 0,
          isWater: result.isWater || false,
          waterCups: result.waterCups || 0,
          imagePrompt: result.imagePrompt || '',
        },
        targetDate
      );

      navigation.goBack();
    } catch (e) {
      Alert.alert('Error', 'Failed to save meal');
    }
  };

  const handleQuickWater = async () => {
    setLoading(true);
    try {
      await addWater(1, targetDate);
      await addMeal(
        {
          name: 'Water',
          description: '1 cup of water',
          calories: 0,
          protein: 0,
          isWater: true,
          waterCups: 1,
        },
        targetDate
      );
      navigation.goBack();
    } catch (e) {
      Alert.alert('Error', 'Failed to add water');
    }
    setLoading(false);
  };

  return (
    <KeyboardAvoidingView
      style={styles.container}
      behavior={Platform.OS === 'ios' ? 'padding' : 'height'}>
      <ScrollView style={styles.scrollView} showsVerticalScrollIndicator={false}>
        {/* Header */}
        <View style={styles.header}>
          <TouchableOpacity onPress={() => navigation.goBack()}>
            <Text style={styles.backBtn}>← Back</Text>
          </TouchableOpacity>
          <Text style={styles.title}>Add Meal</Text>
          {targetDate && (
            <Text style={styles.dateLabel}>Adding to: {targetDate}</Text>
          )}
        </View>

        {/* Quick Water Button */}
        <TouchableOpacity style={styles.quickWater} onPress={handleQuickWater}>
          <Text style={styles.quickWaterEmoji}>💧</Text>
          <Text style={styles.quickWaterText}>Quick Add: Cup of Water</Text>
        </TouchableOpacity>

        {/* Image Input */}
        <View style={styles.imageSection}>
          <Text style={styles.sectionLabel}>Photo (optional)</Text>
          <View style={styles.imageButtons}>
            <TouchableOpacity style={styles.imageBtn} onPress={takePhoto}>
              <Text style={styles.imageBtnEmoji}>📷</Text>
              <Text style={styles.imageBtnText}>Take Photo</Text>
            </TouchableOpacity>
            <TouchableOpacity style={styles.imageBtn} onPress={pickImage}>
              <Text style={styles.imageBtnEmoji}>🖼️</Text>
              <Text style={styles.imageBtnText}>Gallery</Text>
            </TouchableOpacity>
          </View>
          {imageUri && (
            <View style={styles.imagePreview}>
              <Image source={{ uri: imageUri }} style={styles.previewImage} />
              <TouchableOpacity
                style={styles.removeImage}
                onPress={() => {
                  setImageUri(null);
                  setImageBase64(null);
                }}>
                <Text style={styles.removeImageText}>✕</Text>
              </TouchableOpacity>
            </View>
          )}
        </View>

        {/* Text Input */}
        <View style={styles.inputSection}>
          <Text style={styles.sectionLabel}>What did you eat?</Text>
          <TextInput
            style={styles.input}
            placeholder="e.g., Grilled chicken with rice, 2 eggs, tuna sandwich..."
            placeholderTextColor={COLORS.textMuted}
            value={description}
            onChangeText={setDescription}
            multiline
            numberOfLines={3}
            textAlignVertical="top"
          />
        </View>

        {/* Analyze Button */}
        <TouchableOpacity
          style={[styles.analyzeBtn, loading && styles.analyzeBtnDisabled]}
          onPress={handleAnalyze}
          disabled={loading}>
          {loading ? (
            <ActivityIndicator color={COLORS.text} />
          ) : (
            <>
              <Text style={styles.analyzeBtnEmoji}>🤖</Text>
              <Text style={styles.analyzeBtnText}>Analyze with AI</Text>
            </>
          )}
        </TouchableOpacity>

        {/* Result */}
        {result && (
          <View style={styles.resultCard}>
            <Text style={styles.resultTitle}>{result.name}</Text>
            {result.nameHe && (
              <Text style={styles.resultNameHe}>{result.nameHe}</Text>
            )}
            {result.description && (
              <Text style={styles.resultDesc}>{result.description}</Text>
            )}

            <View style={styles.resultStats}>
              <View style={styles.resultStat}>
                <Text style={styles.resultStatValue}>{result.calories}</Text>
                <Text style={styles.resultStatLabel}>calories</Text>
              </View>
              <View style={styles.resultStat}>
                <Text style={[styles.resultStatValue, { color: COLORS.protein }]}>
                  {result.protein}g
                </Text>
                <Text style={styles.resultStatLabel}>protein</Text>
              </View>
              {result.isWater && (
                <View style={styles.resultStat}>
                  <Text style={[styles.resultStatValue, { color: COLORS.water }]}>
                    {result.waterCups}
                  </Text>
                  <Text style={styles.resultStatLabel}>cups water</Text>
                </View>
              )}
            </View>

            <TouchableOpacity style={styles.addBtn} onPress={handleAdd}>
              <Text style={styles.addBtnText}>✓ Add to Log</Text>
            </TouchableOpacity>
          </View>
        )}

        <View style={{ height: 40 }} />
      </ScrollView>
    </KeyboardAvoidingView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: COLORS.background,
  },
  scrollView: {
    flex: 1,
  },
  header: {
    paddingTop: SPACING.xxl + 10,
    paddingHorizontal: SPACING.lg,
    paddingBottom: SPACING.md,
  },
  backBtn: {
    ...FONTS.medium,
    color: COLORS.primaryLight,
    marginBottom: SPACING.sm,
  },
  title: {
    ...FONTS.title,
    fontSize: 28,
  },
  dateLabel: {
    ...FONTS.small,
    color: COLORS.secondary,
    marginTop: SPACING.xs,
  },
  quickWater: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: COLORS.backgroundCard,
    marginHorizontal: SPACING.lg,
    marginBottom: SPACING.md,
    padding: SPACING.md,
    borderRadius: RADIUS.lg,
    borderWidth: 1,
    borderColor: COLORS.water + '40',
  },
  quickWaterEmoji: {
    fontSize: 24,
    marginRight: SPACING.sm,
  },
  quickWaterText: {
    ...FONTS.medium,
    color: COLORS.water,
  },
  imageSection: {
    paddingHorizontal: SPACING.lg,
    marginBottom: SPACING.md,
  },
  sectionLabel: {
    ...FONTS.medium,
    color: COLORS.textSecondary,
    marginBottom: SPACING.sm,
  },
  imageButtons: {
    flexDirection: 'row',
    gap: SPACING.sm,
  },
  imageBtn: {
    flex: 1,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: COLORS.backgroundCard,
    padding: SPACING.md,
    borderRadius: RADIUS.md,
    gap: SPACING.sm,
    borderWidth: 1,
    borderColor: COLORS.border,
  },
  imageBtnEmoji: {
    fontSize: 20,
  },
  imageBtnText: {
    ...FONTS.medium,
    color: COLORS.text,
  },
  imagePreview: {
    marginTop: SPACING.sm,
    borderRadius: RADIUS.md,
    overflow: 'hidden',
    position: 'relative',
  },
  previewImage: {
    width: '100%',
    height: 200,
    borderRadius: RADIUS.md,
  },
  removeImage: {
    position: 'absolute',
    top: 8,
    right: 8,
    width: 32,
    height: 32,
    borderRadius: 16,
    backgroundColor: 'rgba(0,0,0,0.7)',
    alignItems: 'center',
    justifyContent: 'center',
  },
  removeImageText: {
    color: COLORS.text,
    fontSize: 16,
    fontWeight: '700',
  },
  inputSection: {
    paddingHorizontal: SPACING.lg,
    marginBottom: SPACING.md,
  },
  input: {
    backgroundColor: COLORS.backgroundCard,
    borderRadius: RADIUS.md,
    padding: SPACING.md,
    color: COLORS.text,
    fontSize: 16,
    minHeight: 80,
    borderWidth: 1,
    borderColor: COLORS.border,
    lineHeight: 22,
  },
  analyzeBtn: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: COLORS.primary,
    marginHorizontal: SPACING.lg,
    padding: SPACING.md,
    borderRadius: RADIUS.lg,
    gap: SPACING.sm,
    ...SHADOWS.medium,
  },
  analyzeBtnDisabled: {
    opacity: 0.7,
  },
  analyzeBtnEmoji: {
    fontSize: 20,
  },
  analyzeBtnText: {
    ...FONTS.bold,
    fontSize: 16,
  },
  resultCard: {
    backgroundColor: COLORS.backgroundCard,
    marginHorizontal: SPACING.lg,
    marginTop: SPACING.lg,
    borderRadius: RADIUS.xl,
    padding: SPACING.lg,
    borderWidth: 1,
    borderColor: COLORS.primaryLight + '30',
    ...SHADOWS.medium,
  },
  resultTitle: {
    ...FONTS.title,
    fontSize: 20,
    marginBottom: 2,
  },
  resultNameHe: {
    ...FONTS.medium,
    color: COLORS.textSecondary,
    marginBottom: SPACING.xs,
    textAlign: 'right',
  },
  resultDesc: {
    ...FONTS.small,
    color: COLORS.textSecondary,
    marginBottom: SPACING.md,
    lineHeight: 20,
  },
  resultStats: {
    flexDirection: 'row',
    justifyContent: 'space-around',
    backgroundColor: COLORS.backgroundInput,
    borderRadius: RADIUS.md,
    padding: SPACING.md,
    marginBottom: SPACING.md,
  },
  resultStat: {
    alignItems: 'center',
  },
  resultStatValue: {
    ...FONTS.bold,
    fontSize: 24,
    color: COLORS.calories,
  },
  resultStatLabel: {
    ...FONTS.tiny,
    marginTop: 2,
  },
  addBtn: {
    backgroundColor: COLORS.success,
    borderRadius: RADIUS.md,
    padding: SPACING.md,
    alignItems: 'center',
  },
  addBtnText: {
    ...FONTS.bold,
    fontSize: 16,
    color: COLORS.text,
  },
});
