<template>
  <div class="knowledge">
    <h3>知识库管理</h3>
    <div class="knowledge-layout">
      <!-- Left: category management -->
      <aside class="category-panel">
        <div class="category-header">
          <span>分类</span>
          <el-button text size="small" type="primary" @click="handleAddCategory">+</el-button>
        </div>
        <div class="category-item" :class="{ active: selectedCategory === '' }" @click="selectCategory('')">全部</div>
        <div v-for="cat in categoryList" :key="cat.categoryId"
             class="category-item" :class="{ active: selectedCategory === cat.code }"
             @click="selectCategory(cat.code)">
          <span class="category-name">{{ cat.name }}</span>
          <span class="category-actions" @click.stop>
            <el-button text size="small" @click="handleEditCategory(cat)">✎</el-button>
            <el-popconfirm title="确定删除该分类？" @confirm="handleDeleteCategory(cat)">
              <template #reference>
                <el-button text size="small" type="danger">✕</el-button>
              </template>
            </el-popconfirm>
          </span>
        </div>
      </aside>

      <!-- Right: knowledge list -->
      <main class="knowledge-main">
        <div class="toolbar">
          <el-button type="primary" size="small" @click="handleAdd">新增知识条目</el-button>
        </div>
        <el-table :data="entries" stripe border v-loading="loading">
          <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip />
          <el-table-column prop="category" label="分类" width="120">
            <template #default="{ row }">
              {{ categoryNameMap[row.category] || row.category }}
            </template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="statusType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="updatedAt" label="更新时间" width="180" />
          <el-table-column label="操作" min-width="200" fixed="right">
            <template #default="{ row }">
              <span class="action-buttons">
                <el-button text size="small" @click="handleEdit(row)">编辑</el-button>
                <el-button v-if="row.status === 'PENDING_REVIEW'" text size="small" type="success" @click="handleReview(row, 'APPROVE')">通过</el-button>
                <el-button v-if="row.status === 'PENDING_REVIEW'" text size="small" type="danger" @click="handleReview(row, 'REJECT')">驳回</el-button>
                <el-popconfirm title="确定删除该条目？" @confirm="handleDelete(row)">
                  <template #reference>
                    <el-button text size="small" type="danger">删除</el-button>
                  </template>
                </el-popconfirm>
              </span>
            </template>
          </el-table-column>
        </el-table>
      </main>
    </div>

    <!-- Entry dialog -->
    <el-dialog v-model="entryDialogVisible" :title="editingEntry ? '编辑知识条目' : '新增知识条目'" width="600px">
      <el-form :model="entryForm" label-width="80px">
        <el-form-item label="分类">
          <el-select v-model="entryForm.category" placeholder="选择分类">
            <el-option v-for="cat in categoryList" :key="cat.code" :label="cat.name" :value="cat.code" />
          </el-select>
        </el-form-item>
        <el-form-item label="标题">
          <el-input v-model="entryForm.title" />
        </el-form-item>
        <el-form-item label="内容">
          <el-input v-model="entryForm.content" type="textarea" :rows="6" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="entryDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSaveEntry">保存</el-button>
      </template>
    </el-dialog>

    <!-- Category dialog -->
    <el-dialog v-model="categoryDialogVisible" :title="editingCategory ? '编辑分类' : '新增分类'" width="400px">
      <el-form :model="categoryForm" label-width="80px">
        <el-form-item label="编码">
          <el-input v-model="categoryForm.code" placeholder="如 expense-policy" />
        </el-form-item>
        <el-form-item label="名称">
          <el-input v-model="categoryForm.name" placeholder="如 报销制度" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="categoryForm.sortOrder" :min="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="categoryDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSaveCategory">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import {
  addKnowledge, updateKnowledge, queryKnowledge,
  getKnowledgeCategories, addKnowledgeCategory, updateKnowledgeCategory, deleteKnowledgeCategory,
  reviewKnowledge, deleteKnowledge
} from '@/api/admin'
import { ElMessage } from 'element-plus'

// ==================== State ====================
const categoryList = ref([])
const selectedCategory = ref('')
const entries = ref([])
const loading = ref(false)

// Entry dialog
const entryDialogVisible = ref(false)
const editingEntry = ref(null)
const entryForm = ref({ category: '', title: '', content: '' })

// Category dialog
const categoryDialogVisible = ref(false)
const editingCategory = ref(null)
const categoryForm = ref({ code: '', name: '', sortOrder: 0 })

// code → name mapping for display
const categoryNameMap = computed(() => {
  const map = {}
  categoryList.value.forEach(c => { map[c.code] = c.name })
  return map
})

