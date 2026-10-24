import React, { useEffect, useRef, useState } from 'react';
import { Eye, EyeOff } from 'lucide-react';
import { Api, tokenStore } from '../api';
import { navigate } from '../lib/router';
import { Button, Field, Notice, cx, inputClass } from './ui';

const RESEND_DELAY_SECONDS = 30;

/**
 * Connexion et inscription, suivies de la saisie du code reçu (double authentification).
 * Après inscription, on enchaîne directement sur l'envoi du code : pas de second formulaire.
 */
export default function AuthScreen({ query, onAuthenticated }) {
  const [mode, setMode] = useState(query.mode === 'inscription' ? 'register' : 'login');
  const [step, setStep] = useState('form'); // form | code
  const [credentials, setCredentials] = useState(null); // gardés en mémoire pour « Renvoyer le code »
  const [info, setInfo] = useState(null);

  const goToCode = (email, password, message) => {
    setCredentials({ email, password });
    setInfo(message);
    setStep('code');
  };

  const finish = async (token) => {
    tokenStore.set(token);
    const me = await onAuthenticated();
    // Un praticien sans fiche est guidé vers sa création, sinon retour là où il était.
    if (me?.role === 'PROVIDER' && !me.providerId) navigate('/profil-praticien', { replace: true });
    else navigate(query.retour || '/', { replace: true });
  };

  return (
    <div className="mx-auto max-w-md space-y-6 pt-2">
      {step === 'form' ? (
        <>
          <h1 id="page-title" tabIndex={-1} className="text-4xl font-semibold outline-none">
            {mode === 'login' ? 'Se connecter' : 'Créer un compte'}
          </h1>
          <div className="grid grid-cols-2 rounded-full bg-white p-1" role="group" aria-label="Choix du formulaire">
            {[
              ['login', "J'ai déjà un compte"],
              ['register', 'Je suis nouveau'],
            ].map(([value, label]) => (
              <button
                key={value}
                type="button"
                aria-pressed={mode === value}
                onClick={() => setMode(value)}
                className={cx(
                  'rounded-full py-2 font-bold transition-colors',
                  mode === value ? 'bg-nuit text-white' : 'text-nuit-doux hover:text-nuit'
                )}
              >
                {label}
              </button>
            ))}
          </div>
          {mode === 'login' ? (
            <LoginForm onCodeSent={goToCode} />
          ) : (
            <RegisterForm onCodeSent={goToCode} />
          )}
        </>
      ) : (
        <CodeStep
          credentials={credentials}
          info={info}
          onBack={() => {
            setStep('form');
            setInfo(null);
          }}
          onVerified={finish}
        />
      )}
    </div>
  );
}

function PasswordInput({ value, onChange, autoComplete, ...props }) {
  const [visible, setVisible] = useState(false);
  return (
    <div className="relative">
      <input
        {...props}
        type={visible ? 'text' : 'password'}
        autoComplete={autoComplete}
        value={value}
        onChange={onChange}
        className={cx(inputClass, 'pr-12')}
      />
      <button
        type="button"
        onClick={() => setVisible((v) => !v)}
        aria-pressed={visible}
        aria-label={visible ? 'Masquer le mot de passe' : 'Afficher le mot de passe'}
        className="absolute inset-y-0 right-0 flex w-12 items-center justify-center rounded-r-lg text-nuit-doux hover:text-nuit"
      >
        {visible ? <EyeOff className="h-5 w-5" aria-hidden="true" /> : <Eye className="h-5 w-5" aria-hidden="true" />}
      </button>
    </div>
  );
}

