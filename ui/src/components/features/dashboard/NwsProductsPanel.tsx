import type { NwsProduct } from '../../../types'

interface NwsProductsPanelProps {
  products: NwsProduct[]
}

function formatProductTime(issuanceTime?: string): string {
  if (!issuanceTime) {
    return ''
  }
  const date = new Date(issuanceTime)
  if (Number.isNaN(date.getTime())) {
    return ''
  }
  return date.toLocaleString(undefined, { month: 'short', day: 'numeric', hour: 'numeric', minute: '2-digit' })
}

function resolveProductIcon(productCode?: string): string {
  switch (productCode) {
    case 'AFD':
      return '📋'
    case 'HWO':
      return '⚠️'
    case 'SPS':
      return '🔔'
    case 'LSR':
      return '📡'
    default:
      return '📄'
  }
}

export function NwsProductsPanel({ products }: NwsProductsPanelProps) {
  if (products.length === 0) {
    return null
  }

  return (
    <section className="nws-products-panel">
      <div className="panel-title-row">
        <div>
          <p className="eyebrow">NWS Forecasts</p>
          <h3>Area Forecast Discussions</h3>
        </div>
      </div>
      <div className="nws-products-list">
        {products.slice(0, 5).map((product) => (
          <details key={product.id} className="nws-product-item">
            <summary className="nws-product-summary">
              <span className="nws-product-icon" aria-hidden>
                {resolveProductIcon(product.productCode)}
              </span>
              <span className="nws-product-name">{product.productName || product.productCode || 'NWS Product'}</span>
              {product.issuingOffice ? (
                <span className="nws-product-office">{product.issuingOffice}</span>
              ) : null}
              {product.issuanceTime ? (
                <span className="nws-product-time">{formatProductTime(product.issuanceTime)}</span>
              ) : null}
            </summary>
            {product.productText ? (
              <pre className="nws-product-text">{product.productText}</pre>
            ) : null}
          </details>
        ))}
      </div>
    </section>
  )
}
