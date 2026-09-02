import http from 'k6/http';
import { check, sleep } from 'k6';

const baseUrl = (__ENV.BASE_URL || '').replace(/\/$/, '');
const merchantId = __ENV.MERCHANT_ID || '';
const summaryPath = __ENV.SUMMARY_PATH || 'artifacts/hpa/k6-summary.json';

if (!baseUrl) {
  throw new Error('BASE_URL is required, for example http://8.141.112.182');
}

if (!/^\d+$/.test(merchantId)) {
  throw new Error('MERCHANT_ID must be an existing numeric merchant id');
}

export const options = {
  stages: [
    { duration: __ENV.WARMUP_DURATION || '30s', target: 10 },
    { duration: __ENV.LOW_DURATION || '3m', target: 10 },
    { duration: __ENV.RAMP_DURATION || '30s', target: 50 },
    { duration: __ENV.MEDIUM_DURATION || '3m', target: 50 },
    { duration: __ENV.RAMP_DURATION || '30s', target: 100 },
    { duration: __ENV.HIGH_DURATION || '3m', target: 100 },
    { duration: '30s', target: 0 },
  ],
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<2000'],
    checks: ['rate>0.99'],
  },
  tags: {
    experiment: 'clas-catalog-hpa',
    endpoint: 'catalog-product-list',
  },
};

export default function () {
  const requestId = `catalog-load-${__VU}-${__ITER}-${Date.now()}`;
  const response = http.get(`${baseUrl}/api/product/list/${merchantId}`, {
    headers: {
      'X-Request-Id': requestId,
    },
    tags: {
      name: 'GET /api/product/list/{merchantId}',
    },
    timeout: '10s',
  });

  check(response, {
    'status is 200': (result) => result.status === 200,
    'application code is 200': (result) => {
      try {
        return result.json('code') === 200;
      } catch (_) {
        return false;
      }
    },
  });

  sleep(0.05);
}

export function handleSummary(data) {
  return {
    [summaryPath]: JSON.stringify(data, null, 2),
  };
}
