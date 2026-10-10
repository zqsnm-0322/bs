<!-- 正在开发中页面 -->
<template>
  <div class="w-full h-full flex items-center justify-center bg-transparent">
    <div class="art-card-sm p-12 text-center max-w-2xl">
      <div class="mb-6">
        <ArtSvgIcon icon="ri:tools-line" class="text-8xl text-primary" />
      </div>
      <h1 class="text-3xl font-bold mb-4">功能开发中</h1>
      <p class="text-lg text-gray-600 mb-6">
        该功能正在紧张开发中，敬请期待...
      </p>
      <div class="flex items-center justify-center gap-4 text-sm text-gray-500">
        <div class="flex items-center gap-2">
          <ArtSvgIcon icon="ri:time-line" class="text-xl" />
          <span>预计上线时间：待定</span>
        </div>
      </div>
      <div class="mt-8">
        <ElButton type="primary" @click="goBack">
          <ArtSvgIcon icon="ri:arrow-left-line" class="mr-2" />
          返回上一页
        </ElButton>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
  import { useRouter } from 'vue-router'

  defineOptions({ name: 'Developing' })

  const router = useRouter()

  const goBack = () => {
    router.back()
  }
</script>

<style scoped>
  .art-card-sm {
    animation: fadeIn 0.5s ease-in-out;
  }

  @keyframes fadeIn {
    from {
      opacity: 0;
      transform: translateY(20px);
    }
    to {
      opacity: 1;
      transform: translateY(0);
    }
  }
</style>
