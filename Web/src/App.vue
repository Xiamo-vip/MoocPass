<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import Sidebar from './components/Sidebar.vue'
import Header from './components/Header.vue'

const route = useRoute()
const isAuthPage = computed(() => {
  return route.path === '/login' || route.path === '/register'
})
</script>

<template>
  <div class="app-root">
    <!-- Auth Pages (Full Screen) -->
    <template v-if="isAuthPage">
      <router-view />
    </template>

    <!-- Console Layout -->
    <template v-else>
      <div class="app-layout">
        <Sidebar />
        <div class="main-wrapper">
          <Header />
          <main class="content-container">
            <router-view />
          </main>
        </div>
      </div>
    </template>
  </div>
</template>

<style scoped>
.app-root {
  min-height: 100vh;
}

.app-layout {
  display: flex;
  min-height: 100vh;
}

.main-wrapper {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.content-container {
  padding: 0 16px 32px 292px;
  flex: 1;
}
</style>
