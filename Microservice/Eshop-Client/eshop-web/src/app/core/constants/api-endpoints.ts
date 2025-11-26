export const API = {
  AUTH: {
    LOGIN: '/api/auth/login',
    ME: '/api/auth/me',
    ME_PROFILE: '/api/auth/me2',
    REFRESH: '/api/auth/refresh',
    LOGOUT: '/api/auth/logout',
    FORGOT: '/api/auth/forgot-password',
    RESET: '/api/auth/reset-password',
    REGISTER: '/api/auth/register',
    VERIFY: '/api/auth/verify'
  },
  CATEGORIES: {
    TREE: '/api/categories/tree',
    TOP:  '/api/categories/top-level',
    LEAF: '/api/categories/leaf',
    BY_ALIAS: (alias: string) => `/api/categories/alias/${alias}`,
    PARENTS:  (id: number)    => `/api/categories/${id}/parents`,
  },
  PRODUCTS: {
    BY_ID:     (id: number)        => `/api/products/${id}`,
    BY_ALIAS:  (alias: string)     => `/api/products/alias/${alias}`,
    BY_CAT:    (catId: number, page=1, sort='name', dir: 'asc'|'desc' = 'asc') =>
      `/api/products/by-category/${catId}?page=${page}&sort=${sort}&dir=${dir}`,
    SEARCH:    (keyword: string, page=1) =>
      `/api/products/search?keyword=${encodeURIComponent(keyword)}&page=${page}`,
  },
  CART: {
    ROOT: '/api/cart',
    ITEMS: '/api/cart/items'
  },
  ADDRESSES: {
    ROOT: '/api/addresses',
    DEFAULT: '/api/addresses/default'
  },
  CHECKOUT: {
    SUMMARY: (addressId?: number) =>
      addressId ? `/api/checkout/summary?addressId=${addressId}` : `/api/checkout/summary`,
    PLACE_ORDER: `/api/checkout/place-order`,
    CANCEL_ORDER: (orderNumber: string, reason = 'User requested') =>
      `/api/checkout/cancel-order?orderNumber=${encodeURIComponent(orderNumber)}&reason=${encodeURIComponent(reason)}`
  },
  PAYMENTS: {
    PAYPAL: {
      CREATE: `/api/payments/paypal/create`,    // POST x-www-form-urlencoded: addressId, returnUrl, cancelUrl
      CAPTURE: `/api/payments/paypal/capture`,  // POST: paypalOrderId, orderNumber
      CANCEL: `/api/payments/paypal/cancel`     // POST: orderNumber, (optional) paypalOrderId, reason
    }
  }
}
