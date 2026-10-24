// Mise en forme à la française, centralisée pour que tout l'écran parle la même langue :
// « sam. 31 oct. », « 4,8 », « à 2,4 km », « 14:00 – 15:00 ».

export const DAYS = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY'];

export const DAY_LABELS = {
  MONDAY: 'lundi',
  TUESDAY: 'mardi',
  WEDNESDAY: 'mercredi',
  THURSDAY: 'jeudi',
  FRIDAY: 'vendredi',
  SATURDAY: 'samedi',
  SUNDAY: 'dimanche',
};

export const DAY_INITIALS = {
  MONDAY: 'L',
  TUESDAY: 'M',
  WEDNESDAY: 'M',
  THURSDAY: 'J',
  FRIDAY: 'V',
  SATURDAY: 'S',
  SUNDAY: 'D',
};

/** Statuts de séance tels que l'utilisateur les comprend (et non tels que l'API les nomme). */
export const SESSION_STATUS = {
  PENDING: { label: 'Réservée', className: 'bg-abricot-pale text-nuit' },
  ACCEPTED: { label: 'En cours', className: 'bg-sauge-pale text-sauge' },
  DONE: { label: 'Terminée', className: 'bg-lilas-pale text-nuit' },
  CANCELLED: { label: 'Annulée', className: 'bg-white text-nuit-doux ring-1 ring-inset ring-lilas' },
};

export const capitalize = (text) => (text ? text.charAt(0).toUpperCase() + text.slice(1) : text);

const longDateFormat = new Intl.DateTimeFormat('fr-FR', { weekday: 'long', day: 'numeric', month: 'long' });
const weekdayFormat = new Intl.DateTimeFormat('fr-FR', { weekday: 'short' });
const dayMonthFormat = new Intl.DateTimeFormat('fr-FR', { day: 'numeric', month: 'short' });
const monthFormat = new Intl.DateTimeFormat('fr-FR', { month: 'short' });
const monthYearFormat = new Intl.DateTimeFormat('fr-FR', { month: 'long', year: 'numeric' });

/** « Samedi 31 octobre » */
export const formatLongDate = (date) => capitalize(longDateFormat.format(date));
/** « sam. » */
export const formatWeekday = (date) => weekdayFormat.format(date);
/** « 31 oct. » */
export const formatDayMonth = (date) => dayMonthFormat.format(date);
/** « oct » (sans point, pour les pastilles de date) */
export const formatMonthShort = (date) => monthFormat.format(date).replace('.', '');
/** « septembre 2026 » */
export const formatMonthYear = (date) => monthYearFormat.format(date);

const pad = (value) => String(value).padStart(2, '0');

/** « 14:00 » */
export const formatTime = (date) => `${pad(date.getHours())}:${pad(date.getMinutes())}`;

/** « 14:00 – 16:00 » */
export function formatTimeRange(start, durationHours) {
  const end = new Date(start.getTime() + durationHours * 3600 * 1000);
  return `${formatTime(start)} – ${formatTime(end)}`;
}

/**
 * L'API échange des dates locales sans fuseau ("2026-10-31T14:00:00").
 * new Date() interprète ce format comme une heure LOCALE, ce qui est voulu ici.
 */
export const parseLocalDateTime = (value) => new Date(value);

/** Date -> "2026-10-31" (jour local, pas UTC : toISOString décalerait la date après 23 h). */
export const toLocalDate = (date) => `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;

export const dayOfWeekOf = (date) => DAYS[(date.getDay() + 6) % 7];

export const isSameDay = (a, b) =>
  a.getFullYear() === b.getFullYear() && a.getMonth() === b.getMonth() && a.getDate() === b.getDate();

/** "09:30" -> 570 */
export function toMinutes(hhmm) {
  const [hours, minutes] = hhmm.split(':').map(Number);
  return hours * 60 + minutes;
}

/** 570 -> "09:30" */
export const fromMinutes = (total) => `${pad(Math.floor(total / 60))}:${pad(total % 60)}`;

/** 4.8 -> « 4,8 » */
export const formatRating = (rating) =>
  rating.toLocaleString('fr-FR', { minimumFractionDigits: 1, maximumFractionDigits: 1 });

/** 2.4 -> « à 2,4 km » */
export function formatDistance(km) {
  if (km === null || km === undefined) return null;
  if (km < 1) return "à moins d'1 km";
  return `à ${km.toLocaleString('fr-FR', { maximumFractionDigits: 1 })} km`;
}

/** « Léa Dubois » -> « LD » (avatar sans photo) */
export function initials(name) {
  const parts = (name || '').trim().split(/\s+/).filter(Boolean);
  if (parts.length === 0) return '?';
  const first = parts[0][0];
  const last = parts.length > 1 ? parts[parts.length - 1][0] : '';
  return `${first}${last}`.toUpperCase();
}

/** « Léa Dubois » -> « Léa » */
export const firstName = (name) => (name || '').trim().split(/\s+/)[0] || name;

/** Distance à vol d'oiseau en km (formule de Haversine, comme côté serveur). */
export function distanceKm(from, to) {
  if (!from || !to) return null;
  const rad = (deg) => (deg * Math.PI) / 180;
  const dLat = rad(to.latitude - from.lat);
  const dLng = rad(to.longitude - from.lng);
  const a =
    Math.sin(dLat / 2) ** 2 + Math.cos(rad(from.lat)) * Math.cos(rad(to.latitude)) * Math.sin(dLng / 2) ** 2;
  return 2 * 6371 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
}
