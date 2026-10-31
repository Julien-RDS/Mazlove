import React, { useEffect, useId, useRef, useState } from 'react';
import { AlertCircle, BadgeCheck, CheckCircle2, Info, Loader2, Star, X } from 'lucide-react';
import { DAY_INITIALS, DAY_LABELS, DAYS, SESSION_STATUS, formatRating, initials } from '../lib/format';

// Briques d'interface partagées. Un seul rayon de bordure par niveau de hiérarchie :
// rounded-full pour les actions, rounded-lg pour les champs, rounded-2xl/3xl pour les surfaces.

export const cx = (...classes) => classes.filter(Boolean).join(' ');

const BUTTON_VARIANTS = {
  primary: 'bg-nuit text-sur-nuit hover:bg-nuit-800 disabled:bg-nuit/40',
  secondary: 'bg-carte text-nuit ring-1 ring-inset ring-lilas hover:ring-nuit/50 disabled:text-nuit-doux',
  danger: 'bg-carte text-alerte ring-1 ring-inset ring-alerte/40 hover:bg-alerte-pale',
  // Action secondaire présentée comme un lien (annuler, changer d'email…).
  quiet:
    'text-nuit underline decoration-lilas decoration-2 underline-offset-4 hover:decoration-nuit disabled:text-nuit-doux',
};

const BUTTON_SIZES = { sm: 'h-9 px-4 text-[0.95rem]', md: 'h-11 px-5', lg: 'h-12 px-6 text-[1.05rem]' };

export function Button({
  variant = 'primary',
  size = 'md',
  loading = false,
  disabled = false,
  type = 'button',
  className,
  children,
  ...rest
}) {
  return (
    <button
      type={type}
      disabled={disabled || loading}
      aria-busy={loading || undefined}
      className={cx(
        'inline-flex items-center justify-center gap-2 rounded-full font-bold transition-colors disabled:cursor-not-allowed',
        variant !== 'quiet' && BUTTON_SIZES[size],
        BUTTON_VARIANTS[variant],
        className
      )}
      {...rest}
    >
      {loading && <Loader2 className="h-4 w-4 animate-spin" aria-hidden="true" />}
      {children}
    </button>
  );
}

export const inputClass =
  'block w-full rounded-lg border border-lilas bg-carte px-3.5 py-2.5 text-nuit placeholder:text-nuit-doux ' +
  'focus:border-nuit focus:outline-none aria-[invalid=true]:border-alerte';

/**
 * Champ de formulaire accessible : libellé relié, aide et erreur annoncées par les lecteurs d'écran.
 * children reçoit les props à poser sur le contrôle : {(props) => <input {...props} />}
 */
export function Field({ label, hint, error, optional = false, children }) {
  const id = useId();
  const hintId = `${id}-aide`;
  const errorId = `${id}-erreur`;
  const describedBy = error ? errorId : hint ? hintId : undefined;
  return (
    <div className="space-y-1.5">
      <label htmlFor={id} className="block font-bold">
        {label}
        {optional && <span className="font-normal text-nuit-doux"> (facultatif)</span>}
      </label>
      {children({ id, 'aria-describedby': describedBy, 'aria-invalid': error ? true : undefined })}
      {error ? (
        <p id={errorId} className="flex items-start gap-1.5 text-[0.95rem] text-alerte">
          <AlertCircle className="mt-0.5 h-4 w-4 shrink-0" aria-hidden="true" />
          {error}
        </p>
      ) : (
        hint && (
          <p id={hintId} className="text-[0.95rem] text-nuit-doux">
            {hint}
          </p>
        )
      )}
    </div>
  );
}

const NOTICE_STYLES = {
  error: { box: 'bg-alerte-pale text-nuit', icon: AlertCircle, iconClass: 'text-alerte' },
  success: { box: 'bg-sauge-pale text-nuit', icon: CheckCircle2, iconClass: 'text-sauge' },
  info: { box: 'bg-lilas-pale text-nuit', icon: Info, iconClass: 'text-nuit' },
};

