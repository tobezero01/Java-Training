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
    BY_CAT: (catId: number, page=1, size=10, sort='name', dir='asc') =>
      `/api/products/by-category/${catId}?page=${page}&size=${size}&sort=${sort}&dir=${dir}`,

    SEARCH: (keyword: string, page=1, size=10) =>
      `/api/products/search?keyword=${encodeURIComponent(keyword)}&page=${page}&size=${size}`,

    FEATURED: (type: string, page=1, size=10) =>
      `/api/products/featured?type=${type}&page=${page}&size=${size}`,
  },
    CART: {
    // GET & POST /api/cart/items
    VIEW: '/api/cart/items',
    // PATCH /api/cart/items/{productId}
    UPDATE_QTY: (productId: number) => `/api/cart/items/${productId}`,
    // DELETE /api/cart/items/{productId}
    REMOVE_ITEM: (productId: number) => `/api/cart/items/${productId}`,
    // DELETE /api/cart
    CLEAR: '/api/cart'
  },

  ADDRESS: {
    ROOT: '/api/addresses',
    BY_ID: (id: number) => `/api/addresses/${id}`
  },


  CHECKOUT: {
    SUMMARY: (addressId?: number) =>
      addressId
        ? `/api/checkout/summary?addressId=${addressId}`
        : `/api/checkout/summary`,
    PLACE_ORDER: `/api/checkout/place-order`,
    CANCEL_ORDER: (orderNumber: string, reason = 'User requested') =>
      `/api/checkout/cancel-order?orderNumber=${encodeURIComponent(orderNumber)}&reason=${encodeURIComponent(reason)}`
  },

  PAYMENTS: {
    PAYPAL: {
      CREATE: `/api/payments/paypal/create`,
      CAPTURE: `/api/payments/paypal/capture`,
      CANCEL: `/api/payments/paypal/cancel`
    }
  },
  ORDERS: {
    // GET /api/orders/my?page=&size=
    MY_ORDERS: (page: number) => `/api/orders?page=${page}&size=10`,
    // GET /api/orders/{orderNumber}
    DETAIL: (orderNumber: string) => `/api/orders/${orderNumber}`
  }
}
