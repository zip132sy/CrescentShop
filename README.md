# CrescentShop

一个自带经济系统的 Bukkit 玩家商店插件。支持商店合集面板、搜索、自定义界面大小、上架商品、市场价自动定价，以及每日随机刷新的系统回收/出售商店。

## 兼容版本

- Minecraft 1.12.x（Spigot / Paper 服务端）

## 功能概览

- **商店合集面板**：一条命令打开，列出服务器中所有玩家商店
- **直接定位商店**：`/shop <商店名>` 直接进入对应商店
- **彩色商店名**：商店名支持颜色代码与格式代码（如 `&a`、`&l`）
- **搜索功能**：按商店名或物品名搜索，结果以面板形式展示
- **自定义界面大小**：每个商店可单独设置界面行数（1-6 行）
- **上架商品**：手持物品输入价格即可上架，自动扣除上架费用
- **市场价系统**：玩家上架价 + 系统基准价 + 默认价格表三方平均，自动定价
- **默认价格表**：按物品类别规则自动估算全物品价格，OP 可手动覆盖
- **自带经济系统**：优先接入 Vault，未安装时自动回退到内置经济
- **系统商店**：每日随机挑选 10 个物品，价格随市场浮动，既回收也出售
- **回收上限**：支持固定上限 / 每日随机上限，可开关，每个玩家独立计算
- **全 GUI 操作**：创建、上架、编辑、搜索、购买均可在面板中完成

## 命令

| 命令 | 简写 | 说明 | 权限 |
| --- | --- | --- | --- |
| `/shop` | - | 打开商店合集面板 | `crescentshop.use` |
| `/shop <商店名>` | - | 直接进入指定商店 | `crescentshop.use` |
| `/shop create <名称>` | `c` / `new` | 创建商店 | `crescentshop.create` |
| `/shop sell <价格> <数量>` | `s` / `add` | 上架手持物品 | `crescentshop.sell` |
| `/shop edit` | `e` / `mod` | 编辑自己的商店 | `crescentshop.sell` |
| `/shop seticon` | `si` | 设置商店图标（手持物品） | `crescentshop.sell` |
| `/shop setsize <行数>` | `sz` / `size` | 设置自己商店的界面大小 | `crescentshop.setsize` |
| `/shop search <关键词>` | `se` / `find` | 搜索商店或物品 | `crescentshop.use` |
| `/shop sys` | - | 打开系统商店 | `crescentshop.use` |
| `/shop balance` | `b` / `bal` | 查询余额 | `crescentshop.use` |
| `/shop eco` | - | 打开经济面板 | `crescentshop.use` |
| `/shop income` | - | 查看收款记录 | `crescentshop.use` |
| `/shop pay <玩家> <金额> [备注]` | `transfer` | 转账给玩家 | `crescentshop.use` |
| `/shop claim` | - | 领取待收款 | `crescentshop.use` |
| `/shop loan <玩家> <金额> [备注]` | - | 发起借款申请 | `crescentshop.use` |
| `/shop loanaccept <玩家> <天数> <利率>` | `la` | 同意借款 | `crescentshop.use` |
| `/shop loanreject <玩家>` | `lr` | 拒绝借款 | `crescentshop.use` |
| `/shop loanrepay <玩家> [金额]` | - | 提前还款 | `crescentshop.use` |
| `/shop setprice <价格>` | `sp` | 设置手持物品默认价 | `crescentshop.admin` |
| `/shop reload` | `r` | 重载配置与数据 | `crescentshop.admin` |
| `/shop help` | `h` | 查看帮助 | - |

命令别名：`/sp`、`/商店`

## 权限

| 权限节点 | 说明 | 默认 |
| --- | --- | --- |
| `crescentshop.use` | 使用商店基础功能 | 所有人 |
| `crescentshop.create` | 创建商店 | 所有人 |
| `crescentshop.sell` | 上架商品 / 编辑商店 | 所有人 |
| `crescentshop.setsize` | 设置商店界面大小 | 所有人 |
| `crescentshop.admin` | 管理员权限（重载、设置默认价） | OP |

## 使用说明

### 创建商店

- 命令：`/shop create <商店名>`（商店名支持颜色代码，如 `/shop create &a我的店`）
- GUI：打开商店合集面板，点击底部「创建商店」按钮，在聊天栏输入名称
- 创建费用 = 行数 × 9 × 每格费用（默认 4 行 × 9 × 2.5 = 90）
- 每人只能创建一个商店，名称不可重复（忽略颜色代码）

### 上架商品

- 命令：手持物品，输入 `/shop sell <价格> <数量>`
- GUI：进入自己的商店，点击「编辑商店」→「上架手持物品」，在聊天栏输入「价格 数量」
- 数量从手持物品中扣除（手持 64 个，上架 2 个，手上剩 62 个）
- 数量不能超过手持数量，且不能超过 64
- 上架费用 = 格子费（2.5）+ 手续费（单价 × 上架数量 × 5%）
- 商品在商店中显示「剩余数量」，购买后自动减少

### 编辑商店

- 命令：`/shop edit`
- GUI：进入自己的商店，点击底部「编辑商店」按钮
- 可操作：修改商店名、修改界面大小、上架、下架、修改商品价格、设置图标
- 每次操作完成后会自动返回编辑界面，无需重新打开

### 自定义商店图标

- 命令：手持物品，输入 `/shop seticon`
- GUI：编辑界面点击「设置商店图标」，弹出两个选项：
  1. **使用手持物品**：把当前手持的物品设为图标
  2. **从列表选择**：打开物品选择界面，分类浏览或搜索
