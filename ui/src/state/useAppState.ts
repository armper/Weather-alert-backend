import { useContext } from 'react'
import {
  ActionStateContext,
  AppStateContextValue,
  AsyncStateContext,
  AuthStateContext,
  DataStateContext,
  FormStateContext,
  NoticeStateContext,
  SessionStateContext,
} from './AppStateContextValue'

function requireContext<T>(context: T | null, name: string) {
  if (!context) {
    throw new Error(`${name} must be used within AppStateProvider`)
  }
  return context
}

export function useAppState() {
  return requireContext(useContext(AppStateContextValue), 'useAppState')
}

export function useSessionState() {
  return requireContext(useContext(SessionStateContext), 'useSessionState')
}

export function useNoticeState() {
  return requireContext(useContext(NoticeStateContext), 'useNoticeState')
}

export function useAuthState() {
  return requireContext(useContext(AuthStateContext), 'useAuthState')
}

export function useDataState() {
  return requireContext(useContext(DataStateContext), 'useDataState')
}

export function useFormState() {
  return requireContext(useContext(FormStateContext), 'useFormState')
}

export function useAsyncState() {
  return requireContext(useContext(AsyncStateContext), 'useAsyncState')
}

export function useActionState() {
  return requireContext(useContext(ActionStateContext), 'useActionState')
}
