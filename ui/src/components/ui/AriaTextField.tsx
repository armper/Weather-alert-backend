import type { HTMLInputTypeAttribute, ReactNode } from 'react'
import { FieldError, Input, Label, TextField, type TextFieldProps } from 'react-aria-components'

interface AriaTextFieldProps extends Omit<TextFieldProps, 'children' | 'className' | 'isInvalid'> {
  label: string
  value?: string
  onChange?: (value: string) => void
  type?: HTMLInputTypeAttribute
  placeholder?: string
  className?: string
  inputClassName?: string
  inputWrapperClassName?: string
  endAction?: ReactNode
  description?: ReactNode
  errorMessage?: string
  min?: number | string
  max?: number | string
  step?: number | string
  minLength?: number
  required?: boolean
}

export function AriaTextField({
  label,
  value,
  onChange,
  type = 'text',
  placeholder,
  className,
  inputClassName,
  inputWrapperClassName,
  endAction,
  description,
  errorMessage,
  min,
  max,
  step,
  minLength,
  required,
  ...props
}: AriaTextFieldProps) {
  return (
    <TextField {...props} className={['aria-text-field', className ?? ''].filter(Boolean).join(' ')} isInvalid={Boolean(errorMessage)}>
      <Label>{label}</Label>
      {endAction ? (
        <div className={inputWrapperClassName}>
          <Input
            className={inputClassName}
            type={type}
            value={value}
            min={min}
            max={max}
            step={step}
            minLength={minLength}
            required={required}
            placeholder={placeholder}
            onChange={(event) => onChange?.(event.currentTarget.value)}
          />
          {endAction}
        </div>
      ) : (
        <Input
          className={inputClassName}
          type={type}
          value={value}
          min={min}
          max={max}
          step={step}
          minLength={minLength}
          required={required}
          placeholder={placeholder}
          onChange={(event) => onChange?.(event.currentTarget.value)}
        />
      )}
      {description ? <span className="muted small">{description}</span> : null}
      {errorMessage ? <FieldError className="field-error">{errorMessage}</FieldError> : null}
    </TextField>
  )
}
