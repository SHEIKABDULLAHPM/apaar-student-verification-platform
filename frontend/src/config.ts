/**
 * Runtime configuration.
 *
 * The API base is deliberately relative by default. This lets the Vite dev
 * proxy (`/api` -> localhost:8080) and the production nginx container proxy the
 * same paths without rebuilding the app for every environment.
 */
const configuredBase = import.meta.env.VITE_API_BASE_URL?.trim();

export const API_BASE_URL = (configuredBase || '/api/v1').replace(/\/+$/, '');

// Demo conveniences (guest role sign-in) are on by default for this
// demonstration platform. Set VITE_SHOW_DEMO_HINT=false to hide them, e.g.
// before pointing a build at real institutional data.
export const SHOW_DEMO_HINT = import.meta.env.VITE_SHOW_DEMO_HINT !== 'false';

export const APP_NAME = 'APAAR Verification Platform';
export const APP_OWNER = 'All India Council for Technical Education';
