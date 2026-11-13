<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { Check, Minus, Plus, X } from 'lucide-vue-next'
import type { Product } from '../types'
import { money } from '../lib/format'

const props = defineProps<{
  product: Product
}>()

const emit = defineEmits<{
  close: []
  add: [optionIds: number[], quantity: number]
}>()

const quantity = ref(1)
const selected = reactive<Record<number, number>>({})

for (const group of props.product.optionGroups) {
  if (group.required && group.options[0]) {
    selected[group.id] = group.options[0].id
  }
}

const chosenOptions = computed(() =>
  props.product.optionGroups.flatMap((group) =>
    group.options.filter((option) => option.id === selected[group.id]),
  ),
)

const unitPrice = computed(
  () => props.product.price + chosenOptions.value.reduce((sum, option) => sum + option.priceDelta, 0),
)

function selectOption(groupId: number, optionId: number) {
  selected[groupId] = optionId
}

function submit() {
  emit(
    'add',
    chosenOptions.value.map((option) => option.id),
    quantity.value,
  )
}
</script>

<template>
  <div class="modal-backdrop" @click.self="emit('close')">
    <section class="product-modal">
      <button class="modal-close" title="关闭" @click="emit('close')">
        <X :size="20" />
      </button>
      <img class="product-modal-image" :src="product.imageUrl" :alt="product.name" />
      <div class="product-modal-body">
        <div class="product-modal-title">
          <div>
            <h2>{{ product.name }}</h2>
            <p>{{ product.description }}</p>
          </div>
          <strong class="price">{{ money(product.price) }}</strong>
        </div>

        <div v-for="group in product.optionGroups" :key="group.id" class="option-group">
          <div class="option-group-title">
            <strong>{{ group.name }}</strong>
            <span v-if="group.required">必选</span>
          </div>
          <div class="option-list">
            <button
              v-for="option in group.options"
              :key="option.id"
              class="option-chip"
              :class="{ selected: selected[group.id] === option.id }"
              @click="selectOption(group.id, option.id)"
            >
              <Check v-if="selected[group.id] === option.id" :size="14" />
              {{ option.name }}
              <small v-if="option.priceDelta">+{{ money(option.priceDelta) }}</small>
            </button>
          </div>
        </div>

        <div class="product-modal-footer">
          <div class="quantity-stepper">
            <button
              :disabled="quantity <= 1"
              title="减少数量"
              @click="quantity = Math.max(1, quantity - 1)"
            >
              <Minus :size="17" />
            </button>
            <span>{{ quantity }}</span>
            <button title="增加数量" @click="quantity++">
              <Plus :size="17" />
            </button>
          </div>
          <button class="button primary grow" @click="submit">
            加入购物车 · {{ money(unitPrice * quantity) }}
          </button>
        </div>
      </div>
    </section>
  </div>
</template>
