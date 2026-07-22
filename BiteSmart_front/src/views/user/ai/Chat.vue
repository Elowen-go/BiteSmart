<script setup lang="ts">
import { ref, onMounted, nextTick } from 'vue'
import { aiChat } from '../../../api/user/ai'
import { ElMessage } from 'element-plus'
import { ChatDotRound } from '@element-plus/icons-vue'

const loading = ref(false)
const messages = ref<{ role: string; content: string }[]>([])
const inputText = ref('')
const chatContainer = ref<HTMLElement | null>(null)
const sessionId = ref('')

const scrollToBottom = async () => {
  await nextTick()
  if (chatContainer.value) {
    chatContainer.value.scrollTop = chatContainer.value.scrollHeight
  }
}

const sendMessage = async () => {
  const text = inputText.value.trim()
  if (!text || loading.value) return

  messages.value.push({ role: 'user', content: text })
  inputText.value = ''
  loading.value = true
  scrollToBottom()

  try {
    const res = await aiChat(text, sessionId.value || undefined)
    const answer = res.data?.answer || res.data || '抱歉，我暂时无法回答这个问题'
    sessionId.value = res.data?.sessionId || sessionId.value
    messages.value.push({ role: 'assistant', content: answer })
  } catch (e) {
    messages.value.push({ role: 'assistant', content: '连接失败，请稍后再试' })
    ElMessage.error('对话请求失败')
  } finally {
    loading.value = false
    scrollToBottom()
  }
}

const handleKeydown = (e: Event) => {
  const keyboardEvent = e as KeyboardEvent
  if (keyboardEvent.key === 'Enter' && !keyboardEvent.shiftKey) {
    keyboardEvent.preventDefault()
    sendMessage()
  }
}

const startNewChat = () => {
  messages.value = []
  sessionId.value = crypto.randomUUID()
}

onMounted(() => {
  sessionId.value = crypto.randomUUID()
})
</script>

<template>
  <div class="page-container">
    <div class="card-panel" style="display: flex; flex-direction: column; height: calc(100vh - 140px);">
      <div class="card-header">
        <h3>AI对话</h3>
        <el-button size="small" @click="startNewChat">新对话</el-button>
      </div>
      <div ref="chatContainer" style="flex: 1; overflow-y: auto; padding: 16px 0; display: flex; flex-direction: column; gap: 12px;">
        <div v-if="messages.length === 0" style="text-align: center; padding: 60px 20px;">
          <div class="ai-avatar"><el-icon :size="30"><ChatDotRound /></el-icon></div>
          <div style="font-size: 18px; font-weight: 600; color: var(--bs-text-title);">智能助手</div>
          <div style="font-size: 14px; color: var(--bs-text-muted); margin-top: 8px;">有什么饮食问题可以问我</div>
        </div>
        <div v-for="(msg, index) in messages" :key="index" :style="{ display: 'flex', justifyContent: msg.role === 'user' ? 'flex-end' : 'flex-start' }">
          <div
            :style="{
              maxWidth: '75%',
              padding: '10px 16px',
              borderRadius: msg.role === 'user' ? '18px 18px 4px 18px' : '18px 18px 18px 4px',
              background: msg.role === 'user' ? 'var(--green)' : 'var(--bs-card-bg)',
              color: msg.role === 'user' ? '#fff' : 'var(--bs-text-title)',
              boxShadow: 'var(--bs-card-shadow)',
              lineHeight: '1.6',
              whiteSpace: 'pre-wrap'
            }"
          >
            {{ msg.content }}
          </div>
        </div>
        <div v-if="loading" style="display: flex; justify-content: flex-start;">
          <div style="padding: 10px 16px; border-radius: 18px 18px 18px 4px; background: var(--bs-card-bg); color: var(--bs-text-muted);">
            正在输入...
          </div>
        </div>
      </div>
      <div style="display: flex; gap: 8px; padding-top: 12px; border-top: 1px solid var(--bs-border-color);">
        <el-input
          v-model="inputText"
          placeholder="输入问题..."
          :disabled="loading"
          @keydown="handleKeydown"
        />
        <el-button type="primary" :loading="loading" @click="sendMessage">发送</el-button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.page-container {
  animation: fadeIn 0.3s ease;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

.card-panel {
  background: var(--bs-card-bg);
  border-radius: var(--bs-radius-md);
  box-shadow: var(--bs-card-shadow);
  padding: var(--bs-spacing-lg);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--bs-spacing-lg);
}

.card-header h3 {
  font-size: var(--bs-font-size-lg);
  font-weight: 600;
  color: var(--bs-text-title);
}

.ai-avatar {
  display: inline-grid;
  place-items: center;
  width: 64px;
  height: 64px;
  margin-bottom: 16px;
  border-radius: 50%;
  background: var(--green-soft);
  color: var(--green);
}
</style>
