const withNextra = require('nextra')({
  theme: 'nextra-theme-docs',
  themeConfig: './theme.config.tsx',
})

const basePath = process.env.NEXT_PUBLIC_BASE_PATH || ''

/** @type {import('next').NextConfig} */
const nextConfig = {
  output: 'export',
  images: {
    unoptimized: true,
  },
  basePath,
  assetPrefix: basePath,
  trailingSlash: true,
}

const nextraConfig = withNextra(nextConfig)

if (typeof nextraConfig === 'object' && nextraConfig !== null) {
  delete nextraConfig.rewrites
  delete nextraConfig.redirects
  delete nextraConfig.headers
}

module.exports = (phase, context) => {
  const cfg = typeof nextraConfig === 'function' ? nextraConfig(phase, context) : { ...nextraConfig }
  delete cfg.rewrites
  delete cfg.redirects
  delete cfg.headers
  return cfg
}
