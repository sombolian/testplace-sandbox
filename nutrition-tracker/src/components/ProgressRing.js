import React from 'react';
import { View, Text, StyleSheet } from 'react-native';
import Svg, { Circle } from 'react-native-svg';
import { COLORS, FONTS } from '../utils/theme';

export default function ProgressRing({
  progress,
  size = 120,
  strokeWidth = 10,
  color = COLORS.primary,
  bgColor = COLORS.border,
  label,
  value,
  unit,
  goal,
  showCheck = false,
}) {
  const radius = (size - strokeWidth) / 2;
  const circumference = radius * 2 * Math.PI;
  const clampedProgress = Math.min(progress, 1.5);
  const strokeDashoffset = circumference - clampedProgress * circumference;

  return (
    <View style={[styles.container, { width: size, height: size }]}>
      <Svg width={size} height={size}>
        <Circle
          cx={size / 2}
          cy={size / 2}
          r={radius}
          stroke={bgColor}
          strokeWidth={strokeWidth}
          fill="transparent"
        />
        <Circle
          cx={size / 2}
          cy={size / 2}
          r={radius}
          stroke={progress > 1.2 ? COLORS.danger : color}
          strokeWidth={strokeWidth}
          fill="transparent"
          strokeDasharray={circumference}
          strokeDashoffset={strokeDashoffset}
          strokeLinecap="round"
          rotation="-90"
          origin={`${size / 2}, ${size / 2}`}
        />
      </Svg>
      <View style={styles.labelContainer}>
        {showCheck ? (
          <Text style={styles.checkmark}>✓</Text>
        ) : (
          <>
            <Text style={[styles.value, { fontSize: size * 0.18 }]}>{value}</Text>
            {unit && <Text style={[styles.unit, { fontSize: size * 0.1 }]}>{unit}</Text>}
          </>
        )}
        {label && <Text style={[styles.label, { fontSize: size * 0.09 }]}>{label}</Text>}
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    alignItems: 'center',
    justifyContent: 'center',
  },
  labelContainer: {
    position: 'absolute',
    alignItems: 'center',
    justifyContent: 'center',
  },
  value: {
    ...FONTS.bold,
    color: COLORS.text,
  },
  unit: {
    color: COLORS.textSecondary,
    marginTop: -2,
  },
  label: {
    color: COLORS.textMuted,
    marginTop: 2,
    textTransform: 'uppercase',
    letterSpacing: 0.5,
  },
  checkmark: {
    fontSize: 32,
    color: COLORS.success,
    fontWeight: '700',
  },
});