function LoginForm({ onCodeSent }) {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [fields, setFields] = useState({});

  const submit = async (event) => {
    event.preventDefault();
    setLoading(true);
    setError(null);
    setFields({});
    try {
      await Api.login({ email: email.trim(), password });
      onCodeSent(email.trim(), password, null);
    } catch (e) {
      setError(e.fields ? null : e.message);
      setFields(e.fields || {});
      setLoading(false);
    }
  };

  return (
    <form onSubmit={submit} className="space-y-5 rounded-3xl bg-white p-5 sm:p-6" noValidate>
      {error && <Notice tone="error">{error}</Notice>}
      <Field label="Email" error={fields.email}>
        {(props) => (
          <input {...props} type="email" autoComplete="email" required value={email} onChange={(e) => setEmail(e.target.value)} className={inputClass} />
        )}
      </Field>
      <Field label="Mot de passe" error={fields.password}>
        {(props) => (
          <PasswordInput {...props} autoComplete="current-password" required value={password} onChange={(e) => setPassword(e.target.value)} />
        )}
      </Field>
      <Button type="submit" size="lg" className="w-full" loading={loading}>
        Recevoir mon code de connexion
      </Button>
    </form>
  );
}

function RegisterForm({ onCodeSent }) {
  const [form, setForm] = useState({ name: '', email: '', phone: '', password: '', role: 'CLIENT' });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [fields, setFields] = useState({});
  const update = (key) => (event) => setForm((f) => ({ ...f, [key]: event.target.value }));

  const submit = async (event) => {
    event.preventDefault();
    setLoading(true);
    setError(null);
    setFields({});
    try {
      const payload = { ...form, email: form.email.trim(), name: form.name.trim(), phone: form.phone.trim() };
      await Api.register(payload);
      await Api.login({ email: payload.email, password: payload.password });
      onCodeSent(payload.email, payload.password, 'Compte créé. Saisissez le code reçu pour vous connecter.');
    } catch (e) {
      setError(e.fields ? 'Certains champs sont à corriger.' : e.message);
      setFields(e.fields || {});
      setLoading(false);
    }
  };

  return (
    <form onSubmit={submit} className="space-y-5 rounded-3xl bg-white p-5 sm:p-6" noValidate>
      {error && <Notice tone="error">{error}</Notice>}

      <fieldset className="space-y-2">
        <legend className="font-bold">Vous venez pour</legend>
        <div className="grid gap-2 sm:grid-cols-2">
          {[
            ['CLIENT', 'Recevoir des séances', 'Trouver un praticien et réserver.'],
            ['PROVIDER', 'Proposer des séances', 'Publier votre fiche et recevoir des réservations.'],
          ].map(([value, title, description]) => (
            <label
              key={value}
              className={cx(
                'flex cursor-pointer gap-3 rounded-2xl p-3 ring-1 ring-inset transition-colors',
                form.role === value ? 'bg-lilas-pale ring-nuit' : 'ring-lilas hover:ring-nuit/40'
              )}
            >
              <input type="radio" name="role" value={value} checked={form.role === value} onChange={update('role')} className="mt-1 h-4 w-4 accent-nuit" />
              <span>
                <span className="block font-bold">{title}</span>
                <span className="block text-[0.95rem] text-nuit-doux">{description}</span>
              </span>
            </label>
          ))}
        </div>
        {fields.role && <p className="text-[0.95rem] text-alerte">{fields.role}</p>}
      </fieldset>

      <Field label="Prénom et nom" hint="Seuls votre prénom et l'initiale de votre nom apparaissent dans les avis." error={fields.name}>
        {(props) => <input {...props} autoComplete="name" value={form.name} onChange={update('name')} className={inputClass} />}
      </Field>
      <Field label="Email" error={fields.email}>
        {(props) => <input {...props} type="email" autoComplete="email" value={form.email} onChange={update('email')} className={inputClass} />}
      </Field>
      <Field label="Téléphone" hint="Visible uniquement par la personne avec qui vous avez une séance." error={fields.phone}>
        {(props) => <input {...props} type="tel" autoComplete="tel" value={form.phone} onChange={update('phone')} className={inputClass} />}
      </Field>
      <Field label="Mot de passe" hint="8 caractères minimum." error={fields.password}>
        {(props) => <PasswordInput {...props} autoComplete="new-password" value={form.password} onChange={update('password')} />}
      </Field>

      <Button type="submit" size="lg" className="w-full" loading={loading}>
        Créer mon compte
      </Button>
    </form>
  );
}