/** Message contextuel. Les erreurs sont annoncées immédiatement (role="alert"). */
export function Notice({ tone = 'info', title, children, action, className }) {
  const style = NOTICE_STYLES[tone];
  const Icon = style.icon;
  return (
    <div role={tone === 'error' ? 'alert' : 'status'} className={cx('flex gap-3 rounded-2xl px-4 py-3', style.box, className)}>
      <Icon className={cx('mt-0.5 h-5 w-5 shrink-0', style.iconClass)} aria-hidden="true" />
      <div className="min-w-0 flex-1 space-y-1">
        {title && <p className="font-bold">{title}</p>}
        {children && <div>{children}</div>}
        {action && <div className="pt-1">{action}</div>}
      </div>
    </div>
  );
}

export function Spinner({ label = 'Chargement' }) {
  return (
    <div role="status" className="flex items-center gap-2 py-10 text-nuit-doux">
      <Loader2 className="h-5 w-5 animate-spin" aria-hidden="true" />
      <span>{label}…</span>
    </div>
  );
}

/** Note en étoiles (lecture seule). Le texte de remplacement donne la valeur exacte. */
export function Stars({ value, className }) {
  const rounded = Math.round(value);
  return (
    <span className={cx('inline-flex items-center gap-0.5', className)}>
      <span className="sr-only">{`Note : ${formatRating(value)} sur 5`}</span>
      {[1, 2, 3, 4, 5].map((n) => (
        <Star
          key={n}
          aria-hidden="true"
          className={cx('h-4 w-4', n <= rounded ? 'fill-rose text-rose' : 'fill-transparent text-lilas')}
        />
      ))}
    </span>
  );
}

export function VerifiedBadge({ compact = false }) {
  if (compact) {
    return (
      <span className="inline-flex text-sauge" title="Identité vérifiée par Mazlove">
        <BadgeCheck className="h-5 w-5" aria-hidden="true" />
        <span className="sr-only">Identité vérifiée</span>
      </span>
    );
  }
  return (
    <span className="inline-flex items-center gap-1.5 rounded-full bg-sauge-pale px-3 py-1 text-[0.95rem] font-bold text-sauge">
      <BadgeCheck className="h-4 w-4" aria-hidden="true" />
      Identité vérifiée
    </span>
  );
}

export function StatusBadge({ status }) {
  const meta = SESSION_STATUS[status] || SESSION_STATUS.PENDING;
  return (
    <span className={cx('inline-flex shrink-0 items-center rounded-full px-3 py-1 text-[0.9rem] font-bold', meta.className)}>
      {meta.label}
    </span>
  );
}

/** Photo, ou initiales si absente ou cassée (le nom est toujours écrit à côté, d'où aria-hidden). */
export function Avatar({ name, src, className }) {
  const [failed, setFailed] = useState(false);
  if (src && !failed) {
    return (
      <img
        src={src}
        alt={`Photo de ${name}`}
        loading="lazy"
        referrerPolicy="no-referrer"
        onError={() => setFailed(true)}
        className={cx('object-cover', className)}
      />
    );
  }
  return (
    <div aria-hidden="true" className={cx('flex items-center justify-center bg-lilas-pale font-display text-nuit', className)}>
      {initials(name)}
    </div>
  );
}

/** Semaine en 7 pastilles : on voit d'un coup d'œil les jours où le praticien reçoit. */
export function WeekStrip({ availability = [] }) {
  const openDays = new Set(availability.map((slot) => slot.dayOfWeek));
  const label = openDays.size
    ? `Reçoit le ${DAYS.filter((d) => openDays.has(d)).map((d) => DAY_LABELS[d]).join(', ')}`
    : 'Aucun créneau publié';
  return (
    <div className="flex gap-1" role="img" aria-label={label}>
      {DAYS.map((day) => (
        <span
          key={day}
          aria-hidden="true"
          className={cx(
            'flex h-6 w-6 items-center justify-center rounded-full text-[0.7rem] font-bold',
            openDays.has(day) ? 'bg-nuit text-sur-nuit' : 'text-nuit-doux ring-1 ring-inset ring-lilas'
          )}
        >
          {DAY_INITIALS[day]}
        </span>
      ))}
    </div>
  );
}

