import http from 'k6/http';
import { check, sleep } from 'k6';

const baseUrl = (__ENV.BASE_URL || '').replace(/\/$/, '');
const endpoint = __ENV.ENDPOINT || 'product-list';
const merchantId = __ENV.MERCHANT_ID || '1';
const token = __ENV.AUTH_TOKEN || '';
const addressId = __ENV.ADDRESS_ID || '';
const productId = __ENV.PRODUCT_ID || '';
const summaryPath = __ENV.SUMMARY_PATH || 'docs/version_314/experiments/perf/raw/k6-summary.json';
const vus = Number(__ENV.VUS || '10');
const warmup = __ENV.WARMUP_DURATION || '30s';
const measure = __ENV.MEASURE_DURATION || '30s';

if (!baseUrl) {
  throw new Error('BASE_URL is required, for example http://127.0.0.1:8080');
}

const headers = {
  Accept: 'application/json',
};
if (token) {
  headers.Authorization = `Bearer ${token}`;
}

export const options = {
  stages: [
    { duration: warmup, target: vus },
    { duration: measure, target: vus },
    { duration: '5s', target: 0 },
  ],
  thresholds: {
    http_req_failed: ['rate<0.05'],
  },
  tags: {
    experiment: 'clas-perf-compare',
    endpoint,
  },
};

function requestForEndpoint() {
  if (endpoint === 'merchant-list') {
    return http.get(`${baseUrl}/api/merchant/list`, {
      headers,
      tags: { name: 'GET /api/merchant/list' },
      timeout: '15s',
    });
  }
  if (endpoint === 'product-list') {
    return http.get(`${baseUrl}/api/product/list/${merchantId}`, {
      headers,
      tags: { name: 'GET /api/product/list/{merchantId}' },
      timeout: '15s',
    });
  }
  if (endpoint === 'order-create') {
    if (!addressId || !productId) {
      throw new Error('ADDRESS_ID and PRODUCT_ID are required for order-create');
    }
    http.post(`${baseUrl}/api/cart/add`, JSON.stringify({
      productId: Number(productId),
      quantity: 1,
    }), {
      headers: Object.assign({}, headers, { 'Content-Type': 'application/json' }),
      tags: { name: 'POST /api/cart/add' },
      timeout: '15s',
    });
    const body = JSON.stringify({
      merchantId: Number(merchantId),
      addressId: Number(addressId),
      productIds: [Number(productId)],
      remark: `PERF_${__VU}_${__ITER}_${Date.now()}`,
    });
    const writeHeaders = Object.assign({}, headers, {
      'Content-Type': 'application/json',
      'Idempotency-Key': `perf-${__VU}-${__ITER}-${Date.now()}`,
      'X-Request-Id': `perf-${__VU}-${__ITER}-${Date.now()}`,
    });
    return http.post(`${baseUrl}/api/order/create`, body, {
      headers: writeHeaders,
      tags: { name: 'POST /api/order/create' },
      timeout: '15s',
    });
  }
  throw new Error(`Unknown ENDPOINT=${endpoint}`);
}

export default function () {
  const response = requestForEndpoint();
  check(response, {
    'http 200': (result) => result.status === 200,
    'business 200': (result) => {
      try {
        return result.json('code') === 200;
      } catch (_) {
        return false;
      }
    },
  });
  sleep(Number(__ENV.THINK_SECONDS || '1'));
}

export function handleSummary(data) {
  return {
    [summaryPath]: JSON.stringify(data, null, 2),
  };
}
