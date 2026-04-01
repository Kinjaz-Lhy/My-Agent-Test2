<template>
  <div class="session-list">
    <div class="session-header">
      <span>会话历史</span>
      <el-button type="primary" text size="small" @click="$emit('newSession')">+ 新对话</el-button>
    </div>
    <el-scrollbar height="calc(100% - 48px)">
      <div
        v-for="s in sessions"
        :key="s.sessionId"
        class="session-item"
        :class="{ active: s.sessionId === currentId, pinned: s.pinned }"
        @click="$emit('select', s.sessionId)"
        @contextmenu.prevent="openMenu($event, s)"
      >
        <div class="session-title-row">
          <el-icon v-if="s.pinned" class="pin-icon"><Top /></el-icon>
          <span class="session-title">{{ s.title || s.lastMessage || '新对话' }}</span>
          <el-dropdown trigger="click" @command="cmd => handleCommand(cmd, s)" @click.stop>
            <el-icon class="more-btn"><MoreFilled /></el-icon>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item :command="s.pinned ? 'unpin' : 'pin'">
                  <el-icon><Top /></el-icon>{{ s.pinned ? '取消置顶' : '置顶' }}
                </el-dropdown-item>
                <el-dropdown-item command="rename">
                  <el-icon><Edit /></el-icon>重命名
                </el-dropdown-item>
                <el-dropdown-item command="delete" divided>
                  <span style="color: #f56c6c"><el-icon><Delete /></el-icon>删除</span>
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
        <div class="session-meta">
          <span>{{ formatTime(s.createdAt) }}</span>
          <el-tag v-if="s.status === 'CLOSED'" size="small" type="info">已结束</el-tag>
        </div>
      </div>
      <el-empty v-if="!sessions.length" description="暂无会话" :image-size="60" />
    </el-scrollbar>

    <!-- 重命名对话框 -->
    <el-dialog v-model="renameVisible" title="重命名会话" width="400px" @close="renameTitle = ''">
      <el-input v-model="renameTitle" placeholder="请输入新标题" maxlength="200" show-word-limit
                @keyup.enter="confirmRename" />
      <template #footer>
        <el-button @click="renameVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmRename" :disabled="!renameTitle.trim()">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { Top, Edit, Delete, MoreFilled } from '@element-plus/icons-vue'
import { ElMessageBox, ElMessage } from 'element-plus'
import { renameSession, pinSession, deleteSession } from '@/api/chat'

const props = defineProps({
  sessions: { type: Array, default: () => [] },
  currentId: { type: String, default: null }
})

const emit = defineEmits(['select', 'newSession', 'refresh', 'deleted'])

const renameVisible = ref(false)
const renameTitle = ref('')
const renameTarget = ref(null)

function handleCommand(cmd, session) {
  if (cmd === 'pin' || cmd === 'unpin') handlePin(session)
  else if (cmd === 'rename') openRename(session)
  else if (cmd === 'delete') handleDelete(session)
}

async function handlePin(session) {
  try {
    const newPinned = !session.pinned
    await pinSession(session.sessionId, newPinned)
    ElMessage.success(newPinned ? '已置顶' : '已取消置顶')
    emit('refresh')
  } catch { ElMessage.error('操作失败') }
}

function openRename(session) {
  renameTarget.value = session
  renameTitle.value = session.title || session.lastMessage || ''
  renameVisible.value = true
}

async function confirmRename() {
  if (!renameTitle.value.trim() || !renameTarget.value) return
  try {
    await renameSession(renameTarget.value.sessionId, renameTitle.value.trim())
    ElMessage.success('重命名成功')
    renameVisible.value = false
    emit('refresh')
  } catch { ElMessage.error('重命名失败') }
}

async function handleDelete(session) {
  try {
    await ElMessageBox.confirm('删除后无法恢复，确定删除该会话？', '删除会话', {
      confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning'
    })
    await deleteSession(session.sessionId)
    ElMessage.success('已删除')
    emit('deleted', session.sessionId)
    emit('refresh')
  } catch { /* cancelled */ }
}

function openMenu(e, session) {
  // 右键菜单由 el-dropdown 处理，此处预留
}

function formatTime(ts) {
  if (!ts) return ''
  const d = new Date(ts)
  return `${(d.getMonth() + 1).toString().padStart(2, '0')}-${d.getDate().toString().padStart(2, '0')} ${d.getHours().toString().padStart(2, '0')}:${d.getMinutes().toString().padStart(2, '0')}`
}
</script>

<style scoped>
.session-list { height: 100%; display: flex; flex-direction: column; }
.session-header { display: flex; justify-content: space-between; align-items: center; padding: 12px; font-weight: 600; border-bottom: 1px solid #ebeef5; }
.session-item { padding: 10px 12px; cursor: pointer; border-bottom: 1px solid #f5f7fa; transition: background 0.2s; }
.session-item:hover, .session-item.active { background: #ecf5ff; }
.session-item.pinned { background: #fdf6ec; }
.session-item.pinned:hover, .session-item.pinned.active { background: #ecf5ff; }
.session-title-row { display: flex; align-items: center; gap: 4px; }
.pin-icon { color: #e6a23c; font-size: 14px; flex-shrink: 0; }
.session-title { font-size: 14px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; flex: 1; }
.more-btn { color: #909399; cursor: pointer; flex-shrink: 0; opacity: 0; transition: opacity 0.2s; }
.session-item:hover .more-btn { opacity: 1; }
.session-meta { display: flex; justify-content: space-between; align-items: center; margin-top: 4px; font-size: 12px; color: #909399; }
</style>