/** Compteur « 120 / 500 » sous les zones de texte limitées. */
export function CharCount({ value, max }) {
  const remaining = max - (value?.length || 0);
  return (
    <p className={cx('text-right text-[0.9rem]', remaining < 0 ? 'text-alerte' : 'text-nuit-doux')} aria-live="polite">
      {value?.length || 0} / {max}
    </p>
  );
}

/**
 * Panneau modal : glisse depuis le bas sur mobile, centré sur grand écran.
 * Échap ferme, le focus reste piégé dedans puis revient au bouton d'origine à la fermeture.
 */
export function Sheet({ title, onClose, children, footer }) {
  const panelRef = useRef(null);
  const titleId = useId();
  // onClose peut changer à chaque rendu du parent : on le garde dans une ref pour ne pas
  // relancer l'effet (qui volerait le focus du champ en cours de saisie).
  const onCloseRef = useRef(onClose);
  onCloseRef.current = onClose;

  useEffect(() => {
    const previouslyFocused = document.activeElement;
    panelRef.current?.focus();
    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = 'hidden';

    const onKeyDown = (event) => {
      if (event.key === 'Escape') {
        onCloseRef.current();
        return;
      }
      if (event.key !== 'Tab' || !panelRef.current) return;
      const focusables = panelRef.current.querySelectorAll(
        'button:not([disabled]), [href], input:not([disabled]), textarea:not([disabled]), select:not([disabled]), [tabindex]:not([tabindex="-1"])'
      );
      if (focusables.length === 0) return;
      const first = focusables[0];
      const last = focusables[focusables.length - 1];
      if (event.shiftKey && (document.activeElement === first || document.activeElement === panelRef.current)) {
        event.preventDefault();
        last.focus();
      } else if (!event.shiftKey && document.activeElement === last) {
        event.preventDefault();
        first.focus();
      }
    };
    document.addEventListener('keydown', onKeyDown);
    return () => {
      document.removeEventListener('keydown', onKeyDown);
      document.body.style.overflow = previousOverflow;
      if (previouslyFocused && typeof previouslyFocused.focus === 'function') previouslyFocused.focus();
    };
  }, []);

  return (
    <div className="fixed inset-0 z-40 flex items-end justify-center bg-nuit/40 sm:items-center sm:p-6" onMouseDown={(e) => e.target === e.currentTarget && onCloseRef.current()}>
      <div
        ref={panelRef}
        role="dialog"
        aria-modal="true"
        aria-labelledby={titleId}
        tabIndex={-1}
        className="flex max-h-[92vh] w-full flex-col rounded-t-3xl bg-carte shadow-2xl outline-none motion-safe:animate-sheet-in sm:max-w-lg sm:rounded-3xl"
      >
        <div className="flex items-start justify-between gap-4 px-6 pb-2 pt-6">
          <h2 id={titleId} className="text-2xl font-semibold">
            {title}
          </h2>
          <button
            type="button"
            onClick={() => onCloseRef.current()}
            className="-mr-2 rounded-full p-2 text-nuit-doux hover:bg-brume hover:text-nuit"
            aria-label="Fermer"
          >
            <X className="h-5 w-5" aria-hidden="true" />
          </button>
        </div>
        <div className="flex-1 overflow-y-auto px-6 pb-6">{children}</div>
        {footer && <div className="border-t border-lilas/70 px-6 py-4">{footer}</div>}
      </div>
    </div>
  );
}
