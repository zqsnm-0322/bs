<template>
  <div class="resident-search">
    <ElForm :model="model" inline>
      <ElFormItem label="姓名">
        <ElInput v-model="model.name" placeholder="请输入姓名" clearable />
      </ElFormItem>
      <ElFormItem label="房间床位">
        <ElInput v-model="model.roomBedNo" placeholder="请输入房间床位号" clearable />
      </ElFormItem>
      <ElFormItem label="护理区">
        <ElSelect v-model="model.careZone" placeholder="请选择护理区" clearable>
          <ElOption label="自理区" value="自理区" />
          <ElOption label="半护区" value="半护区" />
          <ElOption label="全护区" value="全护区" />
          <ElOption label="失智区" value="失智区" />
        </ElSelect>
      </ElFormItem>
      <ElFormItem label="健康等级">
        <ElSelect v-model="model.healthLevel" placeholder="请选择健康等级" clearable>
          <ElOption label="自理" value="自理" />
          <ElOption label="半护" value="半护" />
          <ElOption label="全护" value="全护" />
          <ElOption label="失智" value="失智" />
        </ElSelect>
      </ElFormItem>
      <ElFormItem>
        <ElButton type="primary" :icon="Search" @click="$emit('search')">搜索</ElButton>
        <ElButton :icon="Refresh" @click="$emit('reset')">重置</ElButton>
      </ElFormItem>
    </ElForm>
  </div>
</template>

<script setup lang="ts">
  import { Refresh, Search } from '@element-plus/icons-vue'

  const model = defineModel<any>({
    required: true,
    default: () => ({
      name: '',
      roomBedNo: '',
      careZone: '',
      healthLevel: ''
    })
  })

  defineEmits<{ search: []; reset: [] }>()
</script>

<style scoped>
  .resident-search {
    margin-bottom: 16px;
  }
</style>
