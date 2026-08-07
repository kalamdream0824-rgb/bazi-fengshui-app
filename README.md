# 八字命理 · 排盘 App

一个基于 Web 的八字（四柱）排盘应用，采用 `lunar-javascript` 计算农历、干支、节气等数据。

## 技术栈

- Vite + React 19 + TypeScript
- [lunar-javascript](https://www.npmjs.com/package/lunar-javascript)（6tail 农历/八字算法库）

## 本地开发

```bash
npm install
npm run dev
```

打开 http://localhost:5173 即可使用。

## 构建

```bash
npm run build
npm run preview
```

## 规划功能

- [x] 四柱八字排盘（年、月、日、时柱 + 十神 + 纳音）
- [ ] 大运与流年推算
- [ ] 五行力量分析
- [ ] 合婚（八字配对）
- [ ] 风水罗盘 / 命理查询（可扩展）
- [ ] 微信小程序 / 线上部署

## 网络说明

如 npm 安装缓慢，可临时使用本地代理：

```bash
npm config set proxy http://127.0.0.1:7890
npm config set https-proxy http://127.0.0.1:7890
```
