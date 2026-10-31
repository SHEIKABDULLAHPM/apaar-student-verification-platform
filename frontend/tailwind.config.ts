import type { Config } from 'tailwindcss';

/**
 * Design tokens for the APAAR platform.
 *
 * Brand and semantic colours are declared here so every screen uses the same
 * palette. Surfaces/ink resolve through CSS variables defined in index.css,
 * which makes light-theme overrides and focus treatment centralised.
 */
export default {
  content: ['./index.html', './src/**/*.{ts,tsx}'],
  theme: {
    extend: {
      colors: {
        brand: {
          50: '#eff5fb',
          100: '#dbe8f5',
          200: '#bcd4ec',
          300: '#91b7dd',
          400: '#5f93c9',
          500: '#3d75b0',
          600: '#2c5d94',
          700: '#254d78',
          800: '#223f61',
          900: '#1f3550',
          950: '#152338',
        },
        accent: {
          50: '#fff8eb',
          100: '#feecc7',
          200: '#fdd78a',
          300: '#fbbd4d',
          400: '#f9a21f',
          500: '#e3820a',
          600: '#c05f05',
          700: '#9a4308',
          800: '#7f390e',
          900: '#6b300f',
        },
        surface: {
          DEFAULT: 'rgb(var(--surface-page) / <alpha-value>)',
          card: 'rgb(var(--surface-card) / <alpha-value>)',
          muted: 'rgb(var(--surface-muted) / <alpha-value>)',
        },
        line: 'rgb(var(--line-subtle) / <alpha-value>)',
        ink: {
          900: 'rgb(var(--ink-strong) / <alpha-value>)',
          800: 'rgb(var(--ink-800) / <alpha-value>)',
          700: 'rgb(var(--ink-700) / <alpha-value>)',
          600: 'rgb(var(--ink-muted) / <alpha-value>)',
          500: 'rgb(var(--ink-soft) / <alpha-value>)',
          400: 'rgb(var(--ink-faint) / <alpha-value>)',
        },
        success: {
          DEFAULT: '#15803d',
          soft: '#dcfce7',
        },
        warning: {
          DEFAULT: '#b45309',
          soft: '#fef3c7',
        },
        danger: {
          DEFAULT: '#b91c1c',
          soft: '#fee2e2',
        },
        info: {
          DEFAULT: '#0369a1',
          soft: '#e0f2fe',
        },
      },
      fontFamily: {
        sans: [
          'Inter',
          'ui-sans-serif',
          'system-ui',
          '-apple-system',
          'Segoe UI',
          'Roboto',
          'Helvetica Neue',
          'Arial',
          'sans-serif',
        ],
        mono: ['ui-monospace', 'SFMono-Regular', 'Menlo', 'Consolas', 'monospace'],
      },
      boxShadow: {
        card: '0 1px 2px 0 rgb(15 27 45 / 0.04), 0 1px 3px 0 rgb(15 27 45 / 0.08)',
        raised: '0 10px 30px -12px rgb(15 27 45 / 0.28)',
      },
      keyframes: {
        'fade-in': {
          from: { opacity: '0' },
          to: { opacity: '1' },
        },
        'slide-in': {
          from: { transform: 'translateX(-100%)' },
          to: { transform: 'translateX(0)' },
        },
      },
      animation: {
        'fade-in': 'fade-in 150ms ease-out',
        'slide-in': 'slide-in 200ms ease-out',
      },
    },
  },
  plugins: [],
} satisfies Config;
