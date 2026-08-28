# Selenium → Playwright 迁移

本目录原有 Selenium WebDriver 基础示例已升级为 Playwright 测试。

## 迁移原因
- Playwright 自带浏览器管理（无需 WebDriver）
- 更好的选择器 API、自动等待、trace 调试
- 同一框架支持 E2E + API 测试
- 更好的 CI/CD 集成

## 新测试位置
- E2E 测试: `../tests/e2e/` — Playwright
- API 测试: `../tests/api/` — Vitest + fetch
- 测试用例文档: `../tests/clas-test-cases.md`

## 运行
```bash
# API 集成测试
cd .. && npx vitest run --config tests/vitest.config.js

# E2E 测试
cd .. && npx playwright test --config tests/playwright.config.js
```

## 保留文件
- `test.js` — 原始 Selenium 官方示例 (参考)
- `screenshot.js` — 原始截图示例 (参考)
