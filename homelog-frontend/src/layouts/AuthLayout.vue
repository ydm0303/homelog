<template>
  <div class="login-page">
    <div class="login-shell">
      <aside class="brand-panel">
        <div class="brand-header">
          <div class="logo-mark">
            <el-icon class="brand-icon"><House /></el-icon>
            <span>家记</span>
          </div>
        </div>

        <div class="brand-copy">
          <p class="eyebrow">把日子，认真收好</p>
          <h1>家里的事，都记在这里</h1>
          <p class="description">
            日常的点滴、重要的计划，还有那些值得珍藏的小事，都有一个温暖的归处。
          </p>
        </div>

        <div class="illustration" aria-hidden="true">
          <div class="house-scene"></div>
          <div class="plant plant-left"></div>
          <div class="plant plant-right"></div>
          <div class="todo-bubble todo-bubble-one">待办</div>
          <div class="todo-bubble todo-bubble-two">清单</div>
        </div>

        <div class="brand-foot">只属于你和家人的私密空间</div>
      </aside>

      <main class="login-panel">
        <div class="login-card">
          <div class="brand-mini">
            <el-icon><House /></el-icon>
            <span>家记</span>
          </div>
          <slot />
        </div>
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { House } from '@element-plus/icons-vue'
</script>

<style lang="scss" scoped>
@use '@/styles/variables' as *;
@use '@/styles/decorations' as *;

.login-page {
  width: 100%;
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 32px 24px;
  background: $gradient-login-bg;
}

.login-shell {
  width: 100%;
  max-width: 1180px;
  min-width: 0;
  min-height: 760px;
  display: flex;
  background: rgba(255, 255, 255, 0.7);
  border: 1px solid rgba($primary-color, 0.12);
  border-radius: $border-radius-xl;
  box-shadow: 0 24px 56px rgba(80, 50, 30, 0.12);
  overflow: hidden;
  backdrop-filter: blur(6px);
}

.brand-panel {
  position: relative;
  flex: 1.2;
  min-width: 0;
  padding: 54px 52px 34px;
  background: $gradient-brand-panel;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.brand-header {
  display: flex;
  align-items: center;
}

.logo-mark {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  color: $primary-text;
  font-size: $text-display;
  font-weight: $font-weight-bold;
  letter-spacing: 0.04em;
}

.brand-icon {
  font-size: $icon-size-xl;
}

.brand-copy {
  max-width: 560px;
  padding-top: 18px;
}

.eyebrow {
  margin: 0 0 12px;
  color: $primary-text;
  font-size: $text-label;
  font-weight: $font-weight-bold;
  letter-spacing: 0.18em;
  text-transform: uppercase;
}

.brand-copy h1 {
  margin: 0;
  color: $text-primary;
  font-size: $text-display;
  line-height: 1.15;
  letter-spacing: -0.02em;
  font-weight: $font-weight-bold;
}

.description {
  margin: 18px 0 0;
  max-width: 560px;
  color: rgba($text-primary, 0.75);
  font-size: $text-body;
  line-height: 1.8;
}

.illustration {
  position: relative;
  width: 100%;
  max-width: 540px;
  height: 240px;
  margin-top: 32px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.72), rgba(255, 255, 255, 0.34));
  border: 1px solid rgba($primary-color, 0.08);
  border-radius: $border-radius-xl;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.6);
}

.house-scene {
  position: absolute;
  left: 50%;
  bottom: 22px;
  width: 200px;
  height: 120px;
  transform: translateX(-50%);
  background: $gradient-house;
  border-radius: 22px 22px 18px 18px;
  box-shadow: 0 18px 24px $illustration-house-shadow;
}

.house-scene::before {
  content: '';
  position: absolute;
  left: 50%;
  top: -56px;
  width: 160px;
  height: 86px;
  transform: translateX(-50%);
  background: $gradient-house-roof;
  clip-path: polygon(50% 0, 100% 42%, 100% 100%, 0 100%, 0 42%);
  border-radius: 12px 12px 0 0;
}

.house-scene::after {
  content: '';
  position: absolute;
  left: 50%;
  bottom: 16px;
  width: 118px;
  height: 54px;
  transform: translateX(-50%);
  background: rgba(255, 255, 255, 0.32);
  border-radius: $border-radius-lg;
}

.plant {
  position: absolute;
  bottom: 18px;
  width: 56px;
  height: 96px;
  border-radius: 26px 26px 12px 12px;
  background: $gradient-plant;
  box-shadow: inset 0 0 0 6px rgba(255, 255, 255, 0.34);
}

.plant::before,
.plant::after {
  content: '';
  position: absolute;
  bottom: 45px;
  width: 18px;
  height: 54px;
  background: $gradient-leaf;
  border-radius: 14px;
}

.plant::before {
  left: 8px;
  transform: rotate(-18deg);
}

.plant::after {
  right: 8px;
  transform: rotate(18deg);
}

.plant-left {
  left: 92px;
}

.plant-right {
  right: 92px;
}

.todo-bubble {
  position: absolute;
  display: flex;
  align-items: center;
  justify-content: center;
  min-width: 86px;
  height: 34px;
  padding: 0 14px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.92);
  border: 1px solid rgba($primary-color, 0.1);
  color: $primary-text;
  box-shadow: 0 14px 22px rgba($text-primary, 0.08);
  font-size: $text-caption;
  font-weight: $font-weight-semibold;
}

.todo-bubble-one {
  top: 26px;
  right: 78px;
}

.todo-bubble-two {
  left: 72px;
  bottom: 18px;
}

