/* eslint-env node */
/**
 * ESLint 配置（.eslintrc 格式，配合 ESLint 8.x）
 *
 * 之前仓库缺少该文件，导致 `npm run lint` 一直报 “couldn't find a configuration file”。
 * 这里采用最小可用配置：Vue3 基础规则 + TypeScript 解析，不做类型感知（避免需要 parserOptions.project）。
 */
module.exports = {
  root: true,
  env: {
    browser: true,
    es2022: true,
    node: true,
  },
  parser: 'vue-eslint-parser',
  parserOptions: {
    parser: '@typescript-eslint/parser',
    ecmaVersion: 2022,
    sourceType: 'module',
  },
  extends: ['eslint:recommended', 'plugin:vue/vue3-essential'],
  plugins: ['@typescript-eslint'],
  rules: {
    // 组件名允许单词（Login / Settings 等）
    'vue/multi-word-component-names': 'off',
    // TypeScript 由编译器负责未定义检查
    'no-undef': 'off',
    'no-unused-vars': 'off',
    '@typescript-eslint/no-unused-vars': [
      'warn',
      { argsIgnorePattern: '^_', varsIgnorePattern: '^_' },
    ],
    'no-empty': ['warn', { allowEmptyCatch: true }],
  },
  ignorePatterns: ['dist', 'dev-dist', 'node_modules', '*.d.ts', 'public'],
};
