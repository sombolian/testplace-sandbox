import { getSettings } from './storage';

async function callGemini(model, contents) {
  const settings = await getSettings();
  if (!settings.geminiApiKey) {
    throw new Error('Please set your Gemini API key in Settings');
  }

  const modelName = model || 'gemini-2.0-flash';
  const url = `https://generativelanguage.googleapis.com/v1beta/models/${modelName}:generateContent?key=${settings.geminiApiKey}`;

  const response = await fetch(url, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ contents }),
  });

  if (!response.ok) {
    const errorText = await response.text();
    throw new Error(`API error (${response.status}): ${errorText}`);
  }

  const data = await response.json();
  if (data.candidates && data.candidates[0]?.content?.parts?.[0]?.text) {
    return data.candidates[0].content.parts[0].text;
  }
  throw new Error('No response from AI');
}

export async function analyzeMeal(description, imageBase64 = null) {
  const settings = await getSettings();
  const parts = [];

  parts.push({
    text: `You are a nutrition analysis expert. Analyze the following food/meal and provide estimated nutritional information.

IMPORTANT RULES:
- If the food sounds homemade (like "chicken", "rice", "salad"), assume it's a homemade dish with typical portions.
- Provide realistic estimates for calories and protein.
- If it's water (e.g., "cup of water", "glass of water"), set calories and protein to 0 and set isWater to true and waterCups to the number of cups.
- Respond ONLY with valid JSON, no markdown, no code blocks.

Response format (JSON only):
{
  "name": "Food name in English",
  "nameHe": "Food name in Hebrew if applicable",
  "calories": number,
  "protein": number (grams),
  "description": "Brief description of the meal",
  "isWater": boolean,
  "waterCups": number (0 if not water),
  "imagePrompt": "A detailed prompt to generate an image of this food. If it's a product you'd buy at a supermarket in Israel, describe it as it would look on Israeli supermarket shelves (canned tuna should be in a can, not a jar, etc). If it's homemade food, describe it as a homemade dish on a plate."
}

Food to analyze: ${description}`,
  });

  if (imageBase64) {
    parts.push({
      inlineData: {
        mimeType: 'image/jpeg',
        data: imageBase64,
      },
    });
  }

  const result = await callGemini(settings.analyzeModel, [{ parts }]);

  try {
    const cleaned = result.replace(/```json\n?/g, '').replace(/```\n?/g, '').trim();
    return JSON.parse(cleaned);
  } catch (e) {
    const jsonMatch = result.match(/\{[\s\S]*\}/);
    if (jsonMatch) {
      return JSON.parse(jsonMatch[0]);
    }
    throw new Error('Could not parse AI response');
  }
}

export async function chatWithCoach(messages, imageBase64 = null) {
  const settings = await getSettings();

  const systemPrompt = `You are a friendly and knowledgeable nutrition coach AI. You help users reach their fitness and nutrition goals.

User's profile:
- Daily calorie goal: ${settings.calorieGoal} kcal
- Daily protein goal: ${settings.proteinGoal}g
- Daily water goal: ${settings.waterGoal} cups
- Overall fitness goal: ${settings.overallGoal || 'Not set'}
- Gym frequency: ${settings.gymFrequency || 'Not set'}

Guidelines:
- Be encouraging but honest
- Give specific, actionable advice
- When asked "what should I eat" or "what can I eat to reach my goal", calculate remaining calories/protein and suggest specific meals
- When asked about a specific food, give nutritional estimates
- Use markdown formatting for better readability (bold, lists, headers, etc.)
- You can respond in the user's language (Hebrew or English)
- Be concise but thorough`;

  const contents = [];

  // Add conversation history
  messages.forEach((msg) => {
    const parts = [];
    if (msg.text) {
      parts.push({ text: msg.role === 'user' && contents.length === 0 ? `${systemPrompt}\n\n${msg.text}` : msg.text });
    }
    if (msg.image) {
      parts.push({
        inlineData: {
          mimeType: 'image/jpeg',
          data: msg.image,
        },
      });
    }
    contents.push({
      role: msg.role === 'user' ? 'user' : 'model',
      parts,
    });
  });

  // If there's an image with the latest message
  if (imageBase64 && contents.length > 0) {
    const lastContent = contents[contents.length - 1];
    lastContent.parts.push({
      inlineData: {
        mimeType: 'image/jpeg',
        data: imageBase64,
      },
    });
  }

  return callGemini(settings.coachModel, contents);
}

export async function generateFoodImage(prompt) {
  // We'll use a placeholder approach since Gemini doesn't generate images
  // Instead we return a themed emoji/icon based on the food
  return null;
}
