<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { auditRiderApplication, auditRiderWithdrawal, listRiderApplications, listRiderWithdrawals } from '../../api/admin'
const applications = ref([]); const withdrawals = ref([]); const loading = ref(false)
async function load(){loading.value=true;try{[applications.value,withdrawals.value]=await Promise.all([listRiderApplications(),listRiderWithdrawals()])}finally{loading.value=false}}
async function auditApplication(item,decision){await auditRiderApplication(item.id,{decision,reason:decision==='APPROVE'?'审核通过':'资料不符合要求',maxActiveOrders:3});ElMessage.success('已处理');load()}
async function auditWithdrawal(item,approved){await auditRiderWithdrawal(item.id,{approved,reason:approved?'审核通过':'审核驳回'});ElMessage.success('已处理');load()}
onMounted(load)
</script>
<template><section v-loading="loading"><h1>骑手运营</h1><el-card><template #header>骑手申请</template><el-table :data="applications"><el-table-column prop="realName" label="姓名"/><el-table-column prop="vehicleType" label="车辆"/><el-table-column prop="serviceArea" label="服务区域"/><el-table-column label="操作"><template #default="{row}"><el-button size="small" type="success" @click="auditApplication(row,'APPROVE')">通过</el-button><el-button size="small" type="danger" @click="auditApplication(row,'REJECT')">驳回</el-button></template></el-table-column></el-table></el-card><el-card class="section"><template #header>提现审核</template><el-table :data="withdrawals"><el-table-column prop="riderId" label="骑手"/><el-table-column prop="amount" label="金额（分）"/><el-table-column prop="status" label="状态"/><el-table-column label="操作"><template #default="{row}"><el-button size="small" type="success" @click="auditWithdrawal(row,true)">批准</el-button><el-button size="small" type="danger" @click="auditWithdrawal(row,false)">驳回</el-button></template></el-table-column></el-table></el-card></section></template><style scoped>.section{margin-top:20px}</style>
