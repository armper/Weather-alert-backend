import type { ReactNode } from 'react'
import { Button, type ButtonProps } from 'react-aria-components'

interface AriaButtonProps extends Omit<ButtonProps, 'className' | 'children'> {
  className?: string
  children: ReactNode
}

export function AriaButton({ className, children, ...props }: AriaButtonProps) {
  return (
    <Button
      {...props}
      className={({ isFocusVisible, isPressed, isHovered, isDisabled }) =>
        [
          className ?? '',
          isFocusVisible ? 'is-focus-visible' : '',
          isPressed ? 'is-pressed' : '',
          isHovered ? 'is-hovered' : '',
          isDisabled ? 'is-disabled' : '',
        ]
          .filter(Boolean)
          .join(' ')
      }
    >
      {children}
    </Button>
  )
}

