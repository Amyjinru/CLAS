## ADDED Requirements

### Requirement: 密码 BCrypt 哈希存储
系统 SHALL 使用 BCrypt 算法对用户密码进行哈希存储。注册和重置密码时，明文密码经 BCrypt 编码后写入数据库。plaintext 密码不得直接持久化。

#### Scenario: 新用户注册时密码被加密
- **WHEN** 用户通过 `POST /api/user/register` 注册新账号
- **THEN** 数据库 `user.password` 字段存储的值为 `$2a$` 开头的 BCrypt 哈希

#### Scenario: 未登录商家入驻时密码被加密
- **WHEN** 游客通过 `POST /api/merchant/register` 入驻并创建用户
- **THEN** 新建用户的 `password` 字段为 `$2a$` 开头的 BCrypt 哈希

#### Scenario: 忘记密码重置后密码被加密
- **WHEN** 用户通过 `POST /api/user/forgot-password/reset` 重置密码
- **THEN** 更新后的 `password` 字段为 `$2a$` 开头的 BCrypt 哈希

### Requirement: 登录兼容明文密码自动升级
系统 SHALL 在登录时兼容旧的明文密码。当数据库中密码不是 BCrypt 格式（不以 `$2a$` 开头）时，系统进行明文比较；验证通过后，自动将密码升级为 BCrypt 哈希。

#### Scenario: 旧明文密码用户登录成功并自动升级
- **WHEN** 已有用户的密码字段为明文 "Abc123!"，用户用正确密码登录
- **THEN** 登录成功，且数据库密码字段被自动更新为 BCrypt 哈希

#### Scenario: 旧明文密码用户输入错误密码
- **WHEN** 已有用户的密码字段为明文 "Abc123!"，用户输入错误密码 "WrongPass1!"
- **THEN** 返回"手机号或密码错误"，密码不被修改

#### Scenario: BCrypt 密码用户正常验证
- **WHEN** 用户的密码字段已是 `$2a$` 开头的 BCrypt 哈希，用户输入正确密码
- **THEN** 登录成功，密码字段保持不变

### Requirement: BCryptPasswordEncoder Bean 可用
系统 SHALL 提供单例 `BCryptPasswordEncoder` Bean（强度 10），供所有 Service 注入使用。

#### Scenario: PasswordEncoder 可被 Spring 注入
- **WHEN** Spring 容器启动
- **THEN** `BCryptPasswordEncoder` 实例被创建且可被 `@Autowired` 注入
