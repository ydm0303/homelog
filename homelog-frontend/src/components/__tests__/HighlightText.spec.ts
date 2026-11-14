import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import HighlightText from '../HighlightText.vue'

describe('HighlightText', () => {
  it('highlights matching text case-insensitively', () => {
    const wrapper = mount(HighlightText, {
      props: { text: '家庭药品 HomeLog', keyword: 'homelog' }
    })
    expect(wrapper.findAll('mark.highlight')).toHaveLength(1)
    expect(wrapper.find('mark.highlight').text()).toBe('HomeLog')
  })

  it('renders HTML-like content as text, not markup', () => {
    const payload = '<img src=x onerror="alert(1)"><script>alert(1)</script>'
    const wrapper = mount(HighlightText, {
      props: { text: payload, keyword: 'alert' }
    })
    expect(wrapper.find('img').exists()).toBe(false)
    expect(wrapper.find('script').exists()).toBe(false)
    expect(wrapper.text()).toContain('<img')
    expect(wrapper.findAll('mark.highlight').length).toBeGreaterThan(0)
  })

  it('renders plain text when no keyword is provided', () => {
    const wrapper = mount(HighlightText, {
      props: { text: '没有关键词' }
    })
    expect(wrapper.findAll('mark')).toHaveLength(0)
    expect(wrapper.text()).toBe('没有关键词')
  })
})
