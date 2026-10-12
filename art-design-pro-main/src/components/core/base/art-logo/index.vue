<!-- 系统logo -->
<template>
  <div class="flex-cc shrink-0" :style="logoStyle" aria-label="颐康云图标">
    <svg viewBox="0 0 100 100" width="100%" height="100%" xmlns="http://www.w3.org/2000/svg">
      <defs>
        <linearGradient id="yc-grad" x1="0%" y1="0%" x2="100%" y2="100%">
          <stop offset="0%" style="stop-color:#4A90D9;stop-opacity:1" />
          <stop offset="100%" style="stop-color:#2E6DB4;stop-opacity:1" />
        </linearGradient>
        <linearGradient id="yc-heart" x1="0%" y1="0%" x2="0%" y2="100%">
          <stop offset="0%" style="stop-color:#FF738F;stop-opacity:1" />
          <stop offset="100%" style="stop-color:#E94F70;stop-opacity:1" />
        </linearGradient>
      </defs>
      <rect x="8" y="8" width="84" height="84" rx="20" fill="url(#yc-grad)"/>
      <!-- 养老之家 -->
      <path d="M22 49 L50 25 L78 49" fill="none" stroke="white" stroke-width="8" stroke-linecap="round" stroke-linejoin="round"/>
      <path d="M29 45 V73 Q29 78 34 78 H66 Q71 78 71 73 V45 Z" fill="white"/>
      <!-- 关爱之心 -->
      <path d="M50 69 C39 61 36 56 36 51 C36 46 40 43 44 43 C47 43 49 45 50 48 C51 45 53 43 56 43 C60 43 64 46 64 51 C64 56 61 61 50 69Z" fill="url(#yc-heart)"/>
    </svg>
  </div>
</template>

<script setup lang="ts">
  defineOptions({ name: 'ArtLogo' })

  interface Props {
    size?: number | string
  }

  const props = withDefaults(defineProps<Props>(), {
    size: 36
  })

  const logoStyle = computed(() => ({ 
    width: `${props.size}px`, 
    height: `${props.size}px`,
    fontSize: `${props.size}px`
  }))
</script>
