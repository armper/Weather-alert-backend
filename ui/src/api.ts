import type { ProblemDetails } from './types'

const API_BASE = import.meta.env.VITE_API_BASE ?? ''

export class ApiError extends Error {
  readonly status: number
  readonly problem: ProblemDetails | null

  constructor(status: number, message: string, problem: ProblemDetails | null) {
    super(message)
    this.status = status
    this.problem = problem
  }
}

interface RequestOptions extends Omit<RequestInit, 'body'> {
  token?: string
  body?: unknown
}

export async function apiRequest<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const headers = new Headers(options.headers)
  const hasBody = options.body !== undefined && options.body !== null

  if (hasBody && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json')
  }
  if (options.token) {
    headers.set('Authorization', `Bearer ${options.token}`)
  }

  const response = await fetch(`${API_BASE}${path}`, {
    ...options,
    headers,
    body: hasBody ? JSON.stringify(options.body) : undefined,
  })

  if (response.status === 204) {
    return undefined as T
  }

  const text = await response.text()
  const payload = text ? tryParseJson(text) : null

  if (!response.ok) {
    const problem = (payload ?? null) as ProblemDetails | null
    const message =
      problem?.detail ??
      problem?.title ??
      `Request failed with status ${response.status}`
    throw new ApiError(response.status, message, problem)
  }

  return payload as T
}

function tryParseJson(value: string): unknown {
  try {
    return JSON.parse(value)
  } catch {
    return value
  }
}

export function toErrorMessage(error: unknown): string {
  if (error instanceof ApiError) {
    const validationMessages = error.problem?.errors?.map((item) => `${item.field}: ${item.message}`) ?? []
    if (validationMessages.length > 0) {
      return `${error.message} (${validationMessages.join(', ')})`
    }
    return error.message
  }

  if (error instanceof Error) {
    return error.message
  }

  return 'Unexpected error'
}
