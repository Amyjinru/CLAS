<script setup>
import { ref, reactive, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { createProduct, updateProduct, uploadProductImage } from '../../api/clas'

const props = defineProps({
  visible: Boolean,
  isEdit: Boolean,
  product: { type: Object, default: null },
  categories: { type: Array, default: () => [] }
})

const emit = defineEmits(['update:visible', 'saved'])

const formRef = ref(null)
const uploadLoading = ref(false)

const form = reactive({
  id: null,
  name: '',
  description: '',
  price: 0,
  stock: 0,
  categoryId: null,
  imageUrl: ''
})

const rules = {
  name: [
    { required: true, message: '商品名称不能为空', trigger: 'blur' },
    { max: 50, message: '商品名称不能超过50个字符', trigger: 'blur' }
  ],
  price: [
    { required: true, message: '商品价格不能为空', trigger: 'blur' },
    { type: 'number', min: 0.01, message: '价格必须大于 0', trigger: 'blur' }
  ],
  stock: [
    { required: true, message: '库存不能为空', trigger: 'blur' },
    { type: 'integer', min: 0, message: '库存不能小于 0', trigger: 'blur' }
  ]
}

function beforeImageUpload(file) {
  const allowed = ['image/jpeg', 'image/png']
  if (!allowed.includes(file.type)) { ElMessage.warning('仅支持 jpg/png 图片'); return false }
  if (file.size > 5 * 1024 * 1024) { ElMessage.warning('图片不能超过 5MB'); return false }
  return true
}

async function handleImageUpload({ file }) {
  uploadLoading.value = true
  try {
    const data = await uploadProductImage(file)
    form.imageUrl = data.url
    ElMessage.success('图片上传成功')
  } finally { uploadLoading.value = false }
}

function submit() {
  if (!formRef.value) return
  formRef.value.validate(async (valid) => {
    if (!valid) return
    const payload = {
      ...form,
      categoryId: form.categoryId || null,
      price: Math.round(form.price * 100)
    }
    try {
      if (props.isEdit) {
        await updateProduct(payload)
        ElMessage.success('商品修改成功')
      } else {
        await createProduct(payload)
        ElMessage.success('商品添加成功，默认为下架状态')
      }
      emit('update:visible', false)
      emit('saved')
    } catch (error) {
      // handled by API interceptor
    }
  })
}

watch(() => props.visible, (v) => {
  if (!v) return
  if (props.product) {
    Object.assign(form, {
      id: props.product.id,
      name: props.product.name,
      description: props.product.description || '',
      price: props.product.price / 100,
      stock: props.product.stock,
      categoryId: props.product.categoryId || null,
      imageUrl: props.product.imageUrl || ''
    })
  } else {
    Object.assign(form, { id: null, name: '', description: '', price: 0, stock: 0, categoryId: null, imageUrl: '' })
  }
  if (formRef.value) formRef.value.resetFields()
})
</script>

<template>
  <el-dialog
    :model-value="visible"
    :title="isEdit ? '编辑商品' : '新增商品'"
    width="550px"
    destroy-on-close
    @update:model-value="emit('update:visible', $event)"
  >
    <el-form ref="formRef" :model="form" :rules="rules" label-width="90px" style="padding: 10px 20px 0 0;">
      <el-form-item label="商品名称" prop="name">
        <el-input v-model="form.name" placeholder="请输入商品名称" maxLength="50" />
      </el-form-item>

      <el-form-item label="商品价格" prop="price">
        <el-input-number v-model="form.price" :precision="2" :step="0.5" :min="0" placeholder="单位：元" style="width: 180px;" />
        <span style="margin-left: 10px; color: var(--text-muted); font-size: 13px;">单位：元</span>
      </el-form-item>

      <el-form-item label="商品库存" prop="stock">
        <el-input-number v-model="form.stock" :min="0" :step="1" placeholder="库存量" style="width: 180px;" />
      </el-form-item>

      <el-form-item label="所属分类" prop="categoryId">
        <el-select v-model="form.categoryId" placeholder="请选择分类" clearable style="width: 100%">
          <el-option label="未分类" :value="null" />
          <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
        </el-select>
      </el-form-item>

      <el-form-item label="商品图片" prop="imageUrl">
        <div class="image-upload-box">
          <el-image v-if="form.imageUrl" :src="form.imageUrl" fit="cover" class="product-preview-img" />
          <el-upload :show-file-list="false" :before-upload="beforeImageUpload" :http-request="handleImageUpload">
            <el-button :loading="uploadLoading" type="primary">上传图片</el-button>
          </el-upload>
          <el-input v-model="form.imageUrl" placeholder="图片 URL 会在上传后自动填入，也可手动输入" />
        </div>
      </el-form-item>

      <el-form-item label="商品描述" prop="description">
        <el-input v-model="form.description" type="textarea" :rows="3" placeholder="请输入商品详细描述" maxLength="200" show-word-limit />
      </el-form-item>
    </el-form>
    <template #footer>
      <div class="dialog-footer">
        <el-button @click="emit('update:visible', false)">取消</el-button>
        <el-button type="primary" @click="submit">确定</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<style scoped>
.image-upload-box { display: grid; gap: 10px; width: 100%; }
.product-preview-img { border: 1px solid var(--border-color); border-radius: var(--radius-sm); height: 120px; width: 120px; }
</style>
