import i18n from 'i18next';
import LanguageDetector from 'i18next-browser-languagedetector';
import { initReactI18next } from 'react-i18next';
import { IS_INTERNATIONALIZATION_ENABLED } from 'modules/featureFlags';

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

function initI18next() {
  if (IS_INTERNATIONALIZATION_ENABLED) {
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
  } else {
    i18n
      .use(initReactI18next)
      .init({
        lng: 'en',
        resources: {
          en
        }
      });
  };
};

initI18next();

export default i18n;
