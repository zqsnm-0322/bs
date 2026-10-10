<template>
  <div class="family-tab">
    <div class="header">
      <span>家属信息</span>
      <ElButton type="primary" size="small" icon="Plus" @click="showAddDialog">添加家属</ElButton>
    </div>
    <ElTable :data="familyList" border stripe>
      <ElTableColumn prop="name" label="姓名" width="100" />
      <ElTableColumn prop="relation" label="关系" width="100" />
      <ElTableColumn prop="phone" label="电话" width="120" />
      <ElTableColumn prop="email" label="邮箱" width="150" />
      <ElTableColumn prop="address" label="住址" />
      <ElTableColumn prop="visitFrequency" label="探访频率" width="100" />
      <ElTableColumn label="操作" width="140">
        <template #default="{ row }">
          <ElButton type="primary" link size="small" @click="showEditDialog(row)">编辑</ElButton>
          <ElButton type="danger" link size="small" @click="handleDelete(row.id)">删除</ElButton>
        </template>
      </ElTableColumn>
    </ElTable>

    <ElDialog v-model="dialogVisible" :title="editingId ? '编辑家属' : '添加家属'" width="560px">
      <ElForm :model="form" label-width="100px">
        <ElFormItem label="姓名" required>
          <ElInput v-model="form.name" placeholder="请输入姓名" />
        </ElFormItem>
        <ElFormItem label="关系">
          <ElSelect v-model="form.relation" placeholder="请选择关系">
            <ElOption label="子女" value="子女" />
            <ElOption label="配偶" value="配偶" />
            <ElOption label="其他" value="其他" />
          </ElSelect>
        </ElFormItem>
        <ElFormItem label="电话">
          <ElInput v-model="form.phone" placeholder="请输入电话" />
        </ElFormItem>
        <ElFormItem label="邮箱">
          <ElInput v-model="form.email" placeholder="请输入邮箱" />
        </ElFormItem>
        <ElFormItem label="住址">
          <ElInput v-model="form.address" placeholder="请输入常住地址" />
        </ElFormItem>
        <ElFormItem label="探访频率">
          <ElInput v-model="form.visitFrequency" placeholder="如：每周一次" />
        </ElFormItem>
        <ElFormItem label="是否主要联系人">
          <ElSwitch v-model="form.isPrimary" />
        </ElFormItem>
      </ElForm>
      <template #footer>
        <ElButton @click="dialogVisible = false">取消</ElButton>
        <ElButton type="primary" @click="handleSubmit" :loading="loading">保存</ElButton>
      </template>
    </ElDialog>
  </div>
</template>

<script setup lang="ts">
  import { ref, watch } from 'vue'
  import { ElMessage, ElMessageBox } from 'element-plus'
  import { fetchFamilyList, addFamily, updateFamily, deleteFamily } from '@/api/family'

  const props = defineProps<{ residentId: number }>()
  const familyList = ref<any[]>([])
  const dialogVisible = ref(false)
  const editingId = ref<number | null>(null)
  const loading = ref(false)
  const form = ref({
    name: '',
    relation: '',
    phone: '',
    email: '',
    address: '',
    visitFrequency: '',
    isPrimary: false
  })

  function resetForm() {
    form.value = {
      name: '', relation: '', phone: '', email: '', address: '', visitFrequency: '', isPrimary: false
    }
  }

  watch(
    () => props.residentId,
    async (id) => {
      if (id) {
        await loadFamily()
      }
    },
    { immediate: true }
  )

  async function loadFamily() {
    try {
      const res: any = await fetchFamilyList(props.residentId)
      familyList.value = res || []
    } catch {
      ElMessage.error('获取家属列表失败')
    }
  }

  function showAddDialog() {
    editingId.value = null
    resetForm()
    dialogVisible.value = true
  }

  function showEditDialog(row: any) {
    editingId.value = row.id
    form.value = {
      name: row.name || '',
      relation: row.relation || '',
      phone: row.phone || '',
      email: row.email || '',
      address: row.address || '',
      visitFrequency: row.visitFrequency || '',
      isPrimary: row.isPrimary === 1
    }
    dialogVisible.value = true
  }

  async function handleSubmit() {
    if (!form.value.name) {
      ElMessage.warning('请输入姓名')
      return
    }
    loading.value = true
    try {
      const payload = {
        ...form.value,
        isPrimary: form.value.isPrimary ? 1 : 0,
        residentId: props.residentId
      }
      if (editingId.value) {
        await updateFamily({ ...payload, id: editingId.value })
        ElMessage.success('家属信息已更新')
      } else {
        await addFamily(payload)
        ElMessage.success('家属信息已添加')
      }
      dialogVisible.value = false
      await loadFamily()
    } catch {
      ElMessage.error('添加失败')
    } finally {
      loading.value = false
    }
  }

  async function handleDelete(id: number) {
    try {
      await ElMessageBox.confirm('确定删除这条家属信息吗？', '删除确认', {
        type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消'
      })
      await deleteFamily(id)
      ElMessage.success('删除成功')
      await loadFamily()
    } catch (error) {
      if (error !== 'cancel' && error !== 'close') ElMessage.error('删除失败')
    }
  }
</script>

<style scoped>
  .family-tab {
    padding: 10px;
  }
  .header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 16px;
  }
</style>
