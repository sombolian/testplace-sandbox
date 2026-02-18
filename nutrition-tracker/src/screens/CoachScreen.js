import React, { useState, useRef, useEffect } from 'react';
import {
  View,
  Text,
  StyleSheet,
  TextInput,
  TouchableOpacity,
  FlatList,
  ActivityIndicator,
  Alert,
  KeyboardAvoidingView,
  Platform,
  Image,
} from 'react-native';
import * as ImagePicker from 'expo-image-picker';
import Markdown from 'react-native-markdown-display';
import { COLORS, FONTS, RADIUS, SPACING, SHADOWS } from '../utils/theme';
import { chatWithCoach } from '../utils/ai';
import { getMeals, getWater, getSettings, getEffectiveDate } from '../utils/storage';

export default function CoachScreen() {
  const [messages, setMessages] = useState([]);
  const [input, setInput] = useState('');
  const [loading, setLoading] = useState(false);
  const [imageBase64, setImageBase64] = useState(null);
  const [imageUri, setImageUri] = useState(null);
  const flatListRef = useRef(null);

  useEffect(() => {
    // Welcome message
    setMessages([
      {
        id: '0',
        role: 'assistant',
        text: "Hey! 👋 I'm your nutrition coach. Ask me anything about your diet, what you should eat, or send me a photo of food for advice!\n\nSome things I can help with:\n- **What should I eat** to reach my goals?\n- **How much protein** is in chicken breast?\n- **Should I eat** this pizza?\n- **What's a good snack** before the gym?",
        timestamp: new Date().toISOString(),
      },
    ]);
  }, []);

  const pickImage = async () => {
    const permResult = await ImagePicker.requestMediaLibraryPermissionsAsync();
    if (!permResult.granted) {
      Alert.alert('Permission needed', 'Please allow access to your photo library');
      return;
    }

    const result = await ImagePicker.launchImageLibraryAsync({
      mediaTypes: ['images'],
      allowsEditing: true,
      quality: 0.7,
      base64: true,
    });

    if (!result.canceled && result.assets[0]) {
      setImageUri(result.assets[0].uri);
      setImageBase64(result.assets[0].base64);
    }
  };

  const takePhoto = async () => {
    const permResult = await ImagePicker.requestCameraPermissionsAsync();
    if (!permResult.granted) {
      Alert.alert('Permission needed', 'Please allow access to your camera');
      return;
    }

    const result = await ImagePicker.launchCameraAsync({
      allowsEditing: true,
      quality: 0.7,
      base64: true,
    });

    if (!result.canceled && result.assets[0]) {
      setImageUri(result.assets[0].uri);
      setImageBase64(result.assets[0].base64);
    }
  };

  const showImageOptions = () => {
    Alert.alert('Add Photo', 'Choose an option', [
      { text: 'Take Photo', onPress: takePhoto },
      { text: 'Choose from Gallery', onPress: pickImage },
      { text: 'Cancel', style: 'cancel' },
    ]);
  };

  const handleSend = async () => {
    if (!input.trim() && !imageBase64) return;

    const today = getEffectiveDate();
    const [meals, waterCount, settings] = await Promise.all([
      getMeals(today),
      getWater(today),
      getSettings(),
    ]);

    const totalCal = meals.reduce((sum, m) => sum + (m.calories || 0), 0);
    const totalPro = meals.reduce((sum, m) => sum + (m.protein || 0), 0);

    const contextText = `[Context: Today I've eaten ${totalCal}/${settings.calorieGoal} calories, ${totalPro}/${settings.proteinGoal}g protein, ${waterCount}/${settings.waterGoal} cups of water. Meals so far: ${meals.map((m) => m.name).join(', ') || 'none'}]`;

    const userMessage = {
      id: Date.now().toString(),
      role: 'user',
      text: `${contextText}\n\n${input.trim()}`,
      displayText: input.trim(),
      imageUri: imageUri,
      timestamp: new Date().toISOString(),
    };

    const newMessages = [...messages, userMessage];
    setMessages(newMessages);
    setInput('');
    setLoading(true);

    const currentImageBase64 = imageBase64;
    setImageBase64(null);
    setImageUri(null);

    try {
      const chatHistory = newMessages
        .filter((m) => m.id !== '0')
        .map((m) => ({
          role: m.role === 'assistant' ? 'model' : 'user',
          text: m.text,
          image: m.imageBase64 || undefined,
        }));

      const response = await chatWithCoach(chatHistory, currentImageBase64);

      const assistantMessage = {
        id: (Date.now() + 1).toString(),
        role: 'assistant',
        text: response,
        timestamp: new Date().toISOString(),
      };

      setMessages((prev) => [...prev, assistantMessage]);
    } catch (e) {
      Alert.alert('Error', e.message || 'Failed to get response. Check your API key in Settings.');
    }
    setLoading(false);
  };

  const markdownStyles = {
    body: {
      color: COLORS.text,
      fontSize: 15,
      lineHeight: 22,
    },
    heading1: {
      color: COLORS.text,
      fontSize: 20,
      fontWeight: '700',
      marginBottom: 8,
      marginTop: 8,
    },
    heading2: {
      color: COLORS.text,
      fontSize: 18,
      fontWeight: '700',
      marginBottom: 6,
      marginTop: 8,
    },
    heading3: {
      color: COLORS.text,
      fontSize: 16,
      fontWeight: '600',
      marginBottom: 4,
      marginTop: 6,
    },
    strong: {
      color: COLORS.primaryLight,
      fontWeight: '700',
    },
    em: {
      color: COLORS.textSecondary,
      fontStyle: 'italic',
    },
    bullet_list: {
      marginBottom: 8,
    },
    ordered_list: {
      marginBottom: 8,
    },
    list_item: {
      marginBottom: 4,
      flexDirection: 'row',
    },
    bullet_list_icon: {
      color: COLORS.primaryLight,
      fontSize: 14,
      marginRight: 8,
    },
    code_inline: {
      backgroundColor: COLORS.backgroundInput,
      color: COLORS.secondary,
      paddingHorizontal: 4,
      borderRadius: 4,
      fontSize: 14,
    },
    fence: {
      backgroundColor: COLORS.backgroundInput,
      borderRadius: 8,
      padding: 12,
      marginVertical: 8,
    },
    code_block: {
      color: COLORS.secondary,
      fontSize: 13,
    },
    blockquote: {
      borderLeftColor: COLORS.primary,
      borderLeftWidth: 3,
      paddingLeft: 12,
      marginVertical: 8,
      backgroundColor: COLORS.backgroundInput,
      borderRadius: 4,
      padding: 8,
    },
    hr: {
      backgroundColor: COLORS.border,
      height: 1,
      marginVertical: 12,
    },
    table: {
      borderColor: COLORS.border,
    },
    th: {
      backgroundColor: COLORS.backgroundInput,
      padding: 8,
      borderColor: COLORS.border,
    },
    td: {
      padding: 8,
      borderColor: COLORS.border,
    },
    link: {
      color: COLORS.secondary,
    },
  };

  const renderMessage = ({ item }) => {
    const isUser = item.role === 'user';
    return (
      <View style={[styles.messageBubble, isUser ? styles.userBubble : styles.assistantBubble]}>
        {!isUser && <Text style={styles.coachLabel}>🤖 Coach</Text>}
        {item.imageUri && (
          <Image source={{ uri: item.imageUri }} style={styles.messageImage} />
        )}
        {isUser ? (
          <Text style={styles.userText}>{item.displayText || item.text}</Text>
        ) : (
          <Markdown style={markdownStyles}>{item.text}</Markdown>
        )}
        <Text style={styles.messageTime}>
          {new Date(item.timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
        </Text>
      </View>
    );
  };

  return (
    <KeyboardAvoidingView
      style={styles.container}
      behavior={Platform.OS === 'ios' ? 'padding' : undefined}
      keyboardVerticalOffset={90}>
      {/* Header */}
      <View style={styles.header}>
        <Text style={styles.headerTitle}>🤖 Nutrition Coach</Text>
        <Text style={styles.headerSubtitle}>AI-powered guidance</Text>
      </View>

      {/* Messages */}
      <FlatList
        ref={flatListRef}
        data={messages}
        renderItem={renderMessage}
        keyExtractor={(item) => item.id}
        style={styles.messageList}
        contentContainerStyle={styles.messageListContent}
        onContentSizeChange={() => flatListRef.current?.scrollToEnd({ animated: true })}
        onLayout={() => flatListRef.current?.scrollToEnd({ animated: true })}
      />

      {loading && (
        <View style={styles.typingIndicator}>
          <ActivityIndicator size="small" color={COLORS.primary} />
          <Text style={styles.typingText}>Coach is thinking...</Text>
        </View>
      )}

      {/* Image preview */}
      {imageUri && (
        <View style={styles.imagePreviewBar}>
          <Image source={{ uri: imageUri }} style={styles.previewThumb} />
          <Text style={styles.previewText}>Photo attached</Text>
          <TouchableOpacity
            onPress={() => {
              setImageUri(null);
              setImageBase64(null);
            }}>
            <Text style={styles.removePreview}>✕</Text>
          </TouchableOpacity>
        </View>
      )}

      {/* Input */}
      <View style={styles.inputBar}>
        <TouchableOpacity style={styles.imageInputBtn} onPress={showImageOptions}>
          <Text style={styles.imageInputIcon}>📷</Text>
        </TouchableOpacity>
        <TextInput
          style={styles.input}
          placeholder="Ask your coach..."
          placeholderTextColor={COLORS.textMuted}
          value={input}
          onChangeText={setInput}
          multiline
          maxLength={2000}
        />
        <TouchableOpacity
          style={[styles.sendBtn, (!input.trim() && !imageBase64) && styles.sendBtnDisabled]}
          onPress={handleSend}
          disabled={(!input.trim() && !imageBase64) || loading}>
          <Text style={styles.sendBtnText}>↑</Text>
        </TouchableOpacity>
      </View>
    </KeyboardAvoidingView>
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
    borderBottomWidth: 1,
    borderBottomColor: COLORS.border,
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
  messageList: {
    flex: 1,
  },
  messageListContent: {
    padding: SPACING.md,
    paddingBottom: SPACING.md,
  },
  messageBubble: {
    maxWidth: '85%',
    marginBottom: SPACING.md,
    borderRadius: RADIUS.lg,
    padding: SPACING.md,
  },
  userBubble: {
    alignSelf: 'flex-end',
    backgroundColor: COLORS.primary,
    borderBottomRightRadius: 4,
  },
  assistantBubble: {
    alignSelf: 'flex-start',
    backgroundColor: COLORS.backgroundCard,
    borderBottomLeftRadius: 4,
    borderWidth: 1,
    borderColor: COLORS.border,
  },
  coachLabel: {
    ...FONTS.tiny,
    color: COLORS.primaryLight,
    marginBottom: 4,
    fontWeight: '600',
  },
  userText: {
    ...FONTS.regular,
    color: COLORS.text,
    lineHeight: 20,
  },
  messageImage: {
    width: 200,
    height: 150,
    borderRadius: RADIUS.sm,
    marginBottom: SPACING.sm,
  },
  messageTime: {
    ...FONTS.tiny,
    color: COLORS.textMuted,
    marginTop: 4,
    textAlign: 'right',
    fontSize: 9,
  },
  typingIndicator: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: SPACING.lg,
    paddingVertical: SPACING.sm,
    gap: SPACING.sm,
  },
  typingText: {
    ...FONTS.small,
    color: COLORS.textMuted,
    fontStyle: 'italic',
  },
  imagePreviewBar: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: SPACING.md,
    paddingVertical: SPACING.sm,
    backgroundColor: COLORS.backgroundCard,
    borderTopWidth: 1,
    borderTopColor: COLORS.border,
    gap: SPACING.sm,
  },
  previewThumb: {
    width: 40,
    height: 40,
    borderRadius: RADIUS.sm,
  },
  previewText: {
    ...FONTS.small,
    color: COLORS.textSecondary,
    flex: 1,
  },
  removePreview: {
    fontSize: 18,
    color: COLORS.danger,
    padding: SPACING.sm,
  },
  inputBar: {
    flexDirection: 'row',
    alignItems: 'flex-end',
    paddingHorizontal: SPACING.md,
    paddingVertical: SPACING.sm,
    paddingBottom: SPACING.lg,
    backgroundColor: COLORS.backgroundLight,
    borderTopWidth: 1,
    borderTopColor: COLORS.border,
    gap: SPACING.sm,
  },
  imageInputBtn: {
    width: 40,
    height: 40,
    borderRadius: 20,
    backgroundColor: COLORS.backgroundCard,
    alignItems: 'center',
    justifyContent: 'center',
    borderWidth: 1,
    borderColor: COLORS.border,
  },
  imageInputIcon: {
    fontSize: 18,
  },
  input: {
    flex: 1,
    backgroundColor: COLORS.backgroundCard,
    borderRadius: RADIUS.lg,
    paddingHorizontal: SPACING.md,
    paddingVertical: SPACING.sm,
    color: COLORS.text,
    fontSize: 15,
    maxHeight: 100,
    borderWidth: 1,
    borderColor: COLORS.border,
  },
  sendBtn: {
    width: 40,
    height: 40,
    borderRadius: 20,
    backgroundColor: COLORS.primary,
    alignItems: 'center',
    justifyContent: 'center',
  },
  sendBtnDisabled: {
    backgroundColor: COLORS.backgroundInput,
  },
  sendBtnText: {
    fontSize: 18,
    color: COLORS.text,
    fontWeight: '700',
  },
});
