<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getUserList, getUserDetail, updateUserStatus } from '../../../api/admin/users'

const loading = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const detailDialogVisible = ref(false)
const detailData = ref<any>(null)

const roleMap: Record<number, string> = { 10: '普通用户', 20: '商家', 30: '配送员', 40: '管理员' }
const statusMap: Record<number, string> = { 10: '正常', 20: '冻结', 30: '注销' }
const orderStatusMap: Record<number, string> = {
  10: '待支付', 20: '待接单', 30: '备餐中', 40: '配送中', 50: '已完成',
  60: '已取消', 70: '退款中', 80: '已退款'
}
const complaintStatusMap: Record<number, string> = { 10: '待处理', 20: '处理中', 30: '已处理', 40: '已驳回' }

const formatValue = (value: any) => value === null || value === undefined || value === '' ? '-' : value
const parseList = (value: any) => {
  if (!value) return '-'
  try {
    const parsed = typeof value === 'string' ? JSON.parse(value) : value
    return Array.isArray(parsed) ? parsed.join('、') : String(parsed)
  } catch {
    return value
  }
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await getUserList({ pageNum: pageNum.value, pageSize: pageSize.value })
    if (res.code === 200) {
      tableData.value = res.data.list || []
      total.value = res.data.total || 0
    }
  } catch (err) {
    console.error('获取用户列表失败', err)
  } finally {
    loading.value = false
  }
}

const handleDetail = async (row: any) => {
  try {
    const res = await getUserDetail(row.id)
    if (res.code === 200) {
      detailData.value = res.data
      detailDialogVisible.value = true
    }
  } catch (err) {
    console.error('获取用户详情失败', err)
  }
}

const handleStatusChange = async (row: any) => {
  const newStatus = row.status === 10 ? 20 : 10
  try {
    const res = await updateUserStatus(row.id, newStatus)
    if (res.code === 200) {
      row.status = newStatus
      ElMessage.success(newStatus === 10 ? '用户已启用' : '用户已冻结')
    }
  } catch (err) {
    console.error('更新用户状态失败', err)
  }
}

onMounted(loadData)
</script>

