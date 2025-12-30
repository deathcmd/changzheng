<template>
  <div class="error-page">
    <h1>404</h1>
    <p>页面不存在</p>
    <el-button type="primary" @click="$router.push('/')">返回首页</el-button>
  </div>
</template>

<style scoped>
.error-page {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100vh;
}
.error-page h1 {
  font-size: 120px;
  color: #409EFF;
  margin: 0;
}
.error-page p {
  font-size: 24px;
  color: #666;
  margin: 20px 0;
}
</style>
