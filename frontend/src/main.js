import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import './styles/theme.css'
import './styles/app.css'
import { initializePreferences } from './utils/preferences'
/* main.css 已移除 — 遗留样式已合并到 theme.css + app.css */

const app = createApp(App)

initializePreferences()

for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

app.use(router)
app.use(ElementPlus)
app.mount('#app')
