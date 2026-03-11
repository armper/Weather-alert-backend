import skypandaLogo from '../../assets/skypanda-logo.svg'

interface BrandLockupProps {
  compact?: boolean
  subtitle?: string
}

export function BrandLockup({ compact = false, subtitle }: BrandLockupProps) {
  return (
    <div className={`brand-lockup${compact ? ' is-compact' : ''}`}>
      <img className="brand-lockup-logo" src={skypandaLogo} alt="SkyPanda" />
      {subtitle ? <p className="brand-lockup-subtitle">{subtitle}</p> : null}
    </div>
  )
}
