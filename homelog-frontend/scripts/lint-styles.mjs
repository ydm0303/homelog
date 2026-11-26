// ============================================================
// 样式守卫（无第三方依赖）
// ------------------------------------------------------------
// 约束：
//   1. .vue 中禁止硬编码十六进制色值（统一使用 styles token）
//   2. .vue 中禁止 !important
//   3. z-index 禁止裸数字（使用 $z-index-* token）
//   4. @media 宽度只允许 640 / 768 / 1024 / 1280 边界
//   5. 全局 .scss 中禁止 :deep()（只对 SFC scoped 样式有效）
//   6. .vue 的 <style> 中禁止裸 px 尺寸（使用 $text-* / $border-radius-* / $spacing-*）
//      存量文件按 scripts/style-baseline.json 的计数豁免，额度只减不增
//
// 运行：pnpm lint:styles
// 重新生成豁免表（收拾完一页后跑一次，额度自动收紧）：
//   node scripts/lint-styles.mjs --update
// ============================================================

import { readdirSync, readFileSync, writeFileSync, statSync, existsSync } from 'node:fs'
import { join, relative, sep } from 'node:path'
import { fileURLToPath } from 'node:url'

const root = fileURLToPath(new URL('..', import.meta.url))
const srcDir = join(root, 'src')
const baselinePath = join(root, 'scripts', 'style-baseline.json')

// --update：把当前各文件的裸 px 数量写回 baseline 后退出。
// 用于「改完一页 → 重新生成 → 额度单调收缩」，避免手抄数字对不上。
const UPDATE_MODE = process.argv.includes('--update')

const MAX_WIDTHS = new Set([639, 767, 1023, 1279])
const MIN_WIDTHS = new Set([640, 768, 1024, 1280])

const errors = []
/** relPath -> 裸 px 违规数组 */
const pxViolationsByFile = new Map()

// ------------------------------------------------------------
// 规则 6：裸 px 尺寸
// ------------------------------------------------------------
// 这五类属性一旦写死 px，就会绕过设计系统造成「同一层级不同长相」：
// 优化前 Dashboard 里同类卡片圆角有 8/10/12/14/18 五种、字号有
// 11/13/15/17/18/22/26 七种，而 AppCard 是 12px —— 用户感知到的
// 「不够细腻」，具体就是这些对不齐。
//
// 不含 width/height：插画尺寸、头像直径、贴合内容的 min-width
// 属于「按内容定形」，不是设计档位，token 化反而稀释令牌表。
const SIZE_PROPS = ['font-size', 'border-radius', 'padding', 'margin', 'gap', 'row-gap', 'column-gap']

// 1px 描边/分隔线不算魔法数字
const HAIRLINE_OK = new Set(['1px'])

// 函数式值里的 px 由运行时或 Sass 决定，不在守卫范围内
const ALLOWED_FN_RE = /calc\([^)]*\)|clamp\([^)]*\)|env\([^)]*\)|v-bind\([^)]*\)|var\([^)]*\)/g

const SIZE_DECL_RE = new RegExp(
  `^\\s*(?:${SIZE_PROPS.join('|')})(?:-(?:top|right|bottom|left|inline|block))?\\s*:`
)

