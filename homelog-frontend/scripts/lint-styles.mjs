// ============================================================
// 样式守卫（无第三方依赖）
// ------------------------------------------------------------
// 约束：
//   1. .vue 中禁止硬编码十六进制色值（统一使用 styles token）
//   2. .vue 中禁止 !important
//   3. z-index 禁止裸数字（使用 $z-index-* token）
//   4. @media 宽度只允许 640 / 768 / 1024 / 1280 边界
//   5. 全局 .scss 中禁止 :deep()（只对 SFC scoped 样式有效）
//
// 运行：pnpm lint:styles
// ============================================================

import { readdirSync, readFileSync, statSync } from 'node:fs'
import { join, relative } from 'node:path'
import { fileURLToPath } from 'node:url'

const root = fileURLToPath(new URL('..', import.meta.url))
const srcDir = join(root, 'src')

const MAX_WIDTHS = new Set([639, 767, 1023, 1279])
const MIN_WIDTHS = new Set([640, 768, 1024, 1280])

const errors = []

function walk(dir) {
  for (const entry of readdirSync(dir)) {
    const full = join(dir, entry)
    const stat = statSync(full)
    if (stat.isDirectory()) walk(full)
    else if (/\.(vue|scss)$/.test(entry)) check(full)
  }
}

function lineOf(content, index) {
  return content.slice(0, index).split('\n').length
}

function isCommentLine(line) {
  const text = line.trim()
  return text.startsWith('//') || text.startsWith('/*') || text.startsWith('*') || text.startsWith('<!--')
}

function lineAt(content, index) {
  return content.split('\n')[lineOf(content, index) - 1] || ''
}

function push(file, line, message) {
  errors.push(`${relative(root, file)}:${line}: ${message}`)
}

function checkScss(file, content) {
  let match

  const deepRe = /:deep\(/g
  while ((match = deepRe.exec(content))) {
    if (isCommentLine(lineAt(content, match.index))) continue
    push(file, lineOf(content, match.index), '全局 SCSS 不允许 :deep()，请改用真实选择器')
  }

  const zIndexRe = /z-index:\s*(\d+)/g
  while ((match = zIndexRe.exec(content))) {
    if (isCommentLine(lineAt(content, match.index))) continue
    push(file, lineOf(content, match.index), `z-index 使用 token（当前为 ${match[1]}）`)
  }
}

function checkMedia(file, content) {
  const mediaRe = /@media\s*\((max|min)-width:\s*(\d+)px\)/g
  let match
  while ((match = mediaRe.exec(content))) {
    const type = match[1]
    const width = Number(match[2])
    const allowed = type === 'max' ? MAX_WIDTHS : MIN_WIDTHS
    if (!allowed.has(width)) {
      push(
        file,
        lineOf(content, match.index),
        `断点 ${type}-width: ${width}px 不在统一边界内（max: 639/767/1023/1279，min: 640/768/1024/1280）`
      )
    }
  }
}

function check(file) {
  const content = readFileSync(file, 'utf8')
  checkMedia(file, content)

  if (file.endsWith('.scss')) {
    checkScss(file, content)
    return
  }

  const lines = content.split(/\r?\n/)
  lines.forEach((line, index) => {
    if (isCommentLine(line)) return

    const lineNumber = index + 1
    const color = line.match(/#[0-9a-fA-F]{3,8}\b/)
    if (color) push(file, lineNumber, `禁止硬编码色值 ${color[0]}，请使用 token`)

    if (line.includes('!important')) push(file, lineNumber, '禁止 !important，请提高选择器或调整结构')

    if (/z-index:\s*\d+/.test(line)) push(file, lineNumber, 'z-index 禁止裸数字，请使用 $z-index-* token')
  })
}

walk(srcDir)

if (errors.length > 0) {
  console.error(`样式守卫发现 ${errors.length} 个问题：\n`)
  console.error(errors.join('\n'))
  process.exit(1)
}

console.log('样式守卫通过：无硬编码色值、!important、裸 z-index 或越界断点。')
