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
          <stop offset="0%" style="stop-color:#E8567F;stop-opacity:1" />
          <stop offset="100%" style="stop-color:#C44060;stop-opacity:1" />
        </linearGradient>
      </defs>
      <rect x="8" y="8" width="84" height="84" rx="20" fill="url(#yc-grad)"/>
      <!-- 爱心 -->
      <path d="M50 70 C32 54, 22 42, 22 34 C22 25, 29 19, 36 19 C41 19, 46 23, 50 29 C54 23, 59 19, 64 19 C71 19, 78 25, 78 34 C78 42, 68 54, 50 70Z" fill="url(#yc-heart)"/>
      <!-- 云朵 -->
      <path d="M28 48 Q28 38, 38 38 Q40 30, 48 30 Q56 30, 58 38 Q68 38, 68 48 Q68 56, 60 57 L34 57 Q26 56, 26 48 Z" fill="white" opacity="0.88"/>
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
