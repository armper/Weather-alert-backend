import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import type { NwsProduct } from '../../../types'

interface NwsProductsPanelProps {
  products: NwsProduct[]
  onLoadProduct?: (productId: string) => Promise<NwsProduct | null>
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

function truncateCopy(value: string, maxLength: number): string {
  if (value.length <= maxLength) {
    return value
  }
  return `${value.slice(0, maxLength).trimEnd()}...`
}

function extractDiscussionExcerpt(productText?: string): string {
  if (!productText?.trim()) {
    return 'Open the latest discussion for the forecaster-written reasoning behind the next weather setup.'
  }

  const normalized = productText.replace(/\r/g, '').trim()
  const sectionMatch = normalized.match(
    /(?:^|\n)\.(?:SYNOPSIS|DISCUSSION)\.\.\.\s*([\s\S]*?)(?=\n\.[A-Z][A-Z /-]*\.\.\.|\n&&|\n\$\$|$)/i,
  )

  const sectionText = sectionMatch?.[1]
    ?.replace(/\n+/g, ' ')
    .replace(/\s+/g, ' ')
    .trim()

  if (sectionText) {
    return truncateCopy(sectionText, 280)
  }

  const body = normalized
    .split('\n')
    .map((line) => line.trim())
    .filter(
      (line) =>
        line.length > 24 &&
        !/^[A-Z0-9]{4,}(?: [A-Z0-9]{2,})*$/.test(line) &&
        !/^\d{3,}$/.test(line) &&
        !/^Area Forecast Discussion$/i.test(line),
    )
    .join(' ')
    .replace(/\s+/g, ' ')
    .trim()

  return body ? truncateCopy(body, 280) : 'Open the latest discussion for the full NWS narrative forecast.'
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

export function NwsProductsPanel({ products, onLoadProduct }: NwsProductsPanelProps) {
  const [openProductId, setOpenProductId] = useState<string | null>(products[0]?.id ?? null)
  const [loadingProductIds, setLoadingProductIds] = useState<string[]>([])
  const [failedProductIds, setFailedProductIds] = useState<string[]>([])
  const requestedProductIdsRef = useRef<Set<string>>(new Set())

  useEffect(() => {
    if (products.length === 0) {
      setOpenProductId(null)
      return
    }

    setOpenProductId((current) => {
      if (current && products.some((product) => product.id === current)) {
        return current
      }
      return products[0].id
    })
  }, [products])

  const loadProduct = useCallback(
    async (productId: string) => {
      if (!onLoadProduct || requestedProductIdsRef.current.has(productId)) {
        return
      }

      requestedProductIdsRef.current.add(productId)
      setFailedProductIds((current) => current.filter((id) => id !== productId))
      setLoadingProductIds((current) => [...current, productId])

      try {
        const product = await onLoadProduct(productId)
        if (!product?.productText?.trim()) {
          setFailedProductIds((current) => [...current, productId])
          requestedProductIdsRef.current.delete(productId)
        }
      } catch {
        setFailedProductIds((current) => [...current, productId])
        requestedProductIdsRef.current.delete(productId)
      } finally {
        setLoadingProductIds((current) => current.filter((id) => id !== productId))
      }
    },
    [onLoadProduct],
  )

  const latestProduct = products[0] ?? null

  useEffect(() => {
    if (!latestProduct?.id || latestProduct.productText?.trim()) {
      return
    }

    void loadProduct(latestProduct.id)
  }, [latestProduct, loadProduct])

  const featuredExcerpt = useMemo(
    () => extractDiscussionExcerpt(latestProduct?.productText),
    [latestProduct?.productText],
  )

  if (products.length === 0) {
    return null
  }

  return (
    <section className="nws-products-panel nws-products-panel-featured">
      <div className="panel-title-row nws-products-header">
        <div>
          <p className="eyebrow">NWS Narrative Forecast</p>
          <h3>Area Forecast Discussions</h3>
          <p className="muted small nws-products-header-copy">
            Forecaster reasoning, pattern changes, and the setup behind the next several days.
          </p>
        </div>
        <span className="badge">{products.length} latest</span>
      </div>

      {latestProduct ? (
        <article className="nws-products-hero">
          <div className="nws-products-hero-main">
            <div className="nws-products-hero-kicker">
              <span className="nws-product-icon" aria-hidden>
                {resolveProductIcon(latestProduct.productCode)}
              </span>
              <span>Latest briefing</span>
            </div>
            <h4>{latestProduct.productName || latestProduct.productCode || 'Area Forecast Discussion'}</h4>
            <p className="nws-products-hero-excerpt">
              {loadingProductIds.includes(latestProduct.id) ? 'Loading the latest forecast discussion...' : featuredExcerpt}
            </p>
            <div className="nws-products-hero-actions">
              <button
                type="button"
                className="ghost button-inline nws-products-hero-button"
                onClick={() => {
                  setOpenProductId(latestProduct.id)
                  if (!latestProduct.productText?.trim()) {
                    void loadProduct(latestProduct.id)
                  }
                }}
              >
                Read latest discussion
              </button>
            </div>
          </div>
          <dl className="nws-products-hero-meta">
            <div>
              <dt>Office</dt>
              <dd>{latestProduct.issuingOffice || 'NWS'}</dd>
            </div>
            <div>
              <dt>Issued</dt>
              <dd>{formatProductTime(latestProduct.issuanceTime) || 'Recently'}</dd>
            </div>
            <div>
              <dt>Product</dt>
              <dd>{latestProduct.productCode || 'AFD'}</dd>
            </div>
          </dl>
        </article>
      ) : null}

      <div className="nws-products-list">
        {products.slice(0, 5).map((product) => (
          <article key={product.id} className={`nws-product-item ${openProductId === product.id ? 'is-open' : ''}`}>
            <button
              type="button"
              className="nws-product-summary"
              aria-expanded={openProductId === product.id}
              onClick={() => {
                const nextOpenProductId = openProductId === product.id ? null : product.id
                setOpenProductId(nextOpenProductId)
                if (nextOpenProductId && !product.productText?.trim()) {
                  void loadProduct(product.id)
                }
              }}
            >
              <span className="nws-product-icon" aria-hidden>
                {resolveProductIcon(product.productCode)}
              </span>
              <span className="nws-product-summary-main">
                <span className="nws-product-name">{product.productName || product.productCode || 'NWS Product'}</span>
                <span className="nws-product-summary-copy">
                  {product.issuingOffice || 'NWS'}
                  {product.issuanceTime ? ` • ${formatProductTime(product.issuanceTime)}` : ''}
                </span>
              </span>
              <span className="nws-product-summary-status">
                {loadingProductIds.includes(product.id) ? 'Loading' : openProductId === product.id ? 'Hide' : 'Read'}
              </span>
            </button>
            {openProductId === product.id ? (
              <div className="nws-product-body">
                {loadingProductIds.includes(product.id) ? (
                  <p className="nws-product-empty">Loading discussion text...</p>
                ) : product.productText?.trim() ? (
                  <pre className="nws-product-text">{product.productText}</pre>
                ) : failedProductIds.includes(product.id) ? (
                  <p className="nws-product-empty">Discussion text is unavailable right now. Try again in a moment.</p>
                ) : (
                  <p className="nws-product-empty">Open this discussion to load the full text.</p>
                )}
              </div>
            ) : null}
          </article>
        ))}
      </div>
    </section>
  )
}
