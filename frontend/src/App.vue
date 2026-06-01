<template>
  <div class="shell">
    <header class="topbar">
      <RouterLink class="brand" to="/home">CLAS</RouterLink>
      <nav>
        <RouterLink to="/login">登录</RouterLink>
        <RouterLink to="/home">商家</RouterLink>
        <RouterLink to="/cart">购物车</RouterLink>
        <RouterLink to="/orders">我的订单</RouterLink>
        <RouterLink to="/merchant-console">商家端</RouterLink>
      </nav>
    </header>
    <main>
      <RouterView />
    </main>
  </div>
</template>
