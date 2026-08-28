import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import 'element-plus/es/components/loading/style/css'
import 'element-plus/es/components/message/style/css'
import 'element-plus/es/components/message-box/style/css'
import './styles/theme.css'
import './styles/app.css'
import { initializePreferences } from './utils/preferences'
/* main.css 已移除 — 遗留样式已合并到 theme.css + app.css */

const app = createApp(App)

initializePreferences()

app.use(router)
app.mount('#app')