- **物品选择界面**：按分类（矿物材料、食物、工具装备、建筑方块、红石杂项、其他）浏览，
  支持搜索与翻页，每页 45 个物品
- 默认图标为箱子，可设置为任意物品
- **地图图标**：手持已绘制的地图设置图标时，插件会复制地图画面到一张新地图，
  在合集面板中显示地图画面
- **大型地图画检测**：若地图边缘一圈大部分有内容（可能是大型地图画的一部分），
  会提示「占用服务器资源较大」，需在聊天栏输入 `confirm` 确认上传
- 配置 `shop.allow-large-map-icon: false` 可直接禁止大型地图画
- 配置 `shop.filter-technical-items: false` 可显示技术性方块（活塞头、命令方块等）

### 帮助

- 输入 `/shop help` 查看所有命令
- 帮助中的命令**可直接点击**，点击后自动填充到聊天栏

### 转账

1. 输入 `/shop pay <玩家> <金额> [备注]`
2. 在聊天栏输入 `confirm` 确认
3. 钱进入「待领取」状态，收款方使用 `/shop claim` 领取
4. 若过期（默认 7 天）未领取，自动退回给转账方

### 借款

1. 借款方输入 `/shop loan <放款方> <金额> [备注]` 发起申请
2. 放款方收到申请，输入 `/shop loanaccept <借款方> <天数> <利率>` 同意
   （或 `/shop loanreject <借款方>` 拒绝）
3. 同意后**立即**把钱给借款方
4. 到期**强制收款**（余额不够允许变负数）
5. 到期前 1 天与到期当天会提醒
6. 借款方可随时用 `/shop loanrepay <放款方> [金额]` 提前还款

### 经济面板

- 输入 `/shop eco` 打开经济面板
- 显示余额、借款（含借款人，最多 5 人）、欠款、待领取转账
- 点击「收款记录」查看所有进账记录

### 收款记录

- 输入 `/shop income` 或经济面板点击「收款记录」
- 记录所有进账：商店售出、系统回收、转账领取、借款放款、借款还款、欠款追回
- 每条含来源、金额、对方、时间
- 保留条数由 `economy.max-income-records` 配置（默认 50）

### 欠款（Vault 不支持负数时）

- 借款到期时若余额不足，记录为欠款
- 逾期每天额外罚息（`economy.overdue-penalty-per-day`，默认 3 点）
- 借款方**收到任何钱时**会立即检查并自动扣款还债
- 扣款时聊天栏提示借款方

### 购买商品

1. 打开商店面板，点击商品
2. 在聊天栏输入购买数量（输入 `cancel` 取消）
3. 购买完成后自动返回商店界面，可继续购买

### 系统商店

- 左键物品 = 卖给系统（回收）
- 右键物品 = 从系统购买
- 每日 0 点自动刷新物品与价格
- 回收/购买完成后自动返回系统商店界面

## 市场价系统

市场价用于上架时的自动定价，计算规则如下：

1. **基础平均**：玩家平均价、系统基准价、默认表价格三方取平均（存在哪几方就用哪几方）
2. **异常修正**：若玩家价格离散度过大（最高价 / 最低价 > 3），取最高价与最低价的中间值，
   再与默认表价格对比，偏离过大时向默认表靠拢
3. **价格保护**：单条上架记录超过默认价 × 保护倍率（默认 10 倍）时忽略，防止恶意刷价
4. **潜影盒**不参与自动定价，需手动输入价格

### 默认价格表

- 按物品类别与材质名关键词自动估算价格（矿物、食物、工具、方块等）
- 覆盖全部物品，无需手动配置
- OP 可手持物品使用 `/shop setprice <价格>` 覆盖特定物品的价格

## 配置文件

配置文件位于 `plugins/CrescentShop/config.yml`，主要配置项：

- `economy.currency-name`：货币显示名称
- `economy.start-balance`：新玩家初始余额
- `economy.transfer-expire-days`：转账过期天数
- `economy.max-loans-per-player`：每人最多借款数
- `economy.max-income-records`：收款记录保留条数
- `economy.overdue-penalty-per-day`：逾期罚息（每天）
- `shop.slot-cost`：每格费用（创建与上架均使用）
- `shop.sell-tax-rate`：上架手续费率
- `shop.default-rows` / `min-rows` / `max-rows`：界面行数设置
- `shop.max-name-length`：商店名最大长度（不含颜色代码）
- `shop.allow-large-map-icon`：是否允许大型地图画作为图标
- `shop.max-map-icons`：全服地图图标数量上限
- `shop.filter-technical-items`：图标列表是否过滤技术性方块
- `market.sell-ratio`：上架自动定价比率
- `market.max-records`：每种物品保留的价格记录上限
- `market.price-cap-multiplier`：价格保护倍率
- `system-shop.daily-items`：每日随机物品数量
- `system-shop.buy-multiplier`：系统出售价格倍率
- `system-shop.price-fluctuation`：价格浮动幅度
- `system-shop.recycle-limit.*`：回收上限设置
- `system-shop.pool`：系统商店物品池

> 配置结构变更时会更新 `config-version`。若检测到旧版配置，OP 登录时会收到提示。

## 数据存储

- `plugins/CrescentShop/data/players.yml`：玩家余额与每日回收量
- `plugins/CrescentShop/data/shops.yml`：玩家商店数据
- `plugins/CrescentShop/data/system-shop.yml`：系统商店当日状态
- `plugins/CrescentShop/data/market.yml`：玩家上架价格记录
- `plugins/CrescentShop/data/prices.yml`：OP 手动覆盖的默认价格

## 依赖

- **Vault**（可选）：安装后自动接入 Vault 经济系统；未安装则使用内置经济
