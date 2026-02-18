import React, { useState } from 'react';
import {
  View,
  Text,
  StyleSheet,
  TouchableOpacity,
  TextInput,
  Alert,
  Modal,
} from 'react-native';
import { COLORS, FONTS, RADIUS, SPACING, SHADOWS, getFoodEmoji } from '../utils/theme';

export default function MealCard({ meal, onUpdate, onDelete }) {
  const [editing, setEditing] = useState(false);
  const [editCalories, setEditCalories] = useState(String(meal.calories || 0));
  const [editProtein, setEditProtein] = useState(String(meal.protein || 0));

  const handleSave = () => {
    onUpdate(meal.id, {
      calories: parseInt(editCalories) || 0,
      protein: parseInt(editProtein) || 0,
    });
    setEditing(false);
  };

  const handleDelete = () => {
    Alert.alert(
      'Delete Meal',
      `Are you sure you want to delete "${meal.name}"?`,
      [
        { text: 'Cancel', style: 'cancel' },
        { text: 'Delete', style: 'destructive', onPress: () => onDelete(meal.id) },
      ]
    );
  };

  const emoji = getFoodEmoji(meal.name);
  const time = meal.timestamp
    ? new Date(meal.timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
    : '';

  return (
    <View style={styles.card}>
      <View style={styles.mainRow}>
        <View style={styles.emojiContainer}>
          <Text style={styles.emoji}>{meal.isWater ? '💧' : emoji}</Text>
        </View>
        <View style={styles.info}>
          <Text style={styles.name} numberOfLines={1}>{meal.name}</Text>
          {meal.description ? (
            <Text style={styles.description} numberOfLines={1}>{meal.description}</Text>
          ) : null}
          <Text style={styles.time}>{time}</Text>
        </View>
        <View style={styles.stats}>
          {!meal.isWater && (
            <>
              <View style={styles.statItem}>
                <Text style={styles.statValue}>{meal.calories}</Text>
                <Text style={styles.statLabel}>kcal</Text>
              </View>
              <View style={styles.statItem}>
                <Text style={[styles.statValue, { color: COLORS.protein }]}>{meal.protein}g</Text>
                <Text style={styles.statLabel}>protein</Text>
              </View>
            </>
          )}
          {meal.isWater && (
            <View style={styles.statItem}>
              <Text style={[styles.statValue, { color: COLORS.water }]}>{meal.waterCups}</Text>
              <Text style={styles.statLabel}>cups</Text>
            </View>
          )}
        </View>
      </View>

      <View style={styles.actions}>
        <TouchableOpacity style={styles.actionBtn} onPress={() => {
          setEditCalories(String(meal.calories || 0));
          setEditProtein(String(meal.protein || 0));
          setEditing(true);
        }}>
          <Text style={styles.actionIcon}>✏️</Text>
          <Text style={styles.actionText}>Edit</Text>
        </TouchableOpacity>
        <TouchableOpacity style={[styles.actionBtn, styles.deleteBtn]} onPress={handleDelete}>
          <Text style={styles.actionIcon}>🗑️</Text>
          <Text style={[styles.actionText, { color: COLORS.danger }]}>Delete</Text>
        </TouchableOpacity>
      </View>

      <Modal visible={editing} transparent animationType="fade">
        <View style={styles.modalOverlay}>
          <View style={styles.modalContent}>
            <Text style={styles.modalTitle}>Edit Nutritional Values</Text>
            <Text style={styles.modalSubtitle}>{meal.name}</Text>

            <View style={styles.inputRow}>
              <Text style={styles.inputLabel}>Calories (kcal)</Text>
              <TextInput
                style={styles.input}
                value={editCalories}
                onChangeText={setEditCalories}
                keyboardType="numeric"
                placeholderTextColor={COLORS.textMuted}
              />
            </View>

            <View style={styles.inputRow}>
              <Text style={styles.inputLabel}>Protein (g)</Text>
              <TextInput
                style={styles.input}
                value={editProtein}
                onChangeText={setEditProtein}
                keyboardType="numeric"
                placeholderTextColor={COLORS.textMuted}
              />
            </View>

            <View style={styles.modalActions}>
              <TouchableOpacity
                style={[styles.modalBtn, styles.cancelBtn]}
                onPress={() => setEditing(false)}>
                <Text style={styles.cancelBtnText}>Cancel</Text>
              </TouchableOpacity>
              <TouchableOpacity style={[styles.modalBtn, styles.saveBtn]} onPress={handleSave}>
                <Text style={styles.saveBtnText}>Save</Text>
              </TouchableOpacity>
            </View>
          </View>
        </View>
      </Modal>
    </View>
  );
}

const styles = StyleSheet.create({
  card: {
    backgroundColor: COLORS.backgroundCard,
    borderRadius: RADIUS.lg,
    marginHorizontal: SPACING.md,
    marginBottom: SPACING.sm,
    padding: SPACING.md,
    ...SHADOWS.small,
  },
  mainRow: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  emojiContainer: {
    width: 48,
    height: 48,
    borderRadius: RADIUS.md,
    backgroundColor: COLORS.backgroundLight,
    alignItems: 'center',
    justifyContent: 'center',
    marginRight: SPACING.sm,
  },
  emoji: {
    fontSize: 24,
  },
  info: {
    flex: 1,
    marginRight: SPACING.sm,
  },
  name: {
    ...FONTS.medium,
    marginBottom: 2,
  },
  description: {
    ...FONTS.small,
    color: COLORS.textSecondary,
  },
  time: {
    ...FONTS.tiny,
    marginTop: 2,
  },
  stats: {
    flexDirection: 'row',
    gap: SPACING.md,
  },
  statItem: {
    alignItems: 'center',
  },
  statValue: {
    ...FONTS.bold,
    fontSize: 16,
    color: COLORS.calories,
  },
  statLabel: {
    ...FONTS.tiny,
    fontSize: 9,
  },
  actions: {
    flexDirection: 'row',
    marginTop: SPACING.sm,
    paddingTop: SPACING.sm,
    borderTopWidth: 1,
    borderTopColor: COLORS.border,
    gap: SPACING.sm,
  },
  actionBtn: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingVertical: SPACING.xs,
    paddingHorizontal: SPACING.sm,
    borderRadius: RADIUS.sm,
    backgroundColor: COLORS.backgroundInput,
    gap: 4,
  },
  deleteBtn: {
    backgroundColor: 'rgba(225, 112, 85, 0.15)',
  },
  actionIcon: {
    fontSize: 14,
  },
  actionText: {
    ...FONTS.small,
    color: COLORS.textSecondary,
  },
  modalOverlay: {
    flex: 1,
    backgroundColor: 'rgba(0,0,0,0.7)',
    justifyContent: 'center',
    alignItems: 'center',
    padding: SPACING.lg,
  },
  modalContent: {
    backgroundColor: COLORS.backgroundCard,
    borderRadius: RADIUS.xl,
    padding: SPACING.lg,
    width: '100%',
    maxWidth: 350,
  },
  modalTitle: {
    ...FONTS.title,
    fontSize: 20,
    marginBottom: SPACING.xs,
  },
  modalSubtitle: {
    ...FONTS.small,
    marginBottom: SPACING.lg,
    color: COLORS.primaryLight,
  },
  inputRow: {
    marginBottom: SPACING.md,
  },
  inputLabel: {
    ...FONTS.small,
    marginBottom: SPACING.xs,
    color: COLORS.textSecondary,
  },
  input: {
    backgroundColor: COLORS.backgroundInput,
    borderRadius: RADIUS.sm,
    padding: SPACING.md,
    color: COLORS.text,
    fontSize: 18,
    fontWeight: '600',
    borderWidth: 1,
    borderColor: COLORS.border,
  },
  modalActions: {
    flexDirection: 'row',
    gap: SPACING.sm,
    marginTop: SPACING.md,
  },
  modalBtn: {
    flex: 1,
    paddingVertical: SPACING.md,
    borderRadius: RADIUS.md,
    alignItems: 'center',
  },
  cancelBtn: {
    backgroundColor: COLORS.backgroundInput,
  },
  cancelBtnText: {
    ...FONTS.medium,
    color: COLORS.textSecondary,
  },
  saveBtn: {
    backgroundColor: COLORS.primary,
  },
  saveBtnText: {
    ...FONTS.medium,
    color: COLORS.text,
  },
});