.brand-foot {
  margin-top: 20px;
  color: rgba($text-primary, 0.7);
  font-size: $text-caption;
  letter-spacing: 0.04em;
}

.login-panel {
  flex: 0.92;
  min-width: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 42px 36px;
  background: rgba(255, 255, 255, 0.62);
}

.login-card {
  width: 100%;
  max-width: 430px;
  background: $card-bg;
  border: 1px solid rgba($text-primary, 0.04);
  border-radius: $border-radius-xl;
  padding: 32px 28px 24px;
  box-shadow: 0 16px 36px rgba(80, 50, 30, 0.1);
}

// 迷你品牌标识仅在窄屏（brand-panel 隐藏后）显示
.brand-mini {
  display: none;
}

@media (max-width: 1023px) {
  .login-page {
    padding: 20px 16px;
  }

  .login-shell {
    min-height: auto;
    flex-direction: column;
    border-radius: $border-radius-xl;
  }

  .brand-panel {
    min-height: 360px;
    padding: 32px 24px 24px;
  }

  .login-panel {
    padding: 24px 20px 30px;
  }
}

@media (max-width: 639px) {
  .login-page {
    // standalone 模式下 Safari 不把视口顶计入刘海，需自行预留状态栏安全区
    padding: calc(14px + env(safe-area-inset-top)) 14px calc(14px + env(safe-area-inset-bottom));
    align-items: flex-start;
    overflow-y: auto;
  }

  .login-shell {
    border-radius: $border-radius-xl;
  }

  .brand-panel {
    display: none;
  }

  .logo-mark {
    font-size: $text-heading;
  }

  .brand-copy h1 {
    font-size: $text-heading;
  }

  .description {
    font-size: $text-label;
  }

  .illustration {
    height: 180px;
  }

  .plant-left {
    left: 36px;
  }

  .plant-right {
    right: 36px;
  }

  .house-scene {
    width: 160px;
    height: 96px;
  }

  .login-card {
    padding: 24px 18px 20px;
    border-radius: $border-radius-xl;

    .brand-mini {
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 8px;
      margin-bottom: 18px;
      color: $primary-color;
      font-size: $text-title;
      font-weight: $font-weight-bold;
      letter-spacing: 0.04em;
    }
  }
}
</style>

<style lang="scss">
@use '@/styles/variables' as *;

// 表单样式（slot 内容渲染于子组件，需全局作用域，限定在 .login-card 下）
.login-card {
  .form-header {
    text-align: left;
    margin-bottom: 28px;

    h2 {
      margin: 0;
      color: $text-primary;
      font-size: $text-heading;
      line-height: 1.2;
      font-weight: $font-weight-bold;
      letter-spacing: -0.02em;
    }

    p {
      margin: 10px 0 0;
      color: rgba($text-primary, 0.68);
      font-size: $text-body;
    }
  }

  .form-label {
    display: block;
    margin-bottom: 8px;
    color: $text-primary;
    font-size: $text-label;
    font-weight: $font-weight-semibold;
  }

  .login-form {
    .el-form-item {
      margin-bottom: 18px;
    }

    .el-form-item__content {
      display: block;
    }

    .el-form-item__error {
      margin-top: 6px;
      font-size: $text-caption;
    }

    .el-checkbox__label {
      color: rgba($text-primary, 0.7);
      font-size: $text-label;

      a {
        color: $primary-text;
        text-decoration: none;

        &:hover {
          text-decoration: underline;
        }
      }
    }
  }

  .el-input__wrapper {
    height: 52px;
    border-radius: $border-radius-lg;
    background: $field-bg;
    border: 1px solid rgba($text-primary, 0.06);
    box-shadow: none;
    transition: all 0.2s ease;

    &:hover {
      border-color: rgba($primary-color, 0.3);
    }

    &.is-focus {
      background: $card-bg;
      border-color: rgba($primary-color, 0.7);
      box-shadow: 0 0 0 4px rgba($primary-color, 0.12);
    }
  }

  .el-input__inner {
    color: $text-primary;
    font-size: $text-body;
  }

  .el-input__prefix {
    color: $text-placeholder;
  }

  .el-checkbox {
    --el-checkbox-checked-bg-color: #{$primary-color};
    --el-checkbox-checked-border-color: #{$primary-color};
    --el-checkbox-checked-text-color: #{$text-primary};
    color: $text-primary;
    font-weight: $font-weight-medium;
  }

  .submit-btn {
    width: 100%;
    height: 52px;
    border-radius: $border-radius-lg;
    border: none;
    background: $gradient-primary;
    color: $card-bg;
    font-size: $text-title;
    font-weight: $font-weight-bold;
    letter-spacing: 0.04em;
    box-shadow: 0 14px 24px rgba($primary-color, 0.2);

    &:hover,
    &:focus {
      background: $gradient-primary-hover;
    }
  }

  .register-line {
    margin-top: 22px;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 8px;
    color: rgba($text-primary, 0.72);
    font-size: $text-body;

    .el-button {
      padding: 0;
      height: auto;
      color: $primary-text;
      font-weight: $font-weight-bold;
    }
  }

  .form-note {
    margin-top: 18px;
    text-align: center;
    color: rgba($text-primary, 0.5);
    font-size: $text-caption;
    line-height: 1.8;
    letter-spacing: 0.02em;
  }
}

@media (max-width: 639px) {
  .login-card {
    .form-header h2 {
      font-size: $text-title;
    }

    .form-label {
      font-size: $text-label;
    }

    .register-line {
      flex-wrap: wrap;
    }
  }
}
</style>
