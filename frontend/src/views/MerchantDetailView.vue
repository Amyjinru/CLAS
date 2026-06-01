<script setup>
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { addCart, getMerchant, listProducts } from '../api/clas'

const route = useRoute()
const merchant = ref(null)
const products = ref([])
const message = ref('')

async function load() {
  merchant.value = await getMerchant(route.params.id)
  products.value = await listProducts(route.params.id)
}

async function add(product) {
  try {
    await addCart({ productId: product.id, quantity: 1 })
    message.value = `${product.name} 已加入购物车`
  } catch (error) {
    message.value = error.response?.data?.message || '请先登录'
  }
}

onMounted(load)
</script>

<template>
  <section class="panel" v-if="merchant">
    <h1>{{ merchant.merchantName }}</h1>
    <p>{{ merchant.category }} · {{ merchant.address }} · {{ merchant.score }} 分</p>
  </section>

  <p class="message">{{ message }}</p>

  <section class="list">
    <article class="row" v-for="product in products" :key="product.id">
      <div>
        <h2>{{ product.name }}</h2>
        <p>{{ product.category }} · 库存 {{ product.stock }}</p>
      </div>
      <div class="row-actions">
        <strong>¥{{ (product.price / 100).toFixed(2) }}</strong>
        <button @click="add(product)">加入</button>
      </div>
    </article>
  </section>

  <RouterLink class="button" to="/cart">去购物车</RouterLink>
</template>
