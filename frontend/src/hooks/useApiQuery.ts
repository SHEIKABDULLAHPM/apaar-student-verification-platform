/* eslint-disable react-hooks/exhaustive-deps -- the hook intentionally accepts a dynamic dependency list from callers */
import { useCallback, useEffect, useRef, useState } from 'react';
import { normalizeError, type AppError } from '@/lib/errors';

export type ApiQueryStatus = 'idle' | 'loading' | 'success' | 'error';

export interface ApiQueryState<T> {
  status: ApiQueryStatus;
  data: T | null;
  error: AppError | null;
  reload: () => void;
  isLoading: boolean;
}

/**
 * Minimal async data hook (no global cache, no extra runtime dependency).
 *
 * `fetcher` is read through a ref so callers can inline arrow functions without
 * causing an infinite fetch loop. Pass every value the fetch depends on in
 * `deps`; `reload()` bumps an internal nonce to refetch.
 */
export function useApiQuery<T>(
  fetcher: () => Promise<T>,
  deps: readonly unknown[],
  options: { enabled?: boolean } = {},
): ApiQueryState<T> {
  const enabled = options.enabled ?? true;
  const fetcherRef = useRef(fetcher);
  fetcherRef.current = fetcher;

  const [state, setState] = useState<Omit<ApiQueryState<T>, 'reload' | 'isLoading'>>({
    status: enabled ? 'loading' : 'idle',
    data: null,
    error: null,
  });
  const [nonce, setNonce] = useState(0);

  useEffect(() => {
    if (!enabled) {
      setState({ status: 'idle', data: null, error: null });
      return;
    }

    let cancelled = false;
    setState({ status: 'loading', data: null, error: null });

    fetcherRef
      .current()
      .then((data) => {
        if (cancelled) return;
        setState({ status: 'success', data, error: null });
      })
      .catch((error: unknown) => {
        if (cancelled) return;
        setState({ status: 'error', data: null, error: normalizeError(error) });
      });

    return () => {
      cancelled = true;
    };
  }, [enabled, nonce, ...deps]);

  const reload = useCallback(() => {
    setNonce((value) => value + 1);
  }, []);

  return {
    ...state,
    reload,
    isLoading: state.status === 'loading',
  };
}
