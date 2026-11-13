export type Role = 'USER' | 'MERCHANT' | 'RIDER' | 'ADMIN'

export interface Account {
  id: number
  phone: string
  name: string
  role: Role
  merchantId: number | null
}

export interface MerchantSummary {
  id: number
  name: string
  description: string
  category: string
  coverUrl: string
  logoUrl: string
  address: string
  rating: number
  monthlySales: number
  minOrderAmount: number
  deliveryFee: number
  packingFee: number
  etaMinutes: number
  notice: string
  open: boolean
}

export interface ProductOption {
  id: number
  name: string
  priceDelta: number
}

export interface ProductOptionGroup {
  id: number
  name: string
  required: boolean
  options: ProductOption[]
}

export interface Product {
  id: number
  merchantId: number
  categoryId: number
  name: string
  description: string
  imageUrl: string
  price: number
  originalPrice: number | null
  soldOut: boolean
  monthlySales: number
  optionGroups: ProductOptionGroup[]
}

export interface ProductCategory {
  id: number
  name: string
  products: Product[]
}

export interface MerchantDetail extends MerchantSummary {
  categories: ProductCategory[]
}

export interface Address {
  id: number
  accountId: number
  label: string
  contactName: string
  phone: string
  address: string
  detail: string
  longitude: number
  latitude: number
  defaultAddress: boolean
}

export interface CartItem {
  id: number
  productId: number
  name: string
  imageUrl: string
  optionsSnapshot: string
  unitPrice: number
  quantity: number
  subtotal: number
}

export interface Cart {
  merchant: MerchantSummary
  items: CartItem[]
  itemAmount: number
  packingFee: number
  deliveryFee: number
  totalAmount: number
  minOrderAmount: number
  canCheckout: boolean
}

export interface OrderItem {
  id: number
  productId: number
  productName: string
  productImage: string
  optionsSnapshot: string
  unitPrice: number
  quantity: number
  totalPrice: number
}

export interface OrderTimeline {
  status: string
  paymentStatus: string
  note: string
  createdAt: string
}

export interface Order {
  id: number
  orderNo: string
  merchantId: number
  merchantName: string
  status: string
  paymentStatus: string
  refundStatus: string
  addressSnapshot: string
  contactName: string
  contactPhone: string
  itemAmount: number
  packingFee: number
  deliveryFee: number
  totalAmount: number
  remark: string
  riderId: number | null
  riderName: string | null
  riderPhone: string | null
  deliveryStatus: string | null
  cancelReason: string | null
  estimatedDeliveryAt: string
  createdAt: string
  paidAt: string | null
  completedAt: string | null
  items: OrderItem[]
  timeline: OrderTimeline[]
  taskId?: number
  taskStatus?: string
  assignedAt?: string
}

export interface RiderSummary {
  id: number
  name: string
  phone: string
  todayTasks: number
  activeTasks: number
}

export interface Refund {
  id: number
  orderId: number
  orderNo: string
  merchantName: string
  amount: number
  reason: string
  status: string
  createdAt: string
}
