<script setup lang="ts">
import { CheckCircle2, CircleAlert, Info } from 'lucide-vue-next'
import { toastState } from '../lib/toast'
</script>

<template>
  <div class="toast-host">
    <TransitionGroup name="toast">
      <div
        v-for="item in toastState.items"
        :key="item.id"
        class="toast"
        :class="`toast-${item.type}`"
      >
        <CheckCircle2 v-if="item.type === 'success'" :size="18" />
        <CircleAlert v-else-if="item.type === 'error'" :size="18" />
        <Info v-else :size="18" />
        <span>{{ item.message }}</span>
      </div>
    </TransitionGroup>
  </div>
</template>
