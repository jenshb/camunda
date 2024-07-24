import i18n from 'i18next';
import LanguageDetector from 'i18next-browser-languagedetector';
import { initReactI18next } from 'react-i18next';

import en from './locales/en.json';
import fr from './locales/fr.json';
import de from './locales/de.json';
import es from './locales/es.json';

const resources = {
  es,
  en,
  fr,
  de,
};

const detection = {
  order: ['localStorage', 'navigator'],
  lookupLocalStorage: 'language',
  caches: ['localStorage'],
  htmlTag: document.documentElement,
  checkWhitelist: true,
};

i18n
  .use(LanguageDetector)
  .use(initReactI18next)
  .init({
    // debug: true,
    detection,
    fallbackLng: 'en',
    resources,
    interpolation: {
      escapeValue: false // not needed for react as it escapes by default
    },
  });

export default i18n;
