import React from 'react'
import { DocsThemeConfig } from 'nextra-theme-docs'

const config: DocsThemeConfig = {
  logo: (
    <span style={{ fontWeight: 650, letterSpacing: '-0.02em', fontSize: '1.05rem' }}>
      FindTheBlock
    </span>
  ),
  project: {
    link: 'https://github.com/tanjim-t1g/Find-The-Block',
  },
  docsRepositoryBase: 'https://github.com/tanjim-t1g/Find-The-Block',
  footer: {
    text: 'FindTheBlock - High-Performance Item Rush Plugin for Paper 1.21+',
  },
  useNextSeoProps() {
    return {
      titleTemplate: '%s - FindTheBlock Documentation'
    }
  },
  feedback: {
    content: null
  },
  editLink: {
    text: null
  }
}

export default config