// ==================== Init ====================
fetchCategories()
fetchEntries()

// ==================== Helpers ====================
function statusType(s) {
  const map = { DRAFT: 'info', PENDING_REVIEW: 'warning', ACTIVE: 'success', ARCHIVED: 'danger' }
  return map[s] || 'info'
}
function statusLabel(s) {
  const map = { DRAFT: '草稿', PENDING_REVIEW: '待审核', ACTIVE: '已生效', ARCHIVED: '已归档' }
  return map[s] || s
}

// ==================== Category ====================
async function fetchCategories() {
  try {
    const data = await getKnowledgeCategories()
    categoryList.value = Array.isArray(data) ? data : []
  } catch { categoryList.value = [] }
}

function selectCategory(code) {
  selectedCategory.value = code
  fetchEntries()
}

function handleAddCategory() {
  editingCategory.value = null
  categoryForm.value = { code: '', name: '', sortOrder: categoryList.value.length + 1 }
  categoryDialogVisible.value = true
}

function handleEditCategory(cat) {
  editingCategory.value = cat
  categoryForm.value = { code: cat.code, name: cat.name, sortOrder: cat.sortOrder || 0 }
  categoryDialogVisible.value = true
}

async function handleSaveCategory() {
  try {
    if (editingCategory.value) {
      await updateKnowledgeCategory(editingCategory.value.categoryId, categoryForm.value)
    } else {
      await addKnowledgeCategory(categoryForm.value)
    }
    ElMessage.success('保存成功')
    categoryDialogVisible.value = false
    fetchCategories()
  } catch { ElMessage.error('保存失败') }
}

async function handleDeleteCategory(cat) {
  try {
    await deleteKnowledgeCategory(cat.categoryId)
    ElMessage.success('删除成功')
    if (selectedCategory.value === cat.code) selectedCategory.value = ''
    fetchCategories()
    fetchEntries()
  } catch { ElMessage.error('删除失败') }
}

// ==================== Entry ====================
async function fetchEntries() {
  loading.value = true
  try {
    const params = selectedCategory.value ? { category: selectedCategory.value } : {}
    const data = await queryKnowledge(params)
    entries.value = Array.isArray(data) ? data : []
  } catch { entries.value = [] } finally { loading.value = false }
}

function handleAdd() {
  editingEntry.value = null
  entryForm.value = { category: selectedCategory.value, title: '', content: '' }
  entryDialogVisible.value = true
}

function handleEdit(row) {
  editingEntry.value = row
  entryForm.value = { category: row.category, title: row.title, content: row.content }
  entryDialogVisible.value = true
}

async function handleSaveEntry() {
  try {
    if (editingEntry.value) {
      await updateKnowledge(editingEntry.value.entryId, entryForm.value)
    } else {
      await addKnowledge(entryForm.value)
    }
    ElMessage.success('保存成功')
    entryDialogVisible.value = false
    fetchEntries()
  } catch { ElMessage.error('保存失败') }
}

async function handleReview(row, action) {
  try {
    await reviewKnowledge(row.entryId, action === 'APPROVE')
    ElMessage.success(action === 'APPROVE' ? '审核通过' : '已驳回')
    fetchEntries()
  } catch { ElMessage.error('操作失败') }
}

async function handleDelete(row) {
  try {
    await deleteKnowledge(row.entryId)
    ElMessage.success('删除成功')
    fetchEntries()
  } catch { ElMessage.error('删除失败') }
}
</script>

<style scoped>
.knowledge { padding: 20px; }
.knowledge-layout { display: flex; gap: 16px; margin-top: 16px; }
.category-panel { width: 200px; flex-shrink: 0; border: 1px solid #ebeef5; border-radius: 4px; padding: 8px; }
.category-header { display: flex; justify-content: space-between; align-items: center; padding: 4px 8px; font-weight: 600; border-bottom: 1px solid #ebeef5; margin-bottom: 4px; }
.category-item { display: flex; justify-content: space-between; align-items: center; padding: 6px 8px; cursor: pointer; border-radius: 4px; }
.category-item:hover { background: #f5f7fa; }
.category-item.active { background: #ecf5ff; color: #409eff; }
.category-name { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.category-actions { display: none; white-space: nowrap; }
.category-item:hover .category-actions { display: inline-flex; }
.knowledge-main { flex: 1; min-width: 0; }
.toolbar { margin-bottom: 12px; }
.action-buttons { display: inline-flex; white-space: nowrap; gap: 2px; }
</style>