<template>
  <div class="page-container">
    <div class="card-panel">
      <div class="card-header">
        <h3>用户管理</h3>
        <button class="btn btn-primary">新增用户</button>
      </div>
      <div style="padding-top: 20px;">
        <el-table :data="tableData" v-loading="loading" border stripe style="width: 100%">
          <el-table-column label="ID" width="190" class-name="user-id-column">
            <template #default="{ row }"><span class="user-id-cell">{{ row.id }}</span></template>
          </el-table-column>
          <el-table-column prop="username" label="账号" min-width="140" />
          <el-table-column prop="nickname" label="昵称" min-width="120" />
          <el-table-column prop="phone" label="手机号" width="140" />
          <el-table-column label="角色" width="100">
            <template #default="{ row }">
              {{ roleMap[row.roleType] || row.roleType }}
            </template>
          </el-table-column>
          <el-table-column label="状态" width="80">
            <template #default="{ row }">
              <el-tag :type="row.status === 10 ? 'success' : 'danger'" size="small">
                {{ statusMap[row.status] || '未知' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="创建时间" min-width="170">
            <template #default="{ row }">
              {{ row.createTime?.slice(0, 16) }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="180" fixed="right">
            <template #default="{ row }">
              <el-button size="small" type="primary" link @click="handleDetail(row)">详情</el-button>
              <el-button
                size="small"
                :type="row.status === 10 ? 'warning' : 'success'"
                link
                @click="handleStatusChange(row)"
              >
                {{ row.status === 10 ? '冻结' : '启用' }}
              </el-button>
            </template>
          </el-table-column>
        </el-table>
        <div style="display:flex;justify-content:flex-end;padding-top:16px;">
          <el-pagination
            v-model:current-page="pageNum"
            v-model:page-size="pageSize"
            :total="total"
            :page-sizes="[10, 20, 50]"
            layout="total, sizes, prev, pager, next"
            @current-change="loadData"
            @size-change="loadData"
          />
        </div>
      </div>
    </div>
  </div>
  <el-dialog v-model="detailDialogVisible" title="用户详情" width="900px" top="6vh">
    <div v-if="detailData" class="user-detail">
      <section class="detail-section">
        <div class="detail-section-title">账户信息</div>
        <el-descriptions :column="3" border>
          <el-descriptions-item label="账号">{{ formatValue(detailData.user?.username) }}</el-descriptions-item>
          <el-descriptions-item label="昵称">{{ formatValue(detailData.user?.nickname) }}</el-descriptions-item>
          <el-descriptions-item label="角色">{{ roleMap[detailData.user?.roleType] || formatValue(detailData.user?.roleType) }}</el-descriptions-item>
          <el-descriptions-item label="手机号">{{ formatValue(detailData.user?.phone) }}</el-descriptions-item>
          <el-descriptions-item label="邮箱">{{ formatValue(detailData.user?.email) }}</el-descriptions-item>
          <el-descriptions-item label="注册时间">{{ formatValue(detailData.user?.createTime) }}</el-descriptions-item>
        </el-descriptions>
      </section>

      <section class="detail-section">
        <div class="detail-section-title">健康档案</div>
        <el-descriptions v-if="detailData.profile" :column="3" border>
          <el-descriptions-item label="年龄">{{ formatValue(detailData.profile.age) }}</el-descriptions-item>
          <el-descriptions-item label="性别">{{ detailData.profile.gender === 10 ? '男' : detailData.profile.gender === 20 ? '女' : '-' }}</el-descriptions-item>
          <el-descriptions-item label="身高">{{ detailData.profile.height ? `${detailData.profile.height} cm` : '-' }}</el-descriptions-item>
          <el-descriptions-item label="体重">{{ detailData.profile.weight ? `${detailData.profile.weight} kg` : '-' }}</el-descriptions-item>
          <el-descriptions-item label="运动等级">{{ formatValue(detailData.profile.activityLevel) }}</el-descriptions-item>
          <el-descriptions-item label="健康目标">{{ formatValue(detailData.profile.healthGoal) }}</el-descriptions-item>
          <el-descriptions-item label="饮食偏好" :span="3">{{ parseList(detailData.profile.dietPreference) }}</el-descriptions-item>
          <el-descriptions-item label="过敏信息" :span="3">{{ parseList(detailData.profile.allergyInfo) }}</el-descriptions-item>
        </el-descriptions>
        <el-empty v-else description="暂无健康档案" :image-size="60" />
      </section>

      <section class="detail-section">
        <div class="detail-section-title">会员状态</div>
        <el-descriptions v-if="detailData.membership" :column="3" border>
          <el-descriptions-item label="会员类型">{{ formatValue(detailData.membership.membershipType) }}</el-descriptions-item>
          <el-descriptions-item label="状态"><el-tag type="success" size="small">生效中</el-tag></el-descriptions-item>
          <el-descriptions-item label="支付金额">￥{{ formatValue(detailData.membership.payAmount) }}</el-descriptions-item>
          <el-descriptions-item label="开始时间">{{ formatValue(detailData.membership.startTime) }}</el-descriptions-item>
          <el-descriptions-item label="到期时间">{{ formatValue(detailData.membership.endTime) }}</el-descriptions-item>
          <el-descriptions-item label="关联订单">{{ formatValue(detailData.membership.orderId) }}</el-descriptions-item>
        </el-descriptions>
        <el-empty v-else description="当前不是会员" :image-size="60" />
      </section>

      <section class="detail-section">
        <div class="detail-section-title">近期订单</div>
        <el-table :data="detailData.orders || []" size="small" border>
          <el-table-column prop="orderNo" label="订单号" min-width="190" />
          <el-table-column label="金额" width="110"><template #default="{ row }">￥{{ formatValue(row.payAmount) }}</template></el-table-column>
          <el-table-column label="状态" width="100"><template #default="{ row }"><el-tag size="small">{{ orderStatusMap[row.orderStatus] || row.orderStatus }}</el-tag></template></el-table-column>
          <el-table-column prop="createTime" label="创建时间" min-width="160" />
          <template #empty><el-empty description="暂无订单" :image-size="48" /></template>
        </el-table>
      </section>

      <section class="detail-section">
        <div class="detail-section-title">投诉记录</div>
        <el-table :data="detailData.complaints || []" size="small" border>
          <el-table-column prop="id" label="投诉ID" min-width="150" />
          <el-table-column prop="orderId" label="关联订单" min-width="150" />
          <el-table-column prop="complaintReason" label="投诉原因" min-width="160" show-overflow-tooltip />
          <el-table-column label="状态" width="100"><template #default="{ row }"><el-tag size="small">{{ complaintStatusMap[row.status] || row.status }}</el-tag></template></el-table-column>
          <el-table-column prop="createTime" label="提交时间" min-width="160" />
          <template #empty><el-empty description="暂无投诉记录" :image-size="48" /></template>
        </el-table>
      </section>
    </div>
  </el-dialog>
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

.btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: var(--bs-spacing-sm) 20px;
  border-radius: var(--bs-radius-md);
  font-size: var(--bs-font-size-base);
  font-weight: 500;
  border: 1px solid transparent;
  cursor: pointer;
  transition: 0.15s;
}

.btn-primary {
  background: var(--bs-primary);
  color: #FFFFFF;
}

.btn-primary:hover {
  background: var(--bs-primary-hover);
}

.user-id-cell {
  display: block;
  white-space: nowrap;
  color: var(--bs-text-title);
  font-variant-numeric: tabular-nums;
}

.user-detail {
  max-height: 72vh;
  overflow-y: auto;
  padding-right: 4px;
}

.detail-section {
  margin-bottom: 20px;
}

.detail-section:last-child {
  margin-bottom: 0;
}

.detail-section-title {
  margin-bottom: 10px;
  color: var(--bs-text-title);
  font-size: 14px;
  font-weight: 600;
}
</style>
