import { useMemo } from 'react'
import {
  Button,
  FieldError,
  Label,
  ListBox,
  ListBoxItem,
  Popover,
  Select,
  SelectValue,
  type Key,
} from 'react-aria-components'

export interface AriaSelectOption {
  id: string
  label: string
}

interface AriaSelectProps {
  label: string
  options: AriaSelectOption[]
  selectedKey?: string
  onSelectionChange: (value: string) => void
  placeholder?: string
  className?: string
  buttonClassName?: string
  listBoxClassName?: string
  popoverClassName?: string
  errorMessage?: string
  isDisabled?: boolean
}

export function AriaSelect({
  label,
  options,
  selectedKey,
  onSelectionChange,
  placeholder = 'Select an option',
  className,
  buttonClassName,
  listBoxClassName,
  popoverClassName,
  errorMessage,
  isDisabled,
}: AriaSelectProps) {
  const selected = useMemo(() => options.find((item) => item.id === selectedKey), [options, selectedKey])

  return (
    <Select
      className={['aria-select-field', className ?? ''].filter(Boolean).join(' ')}
      selectedKey={selectedKey}
      isDisabled={isDisabled}
      isInvalid={Boolean(errorMessage)}
      onSelectionChange={(key: Key | null) => {
        if (key === null) {
          return
        }
        onSelectionChange(String(key))
      }}
    >
      <Label>{label}</Label>
      <Button className={buttonClassName}>
        <SelectValue>{selected?.label ?? placeholder}</SelectValue>
        <span aria-hidden>▾</span>
      </Button>
      <Popover className={popoverClassName}>
        <ListBox className={listBoxClassName}>
          {options.map((item) => (
            <ListBoxItem key={item.id} id={item.id}>
              {item.label}
            </ListBoxItem>
          ))}
        </ListBox>
      </Popover>
      {errorMessage ? <FieldError className="field-error">{errorMessage}</FieldError> : null}
    </Select>
  )
}