/** Saisie du code à 6 chiffres. Un seul champ : collage, remplissage auto par SMS et lecteurs d'écran fonctionnent. */
function CodeStep({ credentials, info, onBack, onVerified }) {
  const [code, setCode] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [resendIn, setResendIn] = useState(RESEND_DELAY_SECONDS);
  const [resent, setResent] = useState(false);
  const inputRef = useRef(null);
  const submittedCode = useRef(null);

  useEffect(() => {
    inputRef.current?.focus();
  }, []);

  useEffect(() => {
    if (resendIn <= 0) return undefined;
    const timer = setTimeout(() => setResendIn((s) => s - 1), 1000);
    return () => clearTimeout(timer);
  }, [resendIn]);

  const verify = async (value) => {
    if (value.length !== 6 || loading) return;
    submittedCode.current = value;
    setLoading(true);
    setError(null);
    try {
      const { token } = await Api.verifyOtp({ email: credentials.email, code: value });
      await onVerified(token);
    } catch (e) {
      setError(e.message);
      setLoading(false);
      setCode('');
      inputRef.current?.focus();
    }
  };

  // Validation automatique dès le 6e chiffre (une seule fois par code saisi).
  useEffect(() => {
    if (code.length === 6 && submittedCode.current !== code) verify(code);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [code]);

  const resend = async () => {
    setError(null);
    setResent(false);
    try {
      await Api.login(credentials);
      submittedCode.current = null;
      setResent(true);
      setResendIn(RESEND_DELAY_SECONDS);
    } catch (e) {
      setError(e.message);
    }
  };

  return (
    <>
      <h1 id="page-title" tabIndex={-1} className="text-4xl font-semibold outline-none">
        Votre code de connexion
      </h1>
      {info && <Notice tone="success">{info}</Notice>}
      <form
        onSubmit={(event) => {
          event.preventDefault();
          verify(code);
        }}
        className="space-y-5 rounded-3xl bg-white p-5 sm:p-6"
        noValidate
      >
        <p>
          Un code à 6 chiffres vient d'être envoyé à <strong className="break-all">{credentials.email}</strong>. Il est
          valable 5 minutes.
        </p>
        {error && <Notice tone="error">{error}</Notice>}
        {resent && <Notice tone="success">Nouveau code envoyé. L'ancien ne fonctionne plus.</Notice>}
        <Field label="Code à 6 chiffres">
          {(props) => (
            <input
              {...props}
              ref={inputRef}
              inputMode="numeric"
              autoComplete="one-time-code"
              maxLength={6}
              value={code}
              onChange={(e) => setCode(e.target.value.replace(/\D/g, '').slice(0, 6))}
              className={cx(inputClass, 'text-center font-display text-3xl tracking-[0.5em] tabular-nums')}
            />
          )}
        </Field>
        <Button type="submit" size="lg" className="w-full" loading={loading} disabled={code.length !== 6}>
          Me connecter
        </Button>
        <div className="flex flex-wrap items-center justify-between gap-3 text-[0.95rem]">
          <Button variant="quiet" onClick={resend} disabled={resendIn > 0}>
            {resendIn > 0 ? `Renvoyer le code (${resendIn} s)` : 'Renvoyer le code'}
          </Button>
          <Button variant="quiet" onClick={onBack}>
            Changer d'email
          </Button>
        </div>
      </form>
      {import.meta.env.DEV && (
        <Notice title="Mode développement">
          Sans envoi d'email configuré, le code s'affiche dans la console du backend (variable MFA_DEV_LOG_OTP=true ou
          profil demo).
        </Notice>
      )}
    </>
  );
}
