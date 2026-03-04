import { Switch } from 'react-aria-components'

interface AriaSwitchProps {
  label: string
  isSelected: boolean
  onChange: (value: boolean) => void
  className?: string
  compact?: boolean
  isDisabled?: boolean
}

export function AriaSwitch({ label, isSelected, onChange, className, compact, isDisabled }: AriaSwitchProps) {
  return (
    <Switch
      isSelected={isSelected}
      isDisabled={isDisabled}
      onChange={onChange}
      className={[
        'switch-field',
        compact ? 'compact' : '',
        className ?? '',
      ]
        .filter(Boolean)
        .join(' ')}
    >
      {({ isSelected: selected }) => (
        <>
          <span>{label}</span>
          <span className={`aria-switch-track ${selected ? 'is-selected' : ''}`}>
            <span className="aria-switch-thumb" />
          </span>
        </>
      )}
    </Switch>
  )
}