function collectPxViolations(content) {
  const found = []
  const lines = content.split(/\r?\n/)
  let inStyle = false

  lines.forEach((raw, index) => {
    const line = raw.trim()

    // 只看 <style> 块；模板内联 style 与 JS 常量交给 ESLint
    if (/<style\b/.test(line)) {
      inStyle = true
      return
    }
    if (/<\/style>/.test(line)) {
      inStyle = false
      return
    }
    if (!inStyle) return
    if (isCommentLine(line)) return
    if (!SIZE_DECL_RE.test(raw)) return

    // 剥掉函数式值、Sass 插值与变量后，剩下的才是「裸写」的 px
    const residual = raw
      .replace(ALLOWED_FN_RE, ' ')
      .replace(/#\{[^}]*\}/g, ' ')
      .replace(/\$[\w-]+/g, ' ')

    const pxValues = (residual.match(/-?\d*\.?\d+px/g) || []).filter(
      (v) => !HAIRLINE_OK.has(v.replace(/^-/, ''))
    )

    if (pxValues.length > 0) found.push({ line: index + 1, values: pxValues, text: line })
  })

  return found
}

function loadBaseline() {
  if (!existsSync(baselinePath)) return {}
  try {
    return JSON.parse(readFileSync(baselinePath, 'utf8'))
  } catch (e) {
    errors.push(`scripts/style-baseline.json 解析失败：${e.message}`)
    return {}
  }
}

function toRel(file) {
  return relative(root, file).split(sep).join('/')
}

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
  return (
    text.startsWith('//') ||
    text.startsWith('/*') ||
    text.startsWith('*') ||
    text.startsWith('<!--')
  )
}

function lineAt(content, index) {
  return content.split('\n')[lineOf(content, index) - 1] || ''
}

function push(file, line, message) {
  errors.push(`${toRel(file)}:${line}: ${message}`)
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

  const violations = collectPxViolations(content)
  if (violations.length > 0) pxViolationsByFile.set(toRel(file), violations)
}

walk(srcDir)

// ------------------------------------------------------------
// --update：重新生成豁免表（只动裸 px 额度，其余违规照常报）
// ------------------------------------------------------------
if (UPDATE_MODE) {
  const sorted = Object.fromEntries(
    [...pxViolationsByFile.entries()]
      .sort((a, b) => b[1].length - a[1].length)
      .map(([file, list]) => [file, list.length])
  )
  writeFileSync(baselinePath, `${JSON.stringify(sorted, null, 2)}\n`, 'utf8')

  const total = Object.values(sorted).reduce((sum, n) => sum + n, 0)
  if (errors.length > 0) {
    console.error(`其余规则仍有 ${errors.length} 个问题（未写入 baseline）：\n`)
    console.error(errors.join('\n'))
    process.exit(1)
  }
  console.log(
    `已写入 scripts/style-baseline.json：${Object.keys(sorted).length} 个文件、共 ${total} 处裸 px 待收敛。`
  )
  process.exit(0)
}

// ------------------------------------------------------------
// 校验裸 px 额度
// ------------------------------------------------------------
const baselineMap = loadBaseline()
const TOKEN_HINT =
  '请改用 $text-* / $border-radius-* / $spacing-*，映射见 src/styles/variables.scss'

for (const [rel, violations] of pxViolationsByFile) {
  const allowed = baselineMap[rel]

  if (allowed === undefined) {
    // 未登记 = 该文件必须已经干净（本轮起 Dashboard / DefaultLayout 走这条）
    for (const v of violations) {
      errors.push(`${rel}:${v.line}: 禁止裸 px（${v.values.join(', ')}），${TOKEN_HINT}`)
    }
    continue
  }

  if (violations.length > allowed) {
    errors.push(
      `${rel}:1: 裸 px 违规 ${violations.length} 处，超出 style-baseline.json 豁免额度 ${allowed}。` +
        `存量额度只减不增，${TOKEN_HINT}；` +
        '确需重新生成额度请跑 node scripts/lint-styles.mjs --update'
    )
  }
}

// 已清零却仍挂在豁免表里的文件：提醒删掉，防止豁免表只增不减
for (const rel of Object.keys(baselineMap)) {
  if (!pxViolationsByFile.has(rel) && !rel.includes('style-baseline')) {
    errors.push(`${rel}:1: 已无裸 px 违规，请从 scripts/style-baseline.json 删除该条目`)
  }
}

if (errors.length > 0) {
  console.error(`样式守卫发现 ${errors.length} 个问题：\n`)
  console.error(errors.join('\n'))
  process.exit(1)
}

console.log('样式守卫通过：无硬编码色值、!important、裸 z-index、越界断点或未豁免的裸 px 尺寸。')
