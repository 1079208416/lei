export const money = (cents: number | null | undefined) =>
  `¥${((cents ?? 0) / 100).toFixed(2)}`

export const shortTime = (value: string | null | undefined) => {
  if (!value) return '--'
  return new Intl.DateTimeFormat('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  }).format(new Date(value))
}

export const fullTime = (value: string | null | undefined) => {
  if (!value) return '--'
  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  }).format(new Date(value))
}

export const orderStatusLabel: Record<string, string> = {
  WAITING_PAYMENT: '待支付',
  WAITING_ACCEPT: '待接单',
  PREPARING: '制作中',
  WAITING_PICKUP: '待取餐',
  DELIVERING: '配送中',
  COMPLETED: '已完成',
  CANCELLED: '已取消',
  REJECTED: '商家拒单',
}

export const deliveryStatusLabel: Record<string, string> = {
  ASSIGNED: '待取餐',
  PICKED_UP: '配送中',
  DELIVERED: '已送达',
  CANCELLED: '已取消',
}

export const refundStatusLabel: Record<string, string> = {
  NONE: '无退款',
  REQUESTED: '退款处理中',
  APPROVED: '退款已通过',
  REJECTED: '退款未通过',
  COMPLETED: '已退款',
}
