<script setup lang="ts">
import { RouterView } from 'vue-router'
import ToastHost from './components/ToastHost.vue'
import { useRealtime } from './composables/useRealtime'

useRealtime()
</script>

<template>
  <RouterView />
  <ToastHost />
</template>
