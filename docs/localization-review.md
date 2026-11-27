# Localization review note

English (`res/values`) and Russian (`res/values-ru`) are the authoritative copy sources.

T46 machine-translated only the safety-critical call, SOS, PIN and kiosk keys into the other existing `values-*` locales so these flows no longer fall back to English. Those machine translations are **not native-reviewed** and must be reviewed before release. All other long-tail copy intentionally remains in English fallback until human translation; do not treat this file as blanket permission to machine-translate the rest.
