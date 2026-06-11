import { reactive } from 'vue'

export const THEME_KEY = 'clas-theme-mode'
export const LANGUAGE_KEY = 'clas-language'

const fallbackTheme = 'light'
const fallbackLanguage = 'zh-CN'

export const preferenceState = reactive({
  theme: readPreference(THEME_KEY, fallbackTheme),
  language: readPreference(LANGUAGE_KEY, fallbackLanguage)
})

function readPreference(key, fallback) {
  try {
    return localStorage.getItem(key) || fallback
  } catch {
    return fallback
  }
}

function writePreference(key, value) {
  try {
    localStorage.setItem(key, value)
  } catch {
    // Ignore storage failures; in-memory preference still updates.
  }
}

export function applyThemePreference(theme = preferenceState.theme) {
  const normalized = theme === 'dark' ? 'dark' : 'light'
  preferenceState.theme = normalized
  document.documentElement.dataset.theme = normalized
  writePreference(THEME_KEY, normalized)
}

export function setLanguagePreference(language) {
  const normalized = language || fallbackLanguage
  preferenceState.language = normalized
  writePreference(LANGUAGE_KEY, normalized)
}

export function initializePreferences() {
  applyThemePreference(preferenceState.theme)
  setLanguagePreference(preferenceState.language)
}

export function t(labels) {
  if (!labels || typeof labels !== 'object') return ''
  return labels[preferenceState.language] || labels[fallbackLanguage] || Object.values(labels)[0] || ''
}
